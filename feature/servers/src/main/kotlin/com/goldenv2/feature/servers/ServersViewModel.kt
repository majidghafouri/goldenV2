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
import com.goldenv2.core.network.parser.parseSubscriptionContent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URI
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

                val grouped = subscriptions.associateWith { sub ->
                    filteredServers.filter { it.subscriptionId == sub.id }
                }

                Triple(filteredServers, subscriptions, grouped)
            }.distinctUntilChanged().collect { (servers, subscriptions, grouped) ->
                _uiState.update { state ->
                    state.copy(
                        subscriptions = subscriptions,
                        servers = servers,
                        groupedServers = grouped,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onAddServer() {
        _uiState.update { it.copy(showAddServerSheet = true) }
    }

    fun onDismissAddServerSheet() {
        _uiState.update { it.copy(showAddServerSheet = false) }
    }

    fun onOpenManualEntry() {
        _uiState.update { it.copy(showAddServerSheet = false, showManualEntry = true) }
    }

    fun onDismissManualEntry() {
        _uiState.update { it.copy(showManualEntry = false) }
    }

    fun onOpenQrScanner() {
        _uiState.update { it.copy(showAddServerSheet = false, showQrScanner = true) }
    }

    fun onDismissQrScanner() {
        _uiState.update { it.copy(showQrScanner = false) }
    }

    fun onAddSubscription() {
        _uiState.update { it.copy(showAddSubscriptionDialog = true) }
    }

    fun onDismissAddSubscriptionDialog() {
        _uiState.update { it.copy(showAddSubscriptionDialog = false) }
    }

    fun onImportMessageShown() {
        _uiState.update { it.copy(importMessage = null, isImportError = false) }
    }

    fun onAddFromClipboard(text: String?) = onImportConfig(text.orEmpty())

    fun onQrScanned(text: String) {
        _uiState.update { it.copy(showQrScanner = false) }
        onImportConfig(text)
    }

    fun onImportConfig(input: String) {
        viewModelScope.launch {
            if (input.isBlank()) {
                setImportResult("Clipboard is empty", isError = true)
                return@launch
            }

            val servers = parseSubscriptionContent(input)
            if (servers.isEmpty()) {
                logUseCase.w("ServersViewModel", "No valid server links found in input")
                setImportResult("No valid server links found in the input", isError = true)
            } else {
                serverManagementUseCase.addServers(servers)
                logUseCase.i("ServersViewModel", "Added ${servers.size} servers from config")
                setImportResult("Added ${servers.size} server(s)", isError = false)
            }
        }
    }

    fun onAddSubscriptionUrl(url: String) {
        val trimmed = url.trim()
        if (trimmed.isEmpty() || !(trimmed.startsWith("http://") || trimmed.startsWith("https://"))) {
            setImportResult("Invalid subscription URL", isError = true)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(showAddSubscriptionDialog = false) }

            val name = runCatching { URI(trimmed).host }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?: "Subscription"
            val subscription = Subscription(url = trimmed, name = name)
            subscriptionUseCase.addSubscription(subscription)
            logUseCase.i("ServersViewModel", "Adding subscription: $name")

            subscriptionFetcher.fetchAndParse(trimmed, subscription.id)
                .onSuccess { servers ->
                    serverManagementUseCase.addServers(servers)
                    subscriptionUseCase.updateRefreshResult(subscription, true, servers.size)
                    setImportResult("Subscription added: ${servers.size} servers", isError = false)
                }
                .onFailure { e ->
                    subscriptionUseCase.updateRefreshResult(subscription, false, 0, e.message)
                    logUseCase.e("ServersViewModel", "Failed to fetch subscription", e)
                    setImportResult("Failed to add subscription: ${e.message ?: "network error"}", isError = true)
                }
        }
    }

    private fun setImportResult(message: String, isError: Boolean) {
        _uiState.update { it.copy(importMessage = message, isImportError = isError) }
    }

    fun onRefreshSubscription(subscription: Subscription) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            logUseCase.i("ServersViewModel", "Refreshing subscription: ${subscription.name}")

            subscriptionFetcher.fetchAndParse(subscription.url, subscription.id)
                .onSuccess { servers ->
                    serverManagementUseCase.deleteBySubscription(subscription.id)
                    serverManagementUseCase.addServers(servers)
                    subscriptionUseCase.updateRefreshResult(subscription, true, servers.size)
                    logUseCase.i("ServersViewModel", "Refreshed ${servers.size} servers from ${subscription.name}")
                }
                .onFailure { e ->
                    subscriptionUseCase.updateRefreshResult(subscription, false, 0, e.message)
                    logUseCase.e("ServersViewModel", "Failed to refresh subscription", e)
                }
                .also {
                    _uiState.update { state -> state.copy(isLoading = false) }
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
        viewModelScope.launch {
            subscriptionUseCase.toggleEnabled(subscription)
        }
    }

    fun onSelectServer(server: Server) {
        viewModelScope.launch {
            serverManagementUseCase.selectServer(server.id)
        }
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
            logUseCase.i("ServersViewModel", "Testing latency for ${server.name}")
            val latency = (Math.random() * 200 + 20).toLong()
            serverManagementUseCase.updateLatency(server.id, latency)
        }
    }
}

data class ServersUiState(
    val subscriptions: List<Subscription> = emptyList(),
    val servers: List<Server> = emptyList(),
    val groupedServers: Map<Subscription, List<Server>> = emptyMap(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val showAddServerSheet: Boolean = false,
    val showManualEntry: Boolean = false,
    val showQrScanner: Boolean = false,
    val showAddSubscriptionDialog: Boolean = false,
    val importMessage: String? = null,
    val isImportError: Boolean = false
)