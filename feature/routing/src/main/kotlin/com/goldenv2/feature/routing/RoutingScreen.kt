package com.goldenv2.feature.routing

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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldenv2.core.domain.model.DomainStrategy
import com.goldenv2.core.domain.model.RoutingRule
import com.goldenv2.core.ui.component.GoldenV2Card
import com.goldenv2.core.ui.component.GoldenV2IconButton
import com.goldenv2.core.ui.component.GoldenV2SectionHeader
import com.goldenv2.core.ui.theme.GoldenV2Theme
import kotlinx.coroutines.flow.StateFlow

@Composable
fun RoutingScreen(
    viewModel: RoutingViewModel,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    onNavigateToHome: () -> Unit,
    onNavigateToServers: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToLogs: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 80.dp)
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text(text = "Routing", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            actions = {
                GoldenV2IconButton(
                    icon = Icons.Filled.ContentCopy,
                    contentDescription = "Export Config",
                    onClick = { /* viewModel.onExportConfig() */ }
                )
                GoldenV2IconButton(
                    icon = Icons.Filled.ContentPaste,
                    contentDescription = "Import Config",
                    onClick = { /* viewModel.onImportConfig("") */ }
                )
            }
        )

        // Preset Routing Modes
        RoutingModeSelector(
            selectedMode = uiState.selectedMode,
            onModeChanged = { viewModel.onRoutingModeChanged(it) }
        )

        // Domain Strategy
        DomainStrategySelector(
            strategy = uiState.routingConfig.domainStrategy,
            onStrategyChanged = { viewModel.onDomainStrategyChanged(it) }
        )

        // Rules List
        RulesList(
            rules = uiState.routingConfig.rules,
            editingRuleId = uiState.editingRuleId,
            onAddRule = { viewModel.onAddRule() },
            onDeleteRule = { viewModel.onDeleteRule(it) },
            onEditRule = { viewModel.onStartEditingRule(it) },
            onCancelEdit = { viewModel.onCancelEditing() },
            onUpdateRule = { viewModel.onUpdateRule(it) },
            onOutboundTagChanged = { ruleId, tag -> viewModel.onOutboundTagChanged(ruleId, tag) },
            onDomainChanged = { ruleId, domains -> viewModel.onDomainChanged(ruleId, domains) },
            onIpChanged = { ruleId, ips -> viewModel.onIpChanged(ruleId, ips) },
            onReorder = { from, to -> viewModel.onReorderRules(from, to) }
        )
    }
}

@Composable
fun RoutingModeSelector(
    selectedMode: RoutingMode,
    onModeChanged: (RoutingMode) -> Unit
) {
    GoldenV2Card(modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(modifier = androidx.compose.ui.Modifier.padding(16.dp)) {
            Text(text = "Routing Mode", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.padding(top = 12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RoutingModeOption(
                    mode = RoutingMode.BypassLAN,
                    title = "Bypass LAN (Default)",
                    description = "Direct connection for local networks, proxy for everything else",
                    icon = Icons.Filled.Lan,
                    selected = selectedMode == RoutingMode.BypassLAN,
                    onClick = { onModeChanged(RoutingMode.BypassLAN) }
                )
                RoutingModeOption(
                    mode = RoutingMode.ProxyAll,
                    title = "Proxy All",
                    description = "Route all traffic through the proxy server",
                    icon = Icons.Filled.VpnLock,
                    selected = selectedMode == RoutingMode.ProxyAll,
                    onClick = { onModeChanged(RoutingMode.ProxyAll) }
                )
                RoutingModeOption(
                    mode = RoutingMode.DirectAll,
                    title = "Direct All",
                    description = "Bypass proxy, connect directly",
                    icon = Icons.Filled.NetworkCheck,
                    selected = selectedMode == RoutingMode.DirectAll,
                    onClick = { onModeChanged(RoutingMode.DirectAll) }
                )
                RoutingModeOption(
                    mode = RoutingMode.Custom,
                    title = "Custom Rules",
                    description = "Define your own routing rules",
                    icon = Icons.Filled.Settings,
                    selected = selectedMode == RoutingMode.Custom,
                    onClick = { onModeChanged(RoutingMode.Custom) }
                )
            }
        }
    }
}

@Composable
fun RoutingModeOption(
    mode: RoutingMode,
    title: String,
    description: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        onClick = onClick
    ) {
        Row(
            modifier = androidx.compose.ui.Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                    modifier = androidx.compose.ui.Modifier.size(24.dp).padding(end = 12.dp)
                )
                Column {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = androidx.compose.material3.RadioButtonDefaults.colors(
                    selectedColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
fun DomainStrategySelector(
    strategy: DomainStrategy,
    onStrategyChanged: (DomainStrategy) -> Unit
) {
    GoldenV2Card(modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(modifier = androidx.compose.ui.Modifier.padding(16.dp)) {
            Text(text = "Domain Strategy", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.padding(top = 12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DomainStrategy.values().forEach { ds ->
                    val selected = ds == strategy
                    Card(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        onClick = { onStrategyChanged(ds) }
                    ) {
                        Row(
                            modifier = androidx.compose.ui.Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = ds.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            RadioButton(
                                selected = selected,
                                onClick = { onStrategyChanged(ds) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RulesList(
    rules: List<RoutingRule>,
    editingRuleId: String?,
    onAddRule: () -> Unit,
    onDeleteRule: (String) -> Unit,
    onEditRule: (String) -> Unit,
    onCancelEdit: () -> Unit,
    onUpdateRule: (RoutingRule) -> Unit,
    onOutboundTagChanged: (String, String) -> Unit,
    onDomainChanged: (String, List<String>) -> Unit,
    onIpChanged: (String, List<String>) -> Unit,
    onReorder: (Int, Int) -> Unit
) {
    GoldenV2SectionHeader(
        title = "Routing Rules (${rules.size})",
        action = {
            OutlinedButton(onClick = onAddRule) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = androidx.compose.ui.Modifier.padding(end = 8.dp))
                Text("Add Rule")
            }
        }
    )

    if (rules.isEmpty()) {
        GoldenV2Card(modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No routing rules defined", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        LazyColumn(
            modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(rules) { rule ->
                val isEditing = editingRuleId == rule.id
                RuleItem(
                    rule = rule,
                    isEditing = isEditing,
                    onEditClick = { onEditRule(rule.id) },
                    onDeleteClick = { onDeleteRule(rule.id) },
                    onCancelEdit = onCancelEdit,
                    onSaveEdit = { updatedRule -> onUpdateRule(updatedRule) },
                    onOutboundTagChanged = { tag -> onOutboundTagChanged(rule.id, tag) },
                    onDomainChanged = { domains -> onDomainChanged(rule.id, domains) },
                    onIpChanged = { ips -> onIpChanged(rule.id, ips) }
                )
            }
        }
    }
}

@Composable
fun RuleItem(
    rule: RoutingRule,
    isEditing: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: (RoutingRule) -> Unit,
    onOutboundTagChanged: (String) -> Unit,
    onDomainChanged: (List<String>) -> Unit,
    onIpChanged: (List<String>) -> Unit
) {
    var domainInput by remember { mutableStateOf(rule.domain.joinToString(",")) }
    var ipInput by remember { mutableStateOf(rule.ip.joinToString(",")) }
    var outboundTag by remember { mutableStateOf(rule.outboundTag) }
    var enabled by remember { mutableStateOf(rule.enabled) }

    Card(
        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEditing) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = androidx.compose.ui.Modifier.padding(12.dp)) {
            if (isEditing) {
                // Editing mode
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = outboundTag,
                        onValueChange = { outboundTag = it; onOutboundTagChanged(it) },
                        label = { Text("Outbound Tag") },
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = domainInput,
                        onValueChange = { domainInput = it; onDomainChanged(it.split(",").map { it.trim() }.filter { it.isNotBlank() }) },
                        label = { Text("Domains (comma separated)") },
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = ipInput,
                        onValueChange = { ipInput = it; onIpChanged(it.split(",").map { it.trim() }.filter { it.isNotBlank() }) },
                        label = { Text("IP/CIDR (comma separated)") },
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(onClick = onCancelEdit) {
                            Text("Cancel")
                        }
                        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.padding(start = 8.dp))
                        Button(onClick = {
                            val updated = rule.copy(
                                outboundTag = outboundTag,
                                domain = domainInput.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                ip = ipInput.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                enabled = enabled
                            )
                            onSaveEdit(updated)
                        }) {
                            Text("Save")
                        }
                    }
                }
            } else {
                // View mode
                Row(
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Rule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = androidx.compose.ui.Modifier.size(20.dp).padding(end = 12.dp)
                        )
                        Column {
                            Text(text = "Rule #${rule.order}", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                text = "→ ${rule.outboundTag} | ${if (rule.domain.isNotEmpty()) "domain: ${rule.domain.joinToString(", ")} " else ""}${if (rule.ip.isNotEmpty()) "ip: ${rule.ip.joinToString(", ")}" else ""}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = enabled,
                            onCheckedChange = { enabled = it }
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.padding(start = 8.dp))
                        IconButton(onClick = onEditClick) {
                            Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = onDeleteClick) {
                            Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}