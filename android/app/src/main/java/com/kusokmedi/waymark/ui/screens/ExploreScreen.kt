package com.kusokmedi.waymark.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kusokmedi.waymark.data.sortedBySeverity
import com.kusokmedi.waymark.domain.CountryCard
import com.kusokmedi.waymark.ui.components.CountryCardView

private enum class ExploreSort { NAME, LEVEL, FAVORITES }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    countries: List<CountryCard>,
    onOpenCountry: (String) -> Unit,
    onToggleFavorite: (CountryCard) -> Unit,
    onToggleWatchlist: (CountryCard) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(ExploreSort.NAME) }

    val filtered = remember(countries, query, sort) {
        val base = countries.filter {
            query.isBlank() ||
                it.name.contains(query, ignoreCase = true) ||
                it.isoAlpha2?.equals(query, ignoreCase = true) == true ||
                it.isoAlpha3?.equals(query, ignoreCase = true) == true
        }
        when (sort) {
            ExploreSort.NAME -> base.sortedBy { it.name.lowercase() }
            ExploreSort.LEVEL -> base.sortedBySeverity()
            ExploreSort.FAVORITES -> base.filter { it.isFavorite }.sortedBy { it.name.lowercase() }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text("Explore", fontWeight = FontWeight.Bold)
                    Text(
                        "${countries.size} destinations",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search countries") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
            )
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(selected = sort == ExploreSort.NAME, onClick = { sort = ExploreSort.NAME }, label = { Text("A–Z") })
                FilterChip(selected = sort == ExploreSort.LEVEL, onClick = { sort = ExploreSort.LEVEL }, label = { Text("Level") })
                FilterChip(selected = sort == ExploreSort.FAVORITES, onClick = { sort = ExploreSort.FAVORITES }, label = { Text("Favorites") })
            }
        }
        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    when {
                        countries.isEmpty() -> "No country catalogue is available yet. Refresh to download advisories."
                        sort == ExploreSort.FAVORITES -> "No favorite countries yet. Tap the star on a country card."
                        else -> "No countries match “$query”."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(filtered, key = { it.id }) { card ->
                    CountryCardView(
                        card = card,
                        onClick = { onOpenCountry(card.id) },
                        onFavorite = { onToggleFavorite(card) },
                        onWatchlist = { onToggleWatchlist(card) },
                        showWatchlistAction = true,
                    )
                }
            }
        }
    }
}
