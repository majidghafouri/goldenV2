package com.goldenv2.feature.home

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldenv2.core.domain.model.VpnStatus
import com.goldenv2.core.ui.component.GoldenV2Card
import com.goldenv2.core.ui.component.GoldenV2IconButton
import com.goldenv2.core.ui.component.GoldenV2SectionHeader
import com.goldenv2.core.ui.theme.GoldenV2Theme
import kotlinx.coroutines.flow.StateFlow

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    onNavigateToServers: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToLogs: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 80.dp), // Space for bottom nav
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text(text = "GoldenV2", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            actions = {
                GoldenV2IconButton(
                    icon = androidx.compose.material.icons.default.Settings,
                    contentDescription = "Settings",
                    onClick = onNavigateToSettings
                )
            }
        )

        // Connection Status Card
        ConnectionStatusCard(
            connectionState = uiState.connectionState,
            selectedServer = uiState.selectedServer,
            onConnectClick = { viewModel.onConnectClick() },
            onServerClick = { onNavigateToServers() },
            vpnPermissionGranted = uiState.vpnPermissionGranted,
            onPermissionClick = { viewModel.requestVpnPermission() }
        )

        // Live Stats Card
        if (uiState.connectionState.isConnected) {
            LiveStatsCard(connectionState = uiState.connectionState)
        }

        // Quick Actions
        QuickActionsCard(
            onServersClick = onNavigateToServers,
            onLogsClick = onNavigateToLogs,
            onSettingsClick = onNavigateToSettings
        )

        // Current Server Info
        if (uiState.selectedServer != null && !uiState.connectionState.isConnected) {
            SelectedServerCard(server = uiState.selectedServer!!, onChangeClick = onNavigateToServers)
        }
    }
}

@Composable
fun ConnectionStatusCard(
    connectionState: com.goldenv2.core.domain.model.ConnectionState,
    selectedServer: com.goldenv2.core.domain.model.Server?,
    onConnectClick: () -> Unit,
    onServerClick: () -> Unit,
    vpnPermissionGranted: Boolean,
    onPermissionClick: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    val isConnected = connectionState.isConnected
    val isConnecting = connectionState.isConnecting
    val status = connectionState.status

    GoldenV2Card(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(
            modifier = androidx.compose.ui.Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status indicator
            Box(
                modifier = androidx.compose.ui.Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background circle
                androidx.compose.foundation.Canvas(modifier = androidx.compose.ui.Modifier.size(120.dp)) {
                    val strokeWidth = 8.dp.toPx()
                    val radius = (120.dp.toPx() - strokeWidth) / 2
                    val center = androidx.compose.ui.geometry.Offset(60.dp.toPx(), 60.dp.toPx())

                    // Track
                    drawCircle(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        radius = radius,
                        style = androidx.compose.ui.draw.Stroke(width = strokeWidth),
                        center = center
                    )

                    // Progress
                    val progress = if (isConnecting) (System.currentTimeMillis() % 2000) / 2000f else if (isConnected) 1f else 0f
                    drawArc(
                        color = getStatusColor(status),
                        startAngle = -90f,
                        sweepAngle = progress * 360f,
                        useCenter = false,
                        radius = radius,
                        style = androidx.compose.ui.draw.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                        center = center
                    )
                }

                // Center icon
                Icon(
                    imageVector = getStatusIcon(status),
                    contentDescription = null,
                    tint = getStatusColor(status),
                    modifier = androidx.compose.ui.Modifier.size(48.dp)
                )
            }

            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.padding(top = 16.dp))

            // Status text
            Text(
                text = getStatusText(status),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.padding(top = 8.dp))

            // Server name or select server button
            if (selectedServer != null) {
                Text(
                    text = selectedServer.name,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.TextOverflow.Ellipsis
                )
            } else {
                androidx.compose.material3.TextButton(onClick = onServerClick) {
                    Text(text = "Select Server", fontSize = 14.sp)
                }
            }

            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.padding(top = 24.dp))

            // Connect/Disconnect button
            if (!vpnPermissionGranted && !isConnected) {
                Button(
                    onClick = onPermissionClick,
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth().height(56.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(text = "Grant VPN Permission", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            } else {
                Button(
                    onClick = onConnectClick,
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth().height(56.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        contentColor = if (isConnected) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                    ),
                    enabled = !isConnecting
                ) {
                    Row(
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isConnecting) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = androidx.compose.ui.Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.padding(end = 12.dp))
                        }
                        Text(
                            text = if (isConnected) "Disconnect" else if (isConnecting) "Connecting…" else "Connect",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LiveStatsCard(
    connectionState: com.goldenv2.core.domain.model.ConnectionState,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    GoldenV2Card(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(
            modifier = androidx.compose.ui.Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Connection Statistics", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.padding(top = 16.dp))

            Row(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Uptime",
                    value = connectionState.formattedUptime,
                    icon = androidx.compose.material.icons.default.AccessTime
                )
                StatItem(
                    label = "↑ Upload",
                    value = connectionState.formattedUploadSpeed,
                    icon = androidx.compose.material.icons.default.ArrowUpward
                )
                StatItem(
                    label = "↓ Download",
                    value = connectionState.formattedDownloadSpeed,
                    icon = androidx.compose.material.icons.default.ArrowDownward
                )
            }

            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.padding(top = 16.dp))

            androidx.compose.foundation.layout.Divider(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.padding(top = 16.dp))

            Row(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Total ↑",
                    value = connectionState.formattedTotalUpload,
                    icon = androidx.compose.material.icons.default.CloudUpload
                )
                StatItem(
                    label = "Total ↓",
                    value = connectionState.formattedTotalDownload,
                    icon = androidx.compose.material.icons.default.CloudDownload
                )
            }
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    Column(
        modifier = modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = androidx.compose.ui.Modifier.size(24.dp))
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun QuickActionsCard(
    onServersClick: () -> Unit,
    onLogsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    GoldenV2Card(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(modifier = androidx.compose.ui.Modifier.padding(16.dp)) {
            Row(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickActionButton(icon = androidx.compose.material.icons.default.Server, label = "Servers", onClick = onServersClick)
                QuickActionButton(icon = androidx.compose.material.icons.default.Route, label = "Routing", onClick = onSettingsClick)
                QuickActionButton(icon = androidx.compose.material.icons.default.BugReport, label = "Logs", onClick = onLogsClick)
                QuickActionButton(icon = androidx.compose.material.icons.default.Settings, label = "Settings", onClick = onSettingsClick)
            }
        }
    }
}

@Composable
fun QuickActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = androidx.compose.ui.Modifier.size(28.dp))
            Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun SelectedServerCard(
    server: com.goldenv2.core.domain.model.Server,
    onChangeClick: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    GoldenV2Card(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = getProtocolIcon(server.protocol),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = androidx.compose.ui.Modifier.size(20.dp)
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.padding(end = 8.dp))
                    Text(text = server.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                }
                Text(text = "${server.address}:${server.port}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            androidx.compose.material3.TextButton(onClick = onChangeClick) {
                Text(text = "Change", fontSize = 14.sp)
            }
        }
    }
}

private fun getStatusColor(status: VpnStatus): Color {
    return when (status) {
        VpnStatus.Connected -> MaterialTheme.colorScheme.primary
        VpnStatus.Connecting, VpnStatus.Reconnecting -> MaterialTheme.colorScheme.tertiary
        VpnStatus.Error -> MaterialTheme.colorScheme.error
        VpnStatus.PermissionRequired -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }
}

private fun getStatusIcon(status: VpnStatus): ImageVector {
    return when (status) {
        VpnStatus.Connected -> androidx.compose.material.icons.default.VpnLock
        VpnStatus.Connecting, VpnStatus.Reconnecting -> androidx.compose.material.icons.default.Sync
        VpnStatus.Error -> androidx.compose.material.icons.default.Error
        VpnStatus.PermissionRequired -> androidx.compose.material.icons.default.LockOpen
        else -> androidx.compose.material.icons.default.VpnLock
    }
}

private fun getStatusText(status: VpnStatus): String {
    return when (status) {
        VpnStatus.Connected -> "Connected"
        VpnStatus.Connecting -> "Connecting…"
        VpnStatus.Reconnecting -> "Reconnecting…"
        VpnStatus.Error -> "Connection Error"
        VpnStatus.PermissionRequired -> "VPN Permission Required"
        else -> "Disconnected"
    }
}

private fun getProtocolIcon(protocol: com.goldenv2.core.domain.model.Protocol): ImageVector {
    return when (protocol) {
        com.goldenv2.core.domain.model.Protocol.VMess -> androidx.compose.material.icons.default.Cloud
        com.goldenv2.core.domain.model.Protocol.VLESS -> androidx.compose.material.icons.default.CloudQueue
        com.goldenv2.core.domain.model.Protocol.Trojan -> androidx.compose.material.icons.default.Security
        com.goldenv2.core.domain.model.Protocol.Shadowsocks -> androidx.compose.material.icons.default.Lock
        com.goldenv2.core.domain.model.Protocol.Hysteria2 -> androidx.compose.material.icons.default.FlashOn
        else -> androidx.compose.material.icons.default.Cloud
    }
}