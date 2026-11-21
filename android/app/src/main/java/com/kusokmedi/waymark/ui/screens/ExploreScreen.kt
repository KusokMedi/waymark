package com.kusokmedi.waymark.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
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
import com.kusokmedi.waymark.domain.AdvisoryLevel
import com.kusokmedi.waymark.domain.CountryCard
import com.kusokmedi.waymark.ui.components.CountryCardView

private enum class ExploreSort(val label: String) {
    NAME_ASC("A-Z"),
    NAME_DESC("Z-A"),
    LEVEL_ASC("Level 1-4"),
    LEVEL_DESC("Level 4-1"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    countries: List<CountryCard>,
    onOpenCountry: (String) -> Unit,
    onToggleFavorite: (CountryCard) -> Unit,
    onToggleWatchlist: (CountryCard) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(ExploreSort.NAME_ASC) }

    val knownCountries = remember(countries) { countries.filter { it.level != null } }
    val filtered = remember(knownCountries, query, sort) {
        val base = knownCountries.filter {
            query.isBlank() ||
                it.name.contains(query, ignoreCase = true) ||
                it.isoAlpha2?.equals(query, ignoreCase = true) == true ||
                it.isoAlpha3?.equals(query, ignoreCase = true) == true
        }
        when (sort) {
            ExploreSort.NAME_ASC -> base.sortedBy { it.name.lowercase() }
            ExploreSort.NAME_DESC -> base.sortedByDescending { it.name.lowercase() }
            ExploreSort.LEVEL_ASC -> base.sortedWith(compareByCountryLevel(ascending = true))
            ExploreSort.LEVEL_DESC -> base.sortedWith(compareByCountryLevel(ascending = false))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text("Explore", fontWeight = FontWeight.Bold)
                    Text(
                        "${knownCountries.size} destinations",
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
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(ExploreSort.entries) { option ->
                    FilterChip(
                        selected = sort == option,
                        onClick = { sort = option },
                        label = { Text(option.label) },
                    )
                }
            }
        }
        AnimatedContent(
            targetState = Triple(knownCountries.isEmpty(), filtered.isEmpty(), query),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "explore-content",
        ) { (catalogueEmpty, noResults, _) ->
            when {
                catalogueEmpty -> {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Loading country catalogue…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "If this takes long, check your connection or sync settings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                noResults -> {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "No countries match \"$query\".",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> {
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
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun compareByCountryLevel(ascending: Boolean): Comparator<CountryCard> {
    val levelValue: (CountryCard) -> Int = { card ->
        card.level?.number ?: if (ascending) Int.MAX_VALUE else Int.MIN_VALUE
    }
    val base = if (ascending) compareBy(levelValue) else compareByDescending(levelValue)
    return base.thenBy { it.name.lowercase() }
}
