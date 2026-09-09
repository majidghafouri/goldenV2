package com.goldenv2.feature.servers

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldenv2.core.domain.model.NetworkType
import com.goldenv2.core.domain.model.Protocol
import com.goldenv2.core.domain.model.Server
import com.goldenv2.core.domain.model.Subscription
import com.goldenv2.core.ui.component.GoldenV2Card
import com.goldenv2.core.ui.component.GoldenV2EmptyState
import com.goldenv2.core.ui.component.GoldenV2IconButton
import com.goldenv2.core.ui.component.GoldenV2SectionHeader
import com.goldenv2.core.ui.theme.GoldenV2Theme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow

@Composable
fun ServersScreen(
    viewModel: ServersViewModel,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    onNavigateToHome: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToLogs: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val localServers = uiState.servers.filter { it.subscriptionId.isBlank() }

    LaunchedEffect(uiState.importMessage) {
        uiState.importMessage?.let { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
            viewModel.onImportMessageShown()
        }
    }

    // Context menu state
    val contextMenuServer = uiState.contextMenuServer
    val showContextMenu = uiState.showServerContextMenu

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)
        ) {
            // Top App Bar
            TopAppBar(
                title = { Text(text = "Servers", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                actions = {
                    GoldenV2IconButton(
                        icon = Icons.Filled.Add,
                        contentDescription = "Add Server",
                        onClick = { viewModel.onAddServer() }
                    )
                    GoldenV2IconButton(
                        icon = Icons.Filled.CloudDownload,
                        contentDescription = "Add Subscription",
                        onClick = { viewModel.onAddSubscription() }
                    )
                    GoldenV2IconButton(
                        icon = Icons.Filled.Refresh,
                        contentDescription = "Refresh All",
                        onClick = { viewModel.onRefreshAll() }
                    )
                    GoldenV2IconButton(
                        icon = Icons.Filled.FlashOn,
                        contentDescription = "Ping All Servers",
                        onClick = { viewModel.onPingAll() }
                    )
                }
            )

            // Search Bar
            androidx.compose.material3.TextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search servers…") },
                leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "Clear")
                        }
                    }
                },
                colors = androidx.compose.material3.TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )

            // Server List
            if (uiState.isLoading) {
                androidx.compose.foundation.layout.Box(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            } else if (uiState.subscriptions.isEmpty() && localServers.isEmpty()) {
                Column(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    GoldenV2EmptyState(
                        icon = Icons.Filled.CloudOff,
                        title = "No Servers",
                        message = "Add a subscription URL, import from clipboard, or enter a server link to get started",
                        actionText = "Add Subscription",
                        onAction = { viewModel.onAddSubscription() }
                    )
                    Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                    TextButton(onClick = { viewModel.onAddServer() }) {
                        Text(text = "Add a server", fontWeight = FontWeight.Medium)
                    }
                }
            } else {
                LazyColumn(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (localServers.isNotEmpty()) {
                        item(key = "local") {
                            LocalServersSection(
                                servers = localServers,
                                onServerClick = { server -> viewModel.onSelectServer(server) },
                                onTestLatency = { server -> viewModel.onTestLatency(server) },
                                onDeleteServer = { server -> viewModel.onDeleteServer(server) },
                                onShowContextMenu = { server -> viewModel.onShowContextMenu(server) }
                            )
                        }
                    }
                    uiState.subscriptions.forEach { subscription ->
                        val servers = uiState.groupedServers[subscription] ?: emptyList()
                        if (servers.isNotEmpty() || subscription.isEnabled) {
                            item(key = subscription.id) {
                                SubscriptionSection(
                                    subscription = subscription,
                                    servers = servers,
                                    onRefresh = { viewModel.onRefreshSubscription(subscription) },
                                    onToggleEnabled = { viewModel.onToggleSubscriptionEnabled(subscription) },
                                    onDelete = { viewModel.onDeleteSubscription(subscription) },
                                    onServerClick = { server -> viewModel.onSelectServer(server) },
                                    onTestLatency = { server -> viewModel.onTestLatency(server) },
                                    onDeleteServer = { server -> viewModel.onDeleteServer(server) },
                                    onShowContextMenu = { server -> viewModel.onShowContextMenu(server) }
                                )
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = androidx.compose.ui.Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )
    }

    // Context Menu Dialog
    if (showContextMenu && contextMenuServer != null) {
        ServerContextMenu(
            server = contextMenuServer,
            onDismiss = viewModel::onDismissContextMenu,
            onTestLatency = { viewModel.onTestLatency(contextMenuServer!!); viewModel.onDismissContextMenu() },
            onEdit = { viewModel.onEditServer(contextMenuServer!!) },
            onDuplicate = { viewModel.onDuplicateServer(contextMenuServer!!); viewModel.onDismissContextMenu() },
            onDelete = { viewModel.onDeleteServer(contextMenuServer!!); viewModel.onDismissContextMenu() }
        )
    }

    if (uiState.showAddServerSheet) {
        AddServerSheet(
            onDismiss = viewModel::onDismissAddServerSheet,
            onFromClipboard = viewModel::onAddFromClipboard,
            onManualEntry = viewModel::onOpenManualEntry,
            onAddServerDetails = viewModel::onOpenDetailedEntry,
            onQrScanner = viewModel::onOpenQrScanner
        )
    }
    if (uiState.showManualEntry) {
        ManualEntrySheet(
            onDismiss = viewModel::onDismissManualEntry,
            onAdd = viewModel::onImportConfig
        )
    }
    if (uiState.showDetailedEntrySheet) {
        DetailedEntrySheet(
            onDismiss = viewModel::onDismissDetailedEntry,
            onAdd = viewModel::onAddServerDetails
        )
    }
    if (uiState.showEditServerSheet && uiState.editServer != null) {
        EditServerSheet(
            server = uiState.editServer!!,
            onDismiss = viewModel::onDismissEditServerSheet,
            onUpdate = viewModel::onUpdateServer
        )
    }
    if (uiState.showQrScanner) {
        QrScannerSheet(
            onDismiss = viewModel::onDismissQrScanner,
            onResult = viewModel::onQrScanned
        )
    }
    if (uiState.showAddSubscriptionDialog) {
        AddSubscriptionDialog(
            onDismiss = viewModel::onDismissAddSubscriptionDialog,
            onAdd = viewModel::onAddSubscriptionUrl
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddServerSheet(
    onDismiss: () -> Unit,
    onFromClipboard: (String?) -> Unit,
    onManualEntry: () -> Unit,
    onAddServerDetails: () -> Unit,
    onQrScanner: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Add Server",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = androidx.compose.ui.Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
            ListItem(
                headlineContent = { Text("From clipboard") },
                supportingContent = { Text("Paste a server link from your clipboard") },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Filled.ContentPaste,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = androidx.compose.ui.Modifier.clickable {
                    val text = clipboardManager.primaryClip
                        ?.getItemAt(0)
                        ?.coerceToText(context)
                        ?.toString()
                    onDismiss()
                    onFromClipboard(text)
                }
            )
            ListItem(
                headlineContent = { Text("Enter manually") },
                supportingContent = { Text("Type or paste server link(s), one per line") },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = androidx.compose.ui.Modifier.clickable {
                    onManualEntry()
                }
            )
            ListItem(
                headlineContent = { Text("Enter details") },
                supportingContent = { Text("Fill each field to build a server manually") },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = androidx.compose.ui.Modifier.clickable {
                    onAddServerDetails()
                }
            )
            ListItem(
                headlineContent = { Text("Scan QR code") },
                supportingContent = { Text("Scan a QR code containing a server link") },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Filled.QrCodeScanner,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = androidx.compose.ui.Modifier.clickable {
                    onQrScanner()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualEntrySheet(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var text by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Add Server Manually",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 10,
                placeholder = { Text("vless://…  vmess://…  ss://…  trojan://…  ssh://…") },
                supportingText = {
                    Text("One server link per line, or paste base64 subscription content")
                }
            )
            Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
            Button(
                onClick = {
                    onDismiss()
                    onAdd(text)
                },
                enabled = text.isNotBlank(),
                modifier = androidx.compose.ui.Modifier.fillMaxWidth()
            ) {
                Text("Add servers")
            }
            Spacer(modifier = androidx.compose.ui.Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailedEntrySheet(
    onDismiss: () -> Unit,
    onAdd: (Server) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var protocol by rememberSaveable { mutableStateOf(Protocol.VMess) }
    var address by rememberSaveable { mutableStateOf("") }
    var port by rememberSaveable { mutableStateOf("") }
    var uuid by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var alterId by rememberSaveable { mutableStateOf("0") }
    var security by rememberSaveable { mutableStateOf("auto") }
    var network by rememberSaveable { mutableStateOf("tcp") }
    var tls by rememberSaveable { mutableStateOf(false) }
    var sni by rememberSaveable { mutableStateOf("") }
    var fingerprint by rememberSaveable { mutableStateOf("") }
    var path by rememberSaveable { mutableStateOf("") }
    var host by rememberSaveable { mutableStateOf("") }
    var headerType by rememberSaveable { mutableStateOf("") }
    var flow by rememberSaveable { mutableStateOf("") }
    var publicKey by rememberSaveable { mutableStateOf("") }
    var shortId by rememberSaveable { mutableStateOf("") }
    var spiderX by rememberSaveable { mutableStateOf("") }
    var obfs by rememberSaveable { mutableStateOf("") }
    var obfsParam by rememberSaveable { mutableStateOf("") }
    var plugin by rememberSaveable { mutableStateOf("") }
    var pluginOpts by rememberSaveable { mutableStateOf("") }
    var remark by rememberSaveable { mutableStateOf("") }
    var group by rememberSaveable { mutableStateOf("") }

    val scrollState = rememberScrollState()
    val protocolEntries = Protocol.values().toList()
    var expanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Add Server Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))

            // Protocol dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = protocol.name,
                    onValueChange = {},
                    label = { Text("Protocol") },
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    singleLine = true,
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    protocolEntries.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p.name) },
                            onClick = {
                                protocol = p
                                expanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Address") },
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))

            OutlinedTextField(
                value = port,
                onValueChange = { port = it },
                label = { Text("Port") },
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))

            // Protocol-specific fields
            when (protocol) {
                Protocol.VMess -> {
                    FieldRow("UUID", uuid, { uuid = it })
                    FieldRow("Alter ID", alterId, { alterId = it }, KeyboardType.Number)
                    FieldRow("Security", security, { security = it })
                    FieldRow("Network", network, { network = it })
                    FieldRow("TLS", tls.toString(), { tls = it.toBoolean() })
                    FieldRow("SNI", sni, { sni = it })
                    FieldRow("Fingerprint", fingerprint, { fingerprint = it })
                    FieldRow("Path", path, { path = it })
                    FieldRow("Host", host, { host = it })
                    FieldRow("Header Type", headerType, { headerType = it })
                }
                Protocol.VLESS -> {
                    FieldRow("UUID", uuid, { uuid = it })
                    FieldRow("Flow", flow, { flow = it })
                    FieldRow("Network", network, { network = it })
                    FieldRow("TLS", tls.toString(), { tls = it.toBoolean() })
                    FieldRow("SNI", sni, { sni = it })
                    FieldRow("Fingerprint", fingerprint, { fingerprint = it })
                    FieldRow("Public Key", publicKey, { publicKey = it })
                    FieldRow("Short ID", shortId, { shortId = it })
                    FieldRow("SpiderX", spiderX, { spiderX = it })
                    FieldRow("Path", path, { path = it })
                    FieldRow("Host", host, { host = it })
                    FieldRow("Header Type", headerType, { headerType = it })
                }
                Protocol.Trojan -> {
                    FieldRow("Password", password, { password = it })
                    FieldRow("Network", network, { network = it })
                    FieldRow("TLS", tls.toString(), { tls = it.toBoolean() })
                    FieldRow("SNI", sni, { sni = it })
                    FieldRow("Fingerprint", fingerprint, { fingerprint = it })
                    FieldRow("Path", path, { path = it })
                    FieldRow("Host", host, { host = it })
                    FieldRow("Header Type", headerType, { headerType = it })
                }
                Protocol.Shadowsocks -> {
                    FieldRow("Password (method:pass)", password, { password = it })
                    FieldRow("Plugin", plugin, { plugin = it })
                    FieldRow("Plugin Options", pluginOpts, { pluginOpts = it })
                    FieldRow("Obfs", obfs, { obfs = it })
                    FieldRow("Obfs Param", obfsParam, { obfsParam = it })
                }
                Protocol.Hysteria2 -> {
                    FieldRow("Password", password, { password = it })
                    FieldRow("SNI", sni, { sni = it })
                    FieldRow("Obfs", obfs, { obfs = it })
                    FieldRow("Obfs Password", obfsParam, { obfsParam = it })
                }
                Protocol.Hysteria -> {
                    FieldRow("Password", password, { password = it })
                    FieldRow("SNI", sni, { sni = it })
                    FieldRow("Obfs", obfs, { obfs = it })
                    FieldRow("Obfs Password", obfsParam, { obfsParam = it })
                }
                Protocol.Tuic -> {
                    FieldRow("Password", password, { password = it })
                    FieldRow("SNI", sni, { sni = it })
                }
                Protocol.WireGuard -> {
                    FieldRow("Public Key", publicKey, { publicKey = it })
                    FieldRow("Private Key", uuid, { uuid = it })
                    FieldRow("Allowed IPs", host, { host = it })
                }
                Protocol.Ssh -> {
                    FieldRow("Username", uuid, { uuid = it })
                    FieldRow("Password", password, { password = it })
                    FieldRow("Key File", path, { path = it })
                    FieldRow("Key Passphrase", host, { host = it })
                }
            }

            Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))

            FieldRow("Remark", remark, { remark = it })
            FieldRow("Group", group, { group = it })

            Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))

            Button(
                onClick = {
                    val server = Server(
                        id = java.util.UUID.randomUUID().toString(),
                        subscriptionId = "",
                        name = name,
                        protocol = protocol,
                        address = address,
                        port = port.toIntOrNull() ?: 0,
                        uuid = uuid.takeIf { it.isNotBlank() },
                        password = password.takeIf { it.isNotBlank() },
                        alterId = alterId.toIntOrNull() ?: 0,
                        security = security,
                        network = NetworkType.entries.firstOrNull { it.name == network } ?: NetworkType.tcp,
                        tls = tls,
                        sni = sni.takeIf { it.isNotBlank() },
                        fingerprint = fingerprint.takeIf { it.isNotBlank() },
                        path = path.takeIf { it.isNotBlank() },
                        host = host.takeIf { it.isNotBlank() },
                        headerType = headerType.takeIf { it.isNotBlank() },
                        flow = flow.takeIf { it.isNotBlank() },
                        publicKey = publicKey.takeIf { it.isNotBlank() },
                        shortId = shortId.takeIf { it.isNotBlank() },
                        spiderX = spiderX.takeIf { it.isNotBlank() },
                        obfs = obfs.takeIf { it.isNotBlank() },
                        obfsParam = obfsParam.takeIf { it.isNotBlank() },
                        plugin = plugin.takeIf { it.isNotBlank() },
                        pluginOpts = pluginOpts.takeIf { it.isNotBlank() },
                        remark = remark.takeIf { it.isNotBlank() },
                        group = group.takeIf { it.isNotBlank() }
                    )
                    onDismiss()
                    onAdd(server)
                },
                enabled = name.isNotBlank() && address.isNotBlank() && port.isNotBlank(),
                modifier = androidx.compose.ui.Modifier.fillMaxWidth()
            ) {
                Text("Add Server")
            }
            Spacer(modifier = androidx.compose.ui.Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditServerSheet(
    server: Server,
    onDismiss: () -> Unit,
    onUpdate: (Server) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(server.name) }
    var protocol by rememberSaveable { mutableStateOf(server.protocol) }
    var address by rememberSaveable { mutableStateOf(server.address) }
    var port by rememberSaveable { mutableStateOf(server.port.toString()) }
    var uuid by rememberSaveable { mutableStateOf(server.uuid.orEmpty()) }
    var password by rememberSaveable { mutableStateOf(server.password.orEmpty()) }
    var alterId by rememberSaveable { mutableStateOf(server.alterId.toString()) }
    var security by rememberSaveable { mutableStateOf(server.security) }
    var network by rememberSaveable { mutableStateOf(server.network) }
    var tls by rememberSaveable { mutableStateOf(server.tls) }
    var sni by rememberSaveable { mutableStateOf(server.sni.orEmpty()) }
    var fingerprint by rememberSaveable { mutableStateOf(server.fingerprint.orEmpty()) }
    var path by rememberSaveable { mutableStateOf(server.path.orEmpty()) }
    var host by rememberSaveable { mutableStateOf(server.host.orEmpty()) }
    var headerType by rememberSaveable { mutableStateOf(server.headerType.orEmpty()) }
    var flow by rememberSaveable { mutableStateOf(server.flow.orEmpty()) }
    var publicKey by rememberSaveable { mutableStateOf(server.publicKey.orEmpty()) }
    var shortId by rememberSaveable { mutableStateOf(server.shortId.orEmpty()) }
    var spiderX by rememberSaveable { mutableStateOf(server.spiderX.orEmpty()) }
    var obfs by rememberSaveable { mutableStateOf(server.obfs.orEmpty()) }
    var obfsParam by rememberSaveable { mutableStateOf(server.obfsParam.orEmpty()) }
    var plugin by rememberSaveable { mutableStateOf(server.plugin.orEmpty()) }
    var pluginOpts by rememberSaveable { mutableStateOf(server.pluginOpts.orEmpty()) }
    var remark by rememberSaveable { mutableStateOf(server.remark.orEmpty()) }
    var group by rememberSaveable { mutableStateOf(server.group.orEmpty()) }

    val protocolEntries = Protocol.values().toList()
    var expanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Edit Server",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))

            androidx.compose.foundation.layout.Column(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))

                // Protocol dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = protocol.name,
                        onValueChange = {},
                        label = { Text("Protocol") },
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        singleLine = true,
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        protocolEntries.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.name) },
                                onClick = {
                                    protocol = p
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))

                // Address
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))

                // Port
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Port") },
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))

                // Protocol-specific fields
                when (protocol) {
                    Protocol.VMess -> {
                        FieldRow("UUID", uuid, { uuid = it })
                        FieldRow("Alter ID", alterId, { alterId = it }, KeyboardType.Number)
                        FieldRow("Security", security)
                        FieldRow("Network", network.name)
                        FieldRow("TLS", tls.toString())
                        FieldRow("SNI", sni)
                        FieldRow("Fingerprint", fingerprint)
                        FieldRow("Path", path)
                        FieldRow("Host", host)
                        FieldRow("Header Type", headerType)
                    }
                    Protocol.VLESS -> {
                        FieldRow("UUID", uuid)
                        FieldRow("Flow", flow)
                        FieldRow("Network", network.name)
                        FieldRow("TLS", tls.toString())
                        FieldRow("SNI", sni)
                        FieldRow("Fingerprint", fingerprint)
                        FieldRow("Public Key", publicKey)
                        FieldRow("Short ID", shortId)
                        FieldRow("SpiderX", spiderX)
                        FieldRow("Path", path)
                        FieldRow("Host", host)
                        FieldRow("Header Type", headerType)
                    }
                    Protocol.Trojan -> {
                        FieldRow("Password", password)
                        FieldRow("Network", network.name)
                        FieldRow("TLS", tls.toString())
                        FieldRow("SNI", sni)
                        FieldRow("Fingerprint", fingerprint)
                        FieldRow("Path", path)
                        FieldRow("Host", host)
                        FieldRow("Header Type", headerType)
                    }
                    Protocol.Shadowsocks -> {
                        FieldRow("Password (method:pass)", password)
                        FieldRow("Plugin", plugin)
                        FieldRow("Plugin Options", pluginOpts)
                        FieldRow("Obfs", obfs)
                        FieldRow("Obfs Param", obfsParam)
                    }
                    Protocol.Hysteria2 -> {
                        FieldRow("Password", password)
                        FieldRow("SNI", sni)
                        FieldRow("Obfs", obfs)
                        FieldRow("Obfs Password", obfsParam)
                    }
                    Protocol.Hysteria -> {
                        FieldRow("Password", password)
                        FieldRow("SNI", sni)
                        FieldRow("Obfs", obfs)
                        FieldRow("Obfs Password", obfsParam)
                    }
                    Protocol.Tuic -> {
                        FieldRow("Password", password)
                        FieldRow("SNI", sni)
                    }
                    Protocol.WireGuard -> {
                        FieldRow("Public Key", publicKey)
                        FieldRow("Private Key", uuid)
                        FieldRow("Endpoint", address)
                        FieldRow("Allowed IPs", host)
                    }
                    Protocol.Ssh -> {
                        FieldRow("Username", uuid)
                        FieldRow("Password", password)
                        FieldRow("Key File", path)
                        FieldRow("Key Passphrase", host)
                    }
                }

                Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))

                // Common fields
                FieldRow("Remark", remark)
                FieldRow("Group", group)

                Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    val updatedServer = server.copy(
                        name = name,
                        protocol = protocol,
                        address = address,
                        port = port.toIntOrNull() ?: server.port,
                        uuid = uuid.takeIf { it.isNotBlank() },
                        password = password.takeIf { it.isNotBlank() },
                        alterId = alterId.toIntOrNull() ?: server.alterId,
                        security = security,
                        network = network,
                        tls = tls,
                        sni = sni.takeIf { it.isNotBlank() },
                        fingerprint = fingerprint.takeIf { it.isNotBlank() },
                        path = path.takeIf { it.isNotBlank() },
                        host = host.takeIf { it.isNotBlank() },
                        headerType = headerType.takeIf { it.isNotBlank() },
                        flow = flow.takeIf { it.isNotBlank() },
                        publicKey = publicKey.takeIf { it.isNotBlank() },
                        shortId = shortId.takeIf { it.isNotBlank() },
                        spiderX = spiderX.takeIf { it.isNotBlank() },
                        obfs = obfs.takeIf { it.isNotBlank() },
                        obfsParam = obfsParam.takeIf { it.isNotBlank() },
                        plugin = plugin.takeIf { it.isNotBlank() },
                        pluginOpts = pluginOpts.takeIf { it.isNotBlank() },
                        remark = remark.takeIf { it.isNotBlank() },
                        group = group.takeIf { it.isNotBlank() },
                        updatedAt = java.time.Instant.now()
                    )
                    onDismiss()
                    onUpdate(updatedServer)
                },
                enabled = name.isNotBlank() && address.isNotBlank() && port.isNotBlank(),
                modifier = androidx.compose.ui.Modifier.fillMaxWidth()
            ) {
                Text("Save Changes")
            }
            Spacer(modifier = androidx.compose.ui.Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit = {},
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )
    Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QrScannerSheet(
    onDismiss: () -> Unit,
    onResult: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        QrScannerContent(
            onResult = onResult,
            onClose = onDismiss
        )
    }
}

@Composable
private fun AddSubscriptionDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var url by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Subscription") },
        text = {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                placeholder = { Text("https://example.com/subscribe") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = androidx.compose.ui.Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onAdd(url)
                },
                enabled = url.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun LocalServersSection(
    servers: List<Server>,
    onServerClick: (Server) -> Unit,
    onTestLatency: (Server) -> Unit,
    onDeleteServer: (Server) -> Unit,
    onShowContextMenu: (Server) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Column(modifier = androidx.compose.ui.Modifier.fillMaxWidth()) {
        Card(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            onClick = { expanded = !expanded }
        ) {
            Row(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = androidx.compose.ui.Modifier.size(24.dp).padding(end = 12.dp)
                    )
                    Column {
                        Text(
                            text = "Local",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${servers.size} servers • No subscription",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = androidx.compose.ui.Modifier.size(24.dp)
                )
            }
        }

        if (expanded) {
            Column(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                servers.forEach { server ->
                    ServerItem(
                        server = server,
                        onClick = { onServerClick(server) },
                        onLongClick = { onShowContextMenu(server) },
                        onTestLatency = { onTestLatency(server) },
                        onDelete = { onDeleteServer(server) }
                    )
                }
            }
        }
    }
}

@Composable
fun SubscriptionSection(
    subscription: Subscription,
    servers: List<Server>,
    onRefresh: () -> Unit,
    onToggleEnabled: () -> Unit,
    onDelete: () -> Unit,
    onServerClick: (Server) -> Unit,
    onTestLatency: (Server) -> Unit,
    onDeleteServer: (Server) -> Unit,
    onShowContextMenu: (Server) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Column(modifier = androidx.compose.ui.Modifier.fillMaxWidth()) {
        // Subscription Header
        Card(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            onClick = { expanded = !expanded }
        ) {
            Row(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = androidx.compose.ui.Modifier.size(24.dp).padding(end = 12.dp)
                    )
                    Column {
                        Text(text = subscription.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = "${servers.size} servers • ${if (subscription.lastRefreshSuccess) "Updated" else "Failed"}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Switch(
                        checked = subscription.isEnabled,
                        onCheckedChange = { onToggleEnabled() },
                        modifier = androidx.compose.ui.Modifier.padding(end = 8.dp)
                    )
                    GoldenV2IconButton(
                        icon = Icons.Filled.Refresh,
                        contentDescription = "Refresh",
                        onClick = onRefresh
                    )
                    GoldenV2IconButton(
                        icon = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        onClick = onDelete
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = androidx.compose.ui.Modifier.size(24.dp)
                    )
                }
            }
        }

        // Server List
        if (expanded) {
            if (servers.isEmpty()) {
                Card(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    androidx.compose.foundation.layout.Box(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "No servers in this subscription", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Column(
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    servers.forEach { server ->
                        ServerItem(
                            server = server,
                            onClick = { onServerClick(server) },
                            onLongClick = { onShowContextMenu(server) },
                            onTestLatency = { onTestLatency(server) },
                            onDelete = { onDeleteServer(server) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ServerItem(
    server: Server,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onTestLatency: () -> Unit,
    onDelete: () -> Unit
) {
    val selected = server.isSelected
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                false
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .background(Color(0xFFB3261E), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = androidx.compose.ui.Modifier.padding(end = 24.dp)
                )
            }
        }
    ) {
        Card(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            colors = CardDefaults.cardColors(
                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = getProtocolIcon(server.protocol),
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                        modifier = androidx.compose.ui.Modifier.size(20.dp).padding(end = 12.dp)
                    )
                    Column {
                        Text(
                            text = server.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${server.address}:${server.port}",
                            fontSize = 12.sp,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (server.latency > 0) {
                        Text(
                            text = "${server.latency}ms",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = getLatencyColor(server.latency)
                        )
                    } else {
                        androidx.compose.material3.TextButton(onClick = onTestLatency) {
                            Text(text = "Test", fontSize = 12.sp)
                        }
                    }
                    if (selected) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = androidx.compose.ui.Modifier.size(20.dp).padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerContextMenu(
    server: Server,
    onDismiss: () -> Unit,
    onTestLatency: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = server.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
            Text(text = "${server.protocol.name} • ${server.address}:${server.port}")
            if (server.latency > 0) {
                Text(text = "Latency: ${server.latency}ms")
            }
            Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ListItem(
                    headlineContent = { Text("Test Latency") },
                    leadingContent = { Icon(Icons.Filled.FlashOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .clickable { onTestLatency() }
                )
                ListItem(
                    headlineContent = { Text("Edit") },
                    leadingContent = { Icon(Icons.Filled.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .clickable { onEdit() }
                )
                ListItem(
                    headlineContent = { Text("Duplicate") },
                    leadingContent = { Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .clickable { onDuplicate() }
                )
                ListItem(
                    headlineContent = { Text("Delete") },
                    leadingContent = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .clickable { onDelete() }
                )
            }
            Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    }
}

private fun getProtocolIcon(protocol: Protocol): ImageVector {
    return when (protocol) {
        Protocol.VMess -> Icons.Filled.Cloud
        Protocol.VLESS -> Icons.Filled.CloudQueue
        Protocol.Trojan -> Icons.Filled.Security
        Protocol.Shadowsocks -> Icons.Filled.Lock
        Protocol.Hysteria2 -> Icons.Filled.FlashOn
        Protocol.Ssh -> Icons.Filled.Security
        else -> Icons.Filled.Cloud
    }
}

private fun getLatencyColor(latency: Long): Color {
    return when {
        latency < 50 -> Color.Green
        latency < 100 -> Color.Yellow
        latency < 200 -> Color(0xFFFF9800)
        else -> Color.Red
    }
}