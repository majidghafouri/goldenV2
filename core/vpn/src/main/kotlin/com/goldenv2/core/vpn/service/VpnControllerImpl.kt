package com.goldenv2.core.vpn.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.goldenv2.core.domain.model.ConnectionState
import com.goldenv2.core.domain.model.Server
import com.goldenv2.core.domain.model.VpnStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton [VpnController] that owns the connection state and orchestrates
 * [VpnServiceImpl] (which is instantiated by the Android system, not DI).
 *
 * The ViewModels observe [connectionState] here, and [start]/[stop]/[reconnect]
 * dispatch work to the VPN service via intents. The service performs the tunnel
 * setup and reports state back through [updateState].
 */
@Singleton
class VpnControllerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : VpnController {

    private val _connectionState = MutableStateFlow(ConnectionState())
    override val connectionState: StateFlow<ConnectionState> = _connectionState

    private val pendingServer = AtomicReference<Server?>(null)

    override suspend fun start(server: Server) {
        pendingServer.set(server)
        _connectionState.update { it.copy(status = VpnStatus.Connecting, currentServer = server) }
        ContextCompat.startForegroundService(
            context,
            Intent(context, VpnServiceImpl::class.java).apply { action = VpnServiceImpl.ACTION_CONNECT }
        )
    }

    override suspend fun stop() {
        context.stopService(Intent(context, VpnServiceImpl::class.java))
        _connectionState.update {
            it.copy(status = VpnStatus.Disconnected, currentServer = null, connectedAt = null)
        }
    }

    override suspend fun reconnect() {
        val server = _connectionState.value.currentServer
        if (server != null) {
            _connectionState.update { it.copy(status = VpnStatus.Reconnecting) }
            context.stopService(Intent(context, VpnServiceImpl::class.java))
            kotlinx.coroutines.delay(1000)
            start(server)
        } else {
            _connectionState.update { it.copy(status = VpnStatus.Disconnected) }
        }
    }

    internal fun consumePendingServer(): Server? = pendingServer.getAndSet(null)

    internal fun updateState(transform: (ConnectionState) -> ConnectionState) {
        _connectionState.update(transform)
    }
}