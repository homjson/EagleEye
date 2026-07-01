package com.hom.monitor.ui.load

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hom.monitor.MonitorService
import com.hom.monitor.data.model.CpuLoad
import com.hom.monitor.data.model.MemoryInfo
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LoadMonitorUiState(
    val cpuLoad: CpuLoad? = null,
    val memoryInfo: MemoryInfo? = null,
    val isLoading: Boolean = true,
    val errorMsg: String? = null
)

class LoadMonitorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LoadMonitorUiState())
    val uiState: StateFlow<LoadMonitorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                MonitorService.cpuLoad,
                MonitorService.memoryInfo
            ) { cpu, mem -> Pair(cpu, mem) }
                .collect { (cpu, mem) ->
                    _uiState.value = _uiState.value.copy(
                        cpuLoad = cpu,
                        memoryInfo = mem,
                        isLoading = false
                    )
                }
        }
    }
}
