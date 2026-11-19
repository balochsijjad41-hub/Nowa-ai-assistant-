package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.HistoryEntry
import com.example.i18n.NovaLocalizedStrings
import com.example.ui.components.NovaGlassCard
import com.example.ui.theme.LocalNovaColors
import com.example.ui.theme.NovaCrimson
import com.example.ui.theme.NovaCyan
import com.example.ui.theme.NovaEmerald
import com.example.ui.theme.NovaMagenta
import com.example.ui.theme.NovaViolet
import com.example.viewmodel.DataUiState
import com.example.viewmodel.NovaRuntimeState
import com.example.viewmodel.NovaViewModel

@Composable
fun HistoryScreen(
    viewModel: NovaViewModel,
    runtime: NovaRuntimeState,
    historyState: DataUiState<List<HistoryEntry>>,
    strings: NovaLocalizedStrings
) {
    val novaColors = LocalNovaColors.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var showStatsDashboard by remember { mutableStateOf(true) }
    var showMicLog by remember { mutableStateOf(false) }

    val filters = listOf("All", "Bookmarked", "Multi-Step", "Driving", "Vision")
    val entries = (historyState as? DataUiState.Success)?.data.orEmpty()
    val filteredEntries = remember(entries, searchQuery, selectedFilter) {
        entries.filter { entry ->
            val matchesQuery = searchQuery.isBlank() ||
                entry.userQuery.contains(searchQuery, ignoreCase = true) ||
                entry.novaReply.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "Bookmarked" -> entry.bookmarked
                "Multi-Step" -> entry.executedActions.size > 1 || entry.intentTag.contains("MULTI", true)
                "Driving" -> entry.modeUsed.equals("DRIVING", true)
                "Vision" -> entry.intentTag.contains("VISION", true)
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Voice History & Insights", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Row {
                    IconButton(onClick = { showStatsDashboard = !showStatsDashboard }) {
                        Icon(Icons.Default.BarChart, contentDescription = "Toggle usage stats", tint = novaColors.cyanAccent)
                    }
                    IconButton(onClick = { showMicLog = !showMicLog }) {
                        Icon(Icons.Default.Mic, contentDescription = "Toggle mic access log", tint = NovaEmerald)
                    }
                }
            }
        }

        // Usage Statistics Dashboard
        item {
            AnimatedVisibility(visible = showStatsDashboard) {
                UsageStatsDashboardCard(totalCommands = entries.size, multiStepCount = entries.count { it.executedActions.size > 1 })
            }
        }

        // Mic Access Log Card
        item {
            AnimatedVisibility(visible = showMicLog) {
                NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mic Access Privacy Log", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { showMicLog = false }) { Text("Hide") }
                    }
                    runtime.micAccessLog.take(5).forEach { log ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(log.source, style = MaterialTheme.typography.labelLarge)
                                Text(log.reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(log.timestampLabel, style = MaterialTheme.typography.labelSmall, color = NovaEmerald)
                        }
                    }
                }
            }
        }

        // Search + Filter Chips
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search voice commands & replies...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_search_input")
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) }
                    )
                }
            }
        }

        when (historyState) {
            is DataUiState.Loading -> {
                item {
                    Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(color = NovaCyan)
                    }
                }
            }
            is DataUiState.Error -> {
                item {
                    NovaGlassCard(Modifier.fillMaxWidth()) {
                        Text(historyState.message, color = NovaCrimson)
                    }
                }
            }
            is DataUiState.Success -> {
                if (filteredEntries.isEmpty()) {
                    item {
                        NovaGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(28.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, tint = novaColors.cyanAccent, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(10.dp))
                                Text(strings.emptyHistoryTitle, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = strings.emptyHistorySubtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(filteredEntries, key = { it.id }) { entry ->
                        NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = novaColors.cyanAccent.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        text = "${entry.intentTag} • ${entry.modeUsed}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = novaColors.cyanAccent,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Row {
                                    IconButton(onClick = { viewModel.replayHistoryAudio(entry) }) {
                                        Icon(Icons.Default.VolumeUp, contentDescription = "Replay Nova audio", tint = novaColors.violetAccent)
                                    }
                                    IconButton(onClick = { viewModel.toggleHistoryBookmark(entry) }) {
                                        Icon(
                                            imageVector = if (entry.bookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = "Bookmark entry",
                                            tint = if (entry.bookmarked) NovaMagenta else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { viewModel.deleteHistoryEntry(entry.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete history entry", tint = NovaCrimson)
                                    }
                                }
                            }

                            Spacer(Modifier.height(6.dp))
                            Text("You: \"${entry.userQuery}\"", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))
                            Text("Nova: ${entry.novaReply}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            if (entry.executedActions.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    entry.executedActions.forEach { step ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                        ) {
                                            Text(
                                                text = step,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UsageStatsDashboardCard(
    totalCommands: Int,
    multiStepCount: Int
) {
    val novaColors = LocalNovaColors.current
    val weeklyValues = remember(totalCommands) {
        listOf(0.45f, 0.70f, 0.55f, 0.85f, 0.62f, 0.90f, (0.4f + totalCommands * 0.08f).coerceAtMost(1f))
    }
    NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Usage Statistics", style = MaterialTheme.typography.titleLarge)
            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = novaColors.cyanAccent)
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatMetric("Voice Turns", totalCommands.toString(), novaColors.cyanAccent)
            StatMetric("Multi-Step", multiStepCount.toString(), novaColors.violetAccent)
            StatMetric("Time Saved", "${(totalCommands * 1.8).toInt()}m", NovaEmerald)
        }
        Spacer(Modifier.height(12.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
        ) {
            val barWidth = (size.width / (weeklyValues.size * 2f))
            weeklyValues.forEachIndexed { idx, fraction ->
                val left = idx * (size.width / weeklyValues.size) + barWidth * 0.5f
                val h = size.height * fraction
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(NovaCyan, NovaViolet, NovaMagenta)),
                    topLeft = Offset(left, size.height - h),
                    size = Size(barWidth, h),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
    }
}

@Composable
private fun StatMetric(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = color, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
