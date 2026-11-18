package com.kusokmedi.waymark.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kusokmedi.waymark.data.local.SyncStateEntity
import com.kusokmedi.waymark.domain.CountryCard
import com.kusokmedi.waymark.ui.components.CountryCardView
import com.kusokmedi.waymark.ui.components.SyncBanner

private enum class WatchlistSort(val label: String) {
    NAME_ASC("A–Z"),
    NAME_DESC("Z–A"),
    FAVORITES("Favorites"),
    LEVEL_ASC("Level 1–4"),
    LEVEL_DESC("Level 4–1"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    countries: List<CountryCard>,
    syncState: SyncStateEntity?,
    onOpenCountry: (String) -> Unit,
    onToggleFavorite: (CountryCard) -> Unit,
    onRefresh: () -> Unit,
    onExplore: () -> Unit,
    onOpenSyncDetails: () -> Unit,
) {
    var sort by remember { mutableStateOf(WatchlistSort.NAME_ASC) }
    val sorted = remember(countries, sort) {
        when (sort) {
            WatchlistSort.NAME_ASC -> countries.sortedBy { it.name.lowercase() }
            WatchlistSort.NAME_DESC -> countries.sortedByDescending { it.name.lowercase() }
            WatchlistSort.FAVORITES -> countries.sortedWith(
                compareByDescending<CountryCard> { it.isFavorite }.thenBy { it.name.lowercase() }
            )
            WatchlistSort.LEVEL_ASC -> countries.sortedWith(
                compareBy<CountryCard> { it.level?.number ?: Int.MAX_VALUE }.thenBy { it.name.lowercase() }
            )
            WatchlistSort.LEVEL_DESC -> countries.sortedWith(
                compareByDescending<CountryCard> { it.level?.number ?: Int.MIN_VALUE }.thenBy { it.name.lowercase() }
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text("Waymark", fontWeight = FontWeight.Bold)
                    Text(
                        "Know before you go.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            actions = {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh advisories")
                }
            },
        )
        SyncBanner(syncState, onClick = onOpenSyncDetails)
        if (countries.isEmpty()) {
            EmptyWatchlist(onExplore)
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(WatchlistSort.entries) { option ->
                    FilterChip(
                        selected = sort == option,
                        onClick = { sort = option },
                        label = { Text(option.label) },
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(sorted, key = { it.id }) { card ->
                    CountryCardView(
                        card = card,
                        onClick = { onOpenCountry(card.id) },
                        onFavorite = { onToggleFavorite(card) },
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun EmptyWatchlist(onExplore: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.TravelExplore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.height(56.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Your watchlist is empty",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Choose the countries you want to follow. Advisories can be viewed even without a connection once they have been downloaded.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onExplore) { Text("Explore countries") }
        }
    }
}
