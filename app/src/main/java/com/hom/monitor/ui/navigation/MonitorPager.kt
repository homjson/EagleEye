package com.hom.monitor.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hom.monitor.MonitorService
import com.hom.monitor.ui.components.SettingsDialog
import com.hom.monitor.ui.load.LoadMonitorScreen
import com.hom.monitor.ui.process.ProcessDetailScreen
import com.hom.monitor.ui.about.AboutScreen
import com.hom.monitor.ui.process.ProcessMonitorScreen
import com.hom.monitor.ui.process.ProcessMonitorViewModel

enum class MonitorTab(val label: String, val index: Int) {
    LOAD("负载", 0),
    PROCESS("进程", 1),
    ABOUT("关于", 2)
}

@Composable
fun MonitorPager(processViewModel: ProcessMonitorViewModel) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(MonitorTab.LOAD) }
    var showDetail by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    // App 启动时自动开始采集数据
    LaunchedEffect(Unit) {
        if (!MonitorService.isRunning()) {
            MonitorService.start(context)
        }
    }

    val floatingState = MonitorService.floatingState.collectAsState().value

    if (showSettings) {
        SettingsDialog(
            floatingState = floatingState,
            onFloatingToggle = { type, enabled ->
                if (enabled) {
                    if (!MonitorService.isRunning()) {
                        MonitorService.start(context)
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            MonitorService.toggleFloating(type, true)
                        }, 500)
                    } else {
                        MonitorService.toggleFloating(type, true)
                    }
                } else {
                    MonitorService.toggleFloating(type, false)
                }
            },
            onDismiss = { showSettings = false }
        )
    }

    Scaffold(
        bottomBar = {
            if (!showDetail) {
                NavigationBar {
                    MonitorTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentTab == tab,
                            onClick = { currentTab = tab },
                            icon = {
                                when (tab) {
                                    MonitorTab.LOAD -> Text("📊", style = MaterialTheme.typography.titleSmall)
                                    MonitorTab.PROCESS -> Text("📋", style = MaterialTheme.typography.titleSmall)
                                    MonitorTab.ABOUT -> Text("ℹ️", style = MaterialTheme.typography.titleSmall)
                                }
                            },
                            label = {
                                Text(
                                    tab.label,
                                    fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (showDetail) {
                ProcessDetailScreen(
                    viewModel = processViewModel,
                    onBack = { showDetail = false }
                )
            } else {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = { showSettings = true }) {
                            Text(
                                "⚙",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    when (currentTab) {
                        MonitorTab.LOAD -> LoadMonitorScreen()
                        MonitorTab.PROCESS -> ProcessMonitorScreen(
                            viewModel = processViewModel,
                            onProcessClick = { process ->
                                processViewModel.selectProcess(process)
                                showDetail = true
                            }
                        )
                        MonitorTab.ABOUT -> AboutScreen()
                    }
                }
            }
        }
    }
}
