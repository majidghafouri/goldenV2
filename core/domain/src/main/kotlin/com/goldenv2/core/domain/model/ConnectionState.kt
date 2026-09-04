package com.goldenv2.core.domain.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class ConnectionState(
    val status: VpnStatus = VpnStatus.Disconnected,
    val currentServer: Server? = null,
    @Contextual val connectedAt: Instant? = null,
    val uploadSpeed: Long = 0, // bytes/sec
    val downloadSpeed: Long = 0, // bytes/sec
    val totalUpload: Long = 0, // bytes
    val totalDownload: Long = 0, // bytes
    val lastError: String? = null,
    val isReconnecting: Boolean = false
) {
    val uptime: Long
        get() = connectedAt?.let { Instant.now().toEpochMilli() - it.toEpochMilli() } ?: 0

    val isConnected: Boolean
        get() = status == VpnStatus.Connected

    val isConnecting: Boolean
        get() = status == VpnStatus.Connecting

    val formattedUptime: String
        get() {
            val seconds = uptime / 1000
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            val secs = seconds % 60
            return if (hours > 0) "%02d:%02d:%02d".format(hours, minutes, secs)
            else "%02d:%02d".format(minutes, secs)
        }

    val formattedUploadSpeed: String
        get() = formatSpeed(uploadSpeed)

    val formattedDownloadSpeed: String
        get() = formatSpeed(downloadSpeed)

    val formattedTotalUpload: String
        get() = formatBytes(totalUpload)

    val formattedTotalDownload: String
        get() = formatBytes(totalDownload)

    private fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec >= 1_000_000_000 -> "%.2f GB/s".format(bytesPerSec / 1_000_000_000.0)
            bytesPerSec >= 1_000_000 -> "%.2f MB/s".format(bytesPerSec / 1_000_000.0)
            bytesPerSec >= 1_000 -> "%.2f KB/s".format(bytesPerSec / 1_000.0)
            else -> "%d B/s".format(bytesPerSec)
        }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1_000_000_000_000L -> "%.2f TB".format(bytes / 1_000_000_000_000.0)
            bytes >= 1_000_000_000 -> "%.2f GB".format(bytes / 1_000_000_000.0)
            bytes >= 1_000_000 -> "%.2f MB".format(bytes / 1_000_000.0)
            bytes >= 1_000 -> "%.2f KB".format(bytes / 1_000.0)
            else -> "%d B".format(bytes)
        }
    }
}

@Serializable
enum class VpnStatus {
    Disconnected, Connecting, Connected, Disconnecting, Reconnecting, Error, PermissionRequired
}