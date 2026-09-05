package com.goldenv2.feature.servers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldenv2.core.domain.model.Protocol
import com.goldenv2.core.domain.model.Server
import com.goldenv2.core.domain.model.Subscription
import com.goldenv2.core.ui.component.GoldenV2Card
import com.goldenv2.core.ui.component.GoldenV2EmptyState
import com.goldenv2.core.ui.component.GoldenV2IconButton
import com.goldenv2.core.ui.component.GoldenV2SectionHeader
import com.goldenv2.core.ui.theme.GoldenV2Theme
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

    Column(
        modifier = modifier
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
        } else if (uiState.subscriptions.isEmpty()) {
            GoldenV2EmptyState(
                icon = Icons.Filled.CloudOff,
                title = "No Subscriptions",
                message = "Add a subscription URL or manually add servers to get started",
                actionText = "Add Subscription",
                onAction = { viewModel.onAddSubscription() }
            )
        } else {
            LazyColumn(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                                onServerLongClick = { server -> showServerContextMenu(server) },
                                onTestLatency = { server -> viewModel.onTestLatency(server) },
                                onDeleteServer = { server -> viewModel.onDeleteServer(server) },
                                onDuplicateServer = { server -> viewModel.onDuplicateServer(server) }
                            )
                        }
                    }
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
    onServerLongClick: (Server) -> Unit,
    onTestLatency: (Server) -> Unit,
    onDeleteServer: (Server) -> Unit,
    onDuplicateServer: (Server) -> Unit
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
                LazyColumn(
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(servers) { server ->
                        ServerItem(
                            server = server,
                            onClick = { onServerClick(server) },
                            onLongClick = { onServerLongClick(server) },
                            onTestLatency = { onTestLatency(server) },
                            onDelete = { onDeleteServer(server) },
                            onDuplicate = { onDuplicateServer(server) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ServerItem(
    server: Server,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onTestLatency: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit
) {
    val selected = server.isSelected

    Card(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(8.dp),
        onClick = onClick
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

fun showServerContextMenu(server: Server) {
    // TODO: Implement context menu
}

private fun getProtocolIcon(protocol: Protocol): ImageVector {
    return when (protocol) {
        Protocol.VMess -> Icons.Filled.Cloud
        Protocol.VLESS -> Icons.Filled.CloudQueue
        Protocol.Trojan -> Icons.Filled.Security
        Protocol.Shadowsocks -> Icons.Filled.Lock
        Protocol.Hysteria2 -> Icons.Filled.FlashOn
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