package com.hom.monitor.ui.load

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.hom.monitor.ui.components.CpuCoreGrid
import com.hom.monitor.ui.components.MemoryBar
@Composable
fun LoadMonitorScreen(
    viewModel: LoadMonitorViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // 标题
        Text(
            "负载监视器",
            modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
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

        if (state.isLoading && state.cpuLoad == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp)
            ) {
                // CPU 总使用率
                state.cpuLoad?.let { cpu ->
                    Text(
                        "CPU 总使用率: %.1f%%".format(cpu.totalUsage),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(8.dp, 4.dp)
                    )

                    // Load Average
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Load Avg: ", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        Text("1m: %.2f".format(cpu.loadAvg1), fontSize = 12.sp)
                        Text("5m: %.2f".format(cpu.loadAvg5), fontSize = 12.sp)
                        Text("15m: %.2f".format(cpu.loadAvg15), fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // CPU 核心网格
                    Text(
                        "CPU 核心",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp, 4.dp)
                    )
                    CpuCoreGrid(cores = cpu.cores)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 内存
                state.memoryInfo?.let { mem ->
                    MemoryBar(memory = mem)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
