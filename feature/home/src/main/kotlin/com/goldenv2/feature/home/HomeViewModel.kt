package com.goldenv2.feature.home

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goldenv2.core.domain.model.AppSettings
import com.goldenv2.core.domain.model.ConnectionState
import com.goldenv2.core.domain.model.Protocol
import com.goldenv2.core.domain.model.Server
import com.goldenv2.core.domain.model.VpnStatus
import com.goldenv2.core.domain.usecase.LogUseCase
import com.goldenv2.core.domain.usecase.ServerManagementUseCase
import com.goldenv2.core.domain.usecase.SettingsUseCase
import com.goldenv2.core.vpn.service.VpnController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val vpnController: VpnController,
    private val serverUseCase: ServerManagementUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val logUseCase: LogUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private val _vpnPermissionGranted = MutableStateFlow(false)
    val vpnPermissionGranted: StateFlow<Boolean> = _vpnPermissionGranted

    /**
     * One-shot signal for the Activity to launch the system VPN permission
     * dialog. Emits the Intent returned by VpnService.prepare().
     */
    private val _vpnPermissionRequest = MutableStateFlow<Intent?>(null)
    val vpnPermissionRequest: StateFlow<Intent?> = _vpnPermissionRequest

    init {
        observeVpnState()
        checkVpnPermission()
        restoreLastSelectedServer()
    }

    private fun restoreLastSelectedServer() {
        viewModelScope.launch {
            val lastServerId = settingsUseCase.lastSelectedServerIdFlow.first()
            lastServerId?.let { id ->
                val server = serverUseCase.getById(id).first()
                server?.let { s ->
                    serverUseCase.selectServer(s.id)
                    settingsUseCase.saveLastSelectedServerId(s.id)
                }
            }
        }
    }

    private fun checkVpnPermission() {
        viewModelScope.launch {
            val granted = vpnController.isVpnPermissionGranted()
            _vpnPermissionGranted.value = granted
            if (granted) {
                logUseCase.i("HomeViewModel", "VPN permission already granted")
            }
        }
    }

    private fun observeVpnState() {
        viewModelScope.launch {
            combine(
                vpnController.connectionState,
                settingsUseCase.settingsFlow,
                serverUseCase.observeSelectedServer(),
                _vpnPermissionGranted
            ) { connectionState, settings, selectedServer, granted ->
                HomeUiState(
                    connectionState = connectionState,
                    settings = settings,
                    selectedServer = connectionState.currentServer ?: selectedServer,
                    vpnPermissionGranted = granted
                )
            }
                .distinctUntilChanged()
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun onConnectClick() {
        val state = _uiState.value
        when (state.connectionState.status) {
            VpnStatus.Disconnected, VpnStatus.Error -> {
                val server = state.selectedServer
                if (server == null) {
                    // No server selected; UI falls back to navigating to the servers page.
                    return
                }
                if (_vpnPermissionGranted.value) {
                    connectToServer(server)
                } else {
                    // Request permission first; onVpnPermissionGranted() will connect right after.
                    requestVpnPermission()
                }
            }
            VpnStatus.Connected, VpnStatus.Connecting, VpnStatus.Reconnecting -> {
                disconnect()
            }
            VpnStatus.PermissionRequired -> {
                requestVpnPermission()
            }
            else -> Unit
        }
    }

    private fun connectToServer(server: Server) {
        viewModelScope.launch {
            logUseCase.i("HomeViewModel", "Connecting to ${server.name}")
            vpnController.start(server)
        }
    }

    private fun disconnect() {
        viewModelScope.launch {
            logUseCase.i("HomeViewModel", "Disconnecting")
            vpnController.stop()
        }
    }

    private fun requestVpnPermission() {
        val intent = vpnController.requestVpnPermission()
        if (intent != null) {
            _vpnPermissionRequest.value = intent
        }
    }

    fun onVpnPermissionGranted() {
        _vpnPermissionGranted.value = true
        val state = _uiState.value
        if (state.connectionState.status != VpnStatus.Connected) {
            state.selectedServer?.let { connectToServer(it) }
        }
    }

    fun onVpnPermissionDenied() {
        _vpnPermissionGranted.value = false
        _uiState.update { it.copy(connectionState = it.connectionState.copy(status = VpnStatus.PermissionRequired)) }
        viewModelScope.launch { logUseCase.w("HomeViewModel", "VPN permission denied") }
    }

    fun onVpnPermissionHandled() {
        _vpnPermissionRequest.value = null
    }
}

data class HomeUiState(
    val connectionState: ConnectionState = ConnectionState(),
    val settings: AppSettings = AppSettings(),
    val selectedServer: Server? = null,
    val vpnPermissionGranted: Boolean = false
)