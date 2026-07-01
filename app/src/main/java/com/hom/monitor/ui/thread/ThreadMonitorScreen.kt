package com.hom.monitor.ui.thread

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hom.monitor.ui.components.ThreadList

@Composable
fun ThreadMonitorScreen(
    viewModel: ThreadMonitorViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // 标题
        Text(
            "线程监视器",
            modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "共 ${state.threads.size} 个线程",
            modifier = Modifier.padding(horizontal = 16.dp),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.outline
        )

        // 错误提示
        state.errorMsg?.let { msg ->
            Text(
                "错误: $msg",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 线程列表
        ThreadList(
            threads = state.threads,
            showPid = true,
            isLoading = state.isLoading
        )
    }
}
