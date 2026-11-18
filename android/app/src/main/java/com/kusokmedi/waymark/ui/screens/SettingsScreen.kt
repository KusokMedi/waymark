package com.kusokmedi.waymark.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kusokmedi.waymark.BuildConfig
import com.kusokmedi.waymark.SyncScheduler
import com.kusokmedi.waymark.data.local.SyncStateEntity
import com.kusokmedi.waymark.data.local.ThemeMode
import com.kusokmedi.waymark.sync.ChangeNotifier
import com.kusokmedi.waymark.ui.AppViewModel
import com.kusokmedi.waymark.ui.components.SyncBanner
import com.kusokmedi.waymark.ui.components.formatTimestamp
import com.kusokmedi.waymark.waymarkContainer
import android.os.PowerManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHomeScreen(onOpen: (String) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Settings", fontWeight = FontWeight.Bold) }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item { SettingsNavItem("Appearance", "Theme and dynamic colors") { onOpen("settings/appearance") } }
            item { SettingsNavItem("Synchronisation", "Background updates and sync status") { onOpen("settings/sync") } }
            item { SettingsNavItem("Notifications", "Permissions, sound and priority") { onOpen("settings/notifications") } }
            item { SettingsNavItem("Data management", "Backup, restore and history") { onOpen("settings/data") } }
            item { SettingsNavItem("About", "Version, privacy and licenses") { onOpen("settings/about") } }
        }
    }
}

@Composable
private fun SettingsNavItem(title: String, subtitle: String, onClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance") },
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Theme", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(ThemeMode.SYSTEM to "System", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark").forEach { (mode, label) ->
                    FilterChip(selected = settings.themeMode == mode, onClick = { viewModel.setThemeMode(mode) }, label = { Text(label) })
                }
            }
            ToggleSetting(
                title = "Dynamic color",
                description = "Use Android system colors on Android 12 and later (applies to app and widgets).",
                checked = settings.dynamicColor,
                onCheckedChange = viewModel::setDynamicColor,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncSettingsScreen(
    viewModel: AppViewModel,
    syncState: SyncStateEntity?,
    onBack: () -> Unit,
    onOpenDetails: () -> Unit,
) {
    val settings by viewModel.settings.collectAsState()
    Scaffold(
        topBar = { TopAppBar(title = { Text("Synchronisation") }, navigationIcon = { BackButton(onBack) }) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SyncBanner(syncState, onClick = onOpenDetails)
            ToggleSetting(
                title = "Automatic background updates",
                description = "Android may delay background work. Advisories remain available offline.",
                checked = settings.automaticSync,
                onCheckedChange = viewModel::setAutomaticSync,
            )
            ToggleSetting(
                title = "Use mobile data for updates",
                description = "When off, Waymark syncs only on Wi-Fi (default).",
                checked = settings.allowMobileData,
                onCheckedChange = viewModel::setAllowMobileData,
            )
            Text("Update interval", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(6 to "6 hours", 12 to "12 hours", 24 to "24 hours").forEach { (h, label) ->
                    FilterChip(selected = settings.syncIntervalHours == h, onClick = { viewModel.setSyncInterval(h) }, label = { Text(label) })
                }
            }
            Button(onClick = viewModel::syncNow) { Text("Sync now") }
            OutlinedButton(onClick = onOpenDetails) { Text("View sync details") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncDetailsScreen(syncState: SyncStateEntity?, onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Sync details") }, navigationIcon = { BackButton(onBack) }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (syncState == null) {
                Text("No synchronization data yet.")
                return@Column
            }
            DetailRow("Status", syncState.status)
            DetailRow("Last attempt", syncState.lastAttemptAt?.let(::formatTimestamp) ?: "never")
            DetailRow("Last successful update", syncState.lastSuccessAt?.let(::formatTimestamp) ?: "never")
            DetailRow("Records", syncState.recordCount.toString())
            DetailRow("First sync completed", syncState.firstSyncCompleted.toString())
            Text("Message", fontWeight = FontWeight.SemiBold)
            Text(
                syncState.message?.ifBlank { "No error message reported." } ?: "No error message reported.",
                style = MaterialTheme.typography.bodyMedium,
                color = if (syncState.status == "failed" || syncState.status == "offline")
                    MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val container = remember { context.applicationContext.waymarkContainer() }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            viewModel.setNotificationsEnabled(false)
            scope.launch { snackbar.showSnackbar("Notification permission was denied.") }
        }
    }

    val soundPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val value = when {
                uri == null -> "silent"
                uri == defaultUri || uri == Settings.System.DEFAULT_NOTIFICATION_URI -> ""
                else -> uri.toString()
            }
            viewModel.setNotificationSoundUri(value)
            container.notifier.applySettings(value, settings.notificationImportance)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Notifications") }, navigationIcon = { BackButton(onBack) }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
            HorizontalDivider()

            Text("Notification priority", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("high" to "High", "default" to "Default", "low" to "Low").forEach { (value, label) ->
                    FilterChip(
                        selected = settings.notificationImportance == value,
                        onClick = {
                            viewModel.setNotificationImportance(value)
                            container.notifier.applySettings(settings.notificationSoundUri, value)
                        },
                        label = { Text(label) },
                    )
                }
            }
            Text(
                "Default is High. On Android 8+, changing priority or sound may update notification channel settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedButton(onClick = {
                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Notification sound")
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                    val existing = settings.notificationSoundUri
                    val existingUri = when {
                        existing.isBlank() -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        existing == "silent" -> null
                        else -> runCatching { Uri.parse(existing) }.getOrNull()
                    }
                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
                }
                soundPicker.launch(intent)
            }) { Text("Change notification sound") }

            val soundLabel = when {
                settings.notificationSoundUri.isBlank() -> "Default system sound"
                settings.notificationSoundUri == "silent" -> "Silent"
                else -> runCatching {
                    RingtoneManager.getRingtone(context, Uri.parse(settings.notificationSoundUri))
                        ?.getTitle(context)
                }.getOrNull() ?: "Custom sound"
            }
            Text("Current: $soundLabel", style = MaterialTheme.typography.bodySmall)

            HorizontalDivider()
            Text("Background updates", style = MaterialTheme.typography.titleSmall)
            Text(
                "To reduce delays, you can allow Waymark to ignore battery optimizations (optional).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
            if (ignoringBattery) {
                OutlinedButton(onClick = {}, enabled = false) { Text("Battery optimization allowed") }
                Text(
                    "Battery optimization exemption is already allowed for Waymark.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                OutlinedButton(onClick = { requestBatteryExemption(context) }) { Text("Allow background updates") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val container = remember { context.applicationContext.waymarkContainer() }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var replaceOnRestore by remember { mutableStateOf(false) }

    val jsonExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            try {
                val content = container.backupManager.exportJson()
                context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
                    ?: throw IOException("Could not open the selected file")
                snackbar.showSnackbar("JSON backup saved.")
            } catch (e: Exception) { snackbar.showSnackbar("Export failed: ${e.message}") }
        }
    }
    val zipExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) scope.launch {
            try {
                val content = container.backupManager.exportZip()
                context.contentResolver.openOutputStream(uri)?.use { it.write(content) }
                    ?: throw IOException("Could not open the selected file")
                snackbar.showSnackbar("ZIP backup saved.")
            } catch (e: Exception) { snackbar.showSnackbar("Export failed: ${e.message}") }
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IOException("Could not read the selected file")
                val result = container.backupManager.restore(bytes, replaceOnRestore)
                if (result.successful) {
                    val s = container.settings.settings.first()
                    if (s.automaticSync) {
                        SyncScheduler.schedulePeriodic(context, s.syncIntervalHours, s.allowMobileData)
                    } else {
                        SyncScheduler.cancelPeriodic(context)
                    }
                }
                val detail = if (result.warnings.isEmpty()) result.message else "${result.message} ${result.warnings.size} warning(s)."
                snackbar.showSnackbar(detail)
            } catch (e: Exception) { snackbar.showSnackbar("Import failed: ${e.message}") }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Data management") }, navigationIcon = { BackButton(onBack) }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Backups include your watchlist, cached advisories, history, widget configurations and preferences. They never leave this device unless you export them.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { jsonExport.launch("waymark-backup-${BuildConfig.VERSION_NAME}.json") }) { Text("Export JSON") }
                OutlinedButton(onClick = { zipExport.launch("waymark-backup-${BuildConfig.VERSION_NAME}.zip") }) { Text("Export ZIP") }
            }
            OutlinedButton(onClick = { importer.launch(arrayOf("application/json", "application/zip", "application/octet-stream")) }) { Text("Import backup") }
            ToggleSetting(
                title = "Replace existing data when importing",
                description = "Off merges the backup into your current data. On deletes current local data first.",
                checked = replaceOnRestore,
                onCheckedChange = { replaceOnRestore = it },
            )
            HorizontalDivider()
            OutlinedButton(onClick = viewModel::clearHistory) { Text("Clear advisory history") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var showPrivacy by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }
    Scaffold(topBar = { TopAppBar(title = { Text("About") }, navigationIcon = { BackButton(onBack) }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Waymark", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Version ${BuildConfig.VERSION_NAME}")
            Text(
                "Waymark is an independent, local-first app for official U.S. Department of State travel advisories. It is not affiliated with or endorsed by the U.S. Department of State.",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = { showPrivacy = true }) { Text("Privacy and data sources") }
            TextButton(onClick = { showLicenses = true }) { Text("Open-source licenses") }
            TextButton(onClick = { openUrl(context, "https://travel.state.gov/en/international-travel/travel-advisories.html") }) { Text("Official travel advisories") }
            TextButton(onClick = { openUrl(context, "https://github.com/KusokMedi/waymark") }) { Text("GitHub repository") }
            Text(
                "GPL-3.0. Advisories are written for U.S. citizens and are not a universal safety rating.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
                        "Kotlin is licensed under the Apache License 2.0. Full license texts are available in the repository."
                )
            },
            confirmButton = { TextButton(onClick = { showLicenses = false }) { Text("Close") } },
        )
    }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
}

@Composable
private fun ToggleSetting(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(label, fontWeight = FontWeight.SemiBold)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

private fun requestBatteryExemption(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
    try {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) } catch (_: Exception) {}
    }
}

private fun openUrl(context: Context, url: String) {
    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (_: Exception) {}
}
