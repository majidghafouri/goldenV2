package com.goldenv2.core.domain.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class LogEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    @Contextual val timestamp: Instant = Instant.now(),
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: String? = null
) {
    override fun toString(): String {
        val time = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date(timestamp.toEpochMilli()))
        val throwableStr = throwable?.let { "\n$it" } ?: ""
        return "[$time] [$level] [$tag] $message$throwableStr"
    }
}

@Serializable
data class LogBuffer(
    val entries: List<LogEntry> = emptyList(),
    val maxSize: Int = 1000
) {
    fun add(entry: LogEntry): LogBuffer {
        val newEntries = (entries + entry).takeLast(maxSize)
        return copy(entries = newEntries)
    }

    fun clear(): LogBuffer = copy(entries = emptyList())

    fun filterByLevel(minLevel: LogLevel): List<LogEntry> {
        return entries.filter { it.level.ordinal >= minLevel.ordinal }
    }

    fun filterByTag(tag: String): List<LogEntry> {
        return entries.filter { it.tag.contains(tag, ignoreCase = true) }
    }

    fun search(query: String): List<LogEntry> {
        return entries.filter { it.message.contains(query, ignoreCase = true) || it.tag.contains(query, ignoreCase = true) }
    }
}