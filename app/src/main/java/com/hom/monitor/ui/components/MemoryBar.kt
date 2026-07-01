package com.hom.monitor.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hom.monitor.data.model.MemoryInfo

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
                    fontSize = 18.sp,
                    color = when {
                        memory.usagePercent > 80f -> MaterialTheme.colorScheme.error
                        memory.usagePercent > 60f -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 进度条
            LinearProgressIndicator(
                progress = { memory.usagePercent / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = when {
                    memory.usagePercent > 80f -> MaterialTheme.colorScheme.error
                    memory.usagePercent > 60f -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                },
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 已用 / 总计
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "已用  ${formatSize(memory.usedMem)}  / 总计  ${formatSize(memory.totalMem)}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 可用 + 缓存
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("可用  ${formatSize(memory.availableMem)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                Text("缓存  ${formatSize(memory.cachedMem)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            }

            // Swap
            if (memory.swapTotal > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Swap  ${formatSize(memory.swapUsed)} / ${formatSize(memory.swapTotal)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

private fun formatSize(kb: Long): String {
    val mb = kb / 1024
    return if (mb >= 1024) "%.1f GB".format(mb / 1024f) else "$mb MB"
}
