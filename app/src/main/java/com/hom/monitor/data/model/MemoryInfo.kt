package com.hom.monitor.data.model

/**
 * 内存信息数据类
 */
data class MemoryInfo(
    val totalMem: Long,              // 总内存 (KB)
    val freeMem: Long,               // 空闲内存 (KB)
    val availableMem: Long,          // 可用内存 (KB)
    val usedMem: Long,               // 已用内存 (KB)
    val usagePercent: Float,         // 使用率 (%)
    val cachedMem: Long,             // 缓存 (KB)
    val swapTotal: Long,             // Swap 总量 (KB)
    val swapFree: Long,              // Swap 空闲 (KB)
    val swapUsed: Long,              // Swap 已用 (KB)
)
