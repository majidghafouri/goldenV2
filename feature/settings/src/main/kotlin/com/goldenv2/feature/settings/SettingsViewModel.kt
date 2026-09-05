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
        updateSettings { selected ->
            selected.copy(dnsStrategy = strategy)
        }
    }

    fun onDnsServersChanged(servers: List<String>) {
        updateSettings { selected ->
            selected.copy(dnsServers = servers)
        }
    }

    fun onFakeDnsChanged(enabled: Boolean) {
        updateSettings { selected ->
            selected.copy(fakeDnsEnabled = enabled)
        }
    }

    fun onFakeDnsRangeChanged(range: String) {
        updateSettings { selected ->
            selected.copy(fakeDnsIpRange = range)
        }
    }

    fun onLocalSocksPortChanged(port: Int) {
        updateSettings { selected ->
            selected.copy(localSocksPort = port)
        }
    }

    fun onLocalHttpPortChanged(port: Int) {
        updateSettings { selected ->
            selected.copy(localHttpPort = port)
        }
    }

    fun onAllowLocalProxyChanged(enabled: Boolean) {
        updateSettings { selected ->
            selected.copy(allowLocalProxy = enabled)
        }
    }

    fun onAutoConnectOnBootChanged(enabled: Boolean) {
        updateSettings { selected ->
            selected.copy(autoConnectOnBoot = enabled)
        }
    }

    fun onAutoReconnectChanged(enabled: Boolean) {
        updateSettings { selected ->
            selected.copy(autoReconnect = enabled)
        }
    }

    fun onKillSwitchChanged(enabled: Boolean) {
        updateSettings { selected ->
            selected.copy(killSwitchEnabled = enabled)
        }
    }

    fun onVpnModeChanged(mode: VpnMode) {
        updateSettings { selected ->
            selected.copy(vpnMode = mode)
        }
    }

    fun onMtuChanged(mtu: Int) {
        updateSettings { selected ->
            selected.copy(mtu = mtu)
        }
    }

    fun onThemeChanged(theme: ThemeMode) {
        updateSettings { selected ->
            selected.copy(theme = theme)
        }
    }

    fun onLanguageChanged(language: String) {
        updateSettings { selected ->
            selected.copy(language = language)
        }
    }

    fun onShowSpeedInNotificationChanged(enabled: Boolean) {
        updateSettings { selected ->
            selected.copy(showSpeedInNotification = enabled)
        }
    }

    fun onShowNotificationChanged(enabled: Boolean) {
        updateSettings { selected ->
            selected.copy(showNotification = enabled)
        }
    }

    fun onSubscriptionAutoRefreshChanged(enabled: Boolean) {
        updateSettings { selected ->
            selected.copy(subscriptionAutoRefresh = enabled)
        }
    }

    fun onSubscriptionRefreshIntervalChanged(hours: Int) {
        updateSettings { selected ->
            selected.copy(subscriptionRefreshIntervalHours = hours)
        }
    }

    fun onLogLevelChanged(level: LogLevel) {
        updateSettings { selected ->
            selected.copy(logLevel = level)
        }
    }

    fun onMaxLogEntriesChanged(entries: Int) {
        updateSettings { selected ->
            selected.copy(maxLogEntries = entries)
        }
    }

    fun onBypassPackageNamesChanged(packages: List<String>) {
        updateSettings { selected ->
            selected.copy(bypassPackageNames = packages)
        }
    }

    fun onBypassUidsChanged(uids: List<Int>) {
        updateSettings { selected ->
            selected.copy(bypassUids = uids)
        }
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