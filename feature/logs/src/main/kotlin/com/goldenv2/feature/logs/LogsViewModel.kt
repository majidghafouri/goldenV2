package com.goldenv2.feature.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goldenv2.core.domain.model.LogEntry
import com.goldenv2.core.domain.model.LogLevel
import com.goldenv2.core.domain.usecase.LogUseCase
import com.goldenv2.core.vpn.service.VpnController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val logUseCase: LogUseCase,
    private val vpnController: VpnController
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogsUiState())
    val uiState: StateFlow<LogsUiState> = _uiState

    init {
        observeLogs()
    }

    private fun observeLogs() {
        viewModelScope.launch {
            combine(
                logUseCase.logsFlow,
                vpnController.connectionState
            ) { logs, connectionState ->
                LogsUiState(
                    logs = logs,
                    filteredLogs = logs,
                    filterLevel = LogLevel.Debug,
                    searchQuery = "",
                    isAutoScroll = true,
                    connectionState = connectionState
                )
            }.distinctUntilChanged().collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onFilterLevelChanged(level: LogLevel) {
        _uiState.update { state ->
            val filtered = state.logs.filter { it.level.ordinal >= level.ordinal }
            state.copy(filterLevel = level, filteredLogs = filtered)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            val filtered = state.logs.filter { log ->
                val matchesLevel = log.level.ordinal >= state.filterLevel.ordinal
                val matchesQuery = query.isBlank() ||
                    log.message.contains(query, ignoreCase = true) ||
                    log.tag.contains(query, ignoreCase = true)
                matchesLevel && matchesQuery
            }
            state.copy(searchQuery = query, filteredLogs = filtered)
        }
    }

    fun onAutoScrollChanged(enabled: Boolean) {
        _uiState.update { it.copy(isAutoScroll = enabled) }
    }

    fun onClearLogs() {
        logUseCase.clearLogs()
    }

    fun onExportLogs() {
        // TODO: Export logs to file
    }

    fun onSpeedTest() {
        // TODO: Implement speed test
    }

    fun onPingTest() {
        // TODO: Implement ping test
    }
}

data class LogsUiState(
    val logs: List<LogEntry> = emptyList(),
    val filteredLogs: List<LogEntry> = emptyList(),
    val filterLevel: LogLevel = LogLevel.Debug,
    val searchQuery: String = "",
    val isAutoScroll: Boolean = true,
    val connectionState: com.goldenv2.core.domain.model.ConnectionState = com.goldenv2.core.domain.model.ConnectionState()
)