package com.hom.monitor.ui.process

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hom.monitor.data.model.ProcessInfo
@Composable
fun ProcessMonitorScreen(
    viewModel: ProcessMonitorViewModel = viewModel(),
    onProcessClick: (ProcessInfo) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // 标题
        Text(
            "进程监视器",
            modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "共 ${state.processes.size} 个进程",
            modifier = Modifier.padding(horizontal = 16.dp),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.outline
        )

        // 排序+刷新控制
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 排序按钮组
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("排序:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.width(4.dp))
                SortBy.values().forEach { sort ->
                    TextButton(
                        onClick = { viewModel.setSortBy(sort) },
                        modifier = Modifier.defaultMinSize(minWidth = 40.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Text(
                            when (sort) {
                                SortBy.CPU -> "CPU"
                                SortBy.MEMORY -> "内存"
                                SortBy.NAME -> "名称"
                            },
                            fontSize = 11.sp,
                            fontWeight = if (state.sortBy == sort) FontWeight.Bold else FontWeight.Normal,
                            color = if (state.sortBy == sort) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        // 错误提示
        state.errorMsg?.let { msg ->
            Text(
                "错误: $msg",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp
            )
        }

        // 表头
        ProcessListHeader()
        HorizontalDivider(thickness = 0.5.dp)

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.processes, key = { it.pid }) { proc ->
                    ProcessRow(process = proc) { onProcessClick(proc) }
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun ProcessListHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("PID", modifier = Modifier.width(48.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text("进程名", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text("线程", modifier = Modifier.width(40.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text("CPU%", modifier = Modifier.width(52.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        Text("内存", modifier = Modifier.width(64.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ProcessRow(process: ProcessInfo, onClick: () -> Unit) {
    val cpuColor = when {
        process.cpuUsage > 50f -> MaterialTheme.colorScheme.error
        process.cpuUsage > 20f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val memColor = when {
        process.memPercentage > 20f -> MaterialTheme.colorScheme.error
        process.memPercentage > 10f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${process.pid}",
            modifier = Modifier.width(48.dp),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            process.name,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "${process.threadCount}",
            modifier = Modifier.width(40.dp),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            "%.1f".format(process.cpuUsage),
            modifier = Modifier.width(52.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = cpuColor,
            fontFamily = FontFamily.Monospace
        )
        Text(
            formatMem(process.memUsage),
            modifier = Modifier.width(64.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = memColor,
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun formatMem(kb: Long): String {
    val mb = kb / 1024
    return if (mb >= 1024) "%.1fG".format(mb / 1024f) else "${mb}M"
}
