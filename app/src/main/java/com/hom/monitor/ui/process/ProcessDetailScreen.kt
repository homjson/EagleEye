package com.hom.monitor.ui.process

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hom.monitor.ui.components.ThreadList

/**
 * 进程详情页 — 展示进程基本信息和线程列表
 */
@Composable
fun ProcessDetailScreen(
    viewModel: ProcessMonitorViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val process = state.selectedProcess ?: return

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部导航
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            TextButton(onClick = {
                viewModel.clearSelection()
                onBack()
            }) {
                Text("< 返回", fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                "${process.name} (PID: ${process.pid})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))
        }

        HorizontalDivider()

        // 进程概要信息
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp, 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    DetailItem("状态", process.state.label)
                    DetailItem("CPU", "%.1f%%".format(process.cpuUsage))
                    DetailItem("内存", formatDetailMem(process.memUsage))
                    DetailItem("线程数", "${process.threadCount}")
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    DetailItem("UID", "${process.uid}")
                    DetailItem("虚拟内存", formatDetailMem(process.vsize))
                }
            }
        }

        // 线程列表标题
        Text(
            "线程列表",
            modifier = Modifier.padding(16.dp, 8.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        // 该进程的线程列表 (不显示 PID)
        ThreadList(
            threads = state.processThreads,
            showPid = false,
            isLoading = state.isDetailLoading
        )
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

private fun formatDetailMem(kb: Long): String {
    val mb = kb / 1024
    return if (mb >= 1024) "%.1f GB".format(mb / 1024f) else "${mb} MB"
}
