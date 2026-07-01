package com.hom.monitor.data.repository

import com.hom.monitor.data.model.CpuLoad
import com.hom.monitor.data.model.ThreadInfo
import com.hom.monitor.data.model.ThreadState
import com.hom.monitor.data.parser.ProcParser

/**
 * 线程数据仓库
 * 通过前后两次采样计算线程级 CPU 占用
 */
class ThreadRepository {

    private var prevPid: Int = -1
    private var prevThreads: Map<Int, ThreadInfo> = emptyMap()
    private var prevTotalCpuTime: Long = 0L
    var foregroundAppName: String = ""
        private set

    /** 获取前台 APP 的线程列表 */
    suspend fun getForegroundAppThreads(): List<ThreadInfo> {
        val fgInfo = ProcParser.getForegroundAppInfo()
        if (fgInfo == null) return emptyList()
        val (pid, name) = fgInfo
        foregroundAppName = name

        // 前台 APP 切换时重置采样状态，立即响应
        if (pid != prevPid) {
            prevPid = pid
            prevThreads = emptyMap()
            prevTotalCpuTime = 0L
        }

        val threads = ProcParser.parseProcessThreads(pid)
        val currentTotalCpuTime = threads.sumOf { it.cpuTime }

        val result = if (prevTotalCpuTime > 0) {
            val deltaTotal = currentTotalCpuTime - prevTotalCpuTime
            threads.map { thread ->
                val prevThread = prevThreads[thread.tid]
                if (prevThread != null && deltaTotal > 0) {
                    val deltaCpu = thread.cpuTime - prevThread.cpuTime
                    thread.copy(cpuUsage = (deltaCpu.toFloat() / deltaTotal * 100f).coerceIn(0f, 100f))
                } else thread
            }
        } else threads

        prevThreads = result.associateBy { it.tid }
        prevTotalCpuTime = currentTotalCpuTime
        // 前台 APP 线程不过滤睡眠，否则几乎所有线程都被过滤掉
        return result.sortedByDescending { it.cpuUsage }
    }

    /** 获取所有线程（含 CPU 占用计算） */
    private suspend fun getAllThreads(): List<ThreadInfo> {
        val threads = ProcParser.parseAllThreads()

        val currentTotalCpuTime = threads.sumOf { it.cpuTime }

        val result = if (prevTotalCpuTime > 0) {
            val deltaTotal = currentTotalCpuTime - prevTotalCpuTime
            threads.map { thread ->
                val prevThread = prevThreads[thread.tid]
                if (prevThread != null && deltaTotal > 0) {
                    val deltaCpu = thread.cpuTime - prevThread.cpuTime
                    val usage = (deltaCpu.toFloat() / deltaTotal * 100f)
                        .coerceIn(0f, 100f)
                    thread.copy(cpuUsage = usage)
                } else {
                    thread
                }
            }
        } else {
            threads
        }

        android.util.Log.d("ThreadRepo", "getForegroundAppThreads: parsed=${threads.size}, prevTotalCpuTime=$prevTotalCpuTime, deltaTotal=${if (prevTotalCpuTime > 0) currentTotalCpuTime - prevTotalCpuTime else 0L}")
        prevThreads = result.associateBy { it.tid }
        prevTotalCpuTime = currentTotalCpuTime
        val finalResult = result.sortedByDescending { it.cpuUsage }
        android.util.Log.d("ThreadRepo", "getForegroundAppThreads: returning ${finalResult.size} threads")
        return finalResult
    }

    private val pidSamples = mutableMapOf<Int, PidSample>()

    private data class PidSample(
        val threads: Map<Int, ThreadInfo>,
        val totalCpuTime: Long
    )

    /** 获取指定进程的线程列表（含 CPU 差值计算） */
    suspend fun getThreadsByPid(pid: Int): List<ThreadInfo> {
        val threads = ProcParser.parseProcessThreads(pid)
        val currentTotalCpuTime = threads.sumOf { it.cpuTime }
        val prev = pidSamples[pid]

        val result = if (prev != null && prev.totalCpuTime > 0) {
            val deltaTotal = currentTotalCpuTime - prev.totalCpuTime
            threads.map { thread ->
                val prevThread = prev.threads[thread.tid]
                if (prevThread != null && deltaTotal > 0) {
                    val deltaCpu = thread.cpuTime - prevThread.cpuTime
                    thread.copy(cpuUsage = (deltaCpu.toFloat() / deltaTotal * 100f).coerceIn(0f, 100f))
                } else thread
            }
        } else threads

        pidSamples[pid] = PidSample(
            threads = result.associateBy { it.tid },
            totalCpuTime = currentTotalCpuTime
        )

        return result.sortedByDescending { it.cpuUsage }
    }
}
