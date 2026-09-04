package com.goldenv2.feature.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioGroup
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldenv2.core.domain.model.DnsStrategy
import com.goldenv2.core.domain.model.LogLevel
import com.goldenv2.core.domain.model.ThemeMode
import com.goldenv2.core.domain.model.VpnMode
import com.goldenv2.core.ui.component.GoldenV2Card
import com.goldenv2.core.ui.component.GoldenV2IconButton
import com.goldenv2.core.ui.component.GoldenV2SectionHeader
import com.goldenv2.core.ui.theme.GoldenV2Theme
import kotlinx.coroutines.flow.StateFlow

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    onNavigateToHome: () -> Unit,
    onNavigateToServers: () -> Unit,
    onNavigateToRouting: () -> Unit,
    onNavigateToLogs: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.settings

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 80.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top App Bar (using item with custom layout)
        item {
            TopAppBar(
                title = { Text(text = "Settings", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
        }

        // DNS Settings
        item {
            DnsSettingsSection(settings = settings, viewModel = viewModel)
        }

        // Local Proxy Settings
        item {
            LocalProxySection(settings = settings, viewModel = viewModel)
        }

        // Connection Settings
        item {
            ConnectionSettingsSection(settings = settings, viewModel = viewModel)
        }

        // UI Settings
        item {
            UiSettingsSection(settings = settings, viewModel = viewModel)
        }

        // Subscription Settings
        item {
            SubscriptionSettingsSection(settings = settings, viewModel = viewModel)
        }

        // Logging Settings
        item {
            LoggingSettingsSection(settings = settings, viewModel = viewModel)
        }

        // Advanced Settings
        item {
            AdvancedSettingsSection(settings = settings, viewModel = viewModel)
        }

        // Import/Export
        item {
            ImportExportSection(viewModel = viewModel)
        }
    }
}

@Composable
fun DnsSettingsSection(settings: com.goldenv2.core.domain.model.AppSettings, viewModel: SettingsViewModel) {
    GoldenV2SectionHeader(title = "DNS Settings")

    GoldenV2Card(modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(modifier = androidx.compose.ui.Modifier.padding(16.dp)) {
            // DNS Strategy
            SettingRow(
                title = "DNS Strategy",
                subtitle = "How to resolve domain names"
            ) {
                DropdownSelector(
                    selected = settings.dnsStrategy,
                    options = DnsStrategy.values().map { it.name },
                    onSelected = { viewModel.onDnsStrategyChanged(DnsStrategy.valueOf(it)) }
                )
            }

            // Custom DNS Servers
            if (settings.dnsStrategy == DnsStrategy.UseCustom) {
                SettingRow(
                    title = "Custom DNS Servers",
                    subtitle = "One per line"
                ) {
                    OutlinedTextField(
                        value = settings.dnsServers.joinToString("\n"),
                        onValueChange = { viewModel.onDnsServersChanged(it.lines().map { it.trim() }.filter { it.isNotBlank() }.toList()) },
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth().height(100.dp),
                        keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Text),
                        visualTransformation = androidx.compose.ui.text.input.VisualTransformation.None
                    )
                }
            }

            // Fake DNS
            SettingRow(
                title = "Fake DNS",
                subtitle = "Use fake IP for domain resolution"
            ) {
                Switch(
                    checked = settings.fakeDnsEnabled,
                    onCheckedChange = { viewModel.onFakeDnsChanged(it) }
                )
            }

            if (settings.fakeDnsEnabled) {
                SettingRow(
                    title = "Fake DNS IP Range",
                    subtitle = "CIDR notation for fake IP pool"
                ) {
                    OutlinedTextField(
                        value = settings.fakeDnsIpRange,
                        onValueChange = { viewModel.onFakeDnsRangeChanged(it) },
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth().width(200.dp),
                        singleLine = true
                    )
                }
            }
        }
    }
}

@Composable
fun LocalProxySection(settings: com.goldenv2.core.domain.model.AppSettings, viewModel: SettingsViewModel) {
    GoldenV2SectionHeader(title = "Local Proxy")

    GoldenV2Card(modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(modifier = androidx.compose.ui.Modifier.padding(16.dp)) {
            SettingRow(
                title = "Allow Local Proxy",
                subtitle = "Enable SOCKS/HTTP proxy on localhost"
            ) {
                Switch(
                    checked = settings.allowLocalProxy,
                    onCheckedChange = { viewModel.onAllowLocalProxyChanged(it) }
                )
            }

            if (settings.allowLocalProxy) {
                SettingRow(
                    title = "SOCKS Port",
                    subtitle = "Local SOCKS5 proxy port"
                ) {
                    OutlinedTextField(
                        value = settings.localSocksPort.toString(),
                        onValueChange = { viewModel.onLocalSocksPortChanged(it.toIntOrNull() ?: 10808) },
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth().width(150.dp),
                        singleLine = true,
                        keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                }

                SettingRow(
                    title = "HTTP Port",
                    subtitle = "Local HTTP proxy port"
                ) {
                    OutlinedTextField(
                        value = settings.localHttpPort.toString(),
                        onValueChange = { viewModel.onLocalHttpPortChanged(it.toIntOrNull() ?: 10809) },
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth().width(150.dp),
                        singleLine = true,
                        keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectionSettingsSection(settings: com.goldenv2.core.domain.model.AppSettings, viewModel: SettingsViewModel) {
    GoldenV2SectionHeader(title = "Connection")

    GoldenV2Card(modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(modifier = androidx.compose.ui.Modifier.padding(16.dp)) {
            SettingRow(
                title = "VPN Mode",
                subtitle = "Full tunnel (system-wide) or local proxy only"
            ) {
                DropdownSelector(
                    selected = settings.vpnMode,
                    options = VpnMode.values().map { it.name },
                    onSelected = { viewModel.onVpnModeChanged(VpnMode.valueOf(it)) }
                )
            }

            SettingRow(
                title = "Auto Reconnect",
                subtitle = "Automatically reconnect on network change"
            ) {
                Switch(
                    checked = settings.autoReconnect,
                    onCheckedChange = { viewModel.onAutoReconnectChanged(it) }
                )
            }

            SettingRow(
                title = "Kill Switch",
                subtitle = "Block all traffic if VPN drops unexpectedly"
            ) {
                Switch(
                    checked = settings.killSwitchEnabled,
                    onCheckedChange = { viewModel.onKillSwitchChanged(it) }
                )
            }

            SettingRow(
                title = "Auto Connect on Boot",
                subtitle = "Start VPN automatically when device boots"
            ) {
                Switch(
                    checked = settings.autoConnectOnBoot,
                    onCheckedChange = { viewModel.onAutoConnectOnBootChanged(it) }
                )
            }

            SettingRow(
                title = "MTU",
                subtitle = "Maximum transmission unit"
            ) {
                OutlinedTextField(
                    value = settings.mtu.toString(),
                    onValueChange = { viewModel.onMtuChanged(it.toIntOrNull() ?: 1500) },
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth().width(150.dp),
                    singleLine = true,
                    keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
            }
        }
    }
}

@Composable
fun UiSettingsSection(settings: com.goldenv2.core.domain.model.AppSettings, viewModel: SettingsViewModel) {
    GoldenV2SectionHeader(title = "Appearance")

    GoldenV2Card(modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(modifier = androidx.compose.ui.Modifier.padding(16.dp)) {
            SettingRow(
                title = "Theme",
                subtitle = "Light, dark, or follow system"
            ) {
                DropdownSelector(
                    selected = settings.theme,
                    options = ThemeMode.values().map { it.name },
                    onSelected = { viewModel.onThemeChanged(ThemeMode.valueOf(it)) }
                )
            }

            SettingRow(
                title = "Language",
                subtitle = "App language"
            ) {
                DropdownSelector(
                    selected = settings.language,
                    options = listOf("en", "zh", "es", "fr", "de", "ja", "ko", "ru"),
                    onSelected = { viewModel.onLanguageChanged(it) }
                )
            }

            SettingRow(
                title = "Show Speed in Notification",
                subtitle = "Display upload/download speed in persistent notification"
            ) {
                Switch(
                    checked = settings.showSpeedInNotification,
                    onCheckedChange = { viewModel.onShowSpeedInNotificationChanged(it) }
                )
            }

            SettingRow(
                title = "Show Notification",
                subtitle = "Display persistent VPN notification"
            ) {
                Switch(
                    checked = settings.showNotification,
                    onCheckedChange = { viewModel.onShowNotificationChanged(it) }
                )
            }
        }
    }
}

@Composable
fun SubscriptionSettingsSection(settings: com.goldenv2.core.domain.model.AppSettings, viewModel: SettingsViewModel) {
    GoldenV2SectionHeader(title = "Subscription")

    GoldenV2Card(modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(modifier = androidx.compose.ui.Modifier.padding(16.dp)) {
            SettingRow(
                title = "Auto Refresh",
                subtitle = "Automatically refresh subscriptions"
            ) {
                Switch(
                    checked = settings.subscriptionAutoRefresh,
                    onCheckedChange = { viewModel.onSubscriptionAutoRefreshChanged(it) }
                )
            }

            SettingRow(
                title = "Refresh Interval (hours)",
                subtitle = "How often to check for subscription updates"
            ) {
                OutlinedTextField(
                    value = settings.subscriptionRefreshIntervalHours.toString(),
                    onValueChange = { viewModel.onSubscriptionRefreshIntervalChanged(it.toIntOrNull() ?: 24) },
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth().width(150.dp),
                    singleLine = true,
                    keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
            }
        }
    }
}

@Composable
fun LoggingSettingsSection(settings: com.goldenv2.core.domain.model.AppSettings, viewModel: SettingsViewModel) {
    GoldenV2SectionHeader(title = "Logging")

    GoldenV2Card(modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(modifier = androidx.compose.ui.Modifier.padding(16.dp)) {
            SettingRow(
                title = "Log Level",
                subtitle = "Minimum log level to record"
            ) {
                DropdownSelector(
                    selected = settings.logLevel,
                    options = LogLevel.values().map { it.name },
                    onSelected = { viewModel.onLogLevelChanged(LogLevel.valueOf(it)) }
                )
            }

            SettingRow(
                title = "Max Log Entries",
                subtitle = "Maximum number of log entries to keep"
            ) {
                OutlinedTextField(
                    value = settings.maxLogEntries.toString(),
                    onValueChange = { viewModel.onMaxLogEntriesChanged(it.toIntOrNull() ?: 1000) },
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth().width(150.dp),
                    singleLine = true,
                    keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
            }

            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.padding(top = 8.dp))

            Row(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = { viewModel.onClearLogs() }) {
                    Text("Clear Logs")
                }
            }
        }
    }
}

@Composable
fun AdvancedSettingsSection(settings: com.goldenv2.core.domain.model.AppSettings, viewModel: SettingsViewModel) {
    GoldenV2SectionHeader(title = "Advanced")

    GoldenV2Card(modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(modifier = androidx.compose.ui.Modifier.padding(16.dp)) {
            SettingRow(
                title = "Bypass Package Names",
                subtitle = "Apps that bypass VPN (comma separated)"
            ) {
                OutlinedTextField(
                    value = settings.bypassPackageNames.joinToString(","),
                    onValueChange = { viewModel.onBypassPackageNamesChanged(it.split(",").map { it.trim() }.filter { it.isNotBlank() }.toList()) },
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            SettingRow(
                title = "Bypass UIDs",
                subtitle = "User IDs that bypass VPN (comma separated)"
            ) {
                OutlinedTextField(
                    value = settings.bypassUids.joinToString(","),
                    onValueChange = { viewModel.onBypassUidsChanged(it.split(",").map { it.trim().toIntOrNull() }.filterNotNull().toList()) },
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
            }
        }
    }
}

@Composable
fun ImportExportSection(viewModel: SettingsViewModel) {
    GoldenV2SectionHeader(title = "Configuration")

    GoldenV2Card(modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(modifier = androidx.compose.ui.Modifier.padding(16.dp)) {
            Row(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = {
                    val json = viewModel.onExportConfig()
                    android.content.ClipboardManager.fromContext(androidx.compose.ui.platform.LocalContext.current).setText(json)
                    // TODO: Show toast "Copied to clipboard"
                }) {
                    Text("Export Config")
                }
                OutlinedButton(onClick = {
                    // TODO: Show import dialog
                }) {
                    Text("Import Config")
                }
            }
        }
    }
}

@Composable
fun SettingRow(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            subtitle?.let {
                Text(text = it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        content()
    }
}

@Composable
fun DropdownSelector(
    selected: Enum<*>,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val selectedText = selected.name

    androidx.compose.material3.MenuAnchor { anchor ->
        OutlinedButton(
            onClick = { anchor.open() },
            modifier = androidx.compose.ui.Modifier.fillMaxWidth().width(200.dp),
            content = {
                Row(
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = selectedText, fontSize = 14.sp)
                    Icon(
                        imageVector = androidx.compose.material.icons.default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
        androidx.compose.material3.DropdownMenu(
            expanded = anchor.isOpen,
            onDismissRequest = { anchor.close() }
        ) {
            options.forEach { option ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(text = option) },
                    onClick = {
                        onSelected(option)
                        anchor.close()
                    },
                    selected = option == selectedText
                )
            }
        }
    }
}