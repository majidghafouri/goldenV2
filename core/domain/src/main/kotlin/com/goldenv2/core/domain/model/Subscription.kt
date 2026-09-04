package com.goldenv2.core.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.goldenv2.core.data.db.Converters
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.Instant

@Entity(tableName = "subscriptions")
@Serializable
data class Subscription(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val url: String,
    val name: String,
    val remark: String? = null,
    val isEnabled: Boolean = true,
    val autoRefresh: Boolean = true,
    val refreshIntervalHours: Int = 24,
    @Contextual val lastRefreshAt: Instant? = null,
    val lastRefreshSuccess: Boolean = false,
    val lastRefreshError: String? = null,
    val serverCount: Int = 0,
    val userAgent: String? = null,
    val headers: Map<String, String> = emptyMap(),
    @Contextual val createdAt: Instant = Instant.now(),
    @Contextual val updatedAt: Instant = Instant.now()
) {
    fun copyWithRefreshResult(
        success: Boolean,
        serverCount: Int,
        error: String? = null
    ): Subscription = copy(
        lastRefreshAt = Instant.now(),
        lastRefreshSuccess = success,
        lastRefreshError = error,
        serverCount = serverCount,
        updatedAt = Instant.now()
    )

    fun copyEnabled(enabled: Boolean): Subscription = copy(isEnabled = enabled, updatedAt = Instant.now())
}