package com.kusokmedi.waymark.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import com.kusokmedi.waymark.domain.AdvisoryLevel
import com.kusokmedi.waymark.domain.AdvisoryLevels
import com.kusokmedi.waymark.SyncScheduler
import com.kusokmedi.waymark.domain.CountryCard
import com.kusokmedi.waymark.ui.components.LevelChip
import com.kusokmedi.waymark.ui.components.levelColor
import com.kusokmedi.waymark.ui.components.levelContainerColor

@Composable
fun OnboardingScreen(
    countries: List<CountryCard>,
    onComplete: (List<String>, Boolean, Boolean) -> Unit,
    onRequestSync: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(0) }
    val selected = remember { mutableStateListOf<String>() }
    var notifications by remember { mutableStateOf(false) }
    var allowMobileData by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Propagate the actual permission result so settings do not stay
        // enabled when the user denied the notification permission.
        onComplete(selected.toList(), notifications && granted, allowMobileData)
    }

    val filtered = remember(countries, query) {
        if (query.isBlank()) countries
        else countries.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.isoAlpha2?.equals(query, ignoreCase = true) == true
        }
    }

    // Trigger a sync when reaching the country step so the catalogue is available.
    var syncRequested by remember { mutableStateOf(false) }
    LaunchedEffect(step, allowMobileData) {
        if (step == 3 && countries.isEmpty() && !syncRequested) {
            syncRequested = true
            onRequestSync(allowMobileData)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
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
                    2 -> PermissionsStep(
                        notificationsEnabled = notifications,
                        onNotificationsChange = { notifications = it },
                        allowMobileData = allowMobileData,
                        onAllowMobileDataChange = { allowMobileData = it },
                        selectedCount = selected.size,
                        onRequestBatteryExemption = { requestBatteryExemption(context) },
                    )
                    else -> CountryStep(
                        query = query,
                        onQueryChange = { query = it },
                        countries = filtered,
                        selected = selected,
                        allowMobileData = allowMobileData,
                        onRequestSync = onRequestSync,
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
                        } else if (notifications && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onComplete(selected.toList(), notifications, allowMobileData)
                        }
                    },
                ) {
                    val label = when {
                        step == 3 && selected.isEmpty() -> "Skip"
                        step < 3 -> "Continue"
                        else -> "Start using Waymark"
                    }
                    Text(label)
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(step: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .height(4.dp)
                    .weight(1f),
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
    val scrollState = rememberScrollState()
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealed = true }

    val levels = listOf(
        Triple(
            AdvisoryLevel.LEVEL_1,
            "Standard awareness",
            "Take the usual precautions you would in any unfamiliar place.",
        ),
        Triple(
            AdvisoryLevel.LEVEL_2,
            "Heightened awareness",
            "Be more alert to safety and security conditions.",
        ),
        Triple(
            AdvisoryLevel.LEVEL_3,
            "Serious risks",
            "Carefully reconsider plans. Safety risks may be significant.",
        ),
        Triple(
            AdvisoryLevel.LEVEL_4,
            "Life-threatening risks",
            "Do not travel. The U.S. government may have limited ability to help.",
        ),
    )

    Column(modifier = Modifier.verticalScroll(scrollState)) {
        Text(
            "Advisory levels",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Each destination gets an official U.S. State Department level from 1 to 4. Higher level means greater risk.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(18.dp))

        levels.forEachIndexed { index, (level, headline, detail) ->
            AnimatedVisibility(
                visible = revealed,
                enter = fadeIn(tween(durationMillis = 350, delayMillis = index * 70)) +
                    slideInVertically(
                        animationSpec = tween(durationMillis = 350, delayMillis = index * 70),
                        initialOffsetY = { it / 4 },
                    ),
            ) {
                LevelCard(
                    level = level,
                    headline = headline,
                    detail = detail,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
        }

        AnimatedVisibility(
            visible = revealed,
            enter = fadeIn(tween(durationMillis = 350, delayMillis = 320)) +
                slideInVertically(
                    animationSpec = tween(durationMillis = 350, delayMillis = 320),
                    initialOffsetY = { it / 4 },
                ),
        ) {
            HigherRiskCallout(modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
        }

        Text(
            "Advisories are written for U.S. citizens and are not a universal safety rating. Always consult the official advisory before travel.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun LevelCard(
    level: AdvisoryLevel,
    headline: String,
    detail: String,
    modifier: Modifier = Modifier,
) {
    val accent = levelColor(level)
    val container = levelContainerColor(level)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(accent),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(container),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${level.number}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                    )
                }
                Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                    Text(
                        text = AdvisoryLevels.label(level),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = accent,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun HigherRiskCallout(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Special case: higher-risk areas",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Some countries have the same base level but include specific areas with higher security risk. Waymark marks them separately.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            LevelChip(
                level = AdvisoryLevel.LEVEL_2,
                higherRisk = true,
                showLabel = true,
                compact = true,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Example: Level 2, with higher-risk areas.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CountryStep(
    query: String,
    onQueryChange: (String) -> Unit,
    countries: List<CountryCard>,
    selected: MutableList<String>,
    allowMobileData: Boolean,
    onRequestSync: (Boolean) -> Unit,
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
            if (countries.isEmpty()) "Loading country catalogue… (this may take a moment)" else "${selected.size} selected",
            modifier = Modifier.padding(vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        if (countries.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.material3.CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Downloading catalogue…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(countries, key = { it.id }) { card ->
                    Surface(
                        onClick = {
                            if (card.id in selected) selected.remove(card.id) else selected.add(card.id)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.background,
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
}

@Composable
private fun PermissionsStep(
    notificationsEnabled: Boolean,
    onNotificationsChange: (Boolean) -> Unit,
    allowMobileData: Boolean,
    onAllowMobileDataChange: (Boolean) -> Unit,
    selectedCount: Int,
    onRequestBatteryExemption: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var ignoringBattery by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                ignoringBattery = isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column {
        Text("Permissions & network", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Choose how Waymark keeps you informed and downloads updates. You can change these later in Settings.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))

        Text("Notifications", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Advisory notifications")
                Text(
                    "Notify about meaningful changes for the countries you follow. Uses Android notifications.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = notificationsEnabled, onCheckedChange = onNotificationsChange)
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Text("Network", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Use mobile data for updates")
                Text(
                    "When off, Waymark syncs only on Wi-Fi. Default is Wi-Fi only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = allowMobileData, onCheckedChange = onAllowMobileDataChange)
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Text("Background updates", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            "To reduce delays in background synchronization, you can allow Waymark to ignore battery optimizations. This is optional.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        if (ignoringBattery) {
            Text(
                "Battery optimization exemption is already allowed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            OutlinedButton(onClick = onRequestBatteryExemption) {
                Text("Allow background updates")
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "You can change these later in Settings. Waymark will not ask for notification permission unless you enable notifications.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

@SuppressLint("BatteryLife")
private fun requestBatteryExemption(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
    try {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        } catch (_: Exception) {
            // No settings activity available.
        }
    }
}
