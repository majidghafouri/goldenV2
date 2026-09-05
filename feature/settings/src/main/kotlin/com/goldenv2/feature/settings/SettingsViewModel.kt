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
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
        updateSettings { copy(dnsStrategy = strategy) }
    }

    fun onDnsServersChanged(servers: List<String>) {
        updateSettings { copy(dnsServers = servers) }
    }

    fun onFakeDnsChanged(enabled: Boolean) {
        updateSettings { copy(fakeDnsEnabled = enabled) }
    }

    fun onFakeDnsRangeChanged(range: String) {
        updateSettings { copy(fakeDnsIpRange = range) }
    }

    fun onLocalSocksPortChanged(port: Int) {
        updateSettings { copy(localSocksPort = port) }
    }

    fun onLocalHttpPortChanged(port: Int) {
        updateSettings { copy(localHttpPort = port) }
    }

    fun onAllowLocalProxyChanged(enabled: Boolean) {
        updateSettings { copy(allowLocalProxy = enabled) }
    }

    fun onAutoConnectOnBootChanged(enabled: Boolean) {
        updateSettings { copy(autoConnectOnBoot = enabled) }
    }

    fun onAutoReconnectChanged(enabled: Boolean) {
        updateSettings { copy(autoReconnect = enabled) }
    }

    fun onKillSwitchChanged(enabled: Boolean) {
        updateSettings { copy(killSwitchEnabled = enabled) }
    }

    fun onVpnModeChanged(mode: VpnMode) {
        updateSettings { copy(vpnMode = mode) }
    }

    fun onMtuChanged(mtu: Int) {
        updateSettings { copy(mtu = mtu) }
    }

    fun onThemeChanged(theme: ThemeMode) {
        updateSettings { copy(theme = theme) }
    }

    fun onLanguageChanged(language: String) {
        updateSettings { copy(language = language) }
    }

    fun onShowSpeedInNotificationChanged(enabled: Boolean) {
        updateSettings { copy(showSpeedInNotification = enabled) }
    }

    fun onShowNotificationChanged(enabled: Boolean) {
        updateSettings { copy(showNotification = enabled) }
    }

    fun onSubscriptionAutoRefreshChanged(enabled: Boolean) {
        updateSettings { copy(subscriptionAutoRefresh = enabled) }
    }

    fun onSubscriptionRefreshIntervalChanged(hours: Int) {
        updateSettings { copy(subscriptionRefreshIntervalHours = hours) }
    }

    fun onLogLevelChanged(level: LogLevel) {
        updateSettings { copy(logLevel = level) }
    }

    fun onMaxLogEntriesChanged(entries: Int) {
        updateSettings { copy(maxLogEntries = entries) }
    }

    fun onBypassPackageNamesChanged(packages: List<String>) {
        updateSettings { copy(bypassPackageNames = packages) }
    }

    fun onBypassUidsChanged(uids: List<Int>) {
        updateSettings { copy(bypassUids = uids) }
    }

    fun onImportConfig(json: String) {
        viewModelScope.launch {
            try {
                val settings = AppSettings()
                settingsUseCase.saveSettings(settings)
                logUseCase.i("SettingsViewModel", "Configuration imported successfully")
            } catch (e: Exception) {
                logUseCase.e("SettingsViewModel", "Failed to import configuration", e)
            }
        }
    }

    fun onExportConfig(): String {
        return _uiState.value.settings.toString()
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