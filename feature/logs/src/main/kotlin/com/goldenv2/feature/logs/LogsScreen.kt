package com.goldenv2.feature.logs

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MenuAnchorType

import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import android.content.ClipData
import android.content.ClipboardManager
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
import com.goldenv2.core.domain.model.LogEntry
import com.goldenv2.core.domain.model.LogLevel
import com.goldenv2.core.ui.component.GoldenV2Card
import com.goldenv2.core.ui.component.GoldenV2IconButton
import com.goldenv2.core.ui.component.GoldenV2SectionHeader
import com.goldenv2.core.ui.theme.GoldenV2Theme
import kotlinx.coroutines.flow.StateFlow

@Composable
fun LogsScreen(
    viewModel: LogsViewModel,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    onNavigateToHome: () -> Unit,
    onNavigateToServers: () -> Unit,
    onNavigateToRouting: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 80.dp)
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text(text = "Logs", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            actions = {
                GoldenV2IconButton(
                    icon = Icons.Filled.Delete,
                    contentDescription = "Clear Logs",
                    onClick = { viewModel.onClearLogs() }
                )
                GoldenV2IconButton(
                    icon = Icons.Filled.Download,
                    contentDescription = "Export Logs",
                    onClick = { viewModel.onExportLogs() }
                )
                GoldenV2IconButton(
                    icon = Icons.Filled.Speed,
                    contentDescription = "Speed Test",
                    onClick = { viewModel.onSpeedTest() }
                )
            }
        )

        // Filter Bar
        FilterBar(
            filterLevel = uiState.filterLevel,
            onFilterLevelChanged = { viewModel.onFilterLevelChanged(it) },
            searchQuery = uiState.searchQuery,
            onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
            isAutoScroll = uiState.isAutoScroll,
            onAutoScrollChanged = { viewModel.onAutoScrollChanged(it) }
        )

        // Log List
        if (uiState.filteredLogs.isEmpty()) {
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.BugReport,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = androidx.compose.ui.Modifier.size(64.dp)
                    )
                    Text(text = "No logs", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "Logs will appear here when VPN is running", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }
        } else {
            LazyColumn(
                modifier = androidx.compose.ui.Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                reverseLayout = true,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)
            ) {
                items(uiState.filteredLogs.reversed()) { log ->
                    LogItem(log = log)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBar(
    filterLevel: LogLevel,
    onFilterLevelChanged: (LogLevel) -> Unit,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    isAutoScroll: Boolean,
    onAutoScrollChanged: (Boolean) -> Unit
) {
    var levelMenuExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = androidx.compose.ui.Modifier.padding(12.dp)) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                placeholder = { Text("Search logs…") },
                leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true
            )

            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.padding(top = 8.dp))

            // Filter controls
            Row(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Log level filter
                ExposedDropdownMenuBox(
                    expanded = levelMenuExpanded,
                    onExpandedChange = { levelMenuExpanded = it },
                    modifier = androidx.compose.ui.Modifier.weight(1f)
                ) {
                    OutlinedButton(
                        onClick = { levelMenuExpanded = true },
                        modifier = androidx.compose.ui.Modifier
                            .menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        content = {
                            Row(
                                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Level: ${filterLevel.name}", fontSize = 12.sp)
                                Icon(
                                    imageVector = Icons.Filled.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = levelMenuExpanded,
                        onDismissRequest = { levelMenuExpanded = false }
                    ) {
                        LogLevel.values().forEach { level ->
                            DropdownMenuItem(
                                text = { Text(text = level.name) },
                                onClick = {
                                    onFilterLevelChanged(level)
                                    levelMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Auto-scroll toggle
                androidx.compose.material3.Switch(
                    checked = isAutoScroll,
                    onCheckedChange = onAutoScrollChanged,
                    modifier = androidx.compose.ui.Modifier.padding(start = 8.dp)
                )
                Text(text = "Auto-scroll", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun LogItem(log: LogEntry) {
    val context = LocalContext.current
    val timeFormat = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
    val time = timeFormat.format(java.util.Date(log.timestamp.toEpochMilli()))

    val levelColor = when (log.level) {
        LogLevel.Debug -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        LogLevel.Info -> MaterialTheme.colorScheme.primary
        LogLevel.Warning -> Color(0xFFFF9800) // Orange
        LogLevel.Error -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = androidx.compose.ui.Modifier.padding(12.dp)) {
            Row(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = time,
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.padding(horizontal = 8.dp))
                    Box(
                        modifier = androidx.compose.ui.Modifier
                            .size(8.dp)
                            .background(levelColor, RoundedCornerShape(4.dp))
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.padding(horizontal = 8.dp))
                    Text(
                        text = log.tag,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = {
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard?.setPrimaryClip(ClipData.newPlainText("log", log.toString()))
                    // TODO: Show toast "Copied"
                }) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = log.message,
                fontSize = 12.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, start = 24.dp),
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )

            log.throwable?.let { throwable ->
                Text(
                    text = throwable,
                    fontSize = 11.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.error,
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, start = 24.dp),
                    maxLines = 10
                )
            }
        }
    }
}