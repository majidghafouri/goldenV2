package com.goldenv2.core.data.repository

import com.goldenv2.core.domain.model.LogEntry
import com.goldenv2.core.domain.model.LogLevel
import com.goldenv2.core.domain.repository.LogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogRepositoryImpl @Inject constructor(
    private val maxSize: Int = 1000
) : LogRepository {

    private val logsRef = AtomicReference<List<LogEntry>>(emptyList())
    private val logChannel = Channel<LogEntry>(maxSize * 2)
    private val _logsFlow = MutableStateFlow<List<LogEntry>>(emptyList())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val logsFlow: StateFlow<List<LogEntry>> = _logsFlow.asStateFlow()

    init {
        scope.launch {
            for (entry in logChannel) {
                val current = logsRef.get()
                val updated = (current + entry).takeLast(maxSize)
                logsRef.set(updated)
                _logsFlow.value = updated
            }
        }
    }

    override suspend fun addLog(entry: LogEntry) {
        logChannel.send(entry)
    }

    override suspend fun clearLogs() {
        logsRef.set(emptyList())
        _logsFlow.value = emptyList()
    }

    override fun getLogs(): StateFlow<List<LogEntry>> = _logsFlow

    suspend fun d(tag: String, message: String) = addLog(LogEntry(level = LogLevel.Debug, tag = tag, message = message))
    suspend fun i(tag: String, message: String) = addLog(LogEntry(level = LogLevel.Info, tag = tag, message = message))
    suspend fun w(tag: String, message: String) = addLog(LogEntry(level = LogLevel.Warning, tag = tag, message = message))
    suspend fun e(tag: String, message: String, throwable: Throwable? = null) =
        addLog(LogEntry(level = LogLevel.Error, tag = tag, message = message, throwable = throwable?.toString()))
}
