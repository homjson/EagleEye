package com.hom.monitor.data.model

/**
 * 温度信息数据类
 */
data class ThermalInfo(
    val batteryTemp: Float,          // 电池温度 (℃)，-1 表示未知
    val cpuTemp: Float,              // CPU 温度 (℃)，-1 表示未知
)
