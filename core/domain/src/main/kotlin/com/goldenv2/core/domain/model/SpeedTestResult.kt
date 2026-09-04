package com.goldenv2.core.domain.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class SpeedTestResult(
    val serverId: String,
    val serverName: String,
    @Contextual val timestamp: Instant = Instant.now(),
    val latencyMs: Long,
    val downloadSpeedMbps: Double,
    val uploadSpeedMbps: Double,
    val jitterMs: Double = 0.0,
    val packetLossPercent: Double = 0.0,
    val success: Boolean = true,
    val errorMessage: String? = null
) {
    val formattedLatency: String
        get() = "${latencyMs}ms"

    val formattedDownload: String
        get() = String.format("%.2f Mbps", downloadSpeedMbps)

    val formattedUpload: String
        get() = String.format("%.2f Mbps", uploadSpeedMbps)

    val qualityRating: QualityRating
        get() {
            return when {
                latencyMs < 50 && packetLossPercent < 1 -> QualityRating.Excellent
                latencyMs < 100 && packetLossPercent < 2 -> QualityRating.Good
                latencyMs < 200 && packetLossPercent < 5 -> QualityRating.Fair
                else -> QualityRating.Poor
            }
        }
}

@Serializable
enum class QualityRating {
    Excellent, Good, Fair, Poor
}

@Serializable
data class PingResult(
    val serverId: String,
    val latencyMs: Long,
    @Contextual val timestamp: Instant = Instant.now(),
    val success: Boolean = true,
    val errorMessage: String? = null
)