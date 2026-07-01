package com.hom.monitor.ui.thread

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hom.monitor.MonitorService
import com.hom.monitor.data.model.ThreadInfo
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ThreadMonitorUiState(
    val threads: List<ThreadInfo> = emptyList(),
    val isLoading: Boolean = true,
    val errorMsg: String? = null
)

class ThreadMonitorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ThreadMonitorUiState())
    val uiState: StateFlow<ThreadMonitorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            MonitorService.threadList.collect { threads ->
                _uiState.value = _uiState.value.copy(threads = threads, isLoading = false)
            }
        }
    }
}
