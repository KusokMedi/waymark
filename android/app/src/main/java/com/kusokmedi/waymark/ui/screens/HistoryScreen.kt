package com.kusokmedi.waymark.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kusokmedi.waymark.domain.ChangeType
import com.kusokmedi.waymark.domain.HistoryItem
import com.kusokmedi.waymark.ui.components.formatTimestamp
import com.kusokmedi.waymark.sync.changeType
import com.kusokmedi.waymark.domain.AdvisoryLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    history: List<HistoryItem>,
    onOpenCountry: (String) -> Unit,
    onClearHistory: () -> Unit,
) {
    var confirmClear by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text("History", fontWeight = FontWeight.Bold)
                    Text(
                        "Recorded changes on this device",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            actions = {
                if (history.isNotEmpty()) {
                    IconButton(onClick = { confirmClear = true }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear history")
                    }
                }
            },
        )
        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.height(56.dp),
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "No recorded changes",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "History starts with the first meaningful advisory change for a country in your watchlist. The first synchronization is a baseline and does not create events.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(history, key = { it.event.id }) { item ->
                    HistoryCard(item, onOpenCountry)
                }
            }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear advisory history?") },
            text = {
                Text(
                    "This permanently deletes all recorded change events on this device. Current advisories and your watchlist are kept."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearHistory()
                        confirmClear = false
                    },
                ) { Text("Clear history") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun HistoryCard(item: HistoryItem, onOpenCountry: (String) -> Unit) {
    val event = item.event
    val type = event.changeType()
    Card(onClick = { onOpenCountry(item.event.countryId) }, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier.width(6.dp).height(44.dp)
                    .background(typeColor(type)),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.countryName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        formatTimestamp(event.occurredAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(typeLabel(type), style = MaterialTheme.typography.labelMedium)
                Text(event.message, style = MaterialTheme.typography.bodySmall)
                if (event.previousLevel != null || event.newLevel != null) {
                    Text(
                        "${levelLabel(event.previousLevel)} → ${levelLabel(event.newLevel)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun typeColor(type: ChangeType?): Color = when (type) {
    ChangeType.WORSENED -> Color(0xFFB3261E)
    ChangeType.IMPROVED -> Color(0xFF2E6B4F)
    ChangeType.REGIONAL -> Color(0xFFB26A00)
    ChangeType.DETAILS -> Color(0xFF4E6359)
    null -> Color(0xFF6E6E6E)
}

private fun typeLabel(type: ChangeType?): String = when (type) {
    ChangeType.WORSENED -> "Advisory worsened"
    ChangeType.IMPROVED -> "Advisory improved"
    ChangeType.REGIONAL -> "New regional advisory"
    ChangeType.DETAILS -> "Advisory details changed"
    null -> "Advisory change"
}

private fun levelLabel(number: Int?): String {
    val level = AdvisoryLevel.fromNumber(number)
    return if (level == null) "Unknown" else "Level ${level.number}"
}
