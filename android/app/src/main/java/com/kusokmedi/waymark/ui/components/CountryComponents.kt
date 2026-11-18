package com.kusokmedi.waymark.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kusokmedi.waymark.domain.AdvisoryLevel
import com.kusokmedi.waymark.domain.AdvisoryLevels
import com.kusokmedi.waymark.domain.CountryCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun levelColor(level: AdvisoryLevel?): Color = when (level) {
    null -> Color(0xFF6E6E6E)
    AdvisoryLevel.LEVEL_1 -> Color(0xFF2E6B4F)
    AdvisoryLevel.LEVEL_2 -> Color(0xFF8A6D00)
    AdvisoryLevel.LEVEL_3 -> Color(0xFFB25E00)
    AdvisoryLevel.LEVEL_4 -> Color(0xFFB3261E)
}

fun levelContainerColor(level: AdvisoryLevel?): Color = when (level) {
    null -> Color(0xFFE3E3E3)
    AdvisoryLevel.LEVEL_1 -> Color(0xFFD7EDE1)
    AdvisoryLevel.LEVEL_2 -> Color(0xFFF5E9B8)
    AdvisoryLevel.LEVEL_3 -> Color(0xFFF8DCC4)
    AdvisoryLevel.LEVEL_4 -> Color(0xFFF6D5D2)
}

@Composable
fun LevelChip(
    level: AdvisoryLevel?,
    higherRisk: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val label = if (level == null) "Unknown" else "Level ${level.number}"
    Surface(
        modifier = modifier,
        color = levelContainerColor(level),
        contentColor = levelColor(level),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(
                AdvisoryLevels.label(level),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (higherRisk) {
                Text(
                    "Contains higher-risk areas",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
fun CountryFlag(isoAlpha2: String?, isoAlpha3: String?, modifier: Modifier = Modifier) {
    val emoji = com.kusokmedi.waymark.domain.flagEmoji(isoAlpha2 ?: isoAlpha3?.let { com.kusokmedi.waymark.domain.IsoCountries.alpha2(it) })
    Box(
        modifier = modifier
            .size(width = 46.dp, height = 34.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            emoji,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
fun CountryCardView(
    card: CountryCard,
    onClick: () -> Unit,
    onFavorite: (() -> Unit)? = null,
    onWatchlist: (() -> Unit)? = null,
    showWatchlistAction: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CountryFlag(card.isoAlpha2, card.isoAlpha3)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        card.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (card.isFavorite) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = "Favorite",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    if (card.level == null) "Advisory unknown" else AdvisoryLevels.label(card.level),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LevelChip(card.level, card.containsHigherRiskAreas)
                    Text(
                        formatAdvisoryDate(card.advisoryDate),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (onFavorite != null) {
                IconButton(onClick = onFavorite) {
                    Icon(
                        if (card.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (card.isFavorite) "Remove favorite" else "Add favorite",
                        tint = if (card.isFavorite) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (showWatchlistAction && onWatchlist != null) {
                IconButton(onClick = onWatchlist) {
                    Icon(
                        if (card.inWatchlist) Icons.Filled.Check else Icons.Outlined.Add,
                        contentDescription = if (card.inWatchlist) "Remove from watchlist" else "Add to watchlist",
                    )
                }
            }
        }
    }
}

fun formatAdvisoryDate(value: String?): String {
    if (value.isNullOrBlank()) return "Advisory date unknown"
    return try {
        val date = LocalDate.parse(value)
        "Advisory " + date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))
    } catch (_: Exception) {
        "Advisory date unknown"
    }
}
