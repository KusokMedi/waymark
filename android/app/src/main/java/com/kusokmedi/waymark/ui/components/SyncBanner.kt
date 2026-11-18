package com.kusokmedi.waymark.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kusokmedi.waymark.data.local.SyncStateEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SyncBanner(state: SyncStateEntity?, modifier: Modifier = Modifier) {
    val status = state?.status ?: "idle"
    val message = state?.message
    val lastSuccess = state?.lastSuccessAt
    val isProblem = status == "failed" || status == "offline"
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (isProblem) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isProblem) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row {
                Text(
                    when (status) {
                        "running" -> "Updating advisories…"
                        "success" -> "Advisories updated"
                        "offline" -> "Offline"
                        "failed" -> "Update failed"
                        else -> "Not updated yet"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (lastSuccess != null) {
                    Text(
                        formatTimestamp(lastSuccess),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            if (isProblem && !message.isNullOrBlank()) {
                Text(message, style = MaterialTheme.typography.bodySmall)
                if (lastSuccess != null) {
                    Text(
                        "Showing saved data from ${formatTimestamp(lastSuccess)}.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            } else if (status == "success" && message.isNullOrBlank()) {
                Text(
                    "Data is cached on this device. Advisories may be delayed by Android background scheduling.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

fun formatTimestamp(millis: Long): String =
    Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.getDefault()))
