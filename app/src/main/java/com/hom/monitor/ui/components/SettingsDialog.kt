package com.hom.monitor.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hom.monitor.FloatingState
import com.hom.monitor.FloatingType
import com.hom.monitor.MonitorService
import com.hom.monitor.util.PermissionHelper

@Composable
fun SettingsDialog(
    floatingState: FloatingState,
    onFloatingToggle: (FloatingType, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val hasOverlayPerm = remember { PermissionHelper.canDrawOverlays(context) }

    val checkOverlayPerm: () -> Unit = {
        if (!hasOverlayPerm) {
            Toast.makeText(context, "请先授予悬浮窗权限", Toast.LENGTH_SHORT).show()
            PermissionHelper.openOverlaySettings(context)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("悬浮窗", fontSize = 14.sp, fontWeight = FontWeight.Medium)

                FloatingToggleRow(
                    label = "线程监视悬浮窗",
                    description = "显示 TOP3 CPU 线程",
                    checked = floatingState.threadVisible,
                    onToggle = { enabled ->
                        if (enabled && !hasOverlayPerm) { checkOverlayPerm(); return@FloatingToggleRow }
                        onFloatingToggle(FloatingType.THREAD, enabled)
                    }
                )

                FloatingToggleRow(
                    label = "负载监视悬浮窗",
                    description = "显示 CPU 和内存使用率",
                    checked = floatingState.loadVisible,
                    onToggle = { enabled ->
                        if (enabled && !hasOverlayPerm) { checkOverlayPerm(); return@FloatingToggleRow }
                        onFloatingToggle(FloatingType.LOAD, enabled)
                    }
                )

                FloatingToggleRow(
                    label = "进程监视悬浮窗",
                    description = "显示 TOP3 CPU 进程",
                    checked = floatingState.processVisible,
                    onToggle = { enabled ->
                        if (enabled && !hasOverlayPerm) { checkOverlayPerm(); return@FloatingToggleRow }
                        onFloatingToggle(FloatingType.PROCESS, enabled)
                    }
                )

                HorizontalDivider()

                Text("权限状态", fontSize = 14.sp, fontWeight = FontWeight.Medium)

                PermissionRow(
                    label = "悬浮窗权限",
                    granted = hasOverlayPerm,
                    onRequest = { PermissionHelper.openOverlaySettings(context) }
                )

                PermissionRow(
                    label = "Root 权限",
                    granted = true,
                    onRequest = { /* libsu 自动处理 */ }
                )

                PermissionRow(
                    label = "通知权限",
                    granted = PermissionHelper.hasNotificationPermission(context),
                    onRequest = { /* 系统自动弹出 */ }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun FloatingToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(description, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
        }
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onRequest: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (granted) "✓ " else "✗ ",
                color = if (granted) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error,
                fontSize = 14.sp
            )
            Text(label, fontSize = 13.sp)
        }
        if (!granted) {
            TextButton(onClick = onRequest, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                Text("授权", fontSize = 12.sp)
            }
        }
    }
}
