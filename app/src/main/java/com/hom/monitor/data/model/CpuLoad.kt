package com.hom.monitor.data.model

/**
 * CPU 负载数据类
 */
data class CpuLoad(
    val totalUsage: Float,           // CPU 总使用率 (%)
    val cores: List<CoreInfo>,       // 各核心信息
    val loadAvg1: Float,             // 1分钟负载均值
    val loadAvg5: Float,             // 5分钟负载均值
    val loadAvg15: Float,            // 15分钟负载均值
)

data class CoreInfo(
    val index: Int,                  // 核心编号
    val usage: Float,                // 使用率 (%)
    val frequency: Long,             // 当前频率 (KHz), -1 表示未知
    val maxFrequency: Long,          // 最大频率 (KHz), -1 表示未知
    val online: Boolean,             // 是否在线
)
