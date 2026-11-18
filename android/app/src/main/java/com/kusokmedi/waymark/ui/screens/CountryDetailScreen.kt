package com.kusokmedi.waymark.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kusokmedi.waymark.data.local.HistoryEntity
import com.kusokmedi.waymark.domain.AdvisoryLevel
import com.kusokmedi.waymark.domain.AdvisoryLevels
import com.kusokmedi.waymark.domain.CountryDetailData
import com.kusokmedi.waymark.ui.AppViewModel
import com.kusokmedi.waymark.ui.components.CountryFlag
import com.kusokmedi.waymark.ui.components.LevelChip
import com.kusokmedi.waymark.ui.components.formatAdvisoryDate
import com.kusokmedi.waymark.ui.components.formatTimestamp
import com.kusokmedi.waymark.ui.components.levelColor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryDetailScreen(
    countryId: String,
    viewModel: AppViewModel,
    onBack: () -> Unit,
) {
    val detail by remember(countryId) { viewModel.observeCountry(countryId) }
        .collectAsState(initial = null)
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    detail?.country?.name ?: countryId,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                val watch = detail?.watchlist
                if (watch != null) {
                    IconButton(onClick = { viewModel.setFavorite(countryId, !watch.isFavorite) }) {
                        Icon(
                            if (watch.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favorite",
                        )
                    }
                }
            },
        )
        val current = detail
        if (current == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading country…")
            }
            return@Column
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { CountryHeader(current) }
            item { AdvisoryCard(current, context) }
            item {
                WatchlistControls(
                    detail = current,
                    onAdd = { viewModel.addToWatchlist(countryId) },
                    onRemove = { viewModel.removeFromWatchlist(countryId) },
                    onNotifications = { enabled -> viewModel.setNotifications(countryId, enabled) },
                )
            }
            if (current.regions.isNotEmpty()) {
                item {
                    Text(
                        "Regional records",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                items(current.regions, key = { it.sourceRecordId }) { region ->
                    RegionRow(region)
                }
            }
            item {
                Text(
                    "Advisory history",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            item { HistorySection(current.history) }
            item {
                HorizontalDivider()
                Text(
                    "Last successful synchronization: ${current.lastSyncAt?.let(::formatTimestamp) ?: "never"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Advisory dates are published by the U.S. Department of State. Synchronization time is local to this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CountryHeader(detail: CountryDetailData) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CountryFlag(detail.country.isoAlpha2, detail.country.isoAlpha3)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                detail.country.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                listOfNotNull(detail.country.isoAlpha2, detail.country.isoAlpha3).joinToString(" / "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    LevelChip(
        level = detail.level,
        higherRisk = detail.containsHigherRiskAreas,
        compact = false,
        showLabel = true,
    )
}

@Composable
private fun AdvisoryCard(detail: CountryDetailData, context: android.content.Context) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Official advisory", style = MaterialTheme.typography.labelLarge)
            com.kusokmedi.waymark.ui.components.AdvisoryDateText(detail.advisoryDate)
            val record = detail.countryRecord
            if (!record?.assistedDepartureText.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text("Departure notice", fontWeight = FontWeight.SemiBold)
                Text(record?.assistedDepartureText ?: "", style = MaterialTheme.typography.bodySmall)
                if (!record?.assistedDepartureDate.isNullOrBlank()) {
                    Text(
                        "Notice date: ${record?.assistedDepartureDate}",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Waymark shows the published level and metadata. Read the full advisory on the official site for current guidance.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val url = detail.advisoryUrl
            if (url != null) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        } catch (_: ActivityNotFoundException) {
                            // No browser is installed; the link remains visible as text.
                        }
                    },
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Open official advisory")
                }
            } else {
                Text(
                    "The official advisory URL is not available for this record.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun WatchlistControls(
    detail: CountryDetailData,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onNotifications: (Boolean) -> Unit,
) {
    Card {
        Column(modifier = Modifier.padding(14.dp)) {
            val watch = detail.watchlist
            if (watch == null) {
                Text("This country is not in your watchlist.", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Button(onClick = onAdd) { Text("Add to watchlist") }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("In your watchlist", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = onRemove) { Text("Remove") }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Notify about changes")
                        Text(
                            "Only meaningful advisory changes for this country.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = watch.notificationsEnabled,
                        onCheckedChange = onNotifications,
                    )
                }
            }
        }
    }
}

@Composable
private fun RegionRow(region: com.kusokmedi.waymark.data.local.AdvisoryEntity) {
    Card {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(region.destinationName, fontWeight = FontWeight.SemiBold)
                com.kusokmedi.waymark.ui.components.AdvisoryDateText(region.advisoryDate)
            }
            LevelChip(region.level, region.containsHigherRiskAreas, compact = true)
        }
    }
}

@Composable
private fun HistorySection(events: List<HistoryEntity>) {
    val timed = events.filter { it.newLevel != null }.sortedBy { it.occurredAt }
    if (timed.isEmpty()) {
        Text(
            "No level changes have been recorded yet. History begins when Waymark observes a meaningful change; no historical values are invented.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Text(
        "History begins with the first recorded change. The chart connects only recorded transitions.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
    HistoryChart(timed)
    Spacer(Modifier.height(8.dp))
    timed.takeLast(6).reversed().forEach { event ->
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
            Text(
                formatEventTime(event.occurredAt),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.width(92.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(event.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                Text(
                    "${levelName(event.previousLevel)} → ${levelName(event.newLevel)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HistoryChart(events: List<HistoryEntity>) {
    val primary = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
        val left = 8.dp.toPx()
        val right = size.width - 8.dp.toPx()
        val top = 8.dp.toPx()
        val bottom = size.height - 8.dp.toPx()
        val width = (right - left).coerceAtLeast(1f)
        val height = (bottom - top).coerceAtLeast(1f)
        // Horizontal guides for levels 1..4. No values are drawn outside recorded transitions.
        for (level in 1..4) {
            val y = bottom - (level - 1) / 3f * height
            drawLine(grid, Offset(left, y), Offset(right, y), strokeWidth = 1f)
        }
        val start = events.first().occurredAt
        val end = events.last().occurredAt
        val span = (end - start).coerceAtLeast(1L)
        fun x(time: Long): Float = left + ((time - start).toFloat() / span.toFloat()) * width
        fun y(level: Int): Float = bottom - ((level.coerceIn(1, 4) - 1) / 3f) * height

        val path = Path()
        var current = events.first().previousLevel ?: requireNotNull(events.first().newLevel)
        path.moveTo(left, y(current))
        events.forEach { event ->
            val eventX = x(event.occurredAt)
            path.lineTo(eventX, y(current))
            val next = event.newLevel ?: current
            path.lineTo(eventX, y(next))
            current = next
        }
        path.lineTo(right, y(current))
        drawPath(path, primary, style = Stroke(width = 3f))
        events.forEach { event ->
            event.newLevel?.let { drawCircle(primary, radius = 5f, center = Offset(x(event.occurredAt), y(it))) }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(1, 2, 3, 4).forEach {
            Text("L$it", style = MaterialTheme.typography.labelSmall, color = levelColor(AdvisoryLevel.fromNumber(it)))
        }
    }
}

private fun levelName(number: Int?): String {
    val level = AdvisoryLevel.fromNumber(number)
    return if (level == null) "Unknown" else "Level ${level.number}"
}

private fun formatEventTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))
