package com.goldenv2.feature.logs

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goldenv2.core.domain.model.LogEntry
import com.goldenv2.core.domain.model.LogLevel
import com.goldenv2.core.domain.model.SpeedTestResult
import com.goldenv2.core.domain.usecase.LogUseCase
import com.goldenv2.core.domain.usecase.SettingsUseCase
import com.goldenv2.core.vpn.service.VpnController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.URL
import javax.inject.Inject

@HiltViewModel
class LogsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val logUseCase: LogUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val vpnController: VpnController
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogsUiState())
    val uiState: StateFlow<LogsUiState> = _uiState

    private val _events = MutableSharedFlow<LogsEvent>()
    val events: SharedFlow<LogsEvent> = _events

    init {
        observeLogs()
    }

    private fun observeLogs() {
        viewModelScope.launch {
            combine(
                logUseCase.logsFlow,
                vpnController.connectionState
            ) { logs, connectionState ->
                logs to connectionState
            }.collect { (logs, connectionState) ->
                _uiState.update { state ->
                    val filtered = logs
                        .filter { it.level.ordinal >= state.filterLevel.ordinal }
                        .filter { log ->
                            state.searchQuery.isBlank() ||
                                log.message.contains(state.searchQuery, ignoreCase = true) ||
                                log.tag.contains(state.searchQuery, ignoreCase = true)
                        }
                    state.copy(
                        logs = logs,
                        filteredLogs = filtered,
                        connectionState = connectionState
                    )
                }
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
        viewModelScope.launch {
            logUseCase.clearLogs()
        }
    }

    fun onExportLogs() {
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val dir = File(appContext.getExternalFilesDir(null) ?: appContext.filesDir, "logs")
                    if (!dir.exists()) dir.mkdirs()
                    val file = File(dir, "goldenv2_logs_${System.currentTimeMillis()}.txt")
                    file.writeText(_uiState.value.logs.joinToString("\n") { it.toString() })
                    file.absolutePath
                }
            }
            result.fold(
                onSuccess = { path ->
                    logUseCase.i("LogsViewModel", "Logs exported to $path")
                    _events.emit(LogsEvent.LogsExported(path))
                },
                onFailure = { e ->
                    logUseCase.e("LogsViewModel", "Failed to export logs", e)
                    _events.emit(LogsEvent.Error("Failed to export logs: ${e.message}"))
                }
            )
        }
    }

    fun onSpeedTest() {
        val server = _uiState.value.connectionState.currentServer ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSpeedTestRunning = true) }
            val result = runSpeedTest(server.address)
            logUseCase.i("LogsViewModel", "Speed test ${if (result.success) "completed" else "failed"}: ${result.downloadSpeedMbps} Mbps down / ${result.uploadSpeedMbps} Mbps up")
            _uiState.update {
                it.copy(
                    isSpeedTestRunning = false,
                    lastSpeedTest = result,
                    speedTests = it.speedTests + result
                )
            }
        }
    }

    fun onPingTest() {
        val server = _uiState.value.connectionState.currentServer ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isPingTestRunning = true) }
            val result = runPingTest(server.address, server.port)
            logUseCase.i("LogsViewModel", "Ping test ${if (result.success) "completed" else "failed"}: ${result.latencyMs}ms")
            _uiState.update {
                it.copy(isPingTestRunning = false, lastPing = result)
            }
        }
    }

    private suspend fun runSpeedTest(address: String): SpeedTestResult = withContext(Dispatchers.IO) {
        val downloadSpeedMbps = measureDownloadMbps()
        SpeedTestResult(
            serverId = "current",
            serverName = address,
            latencyMs = 0,
            downloadSpeedMbps = downloadSpeedMbps,
            uploadSpeedMbps = 0.0,
            success = downloadSpeedMbps > 0,
            errorMessage = if (downloadSpeedMbps > 0) null else "Download test failed"
        )
    }

    /**
     * Measures throughput by downloading a test file through the VPN tunnel.
     */
    private fun measureDownloadMbps(): Double {
        return try {
            val url = URL("https://speed.cloudflare.com/__down?bytes=10000000")
            val start = System.currentTimeMillis()
            var bytes = 0L
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.inputStream.use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    bytes += read
                }
            }
            val elapsedSec = (System.currentTimeMillis() - start) / 1000.0
            if (elapsedSec > 0) (bytes * 8) / elapsedSec / 1_000_000 else 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    private suspend fun runPingTest(address: String, port: Int): com.goldenv2.core.domain.model.PingResult =
        withContext(Dispatchers.IO) {
            // ICMP requires root; approximate latency with a TCP handshake
            // to the server endpoint.
            val latency = try {
                val socket = java.net.Socket()
                val start = System.currentTimeMillis()
                socket.connect(InetSocketAddress(address, port), 3000)
                socket.close()
                System.currentTimeMillis() - start
            } catch (e: Exception) {
                null
            }
            if (latency != null) {
                com.goldenv2.core.domain.model.PingResult(serverId = "current", latencyMs = latency)
            } else {
                com.goldenv2.core.domain.model.PingResult(
                    serverId = "current",
                    latencyMs = -1,
                    success = false,
                    errorMessage = "Ping failed: host unreachable"
                )
            }
        }
}

sealed interface LogsEvent {
    data class LogsExported(val filePath: String) : LogsEvent
    data class Error(val message: String) : LogsEvent
    data class Copied(val text: String) : LogsEvent
}

data class LogsUiState(
    val logs: List<LogEntry> = emptyList(),
    val filteredLogs: List<LogEntry> = emptyList(),
    val filterLevel: LogLevel = LogLevel.Debug,
    val searchQuery: String = "",
    val isAutoScroll: Boolean = true,
    val connectionState: com.goldenv2.core.domain.model.ConnectionState = com.goldenv2.core.domain.model.ConnectionState(),
    val lastSpeedTest: SpeedTestResult? = null,
    val speedTests: List<SpeedTestResult> = emptyList(),
    val lastPing: com.goldenv2.core.domain.model.PingResult? = null,
    val isSpeedTestRunning: Boolean = false,
    val isPingTestRunning: Boolean = false
)
