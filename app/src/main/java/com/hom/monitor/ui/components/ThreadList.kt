package com.hom.monitor.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hom.monitor.data.model.ThreadInfo

/**
 * 可复用线程列表组件
 * @param threads 线程列表
 * @param showPid 是否显示 PID 列（全局视图显示，进程内视图隐藏）
 * @param isLoading 是否加载中
 */
@Composable
fun ThreadList(
    threads: List<ThreadInfo>,
    showPid: Boolean = true,
    isLoading: Boolean = false
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (threads.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无数据", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    // 表头
    ThreadListHeader(showPid = showPid)
    HorizontalDivider(thickness = 0.5.dp)

    // 列表
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(threads, key = { "${it.pid}:${it.tid}" }) { thread ->
            ThreadRow(thread = thread, showPid = showPid)
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun ThreadListHeader(showPid: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("线程名", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text("CPU%", modifier = Modifier.width(56.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        Text("核心", modifier = Modifier.width(44.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        if (showPid) {
            Text("PID", modifier = Modifier.width(56.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ThreadRow(thread: ThreadInfo, showPid: Boolean) {
    val cpuColor = when {
        thread.cpuUsage > 50f -> MaterialTheme.colorScheme.error
        thread.cpuUsage > 20f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            thread.name,
            modifier = Modifier.weight(1f),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "%.1f".format(thread.cpuUsage),
            modifier = Modifier.width(56.dp),
            color = cpuColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            if (thread.cpuCore >= 0) "${thread.cpuCore}" else "-",
            modifier = Modifier.width(44.dp),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
        if (showPid) {
            Text(
                "${thread.pid}",
                modifier = Modifier.width(56.dp),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
