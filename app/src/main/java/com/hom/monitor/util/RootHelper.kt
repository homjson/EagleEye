package com.hom.monitor.util

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * libsu Root 执行封装
 * 优先使用原生 File API 读取 /proc，失败时通过 Root 补全
 */
object RootHelper {

    /** 检查 Root 权限是否可用 */
    suspend fun isRootAvailable(): Boolean = withContext(Dispatchers.IO) {
        Shell.getShell().isRoot
    }

    /**
     * 读取文件内容 — 优先 Root（绕过 SELinux），Root 不可用时降级 File API
     */
    suspend fun readFileWithFallback(filePath: String): String = withContext(Dispatchers.IO) {
        // Root 优先 — 因为 SELinux 可能阻止 File API 读取 /proc
        try {
            if (Shell.getShell().isRoot) {
                return@withContext readViaRoot(filePath)
            }
        } catch (_: Exception) {}

        // 降级到 File API
        try {
            File(filePath).readText()
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * 通过 Root 执行命令并返回输出
     */
    suspend fun execCmd(cmd: String): String = withContext(Dispatchers.IO) {
        val result = Shell.cmd(cmd).exec()
        return@withContext if (result.isSuccess) result.out.joinToString("\n") else ""
    }

    private suspend fun readViaRoot(filePath: String): String = withContext(Dispatchers.IO) {
        val result = Shell.cmd("cat $filePath").exec()
        result.out.joinToString("\n")
    }

    /** 检查文件是否可读，包括通过 Root */
    suspend fun isReadable(filePath: String): Boolean = withContext(Dispatchers.IO) {
        File(filePath).canRead() || try {
            val result = Shell.cmd("test -r $filePath && echo ok").exec()
            result.out.any { it.contains("ok") }
        } catch (_: Exception) {
            false
        }
    }
}
