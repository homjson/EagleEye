package com.hom.monitor.data.repository

import com.hom.monitor.data.model.CpuLoad
import com.hom.monitor.data.model.MemoryInfo
import com.hom.monitor.data.parser.ProcParser

/**
 * 负载数据仓库
 */
class LoadRepository {

    /** 获取 CPU 负载 */
    suspend fun getCpuLoad(): CpuLoad {
        return ProcParser.parseCpuLoad()
    }

    /** 获取内存信息 */
    suspend fun getMemoryInfo(): MemoryInfo {
        return ProcParser.parseMemoryInfo()
    }
}
