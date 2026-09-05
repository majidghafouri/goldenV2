package com.goldenv2.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goldenv2.core.domain.model.AppSettings
import com.goldenv2.core.domain.model.ConnectionState
import com.goldenv2.core.domain.model.Server
import com.goldenv2.core.domain.model.VpnStatus
import com.goldenv2.core.domain.usecase.LogUseCase
import com.goldenv2.core.domain.usecase.ServerManagementUseCase
import com.goldenv2.core.domain.usecase.SettingsUseCase
import com.goldenv2.core.vpn.service.VpnController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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

    init {
        observeVpnState()
    }

    private fun observeVpnState() {
        viewModelScope.launch {
            vpnController.connectionState
                .combine(settingsUseCase.settingsFlow) { connectionState, settings ->
                    HomeUiState(
                        connectionState = connectionState,
                        settings = settings,
                        selectedServer = connectionState.currentServer,
                        vpnPermissionGranted = _vpnPermissionGranted.value
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
                state.selectedServer?.let { server ->
                    if (!_vpnPermissionGranted.value) {
                        // Request VPN permission
                        return
                    }
                    connectToServer(server)
                }
            }
            VpnStatus.Connected, VpnStatus.Connecting, VpnStatus.Reconnecting -> {
                disconnect()
            }
            VpnStatus.PermissionRequired -> {
                // Request VPN permission
            }
            else -> {
                // Handle any unmatched status
            }
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

    fun onVpnPermissionGranted() {
        _vpnPermissionGranted.value = true
        val state = _uiState.value
        state.selectedServer?.let { connectToServer(it) }
    }

    fun onVpnPermissionDenied() {
        _vpnPermissionGranted.value = false
        _uiState.update { it.copy(connectionState = it.connectionState.copy(status = VpnStatus.PermissionRequired)) }
        viewModelScope.launch { logUseCase.w("HomeViewModel", "VPN permission denied") }
    }

    fun onServerSelected(server: Server) {
        _uiState.update { it.copy(selectedServer = server) }
    }

    fun onNavigateToServers() {
        // Navigation handled by activity
    }

    fun onNavigateToSettings() {
        // Navigation handled by activity
    }

    fun onNavigateToLogs() {
        // Navigation handled by activity
    }
}

data class HomeUiState(
    val connectionState: ConnectionState = ConnectionState(),
    val settings: AppSettings = AppSettings(),
    val selectedServer: Server? = null,
    val vpnPermissionGranted: Boolean = false
)