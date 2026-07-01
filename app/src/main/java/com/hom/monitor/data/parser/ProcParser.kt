package com.hom.monitor.data.parser

import com.hom.monitor.data.model.*
import com.hom.monitor.util.RootHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File

/**
 * /proc 文件系统解析器
 */
object ProcParser {

    /** 获取 CPU 核心数 */
    suspend fun getCpuCoreCount(): Int = withContext(Dispatchers.IO) {
        try {
            RootHelper.readFileWithFallback("/sys/devices/system/cpu/present")
                .trim()
                .split("-")
                .lastOrNull()
                ?.toIntOrNull()
                ?.plus(1) ?: Runtime.getRuntime().availableProcessors()
        } catch (_: Exception) {
            Runtime.getRuntime().availableProcessors()
        }
    }

    /** 解析 /proc/stat 和 /sys/devices/system/cpu 获取 CPU 负载 */
    suspend fun parseCpuLoad(): CpuLoad = withContext(Dispatchers.IO) {
        val coreCount = getCpuCoreCount()
        val statContent = RootHelper.readFileWithFallback("/proc/stat")

        // 解析各核心使用率
        val cores = (0 until coreCount).map { index ->
            parseCoreInfo(statContent, index)
        }

        // 总使用率
        val totalUsage = parseTotalCpuUsage(statContent)

        // loadavg
        val loadavg = RootHelper.readFileWithFallback("/proc/loadavg")
            .trim()
            .split(" ")
        val loadAvg1 = loadavg.getOrElse(0) { "0" }.toFloatOrNull() ?: 0f
        val loadAvg5 = loadavg.getOrElse(1) { "0" }.toFloatOrNull() ?: 0f
        val loadAvg15 = loadavg.getOrElse(2) { "0" }.toFloatOrNull() ?: 0f

        CpuLoad(totalUsage = totalUsage, cores = cores, loadAvg1 = loadAvg1, loadAvg5 = loadAvg5, loadAvg15 = loadAvg15)
    }

    /** 根据 maxFrequency 自动分类核心类型 */
    /** 解析 /proc/meminfo 获取内存信息 */
    suspend fun parseMemoryInfo(): MemoryInfo = withContext(Dispatchers.IO) {
        val content = RootHelper.readFileWithFallback("/proc/meminfo")
        val map = content.lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.trim().split("\\s+".toRegex(), limit = 3)
                if (parts.size >= 2) {
                    parts[0].removeSuffix(":") to parts[1].toLongOrNull()
                } else null
            }
            .toMap()
            .mapValues { it.value ?: 0L }

        val total = map["MemTotal"] ?: 0L
        val free = map["MemFree"] ?: 0L
        val available = map["MemAvailable"] ?: free
        val cached = (map["Cached"] ?: 0L) + (map["Buffers"] ?: 0L)
        val swapTotal = map["SwapTotal"] ?: 0L
        val swapFree = map["SwapFree"] ?: 0L

        MemoryInfo(
            totalMem = total,
            freeMem = free,
            availableMem = available,
            usedMem = total - available,
            usagePercent = if (total > 0) ((total - available).toFloat() / total * 100f) else 0f,
            cachedMem = cached,
            swapTotal = swapTotal,
            swapFree = swapFree,
            swapUsed = swapTotal - swapFree
        )
    }

    /** 使用 ps 命令获取进程列表（快，单次 Root 调用） */
    suspend fun parseProcessList(): List<ProcessInfo> = withContext(Dispatchers.IO) {
        try {
            val totalMem = try {
                RootHelper.readFileWithFallback("/proc/meminfo")
                    .lines().first { it.startsWith("MemTotal") }
                    .split("\\s+".toRegex())[1].toLongOrNull() ?: 1L
            } catch (_: Exception) { 1L }

            val psOutput = RootHelper.execCmd("ps -A -o pid,args,stat,rss")
            val processes = psOutput.lines().drop(1)
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    val tokens = line.trim().split("\\s+".toRegex())
                    if (tokens.size < 4) return@mapNotNull null
                    val pid = tokens[0].toIntOrNull() ?: return@mapNotNull null
                    val rss = tokens.last().toLongOrNull() ?: 0L
                    val stateChar = tokens[tokens.size - 2].firstOrNull() ?: '?'
                    val rawName = tokens.subList(1, tokens.size - 2).joinToString(" ").take(50)
                    // 过滤内核线程（args 以 [ 开头，如 [kworker/0:0]）
                    if (rawName.startsWith("[")) return@mapNotNull null

                    ProcessInfo(
                        pid = pid,
                        name = rawName,
                        state = ThreadState.fromChar(stateChar),
                        cpuUsage = 0f,
                        memUsage = rss,
                        memPercentage = if (totalMem > 0) rss.toFloat() / totalMem * 100f else 0f,
                        threadCount = 0,
                        uid = 0,
                        vsize = 0L
                    )
                }

            // 用 glob 展开批量读取 stat 文件 — 避免长命令被 shell 截断
            if (processes.isNotEmpty()) {
                val statOutput = RootHelper.execCmd("cat /proc/[0-9]*/stat 2>/dev/null")
                val pidSet = processes.map { it.pid }.toSet()
                val cpuTimeMap = statOutput.lines()
                    .filter { it.isNotBlank() }
                    .mapNotNull { line ->
                        val pidStr = line.substringBefore(' ')
                        val pid = pidStr.toIntOrNull() ?: return@mapNotNull null
                        if (pid !in pidSet) return@mapNotNull null
                        val closeParen = line.lastIndexOf(')')
                        if (closeParen < 0) return@mapNotNull null
                        val afterName = line.substring(closeParen + 2).split(" ")
                        val utime = afterName.getOrElse(11) { "0" }.toLongOrNull() ?: 0L
                        val stime = afterName.getOrElse(12) { "0" }.toLongOrNull() ?: 0L
                        pid to (utime + stime)
                    }
                    .toMap()

                processes.map { p ->
                    p.copy(cpuTime = cpuTimeMap[p.pid] ?: 0L)
                }
            } else {
                processes
            }
        } catch (_: Exception) { emptyList() }
    }

    /** 获取单个进程信息 */
    suspend fun parseProcessInfo(pid: Int): ProcessInfo? = withContext(Dispatchers.IO) {
        try {
            val statContent = RootHelper.readFileWithFallback("/proc/$pid/stat")
            val statusContent = RootHelper.readFileWithFallback("/proc/$pid/status")

            // /proc/pid/stat 格式: pid (name) state ...
            val closeParen = statContent.lastIndexOf(')')
            if (closeParen < 0) return@withContext null

            val afterName = statContent.substring(closeParen + 2).split(" ")
            val stateChar = afterName.getOrElse(0) { "?" }[0]

            // 线程数
            val threadCount = afterName.getOrElse(16) { "0" }.toIntOrNull() ?: 0

            // 解析 /proc/pid/status 获取 Uid, VmSize
            val statusMap = statusContent.lines()
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    val parts = line.trim().split("\\s+".toRegex(), limit = 2)
                    if (parts.size >= 2) parts[0].removeSuffix(":") to parts[1]
                    else null
                }
                .toMap()

            val uid = statusMap["Uid"]?.split("\t")?.firstOrNull()?.toIntOrNull() ?: 0
            val vsize = statusMap["VmSize"]?.replace("kB", "")?.trim()?.toLongOrNull() ?: 0L
            val rss = statusMap["VmRSS"]?.replace("kB", "")?.trim()?.toLongOrNull() ?: 0L

            // 简单估算CPU占用 — 后续通过采样计算
            val utime = afterName.getOrElse(10) { "0" }.toLongOrNull() ?: 0L
            val stime = afterName.getOrElse(11) { "0" }.toLongOrNull() ?: 0L

            val totalMem = try {
                RootHelper.readFileWithFallback("/proc/meminfo")
                    .lines().first { it.startsWith("MemTotal") }
                    .split("\\s+".toRegex())[1].toLongOrNull() ?: 1L
            } catch (_: Exception) { 1L }

            ProcessInfo(
                pid = pid,
                name = statContent.substring(statContent.indexOf('(') + 1, closeParen),
                state = ThreadState.fromChar(stateChar),
                cpuUsage = 0f,  // 需要前后两次采样才能计算，初始为0
                memUsage = rss,
                memPercentage = if (totalMem > 0) rss.toFloat() / totalMem * 100f else 0f,
                threadCount = threadCount,
                uid = uid,
                vsize = vsize
            )
        } catch (_: Exception) {
            null
        }
    }

    /** 获取进程下的所有线程 — 一次性读取所有 stat 文件，快 */
    suspend fun parseProcessThreads(pid: Int): List<ThreadInfo> = withContext(Dispatchers.IO) {
        // cat /proc/pid/task/*/stat 一次性读取所有线程的 stat
        val allStats = RootHelper.execCmd("cat /proc/$pid/task/*/stat 2>/dev/null")
        if (allStats.isBlank()) return@withContext emptyList()

        allStats.lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line -> parseThreadStatLine(line, pid) }
    }

    /** 解析单个 stat 行内容 */
    private fun parseThreadStatLine(line: String, pid: Int): ThreadInfo? {
        return try {
            val tid = line.substringBefore(' ').toIntOrNull() ?: return null
            val closeParen = line.lastIndexOf(')')
            if (closeParen < 0) return null
            val name = line.substring(line.indexOf('(') + 1, closeParen)

            val afterName = line.substring(closeParen + 2).split(" ")
            val stateChar = afterName.getOrElse(0) { "?" }[0]
            val priority = afterName.getOrElse(15) { "0" }.toIntOrNull() ?: 0
            val utime = afterName.getOrElse(11) { "0" }.toLongOrNull() ?: 0L
            val stime = afterName.getOrElse(12) { "0" }.toLongOrNull() ?: 0L
            val cpuCore = afterName.getOrElse(36) { "-1" }.toIntOrNull() ?: -1

            ThreadInfo(
                tid = tid,
                pid = pid,
                name = name,
                state = ThreadState.fromChar(stateChar),
                cpuUsage = 0f,
                priority = priority,
                cpuCore = cpuCore,
                cpuTime = utime + stime
            )
        } catch (_: Exception) { null }
    }

    /** 获取系统所有线程 */
    suspend fun parseAllThreads(): List<ThreadInfo> = coroutineScope {
        val processList = parseProcessList()
        processList.map { proc ->
            async { parseProcessThreads(proc.pid) }
        }.awaitAll().flatten()
    }



    /** 解析总 CPU 使用率 */
    private fun parseTotalCpuUsage(statContent: String): Float {
        val cpuLine = statContent.lines().firstOrNull { it.startsWith("cpu ") } ?: return 0f
        val fields = cpuLine.trim().split("\\s+".toRegex()).drop(1).map { it.toLongOrNull() ?: 0L }
        if (fields.size < 4) return 0f
        val total = fields.sum()
        val idle = fields.getOrElse(3) { 0L } + fields.getOrElse(4) { 0L }
        return if (total > 0) ((total - idle).toFloat() / total * 100f) else 0f
    }

    /** 解析单个核心的频率和使用率 */
    private suspend fun parseCoreInfo(statContent: String, index: Int): CoreInfo {
        val cpuLine = statContent.lines().firstOrNull { it.startsWith("cpu$index ") }
        var usage = 0f
        if (cpuLine != null) {
            val fields = cpuLine.trim().split("\\s+".toRegex()).drop(1).map { it.toLongOrNull() ?: 0L }
            if (fields.size >= 4) {
                val total = fields.sum()
                val idle = fields.getOrElse(3) { 0L } + fields.getOrElse(4) { 0L }
                usage = if (total > 0) ((total - idle).toFloat() / total * 100f) else 0f
            }
        }

        val freqPath = "/sys/devices/system/cpu/cpu$index/cpufreq"
        var frequency = -1L
        var maxFreq = -1L

        // 合并为一个 root 命令读取两个文件
        try {
            val freqOutput = RootHelper.execCmd(
                "cat $freqPath/scaling_cur_freq $freqPath/scaling_max_freq 2>/dev/null"
            ).trim().lines()
            frequency = freqOutput.getOrNull(0)?.trim()?.toLongOrNull() ?: -1L
            maxFreq = freqOutput.getOrNull(1)?.trim()?.toLongOrNull() ?: -1L
        } catch (_: Exception) {}

        val online = try {
            File("/sys/devices/system/cpu/cpu$index/online").readText().trim() == "1"
        } catch (_: Exception) { index == 0 }  // cpu0 默认在线

        return CoreInfo(index = index, usage = usage, frequency = frequency, maxFrequency = maxFreq, online = online)
    }

    /** 获取前台 APP 信息 (PID, 进程名) — 使用 dumpsys */
    suspend fun getForegroundAppInfo(): Pair<Int, String>? = withContext(Dispatchers.IO) {
        try {
            // dumpsys activity 直接给出前台 Activity 的包名
            val dumpsys = RootHelper.execCmd("dumpsys activity activities 2>/dev/null | grep -E 'topResumedActivity|mResumedActivity' | head -1")
            val pkgMatch = Regex("u0\\s+(\\S+)/").find(dumpsys)
            val pkgName = pkgMatch?.groupValues?.get(1) ?: run {
                // 备用: 从 mFocusedApp 获取
                val focused = RootHelper.execCmd("dumpsys window 2>/dev/null | grep mFocusedApp | head -1")
                Regex("u0\\s+(\\S+)/").find(focused)?.groupValues?.get(1) ?: ""
            }
            if (pkgName.isBlank()) return@withContext null

            // 根据包名找 PID
            val psResult = RootHelper.execCmd("ps -A 2>/dev/null | grep $pkgName | head -1")
            val pid = psResult.trim().split("\\s+".toRegex()).getOrNull(1)?.toIntOrNull()
                ?: return@withContext null

            Pair(pid, pkgName)
        } catch (_: Exception) { null }
    }

    /** 计算两次采样之间的 CPU 使用率差值 */
    fun calcCpuUsageDelta(prev: List<ProcessInfo>, curr: List<ProcessInfo>): List<ProcessInfo> {
        val prevMap = prev.associateBy { it.pid }
        return curr.map { proc ->
            val prevProc = prevMap[proc.pid]
            if (prevProc != null) {
                proc.copy(cpuUsage = proc.cpuUsage)  // 由Repository层通过utime差值计算
            } else proc
        }
    }
}
