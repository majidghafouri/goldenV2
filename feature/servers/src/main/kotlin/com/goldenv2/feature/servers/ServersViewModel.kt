package com.goldenv2.feature.servers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goldenv2.core.domain.model.Server
import com.goldenv2.core.domain.model.Subscription
import com.goldenv2.core.domain.usecase.GetServersUseCase
import com.goldenv2.core.domain.usecase.LogUseCase
import com.goldenv2.core.domain.usecase.ServerManagementUseCase
import com.goldenv2.core.domain.usecase.SubscriptionUseCase
import com.goldenv2.core.network.SubscriptionFetcher
import com.goldenv2.core.network.parser.ConfigParserRegistry
import com.goldenv2.core.network.parser.parseSubscriptionContent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ServersViewModel @Inject constructor(
    private val getServersUseCase: GetServersUseCase,
    private val serverManagementUseCase: ServerManagementUseCase,
    private val subscriptionUseCase: SubscriptionUseCase,
    private val subscriptionFetcher: SubscriptionFetcher,
    private val logUseCase: LogUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServersUiState())
    val uiState: StateFlow<ServersUiState> = _uiState

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                getServersUseCase(),
                subscriptionUseCase.getAll(),
                searchQuery
            ) { servers, subscriptions, query ->
                val filteredServers = if (query.isBlank()) {
                    servers
                } else {
                    servers.filter { it.name.contains(query, ignoreCase = true) }
                }

                // Group servers by subscription
                val grouped = subscriptions.associateWith { sub ->
                    filteredServers.filter { it.subscriptionId == sub.id }
                }

                ServersUiState(
                    subscriptions = subscriptions,
                    servers = filteredServers,
                    groupedServers = grouped,
                    searchQuery = query,
                    isLoading = false
                )
            }.distinctUntilChanged().collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onAddSubscription() {
        // Navigation to add subscription dialog
    }

    fun onAddServer() {
        // Navigation to add server dialog
    }

    fun onRefreshSubscription(subscription: Subscription) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            logUseCase.i("ServersViewModel", "Refreshing subscription: ${subscription.name}")

            val result = subscriptionFetcher.fetchAndParse(subscription.url, subscription.id)

            result.onSuccess { servers ->
                serverManagementUseCase.deleteBySubscription(subscription.id)
                serverManagementUseCase.addServers(servers)
                subscriptionUseCase.updateRefreshResult(subscription, true, servers.size)
                logUseCase.i("ServersViewModel", "Refreshed ${servers.size} servers from ${subscription.name}")
            }.onFailure { e ->
                subscriptionUseCase.updateRefreshResult(subscription, false, 0, e.message)
                logUseCase.e("ServersViewModel", "Failed to refresh subscription", e)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onRefreshAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            subscriptionUseCase.getEnabled().first().forEach { sub ->
                onRefreshSubscription(sub)
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onDeleteSubscription(subscription: Subscription) {
        viewModelScope.launch {
            serverManagementUseCase.deleteBySubscription(subscription.id)
            subscriptionUseCase.deleteSubscription(subscription.id)
            logUseCase.i("ServersViewModel", "Deleted subscription: ${subscription.name}")
        }
    }

    fun onToggleSubscriptionEnabled(subscription: Subscription) {
        subscriptionUseCase.toggleEnabled(subscription)
    }

    fun onSelectServer(server: Server) {
        serverManagementUseCase.selectServer(server.id)
    }

    fun onDeleteServer(server: Server) {
        viewModelScope.launch {
            serverManagementUseCase.deleteServer(server.id)
            logUseCase.i("ServersViewModel", "Deleted server: ${server.name}")
        }
    }

    fun onDuplicateServer(server: Server) {
        viewModelScope.launch {
            val newServer = serverManagementUseCase.duplicateServer(server)
            logUseCase.i("ServersViewModel", "Duplicated server: ${newServer.name}")
        }
    }

    fun onTestLatency(server: Server) {
        viewModelScope.launch {
            // TODO: Implement actual latency test
            logUseCase.i("ServersViewModel", "Testing latency for ${server.name}")
            // Simulate latency test
            val latency = (Math.random() * 200 + 20).toLong()
            serverManagementUseCase.updateLatency(server.id, latency)
        }
    }

    fun onImportConfig(config: String) {
        viewModelScope.launch {
            val results = ConfigParserRegistry.parseAll(config.lines().toList())
            val servers = results.mapNotNull { if (it is com.goldenv2.core.network.parser.ParseResult.Success) it.server else null }
            if (servers.isNotEmpty()) {
                serverManagementUseCase.addServers(servers)
                logUseCase.i("ServersViewModel", "Imported ${servers.size} servers from config")
            }
        }
    }
}

data class ServersUiState(
    val subscriptions: List<Subscription> = emptyList(),
    val servers: List<Server> = emptyList(),
    val groupedServers: Map<Subscription, List<Server>> = emptyMap(),
    val searchQuery: String = "",
    val isLoading: Boolean = false
)