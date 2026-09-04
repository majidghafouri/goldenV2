package com.goldenv2.core.domain.usecase

import com.goldenv2.core.domain.model.LogEntry
import com.goldenv2.core.domain.model.LogLevel
import com.goldenv2.core.domain.repository.LogRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogUseCase @Inject constructor(
    private val repository: LogRepository
) {
    val logsFlow = repository.logsFlow

    suspend fun addLog(entry: LogEntry) = repository.addLog(entry)

    suspend fun clearLogs() = repository.clearLogs()

    suspend fun d(tag: String, message: String) = addLog(LogEntry(level = LogLevel.Debug, tag = tag, message = message))
    suspend fun i(tag: String, message: String) = addLog(LogEntry(level = LogLevel.Info, tag = tag, message = message))
    suspend fun w(tag: String, message: String) = addLog(LogEntry(level = LogLevel.Warning, tag = tag, message = message))
    suspend fun e(tag: String, message: String, throwable: Throwable? = null) =
        addLog(LogEntry(level = LogLevel.Error, tag = tag, message = message, throwable = throwable?.toString()))
}