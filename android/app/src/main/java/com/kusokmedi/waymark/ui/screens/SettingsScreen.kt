package com.kusokmedi.waymark.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kusokmedi.waymark.BuildConfig
import com.kusokmedi.waymark.data.local.ThemeMode
import com.kusokmedi.waymark.ui.AppViewModel
import com.kusokmedi.waymark.ui.components.SyncBanner
import com.kusokmedi.waymark.waymarkContainer
import kotlinx.coroutines.launch
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val container = remember { context.applicationContext.waymarkContainer() }
    val settings by viewModel.settings.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var replaceOnRestore by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }

    val jsonExport = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val content = container.backupManager.exportJson()
                    context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
                        ?: throw IOException("Could not open the selected file")
                    snackbar.showSnackbar("JSON backup saved.")
                } catch (e: Exception) {
                    snackbar.showSnackbar("Export failed: ${e.message}")
                }
            }
        }
    }
    val zipExport = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val content = container.backupManager.exportZip()
                    context.contentResolver.openOutputStream(uri)?.use { it.write(content) }
                        ?: throw IOException("Could not open the selected file")
                    snackbar.showSnackbar("ZIP backup saved.")
                } catch (e: Exception) {
                    snackbar.showSnackbar("Export failed: ${e.message}")
                }
            }
        }
    }
    val importer = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw IOException("Could not read the selected file")
                    val result = container.backupManager.restore(bytes, replaceOnRestore)
                    val detail = if (result.warnings.isEmpty()) {
                        result.message
                    } else {
                        "${result.message} ${result.warnings.size} warning(s)."
                    }
                    snackbar.showSnackbar(detail)
                } catch (e: Exception) {
                    snackbar.showSnackbar("Import failed: ${e.message}")
                }
            }
        }
    }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            viewModel.setNotificationsEnabled(false)
            scope.launch { snackbar.showSnackbar("Notification permission was denied.") }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Settings", fontWeight = FontWeight.Bold) })
        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                SettingsSection("Appearance") {
                    SettingsChoice(
                        label = "Theme",
                        options = listOf(
                            ThemeMode.SYSTEM to "System",
                            ThemeMode.LIGHT to "Light",
                            ThemeMode.DARK to "Dark",
                        ),
                        selected = settings.themeMode,
                        onSelected = viewModel::setThemeMode,
                    )
                    ToggleSetting(
                        title = "Dynamic color",
                        description = "Use Android system colors on Android 12 and later.",
                        checked = settings.dynamicColor,
                        onCheckedChange = viewModel::setDynamicColor,
                    )
                }
            }
            item {
                SettingsSection("Synchronization") {
                    SyncBanner(syncState)
                    ToggleSetting(
                        title = "Automatic background updates",
                        description = "Android may delay background work. Advisories remain available offline.",
                        checked = settings.automaticSync,
                        onCheckedChange = viewModel::setAutomaticSync,
                    )
                    SettingsChoice(
                        label = "Update interval",
                        options = listOf(6 to "6 hours", 12 to "12 hours", 24 to "24 hours"),
                        selected = settings.syncIntervalHours,
                        onSelected = viewModel::setSyncInterval,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = viewModel::syncNow) { Text("Sync now") }
                    }
                }
            }
            item {
                SettingsSection("Notifications") {
                    ToggleSetting(
                        title = "Advisory notifications",
                        description = "Opt-in. Notifications are sent only for countries you follow and only for meaningful changes.",
                        checked = settings.notificationsEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            viewModel.setNotificationsEnabled(enabled)
                        },
                    )
                    Text(
                        "Per-country switches are available on each country page.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                SettingsSection("Data management") {
                    Text(
                        "Backups include your watchlist, cached advisories, history, widget configurations and preferences. They never leave this device unless you export them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            jsonExport.launch("waymark-backup-${BuildConfig.VERSION_NAME}.json")
                        }) { Text("Export JSON") }
                        OutlinedButton(onClick = {
                            zipExport.launch("waymark-backup-${BuildConfig.VERSION_NAME}.zip")
                        }) { Text("Export ZIP") }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = {
                        importer.launch(arrayOf("application/json", "application/zip", "application/octet-stream"))
                    }) { Text("Import backup") }
                    ToggleSetting(
                        title = "Replace existing data when importing",
                        description = "Off merges the backup into your current data. On deletes current local data first.",
                        checked = replaceOnRestore,
                        onCheckedChange = { replaceOnRestore = it },
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    OutlinedButton(onClick = viewModel::clearHistory) { Text("Clear advisory history") }
                }
            }
            item {
                SettingsSection("About Waymark") {
                    Text("Version ${BuildConfig.VERSION_NAME}", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Waymark is an independent, local-first app for official U.S. Department of State travel advisories. It is not affiliated with or endorsed by the U.S. Department of State.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { showPrivacy = true }) { Text("Privacy and data sources") }
                    TextButton(onClick = { showLicenses = true }) { Text("Open-source licenses") }
                    TextButton(onClick = {
                        openUrl(context, "https://travel.state.gov/en/international-travel/travel-advisories.html")
                    }) { Text("Official travel advisories") }
                    TextButton(onClick = {
                        openUrl(context, "https://github.com/KusokMedi/waymark")
                    }) { Text("GitHub repository") }
                    Text(
                        "GPL-3.0. Advisories are written for U.S. citizens and are not a universal safety rating.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
        SnackbarHost(snackbar)
    }

    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = { showPrivacy = false },
            title = { Text("Privacy and data sources") },
            text = {
                Text(
                    "Waymark keeps your watchlist, favorites, history, widget settings and preferences on this device. There is no Waymark account, backend, analytics, telemetry or advertising SDK.\n\n" +
                        "The app connects only to the official U.S. Department of State ArcGIS travel advisory service to retrieve public advisory data. Cached data remains available offline. The app does not upload user-specific information.\n\n" +
                        "Waymark is independent and is not an official U.S. government product."
                )
            },
            confirmButton = { TextButton(onClick = { showPrivacy = false }) { Text("Close") } },
        )
    }
    if (showLicenses) {
        AlertDialog(
            onDismissRequest = { showLicenses = false },
            title = { Text("Open-source licenses") },
            text = {
                Text(
                    "Waymark is licensed under GPL-3.0.\n\n" +
                        "It uses AndroidX, Jetpack Compose, Material 3, Room, DataStore, WorkManager, Glance, OkHttp and Kotlinx Serialization under the Apache License 2.0. " +
                        "Kotlin is licensed under the Apache License 2.0. ISO country names are derived from i18n-iso-countries (MIT). " +
                        "Full license texts are available in the repository's LICENSE and dependency notices."
                )
            },
            confirmButton = { TextButton(onClick = { showLicenses = false }) { Text("Close") } },
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        content()
        Spacer(Modifier.height(6.dp))
        HorizontalDivider()
    }
}

@Composable
private fun ToggleSetting(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun <T> SettingsChoice(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, text) ->
                FilterChip(
                    selected = value == selected,
                    onClick = { onSelected(value) },
                    label = { Text(text) },
                )
            }
        }
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: Exception) {
        // No browser is available.
    }
}
