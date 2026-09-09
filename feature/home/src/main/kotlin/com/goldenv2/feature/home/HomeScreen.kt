package com.goldenv2.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
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
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Handle VPN permission request from ViewModel
    LaunchedEffect(viewModel.vpnPermissionRequest) {
        viewModel.vpnPermissionRequest.collect { intent ->
            intent?.let {
                context.startActivity(it)
                // Reset to avoid re-launching on recomposition
                viewModel.onVpnPermissionHandled()
            }
        }
    }

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
                    icon = Icons.Filled.Settings,
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
            onServerClick = { onNavigateToServers() }
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
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    val isConnected = connectionState.isConnected
    val isConnecting = connectionState.isConnecting
    val status = connectionState.status

    val colorScheme = MaterialTheme.colorScheme
    val statusColor = getStatusColor(status)

    val onCircleClick: () -> Unit = {
        if (selectedServer == null && !isConnected) onServerClick() else onConnectClick()
    }

    GoldenV2Card(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(
            modifier = androidx.compose.ui.Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = androidx.compose.ui.Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onCircleClick),
                contentAlignment = Alignment.Center
            ) {
                // Background circle
                androidx.compose.foundation.Canvas(modifier = androidx.compose.ui.Modifier.size(120.dp)) {
                    val strokeWidth = 8.dp.toPx()
                    val radius = (120.dp.toPx() - strokeWidth) / 2
                    val center = androidx.compose.ui.geometry.Offset(60.dp.toPx(), 60.dp.toPx())

                    // Track
                    drawCircle(
                        color = colorScheme.surfaceVariant,
                        radius = radius,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth),
                        center = center
                    )

                    // Progress
                    val progress = if (isConnecting) (System.currentTimeMillis() % 2000) / 2000f else if (isConnected) 1f else 0f
                    drawArc(
                        color = statusColor,
                        startAngle = -90f,
                        sweepAngle = progress * 360f,
                        useCenter = false,
                        topLeft = center - androidx.compose.ui.geometry.Offset(radius, radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                }

                // Center icon
                Icon(
                    imageVector = getStatusIcon(status),
                    contentDescription = null,
                    tint = statusColor,
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
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            } else {
                androidx.compose.material3.TextButton(onClick = onServerClick) {
                    Text(text = "Select Server", fontSize = 14.sp)
                }
            }

            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.padding(top = 24.dp))

            Button(
                    onClick = {
                        if (selectedServer == null && !isConnected) onServerClick() else onConnectClick()
                    },
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
                    icon = Icons.Filled.AccessTime,
                    modifier = androidx.compose.ui.Modifier.weight(1f)
                )
                StatItem(
                    label = "↑ Upload",
                    value = connectionState.formattedUploadSpeed,
                    icon = Icons.Filled.ArrowUpward,
                    modifier = androidx.compose.ui.Modifier.weight(1f)
                )
                StatItem(
                    label = "↓ Download",
                    value = connectionState.formattedDownloadSpeed,
                    icon = Icons.Filled.ArrowDownward,
                    modifier = androidx.compose.ui.Modifier.weight(1f)
                )
            }

            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.padding(top = 16.dp))

            androidx.compose.material3.HorizontalDivider(
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
                    icon = Icons.Filled.CloudUpload,
                    modifier = androidx.compose.ui.Modifier.weight(1f)
                )
                StatItem(
                    label = "Total ↓",
                    value = connectionState.formattedTotalDownload,
                    icon = Icons.Filled.CloudDownload,
                    modifier = androidx.compose.ui.Modifier.weight(1f)
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
        modifier = modifier,
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
                QuickActionButton(icon = Icons.Filled.Dns, label = "Servers", onClick = onServersClick)
                QuickActionButton(icon = Icons.Filled.Route, label = "Routing", onClick = onSettingsClick)
                QuickActionButton(icon = Icons.Filled.BugReport, label = "Logs", onClick = onLogsClick)
                QuickActionButton(icon = Icons.Filled.Settings, label = "Settings", onClick = onSettingsClick)
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

@Composable
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
        VpnStatus.Connected -> Icons.Filled.VpnLock
        VpnStatus.Connecting, VpnStatus.Reconnecting -> Icons.Filled.Sync
        VpnStatus.Error -> Icons.Filled.Error
        VpnStatus.PermissionRequired -> Icons.Filled.LockOpen
        else -> Icons.Filled.VpnLock
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
        com.goldenv2.core.domain.model.Protocol.VMess -> Icons.Filled.Cloud
        com.goldenv2.core.domain.model.Protocol.VLESS -> Icons.Filled.CloudQueue
        com.goldenv2.core.domain.model.Protocol.Trojan -> Icons.Filled.Security
        com.goldenv2.core.domain.model.Protocol.Shadowsocks -> Icons.Filled.Lock
        com.goldenv2.core.domain.model.Protocol.Hysteria2 -> Icons.Filled.FlashOn
        else -> Icons.Filled.Cloud
    }
}