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
import com.goldenv2.core.domain.model.Protocol
import com.goldenv2.core.domain.model.Server
import com.goldenv2.core.domain.model.VpnStatus
import com.goldenv2.core.vpn.R
import com.goldenv2.core.vpn.xray.XrayConfigBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

interface VpnController {
    val connectionState: StateFlow<ConnectionState>
    fun isVpnPermissionGranted(): Boolean
    fun requestVpnPermission(): Intent?
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
    private var sshTunnel: SshTunnel? = null
    private val isRunning = AtomicBoolean(false)
    private val stopRequested = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Serializes all native tunnel lifecycle operations (start/stop/stats-read).
     * Prevents the disconnect crash caused by concurrent native calls into
     * hev-socks5-tunnel (stop racing a stats read) and by closing the TUN fd
     * while the tunnel's native reader threads are still using it.
     */
    private val tunnelMutex = Mutex()

    /** True while a teardown is in progress or a teardown job is pending. */
    private val teardownInProgress = AtomicBoolean(false)

    /**
     * Dedicated scope for teardown work: it must survive the cancellation of
     * [scope] (which happens in [onDestroy]) or the native cleanup would be
     * cancelled mid-flight and leak the tunnel/fd.
     */
    private val teardownScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Completes when the currently-running teardown finishes; a connect arriving
     * while teardown is in flight awaits this instead of silently early-returning.
     */
    private val teardownSignal = AtomicReference(
        CompletableDeferred<Unit>().apply { complete(Unit) }
    )

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
    }

    private suspend fun extractXrayAssets(): Boolean = withContext(Dispatchers.IO) {
        // The Xray binary ships as a native lib (jniLibs/arm64-v8a/libxray.so)
        // and is extracted to nativeLibraryDir by the package manager. It must
        // be executed from there: on Android 10+ SELinux denies exec() on app
        // data dirs (app_data_file), but allows it on apk_data_file
        // (nativeLibraryDir).
        val xrayFile = File(applicationInfo.nativeLibraryDir, "libxray.so")
        val geoipFile = File(filesDir, "geoip.dat")
        val geositeFile = File(filesDir, "geosite.dat")

        if (!xrayFile.exists()) {
            android.util.Log.e(TAG, "Xray binary missing in nativeLibraryDir: ${xrayFile.absolutePath}")
            return@withContext false
        }
        xrayBinaryPath = xrayFile.absolutePath

        try {
            // geoip/geosite load from filesDir via the XRAY_LOCATION_ASSET env
            // var (set in startXrayProcess); Xray 26 resolves the databases
            // relative to that directory, ignoring routing.geoip.path.
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

            geoipPath = geoipFile.absolutePath
            geositePath = geositeFile.absolutePath
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
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

        while (teardownInProgress.get()) teardownSignal.get().await()

        // Check VPN preparation before attempting to create tunnel
        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent != null) {
            controller.updateState { it.copy(
                status = VpnStatus.Error,
                lastError = "VPN permission not granted. Please grant VPN permission in system settings."
            ) }
            return
        }

        tunnelMutex.withLock {
            if (isRunning.get() || teardownInProgress.get()) return

            if (!extractXrayAssets()) {
                controller.updateState { it.copy(status = VpnStatus.Error, lastError = "Failed to extract Xray assets") }
                return
            }

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

                // SSH protocol needs a live local tunnel before Xray can route through it
                if (server.protocol == Protocol.Ssh) {
                    val tunnel = SshTunnel(server, this)
                    val err = tunnel.start()
                    if (err != null) {
                        controller.updateState { it.copy(status = VpnStatus.Error, lastError = "SSH tunnel: $err") }
                        vpnInterface?.close()
                        vpnInterface = null
                        return
                    }
                    sshTunnel = tunnel
                }

                // Start Xray process
                startXrayProcess(configFile.absolutePath)

                // If a disconnect arrived while starting Xray, stop before the
                // native tunnel starts reading the fd.
                if (stopRequested.get()) {
                    xrayProcess?.destroy()
                    xrayProcess = null
                    sshTunnel?.stop()
                    sshTunnel = null
                    vpnInterface?.close()
                    vpnInterface = null
                    return
                }

                // Start the hev-socks5-tunnel datapath: reads packets from the TUN
                // fd and forwards them to the local SOCKS5 inbound of the Xray process
                val hevConfigFile = writeHevConfig(settings)
                val fd = vpnInterface?.fd ?: throw IllegalStateException("VPN interface fd unavailable")
                if (!HevTun2Socks.TProxyStartService(hevConfigFile.absolutePath, fd)) {
                    throw IllegalStateException("Failed to start tunnel datapath")
                }

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

            } catch (e: Exception) {
                controller.updateState { it.copy(status = VpnStatus.Error, lastError = e.message) }
                teardown()
            }
        }

        // Stats monitoring runs OUTSIDE the tunnel mutex so it never blocks teardown.
        if (isRunning.get()) {
            startStatsMonitoring()
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

    private fun writeHevConfig(settings: com.goldenv2.core.domain.model.AppSettings): File {
        val config = """
            tunnel:
              mtu: ${settings.mtu}
              ipv4: '10.0.0.1'
            socks5:
              address: 127.0.0.1
              port: ${settings.localSocksPort}
              udp: 'udp'
            misc:
              log-level: 'info'
        """.trimIndent()
        return File(filesDir, "hev-socks5-tunnel.yml").apply {
            writeText(config)
        }
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
        val binaryPath = xrayBinaryPath
            ?: throw IllegalStateException("Xray binary path not set")

        // Xray 26 loads geoip.dat/geosite.dat from the XRAY_LOCATION_ASSET
        // directory (there are no -geoip/-geosite CLI flags anymore).
        val pb = mutableListOf(binaryPath, "-config", configPath)
        xrayProcess = ProcessBuilder(pb)
            .apply {
                environment()["XRAY_LOCATION_ASSET"] = filesDir.absolutePath
                redirectErrorStream(true)
            }
            .start()

        scope.launch {
            try {
                xrayProcess?.inputStream?.bufferedReader()?.use { reader ->
                    reader.forEachLine { line ->
                        parseStatsLine(line)
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                // Stream closes when teardown() destroys the Xray process while
                // this reader is blocked; an unhandled coroutine exception here
                // would crash the app (FATAL EXCEPTION on Android).
                android.util.Log.d(TAG, "Xray output stream closed: ${e.message}")
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
                    val currTime = System.currentTimeMillis()
                    val elapsed = (currTime - lastTime) / 1000.0

                    val stats = tunnelMutex.withLock {
                        if (!isRunning.get() || teardownInProgress.get()) null
                        else HevTun2Socks.TProxyGetStats()
                    }
                    val currUpload = stats?.getOrNull(1) ?: lastUpload
                    val currDownload = stats?.getOrNull(3) ?: lastDownload

                    val uploadDelta = (currUpload - lastUpload).coerceAtLeast(0)
                    val downloadDelta = (currDownload - lastDownload).coerceAtLeast(0)

                    controller.updateState {
                        it.copy(
                            uploadSpeed = if (elapsed > 0) (uploadDelta / elapsed).toLong() else 0,
                            downloadSpeed = if (elapsed > 0) (downloadDelta / elapsed).toLong() else 0,
                            totalUpload = currUpload,
                            totalDownload = currDownload
                        )
                    }

                    lastUpload = currUpload
                    lastDownload = currDownload
                    lastTime = currTime

                    updateNotification()
                    kotlinx.coroutines.delay(1000)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
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
        // Idempotent: a disconnect racing connect/catch/onDestroy must only
        // run the native cleanup once.
        if (!teardownInProgress.compareAndSet(false, true)) {
            return
        }

        stopRequested.set(true)
        isRunning.set(false)

        val signal = CompletableDeferred<Unit>()
        teardownSignal.set(signal)

        teardownScope.launch {
            tunnelMutex.withLock {
                try {
                    // Stop hev-socks5-tunnel first: it owns the native threads
                    // reading the TUN fd, so it must release the fd before we
                    // close it underneath them.
                    runCatching { HevTun2Socks.TProxyStopService() }

                    xrayProcess?.destroy()
                    xrayProcess = null

                    sshTunnel?.stop()
                    sshTunnel = null

                    runCatching { vpnInterface?.close() }
                    vpnInterface = null
                } finally {
                    teardownInProgress.set(false)
                }
            }
            withContext(Dispatchers.Main) {
                runCatching {
                    val nm = getSystemService(NotificationManager::class.java)
                    nm.cancel(NOTIFICATION_ID)
                    stopForeground(true)
                }
            }
            signal.complete(Unit)
        }
    }

    override fun onDestroy() {
        // teardown() runs on its own scope; cancelling [scope] first would
        // kill the native cleanup job before it releases the tunnel/fd.
        teardown()
        scope.coroutineContext.cancelChildren()
        super.onDestroy()
    }

    companion object {
        const val ACTION_CONNECT = "com.goldenv2.core.vpn.action.CONNECT"
        const val ACTION_STOP = "com.goldenv2.core.vpn.action.STOP"

        private const val TAG = "VpnServiceImpl"

        fun prepare(context: Context): Boolean {
            val intent = VpnService.prepare(context)
            return intent == null
        }
    }
}