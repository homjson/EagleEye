package com.hom.monitor.ui.process

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hom.monitor.MonitorService
import com.hom.monitor.data.model.ProcessInfo
import com.hom.monitor.data.model.ThreadInfo
import com.hom.monitor.data.repository.ThreadRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProcessMonitorUiState(
    val processes: List<ProcessInfo> = emptyList(),
    val sortBy: SortBy = SortBy.CPU,
    val isLoading: Boolean = true,
    val errorMsg: String? = null,

    // 详情页
    val selectedProcess: ProcessInfo? = null,
    val processThreads: List<ThreadInfo> = emptyList(),
    val isDetailLoading: Boolean = false
)

enum class SortBy { CPU, MEMORY, NAME }

class ProcessMonitorViewModel : ViewModel() {

    private val threadRepository = ThreadRepository()
    private val _uiState = MutableStateFlow(ProcessMonitorUiState())
    val uiState: StateFlow<ProcessMonitorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            MonitorService.processList.collect { processes ->
                val sorted = sortProcesses(processes, _uiState.value.sortBy)
                _uiState.value = _uiState.value.copy(processes = sorted, isLoading = false)
            }
        }
    }

    fun setSortBy(sortBy: SortBy) {
        val sorted = sortProcesses(_uiState.value.processes, sortBy)
        _uiState.value = _uiState.value.copy(sortBy = sortBy, processes = sorted)
    }

    /** 选择进程并加载线程 */
    fun selectProcess(process: ProcessInfo) {
        _uiState.value = _uiState.value.copy(selectedProcess = process)
        loadProcessThreads(process.pid)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedProcess = null, processThreads = emptyList())
    }

    private fun loadProcessThreads(pid: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDetailLoading = true)
            try {
                val threads = threadRepository.getThreadsByPid(pid)
                _uiState.value = _uiState.value.copy(processThreads = threads, isDetailLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isDetailLoading = false, errorMsg = e.message)
            }
        }
    }

    private fun sortProcesses(list: List<ProcessInfo>, sortBy: SortBy): List<ProcessInfo> {
        return when (sortBy) {
            SortBy.CPU -> list.sortedByDescending { it.cpuUsage }
            SortBy.MEMORY -> list.sortedByDescending { it.memUsage }
            SortBy.NAME -> list.sortedBy { it.name.lowercase() }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
