package com.hom.monitor.data.repository

import com.hom.monitor.data.model.ProcessInfo
import com.hom.monitor.data.parser.ProcParser

/**
 * 进程数据仓库
 * 通过前后两次采样计算进程级 CPU 占用
 */
class ProcessRepository {

    private var prevSample: PrevSample? = null

    private data class PrevSample(
        val cpuTimeByPid: Map<Int, Long>,
        val timestamp: Long
    )

    /** 获取进程列表（含 CPU 占用计算） */
    suspend fun getProcessList(): List<ProcessInfo> {
        val processes = ProcParser.parseProcessList()
        val now = System.currentTimeMillis()
        val prev = prevSample

        val result = if (prev != null) {
            val deltaMs = now - prev.timestamp
            processes.map { proc ->
                val prevCpuTime = prev.cpuTimeByPid[proc.pid] ?: proc.cpuTime
                val deltaTicks = proc.cpuTime - prevCpuTime
                // CPU% = delta_ticks * 1000 / delta_ms (CLK_TCK = 100)
                val cpuUsage = if (deltaMs > 0 && deltaTicks > 0) {
                    (deltaTicks * 1000f / deltaMs).coerceAtMost(100f * Runtime.getRuntime().availableProcessors())
                } else 0f
                proc.copy(cpuUsage = cpuUsage)
            }
        } else {
            processes
        }

        prevSample = PrevSample(
            cpuTimeByPid = result.associate { it.pid to it.cpuTime },
            timestamp = now
        )

        return result
    }

    /** 获取单个进程信息（不含线程） */
    suspend fun getProcess(pid: Int): ProcessInfo? {
        return ProcParser.parseProcessInfo(pid)
    }
}
