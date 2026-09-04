package com.goldenv2.feature.routing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goldenv2.core.domain.model.RoutingConfig
import com.goldenv2.core.domain.model.RoutingRule
import com.goldenv2.core.domain.model.RuleType
import com.goldenv2.core.domain.usecase.LogUseCase
import com.goldenv2.core.domain.usecase.SettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class RoutingViewModel @Inject constructor(
    private val settingsUseCase: SettingsUseCase,
    private val logUseCase: LogUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutingUiState())
    val uiState: StateFlow<RoutingUiState> = _uiState

    init {
        observeRoutingConfig()
    }

    private fun observeRoutingConfig() {
        viewModelScope.launch {
            settingsUseCase.settingsFlow.collect { settings ->
                // TODO: Get routing config from repository
                // For now, use default
            }
        }
    }

    fun onRoutingModeChanged(mode: RoutingMode) {
        val newConfig = when (mode) {
            RoutingMode.ProxyAll -> RoutingConfig.proxyAll()
            RoutingMode.BypassLAN -> RoutingConfig.default()
            RoutingMode.DirectAll -> RoutingConfig.directAll()
            RoutingMode.Custom -> _uiState.value.routingConfig
        }
        _uiState.update { it.copy(routingConfig = newConfig, selectedMode = mode) }
        saveRoutingConfig(newConfig)
    }

    fun onAddRule() {
        val newRule = RoutingRule(
            id = UUID.randomUUID().toString(),
            type = RuleType.custom,
            outboundTag = "proxy",
            order = _uiState.value.routingConfig.rules.size
        )
        _uiState.update {
            it.copy(
                routingConfig = it.routingConfig.copy(rules = it.routingConfig.rules + newRule),
                editingRuleId = newRule.id
            )
        }
    }

    fun onUpdateRule(rule: RoutingRule) {
        _uiState.update {
            val updatedRules = it.routingConfig.rules.map { if (it.id == rule.id) rule else it }
            it.copy(routingConfig = it.routingConfig.copy(rules = updatedRules), editingRuleId = null)
        }
        saveRoutingConfig(_uiState.value.routingConfig)
    }

    fun onDeleteRule(ruleId: String) {
        _uiState.update {
            val updatedRules = it.routingConfig.rules.filter { it.id != ruleId }
            it.copy(routingConfig = it.routingConfig.copy(rules = updatedRules))
        }
        saveRoutingConfig(_uiState.value.routingConfig)
    }

    fun onReorderRules(fromIndex: Int, toIndex: Int) {
        _uiState.update {
            val rules = it.routingConfig.rules.toMutableList()
            val item = rules.removeAt(fromIndex)
            rules.add(toIndex, item)
            // Update order
            val updatedRules = rules.mapIndexed { index, rule -> rule.copy(order = index) }
            it.copy(routingConfig = it.routingConfig.copy(rules = updatedRules))
        }
        saveRoutingConfig(_uiState.value.routingConfig)
    }

    fun onStartEditingRule(ruleId: String) {
        _uiState.update { it.copy(editingRuleId = ruleId) }
    }

    fun onCancelEditing() {
        _uiState.update { it.copy(editingRuleId = null) }
    }

    fun onDomainStrategyChanged(strategy: com.goldenv2.core.domain.model.DomainStrategy) {
        _uiState.update { it.copy(routingConfig = it.routingConfig.copy(domainStrategy = strategy)) }
        saveRoutingConfig(_uiState.value.routingConfig)
    }

    fun onOutboundTagChanged(ruleId: String, tag: String) {
        _uiState.update {
            val updatedRules = it.routingConfig.rules.map { if (it.id == ruleId) it.copy(outboundTag = tag) else it }
            it.copy(routingConfig = it.routingConfig.copy(rules = updatedRules))
        }
        saveRoutingConfig(_uiState.value.routingConfig)
    }

    fun onDomainChanged(ruleId: String, domains: List<String>) {
        _uiState.update {
            val updatedRules = it.routingConfig.rules.map { if (it.id == ruleId) it.copy(domain = domains) else it }
            it.copy(routingConfig = it.routingConfig.copy(rules = updatedRules))
        }
        saveRoutingConfig(_uiState.value.routingConfig)
    }

    fun onIpChanged(ruleId: String, ips: List<String>) {
        _uiState.update {
            val updatedRules = it.routingConfig.rules.map { if (it.id == ruleId) it.copy(ip = ips) else it }
            it.copy(routingConfig = it.routingConfig.copy(rules = updatedRules))
        }
        saveRoutingConfig(_uiState.value.routingConfig)
    }

    fun onImportConfig(json: String) {
        // TODO: Parse and import routing config from JSON
    }

    fun onExportConfig(): String {
        // TODO: Export routing config as JSON
        return ""
    }

    private fun saveRoutingConfig(config: RoutingConfig) {
        viewModelScope.launch {
            // TODO: Save to SettingsRepository
            logUseCase.i("RoutingViewModel", "Routing config saved: ${config.rules.size} rules")
        }
    }
}

data class RoutingUiState(
    val routingConfig: RoutingConfig = RoutingConfig.default(),
    val selectedMode: RoutingMode = RoutingMode.BypassLAN,
    val editingRuleId: String? = null
)

enum class RoutingMode {
    ProxyAll, BypassLAN, DirectAll, Custom
}