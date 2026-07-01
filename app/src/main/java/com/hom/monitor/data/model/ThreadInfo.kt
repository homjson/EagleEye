package com.hom.monitor.data.model

/**
 * 线程信息数据类
 */
data class ThreadInfo(
    val tid: Int,                    // 线程ID
    val pid: Int,                    // 所属进程ID
    val name: String,                // 线程名
    val state: ThreadState,          // 状态: R/S/D/Z/T
    val cpuUsage: Float,             // CPU占用百分比
    val priority: Int,               // 优先级 (nice值)
    val cpuCore: Int,                // 运行核心编号 (-1 表示未知)
    val cpuTime: Long,               // 累计CPU时间 (jiffies)
)

enum class ThreadState(val label: String, val symbol: Char) {
    RUNNING("运行", 'R'),
    SLEEPING("睡眠", 'S'),
    DISK_SLEEP("磁盘休眠", 'D'),
    ZOMBIE("僵尸", 'Z'),
    STOPPED("停止", 'T'),
    UNKNOWN("未知", '?');

    companion object {
        fun fromChar(c: Char): ThreadState = entries.find { it.symbol == c } ?: UNKNOWN
    }
}
