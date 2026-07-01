package com.hom.monitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.hom.monitor.ui.navigation.MonitorPager
import com.hom.monitor.ui.process.ProcessMonitorViewModel
import com.hom.monitor.ui.theme.EagleEyeTheme

class MainActivity : ComponentActivity() {

    private val processViewModel: ProcessMonitorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            EagleEyeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MonitorPager(processViewModel = processViewModel)
                }
            }
        }
    }
}
