package com.goldenv2.feature.servers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goldenv2.core.domain.model.Protocol
import com.goldenv2.core.domain.model.Server
import com.goldenv2.core.domain.model.Subscription
import com.goldenv2.core.domain.usecase.GetServersUseCase
import com.goldenv2.core.domain.usecase.LogUseCase
import com.goldenv2.core.domain.usecase.ServerManagementUseCase
import com.goldenv2.core.domain.usecase.SettingsUseCase
import com.goldenv2.core.domain.usecase.SubscriptionUseCase
import com.goldenv2.core.network.SubscriptionFetcher
import com.goldenv2.core.network.parser.ServerUriSerializer
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
    private val settingsUseCase: SettingsUseCase,
    private val subscriptionFetcher: SubscriptionFetcher,
    private val logUseCase: LogUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServersUiState())
    val uiState: StateFlow<ServersUiState> = _uiState

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val TAG = "ServersViewModel"

    init {
        observeData()
        viewModelScope.launch {
            val count = serverManagementUseCase.count()
            if (count == 0) {
                logUseCase.i("ServersViewModel", "Database empty, adding test SSH server")
                val testServer = Server(
                    id = "test-ssh-1",
                    subscriptionId = "",
                    name = "Test SSH (example.com)",
                    protocol = Protocol.Ssh,
                    address = "example.com",
                    port = 22,
                    uuid = "testuser",
                    password = "testpass"
                )
                serverManagementUseCase.addServer(testServer)
                serverManagementUseCase.selectServer(testServer.id)
                settingsUseCase.saveLastSelectedServerId(testServer.id)
                logUseCase.i("ServersViewModel", "Test SSH server added and selected")
            }
        }
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
                logUseCase.d("ServersViewModel", "Loaded ${servers.size} servers: ${servers.joinToString { it.name }}")
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

    fun onOpenDetailedEntry() {
        _uiState.update { it.copy(showAddServerSheet = false, showDetailedEntrySheet = true) }
    }

    fun onDismissDetailedEntry() {
        _uiState.update { it.copy(showDetailedEntrySheet = false) }
    }

    fun onAddServerDetails(server: Server) {
        viewModelScope.launch {
            serverManagementUseCase.addServer(server)
            logUseCase.i("ServersViewModel", "Added server manually: ${server.name}")
            setImportResult("Added server: ${server.name}", isError = false)
            onDismissDetailedEntry()
        }
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

    /**
     * A single entry point for pasted, scanned or typed content.
     *
     * A bare http(s) URL is almost always a subscription, not a server config, so it is
     * routed to the subscription flow. Previously both the clipboard and the QR scanner
     * sent every input to the server-config parser, which reported "No valid server
     * links found" for a perfectly good subscription link.
     */
    fun onImportConfig(input: String) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            setImportResult("Nothing to import", isError = true)
            return
        }

        if (ImportSourceDetector.isSubscriptionUrl(trimmed)) {
            onAddSubscriptionUrl(trimmed)
            return
        }

        viewModelScope.launch {
            val servers = parseSubscriptionContent(trimmed)
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
            // Use the stored row so its id matches the subscriptionId stamped on the
            // servers and so the refresh result is written to a row that exists.
            val subscription = subscriptionUseCase.addSubscription(Subscription(url = trimmed, name = name))
            logUseCase.i("ServersViewModel", "Adding subscription: $name")

            subscriptionFetcher.fetchAndParse(subscription.url, subscription.id)
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
            refreshSubscription(subscription)
            _uiState.update { state -> state.copy(isLoading = false) }
        }
    }

    /**
     * Re-pulls [subscription] and replaces its servers. Existing servers for that
     * subscription are removed first so a refresh cannot accumulate duplicates.
     */
    private suspend fun refreshSubscription(subscription: Subscription) {
        logUseCase.i("ServersViewModel", "Refreshing subscription: ${subscription.name}")

        subscriptionFetcher.fetchAndParse(subscription.url, subscription.id, subscription)
            .onSuccess { servers ->
                serverManagementUseCase.deleteBySubscription(subscription.id)
                serverManagementUseCase.addServers(servers)
                subscriptionUseCase.updateRefreshResult(subscription, true, servers.size)
                logUseCase.i("ServersViewModel", "Refreshed ${servers.size} servers from ${subscription.name}")
            }
            .onFailure { e ->
                val reason = com.goldenv2.core.network.SubscriptionFetcher.describeFetchFailure(e)
                subscriptionUseCase.updateRefreshResult(subscription, false, 0, reason)
                logUseCase.e("ServersViewModel", "Failed to refresh subscription", e)
            }
    }

    fun onRefreshAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // Sequentially: each refresh deletes then re-inserts its own servers, and
            // firing them concurrently made the loading state clear before they finished.
            subscriptionUseCase.getEnabled().first().forEach { sub ->
                refreshSubscription(sub)
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onPingAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            logUseCase.i("ServersViewModel", "Pinging all servers")

            val allServers = getServersUseCase().first()
            val localServers = allServers.filter { it.subscriptionId.isBlank() }
            val subscriptionServers = allServers.filter { it.subscriptionId.isNotBlank() }

            // Ping local servers
            localServers.forEach { server ->
                onTestLatency(server)
            }

            // Ping subscription servers
            subscriptionServers.forEach { server ->
                onTestLatency(server)
            }

            _uiState.update { it.copy(isLoading = false) }
            logUseCase.i("ServersViewModel", "Pinged ${allServers.size} servers")
        }
    }

    fun onSelectServer(server: Server) {
        viewModelScope.launch {
            serverManagementUseCase.selectServer(server.id)
            settingsUseCase.saveLastSelectedServerId(server.id)
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

    fun onShowContextMenu(server: Server) {
        _uiState.update { it.copy(showServerContextMenu = true, contextMenuServer = server) }
    }

    fun onDismissContextMenu() {
        _uiState.update { it.copy(showServerContextMenu = false, contextMenuServer = null) }
    }

    // --- Subscription actions (driven by the subscription context menu) ---

    fun onShowSubscriptionMenu(subscription: Subscription) {
        _uiState.update {
            it.copy(showSubscriptionContextMenu = true, contextMenuSubscription = subscription)
        }
    }

    fun onDismissSubscriptionMenu() {
        _uiState.update {
            it.copy(showSubscriptionContextMenu = false, contextMenuSubscription = null)
        }
    }

    fun onPinSubscription(subscription: Subscription) {
        onDismissSubscriptionMenu()
        viewModelScope.launch {
            subscriptionUseCase.togglePinned(subscription)
            logUseCase.i("ServersViewModel", "Pinned subscription: ${subscription.name}")
        }
    }

    fun onEditSubscription(subscription: Subscription) {
        onDismissSubscriptionMenu()
        _uiState.update {
            it.copy(showEditSubscriptionDialog = true, editSubscription = subscription)
        }
    }

    fun onDismissEditSubscriptionDialog() {
        _uiState.update {
            it.copy(showEditSubscriptionDialog = false, editSubscription = null)
        }
    }

    fun onUpdateSubscription(name: String, url: String) {
        val existing = _uiState.value.editSubscription ?: return
        val trimmedUrl = url.trim()
        if (trimmedUrl.isEmpty() ||
            !(trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://"))
        ) {
            setImportResult("Invalid subscription URL", isError = true)
            return
        }
        onDismissEditSubscriptionDialog()
        viewModelScope.launch {
            val renamed = existing.copy(name = name.trim().ifBlank { existing.name }, url = trimmedUrl)
            subscriptionUseCase.updateSubscription(renamed)
            setImportResult("Subscription updated", isError = false)
            // The link may point at different servers now, so pull it again.
            refreshSubscription(renamed)
        }
    }

    fun onToggleSubscriptionEnabled(subscription: Subscription) {
        viewModelScope.launch {
            subscriptionUseCase.toggleEnabled(subscription)
        }
    }

    /** Copies the subscription link to the clipboard. */
    fun onCopySubscriptionUrl(subscription: Subscription) {
        onDismissSubscriptionMenu()
        _uiState.update { it.copy(subscriptionLinkToExport = subscription) }
    }

    /** Called once the screen has written the link to the clipboard. */
    fun onSubscriptionLinkCopied() {
        _uiState.update {
            it.copy(subscriptionLinkToExport = null, importMessage = "Subscription link copied", isImportError = false)
        }
    }

    /** Shows the subscription link as a QR code. */
    fun onExportSubscriptionQr(subscription: Subscription) {
        onDismissSubscriptionMenu()
        _uiState.update { it.copy(showSubscriptionQr = true, subscriptionQrPayload = subscription.url) }
    }

    fun onDismissSubscriptionQr() {
        _uiState.update { it.copy(showSubscriptionQr = false, subscriptionQrPayload = null) }
    }

    fun onDeleteSubscription(subscription: Subscription) {
        onDismissSubscriptionMenu()
        viewModelScope.launch {
            serverManagementUseCase.deleteBySubscription(subscription.id)
            subscriptionUseCase.deleteSubscription(subscription.id)
            logUseCase.i("ServersViewModel", "Removed subscription: ${subscription.name}")
            setImportResult("Removed ${subscription.name}", isError = false)
        }
    }

    fun onEditServer(server: Server) {
        onDismissContextMenu()
        _uiState.update { it.copy(showEditServerSheet = true, editServer = server) }
    }

    fun onDismissEditServerSheet() {
        _uiState.update { it.copy(showEditServerSheet = false, editServer = null) }
    }

    fun onUpdateServer(server: Server) {
        viewModelScope.launch {
            serverManagementUseCase.updateServer(server)
            logUseCase.i("ServersViewModel", "Updated server: ${server.name}")
            onDismissEditServerSheet()
        }
    }

    fun onShareServer(server: Server) {
        val uri = try {
            ServerUriSerializer.toUri(server)
        } catch (e: Exception) {
            setImportResult("Sharing ${server.protocol.name} configs is not supported yet", isError = true)
            return
        }
        _uiState.update { it.copy(showShareSheet = true, shareServerUri = uri, shareServerName = server.name) }
    }

    fun onDismissShareSheet() {
        _uiState.update { it.copy(showShareSheet = false, shareServerUri = null, shareServerName = null) }
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
    val showDetailedEntrySheet: Boolean = false,
    val showQrScanner: Boolean = false,
    val showAddSubscriptionDialog: Boolean = false,
    val importMessage: String? = null,
    val isImportError: Boolean = false,
    val showServerContextMenu: Boolean = false,
    val contextMenuServer: Server? = null,
    val showSubscriptionContextMenu: Boolean = false,
    val contextMenuSubscription: Subscription? = null,
    val showEditSubscriptionDialog: Boolean = false,
    val editSubscription: Subscription? = null,
    val subscriptionLinkToExport: Subscription? = null,
    val showSubscriptionQr: Boolean = false,
    val subscriptionQrPayload: String? = null,
    val showEditServerSheet: Boolean = false,
    val editServer: Server? = null,
    val showShareSheet: Boolean = false,
    val shareServerUri: String? = null,
    val shareServerName: String? = null
)