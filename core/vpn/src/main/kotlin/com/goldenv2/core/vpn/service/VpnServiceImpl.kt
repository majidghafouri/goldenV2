package com.goldenv2.core.vpn.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.goldenv2.core.domain.model.ConnectionState
import com.goldenv2.core.domain.model.Server
import com.goldenv2.core.domain.model.VpnStatus
import com.goldenv2.core.vpn.R
import com.goldenv2.core.vpn.xray.XrayConfigBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

interface VpnController {
    val connectionState: StateFlow<ConnectionState>
    fun isVpnPermissionGranted(): Boolean
    suspend fun start(server: Server)
    suspend fun stop()
    suspend fun reconnect()
}

@AndroidEntryPoint
class VpnServiceImpl : VpnService() {

    @Inject
    lateinit var controller: VpnControllerImpl

    private var vpnInterface: ParcelFileDescriptor? = null
    private var xrayProcess: Process? = null
    private val isRunning = AtomicBoolean(false)
    private val stopRequested = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Notification
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "vpn_channel"

    // Xray binary path - TODO: Replace with actual Xray-core AAR integration
    private var xrayBinaryPath: String? = null
    private var geoipPath: String? = null
    private var geositePath: String? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        extractXrayAssets()
    }

    private fun extractXrayAssets() {
        val xrayFile = File(filesDir, "xray")
        val geoipFile = File(filesDir, "geoip.dat")
        val geositeFile = File(filesDir, "geosite.dat")

        if (!xrayFile.exists() || !geoipFile.exists() || !geositeFile.exists()) {
            scope.launch(Dispatchers.IO) {
                try {
                    if (!xrayFile.exists()) {
                        assets.open("xray").use { input ->
                            FileOutputStream(xrayFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        xrayFile.setExecutable(true)
                    }

                    if (!geoipFile.exists()) {
                        assets.open("geoip.dat").use { input ->
                            FileOutputStream(geoipFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }

                    if (!geositeFile.exists()) {
                        assets.open("geosite.dat").use { input ->
                            FileOutputStream(geositeFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }

                    xrayBinaryPath = xrayFile.absolutePath
                    geoipPath = geoipFile.absolutePath
                    geositePath = geositeFile.absolutePath
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            xrayBinaryPath = xrayFile.absolutePath
            geoipPath = geoipFile.absolutePath
            geositePath = geositeFile.absolutePath
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notification = buildNotification("GoldenV2 VPN", "Ready to connect", 0, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        }
        if (intent?.action == ACTION_CONNECT) {
            scope.launch { establishTunnel() }
        } else if (intent?.action == ACTION_STOP) {
            scope.launch {
                teardown()
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun establishTunnel() {
        val server = controller.consumePendingServer() ?: return
        if (isRunning.get()) return

        try {
            stopRequested.set(false)
            val settings = getSettings()
            val config = XrayConfigBuilder.buildConfig(
                server,
                settings,
                getRoutingConfig(),
                geoipPath ?: "",
                geositePath ?: ""
            )

            // Write config to file
            val configFile = File(filesDir, "xray_config.json")
            FileOutputStream(configFile).use { it.write(config.toByteArray()) }

            // Start VPN interface
            val builder = Builder()
                .setSession("GoldenV2 VPN")
                .addAddress("10.0.0.1", 24)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")
                .setMtu(1500)

            // Exclude local networks
            addExcludedRoutes(builder)

            vpnInterface = builder.establish()

            if (vpnInterface == null) {
                throw IllegalStateException("Failed to establish VPN interface")
            }
            if (stopRequested.get()) {
                vpnInterface?.close()
                vpnInterface = null
                return
            }

            // Start Xray process
            startXrayProcess(configFile.absolutePath)

            isRunning.set(true)
            val connectedAt = java.time.Instant.now()

            controller.updateState {
                it.copy(
                    status = VpnStatus.Connected,
                    currentServer = server,
                    connectedAt = connectedAt,
                    totalUpload = 0,
                    totalDownload = 0
                )
            }

            // Start stats monitoring
            startStatsMonitoring()

        } catch (e: Exception) {
            controller.updateState { it.copy(status = VpnStatus.Error, lastError = e.message) }
            teardown()
        }
    }

    private fun getSettings(): com.goldenv2.core.domain.model.AppSettings {
        // TODO: Get from SettingsRepository
        return com.goldenv2.core.domain.model.AppSettings()
    }

    private fun getRoutingConfig(): com.goldenv2.core.domain.model.RoutingConfig {
        // TODO: Get from SettingsRepository
        return com.goldenv2.core.domain.model.RoutingConfig.default()
    }

    private fun addExcludedRoutes(builder: Builder) {
        try {
            // Exclude local networks
            builder.addDisallowedApplication(packageName)

            // Add common private ranges
            val privateRanges = listOf(
                "10.0.0.0/8",
                "172.16.0.0/12",
                "192.168.0.0/16",
                "127.0.0.0/8",
                "169.254.0.0/16"
            )
            // Note: VpnService.Builder doesn't have direct CIDR exclusion
            // This is handled by routing rules in Xray config
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun startXrayProcess(configPath: String) {
        xrayBinaryPath?.let { binaryPath ->
            try {
                val pb = mutableListOf(binaryPath, "-config", configPath)
                geoipPath?.let { pb.add("-geoip"); pb.add(it) }
                geositePath?.let { pb.add("-geosite"); pb.add(it) }
                ProcessBuilder(pb).apply { redirectErrorStream(true) }.start().also { xrayProcess = it }

                scope.launch {
                    xrayProcess?.inputStream?.bufferedReader()?.use { reader ->
                        reader.forEachLine { line ->
                            parseStatsLine(line)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun parseStatsLine(line: String) {
        // TODO: Parse Xray traffic stats
        // Example: parse upload/download from log output
    }

    private fun startStatsMonitoring() {
        scope.launch {
            var lastUpload = 0L
            var lastDownload = 0L
            var lastTime = System.currentTimeMillis()

            while (isRunning.get()) {
                try {
                    // TODO: Get actual stats from Xray API or TUN interface
                    // For now, simulate
                    val currentTime = System.currentTimeMillis()
                    val elapsed = (currentTime - lastTime) / 1000.0

                    // Simulate some traffic for UI testing
                    val simulatedUpload = (Math.random() * 10000).toLong()
                    val simulatedDownload = (Math.random() * 50000).toLong()

                    lastUpload += simulatedUpload
                    lastDownload += simulatedDownload
                    lastTime = currentTime

                    controller.updateState {
                        it.copy(
                            uploadSpeed = if (elapsed > 0) (simulatedUpload / elapsed).toLong() else 0,
                            downloadSpeed = if (elapsed > 0) (simulatedDownload / elapsed).toLong() else 0,
                            totalUpload = lastUpload,
                            totalDownload = lastDownload
                        )
                    }

                    updateNotification()
                    kotlinx.coroutines.delay(1000)
                } catch (e: Exception) {
                    break
                }
            }
        }
    }

    private fun updateNotification() {
        val state = controller.connectionState.value
        val server = state.currentServer
        val notification = buildNotification(
            server?.name ?: "GoldenV2 VPN",
            "Connected: ${state.formattedUptime}",
            state.uploadSpeed,
            state.downloadSpeed
        )
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(title: String, text: String, uploadSpeed: Long, downloadSpeed: Long): Notification {
        val intent = Intent(this, VpnServiceImpl::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE
        )

        val speedText = if (uploadSpeed > 0 || downloadSpeed > 0) {
            "↑ ${formatSpeed(uploadSpeed)} ↓ ${formatSpeed(downloadSpeed)}"
        } else ""

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("$text $speedText")
            .setSmallIcon(R.drawable.ic_vpn_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec >= 1_000_000 -> String.format("%.1f MB/s", bytesPerSec / 1_000_000.0)
            bytesPerSec >= 1_000 -> String.format("%.1f KB/s", bytesPerSec / 1_000.0)
            else -> "$bytesPerSec B/s"
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VPN Connection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "VPN connection status and traffic statistics"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun teardown() {
        stopRequested.set(true)
        isRunning.set(false)
        xrayProcess?.destroy()
        xrayProcess = null

        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            // Ignore
        }
        vpnInterface = null

        val nm = getSystemService(NotificationManager::class.java)
        nm.cancel(NOTIFICATION_ID)
        stopForeground(true)
    }

    override fun onDestroy() {
        scope.coroutineContext.cancelChildren()
        teardown()
        super.onDestroy()
    }

    companion object {
        const val ACTION_CONNECT = "com.goldenv2.core.vpn.action.CONNECT"
        const val ACTION_STOP = "com.goldenv2.core.vpn.action.STOP"

        fun prepare(context: Context): Boolean {
            val intent = VpnService.prepare(context)
            return intent == null
        }
    }
}