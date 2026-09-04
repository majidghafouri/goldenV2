package com.goldenv2.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goldenv2.core.domain.model.AppSettings
import com.goldenv2.core.domain.model.DnsStrategy
import com.goldenv2.core.domain.model.LogLevel
import com.goldenv2.core.domain.model.ThemeMode
import com.goldenv2.core.domain.model.VpnMode
import com.goldenv2.core.domain.usecase.LogUseCase
import com.goldenv2.core.domain.usecase.SettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsUseCase: SettingsUseCase,
    private val logUseCase: LogUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsUseCase.settingsFlow.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    fun onDnsStrategyChanged(strategy: DnsStrategy) {
        updateSettings { it.copy(dnsStrategy = strategy) }
    }

    fun onDnsServersChanged(servers: List<String>) {
        updateSettings { it.copy(dnsServers = servers) }
    }

    fun onFakeDnsChanged(enabled: Boolean) {
        updateSettings { it.copy(fakeDnsEnabled = enabled) }
    }

    fun onFakeDnsRangeChanged(range: String) {
        updateSettings { it.copy(fakeDnsIpRange = range) }
    }

    fun onLocalSocksPortChanged(port: Int) {
        updateSettings { it.copy(localSocksPort = port) }
    }

    fun onLocalHttpPortChanged(port: Int) {
        updateSettings { it.copy(localHttpPort = port) }
    }

    fun onAllowLocalProxyChanged(enabled: Boolean) {
        updateSettings { it.copy(allowLocalProxy = enabled) }
    }

    fun onAutoConnectOnBootChanged(enabled: Boolean) {
        updateSettings { it.copy(autoConnectOnBoot = enabled) }
    }

    fun onAutoReconnectChanged(enabled: Boolean) {
        updateSettings { it.copy(autoReconnect = enabled) }
    }

    fun onKillSwitchChanged(enabled: Boolean) {
        updateSettings { it.copy(killSwitchEnabled = enabled) }
    }

    fun onVpnModeChanged(mode: VpnMode) {
        updateSettings { it.copy(vpnMode = mode) }
    }

    fun onMtuChanged(mtu: Int) {
        updateSettings { it.copy(mtu = mtu) }
    }

    fun onThemeChanged(theme: ThemeMode) {
        updateSettings { it.copy(theme = theme) }
    }

    fun onLanguageChanged(language: String) {
        updateSettings { it.copy(language = language) }
    }

    fun onShowSpeedInNotificationChanged(enabled: Boolean) {
        updateSettings { it.copy(showSpeedInNotification = enabled) }
    }

    fun onShowNotificationChanged(enabled: Boolean) {
        updateSettings { it.copy(showNotification = enabled) }
    }

    fun onSubscriptionAutoRefreshChanged(enabled: Boolean) {
        updateSettings { it.copy(subscriptionAutoRefresh = enabled) }
    }

    fun onSubscriptionRefreshIntervalChanged(hours: Int) {
        updateSettings { it.copy(subscriptionRefreshIntervalHours = hours) }
    }

    fun onLogLevelChanged(level: LogLevel) {
        updateSettings { it.copy(logLevel = level) }
    }

    fun onMaxLogEntriesChanged(entries: Int) {
        updateSettings { it.copy(maxLogEntries = entries) }
    }

    fun onBypassPackageNamesChanged(packages: List<String>) {
        updateSettings { it.copy(bypassPackageNames = packages) }
    }

    fun onBypassUidsChanged(uids: List<Int>) {
        updateSettings { it.copy(bypassUids = uids) }
    }

    fun onImportConfig(json: String) {
        viewModelScope.launch {
            try {
                val settings = com.goldenv2.core.domain.model.AppSettings.serializer().deserialize(
                    kotlinx.serialization.json.Json.Default.decodeFromString(json)
                )
                settingsUseCase.saveSettings(settings)
                logUseCase.i("SettingsViewModel", "Configuration imported successfully")
            } catch (e: Exception) {
                logUseCase.e("SettingsViewModel", "Failed to import configuration", e)
            }
        }
    }

    fun onExportConfig(): String {
        val json = com.goldenv2.core.domain.model.AppSettings.serializer().serialize(_uiState.value.settings)
        return kotlinx.serialization.json.Json.Default.encodeToString(json)
    }

    fun onClearLogs() {
        // Handled by LogUseCase
    }

    private fun updateSettings(block: AppSettings.() -> AppSettings) {
        val newSettings = _uiState.value.settings.block()
        _uiState.update { it.copy(settings = newSettings) }
        viewModelScope.launch {
            settingsUseCase.saveSettings(newSettings)
            logUseCase.i("SettingsViewModel", "Settings saved")
        }
    }
}

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val isLoading: Boolean = false
)