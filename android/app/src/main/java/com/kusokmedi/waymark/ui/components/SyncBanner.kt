package com.kusokmedi.waymark.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kusokmedi.waymark.data.local.SyncStateEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SyncBanner(
    state: SyncStateEntity?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val status = effectiveStatus(state)
    val lastSuccess = state?.lastSuccessAt
    val isProblem = status == "failed" || status == "offline"
    val isRunning = status == "running"
    val title = when (status) {
        "running" -> "Updating advisories…"
        "success" -> "Advisories updated"
        "offline" -> "Offline"
        "failed" -> "Error, last update at ${lastSuccess?.let(::formatTimestampShort) ?: "never"}"
        else -> "Not updated yet"
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        color = if (isProblem) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isProblem) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        onClick = { if (onClick != null && isProblem) onClick() },
        enabled = onClick != null && isProblem,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (lastSuccess != null && status != "failed") {
                    Text(
                        formatTimestamp(lastSuccess),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            AnimatedVisibility(visible = isRunning) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            AnimatedVisibility(visible = isProblem) {
                Text(
                    "Tap to view synchronization details.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            AnimatedVisibility(visible = status == "success") {
                Text(
                    "Data is cached on this device. Advisories may be delayed by Android background scheduling.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

private fun effectiveStatus(state: SyncStateEntity?): String {
    if (state == null) return "idle"
    if (state.status == "running") {
        val age = System.currentTimeMillis() - (state.lastAttemptAt ?: 0L)
        if (age > java.util.concurrent.TimeUnit.MINUTES.toMillis(3)) return "failed"
    }
    return state.status
}

fun formatTimestampShort(millis: Long): String =
    Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.getDefault()))

fun formatTimestamp(millis: Long): String =
    Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.getDefault()))
