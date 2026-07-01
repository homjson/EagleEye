package com.hom.monitor.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hom.monitor.data.model.MemoryInfo

/**
 * 内存信息展示组件
 */
@Composable
fun MemoryBar(memory: MemoryInfo) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("内存", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    "%.1f%%".format(memory.usagePercent),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (memory.usagePercent > 80f) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 使用率进度条
            LinearProgressIndicator(
                progress = { memory.usagePercent / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = when {
                    memory.usagePercent > 80f -> MaterialTheme.colorScheme.error
                    memory.usagePercent > 60f -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 详细数据行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MemLabel("总计", formatSize(memory.totalMem))
                MemLabel("已用", formatSize(memory.usedMem))
                MemLabel("可用", formatSize(memory.availableMem))
                MemLabel("缓存", formatSize(memory.cachedMem))
            }

            // Swap 行
            if (memory.swapTotal > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MemLabel("Swap总计", formatSize(memory.swapTotal))
                    MemLabel("Swap已用", formatSize(memory.swapUsed))
                    MemLabel("Swap空闲", formatSize(memory.swapFree))
                    Spacer(modifier = Modifier.width(1.dp))
                }
            }
        }
    }
}

@Composable
private fun MemLabel(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

private fun formatSize(kb: Long): String {
    val mb = kb / 1024
    return if (mb >= 1024) {
        "%.1f GB".format(mb / 1024f)
    } else {
        "${mb} MB"
    }
}
