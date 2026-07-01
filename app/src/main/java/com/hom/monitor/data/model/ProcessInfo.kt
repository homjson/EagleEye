package com.hom.monitor.data.model

/**
 * 进程信息数据类
 */
data class ProcessInfo(
    val pid: Int,                    // 进程ID
    val name: String,                // 进程名
    val state: ThreadState,          // 进程状态
    val cpuUsage: Float,             // CPU占用百分比
    val memUsage: Long,              // 内存占用 (KB)
    val memPercentage: Float,        // 内存占用百分比
    val threadCount: Int,            // 线程数
    val uid: Int,                    // 用户ID
    val vsize: Long,                 // 虚拟内存大小
    val cpuTime: Long = 0,           // utime + stime (clock ticks)，用于CPU差值计算
)
