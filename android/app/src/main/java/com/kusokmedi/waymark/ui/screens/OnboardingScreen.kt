package com.kusokmedi.waymark.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kusokmedi.waymark.domain.AdvisoryLevel
import com.kusokmedi.waymark.domain.AdvisoryLevels
import com.kusokmedi.waymark.domain.CountryCard
import com.kusokmedi.waymark.ui.components.LevelChip

@Composable
fun OnboardingScreen(
    countries: List<CountryCard>,
    onComplete: (List<String>, Boolean) -> Unit,
) {
    var step by remember { mutableStateOf(0) }
    val selected = remember { mutableStateListOf<String>() }
    var notifications by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    val filtered = remember(countries, query) {
        if (query.isBlank()) countries
        else countries.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.isoAlpha2?.equals(query, ignoreCase = true) == true
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            StepIndicator(step)
            Spacer(Modifier.height(24.dp))
            when (step) {
                0 -> WelcomeStep()
                1 -> LevelsStep()
                2 -> CountryStep(
                    query = query,
                    onQueryChange = { query = it },
                    countries = filtered,
                    selected = selected,
                )
                else -> NotificationStep(
                    enabled = notifications,
                    onEnabledChange = { notifications = it },
                    selectedCount = selected.size,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (step > 0) {
                TextButton(onClick = { step -= 1 }) { Text("Back") }
            } else {
                Spacer(Modifier)
            }
            Button(
                onClick = {
                    if (step < 3) {
                        step += 1
                    } else {
                        onComplete(selected.toList(), notifications)
                        if (notifications && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                },
            ) {
                Text(if (step < 3) "Continue" else "Start using Waymark")
            }
        }
    }
}

@Composable
private fun StepIndicator(step: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .height(4.dp)
                    .weight(1f)
                    .padding(0.dp),
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (index <= step) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    content = {},
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Column {
        Text("Waymark", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text(
            "Know before you go.",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Waymark keeps official U.S. Department of State travel advisories for the countries you choose.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(18.dp))
        InfoLine("Local-first", "Your watchlist, history and settings stay on this device.")
        InfoLine("No account", "There is no Waymark account, server or cloud sync.")
        InfoLine(
            "Official source",
            "Advisory data is retrieved directly from the U.S. Department of State's public ArcGIS service.",
        )
        InfoLine(
            "Independent",
            "Waymark is not affiliated with or endorsed by the U.S. Department of State.",
        )
    }
}

@Composable
private fun LevelsStep() {
    Column {
        Text("Advisory levels", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "The U.S. Department of State publishes a level from 1 to 4 for each destination. Some levels note that the country contains areas with a higher security risk.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        listOf(
            AdvisoryLevel.LEVEL_1 to "Exercise normal precautions.",
            AdvisoryLevel.LEVEL_2 to "Exercise increased caution.",
            AdvisoryLevel.LEVEL_3 to "Reconsider travel.",
            AdvisoryLevel.LEVEL_4 to "Do not travel.",
        ).forEach { (level, description) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LevelChip(level)
                Spacer(Modifier.height(0.dp))
                Text(
                    description,
                    modifier = Modifier.padding(start = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Advisories are written for U.S. citizens and are not a universal safety rating. Always consult the official advisory.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CountryStep(
    query: String,
    onQueryChange: (String) -> Unit,
    countries: List<CountryCard>,
    selected: MutableList<String>,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Choose countries", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Select the destinations you want to follow. You can change this at any time.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Search countries") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        )
        Text(
            if (countries.isEmpty()) "Waiting for the country catalogue…" else "${selected.size} selected",
            modifier = Modifier.padding(vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(countries, key = { it.id }) { card ->
                Surface(
                    onClick = {
                        if (card.id in selected) selected.remove(card.id) else selected.add(card.id)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = card.id in selected,
                            onCheckedChange = {
                                if (card.id in selected) selected.remove(card.id) else selected.add(card.id)
                            },
                        )
                        Text(card.name, modifier = Modifier.weight(1f))
                        Text(
                            card.isoAlpha2 ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationStep(enabled: Boolean, onEnabledChange: (Boolean) -> Unit, selectedCount: Int) {
    Column {
        Text("Stay informed", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Waymark can notify you when an advisory for one of your $selectedCount selected countries changes.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Advisory notifications", fontWeight = FontWeight.SemiBold)
                Text(
                    "Optional. Uses Android notifications and the standard notification sound.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = enabled, onCheckedChange = onEnabledChange)
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "You can enable notifications later in Settings. Waymark will not ask for notification permission unless you choose to enable them.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))
        Text("What Waymark sends", fontWeight = FontWeight.SemiBold)
        Text(
            "Only advisory changes for countries you follow. The first synchronization never sends notifications.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun InfoLine(title: String, body: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 2.dp),
        )
        Column(modifier = Modifier.padding(start = 10.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodySmall)
        }
    }
}
