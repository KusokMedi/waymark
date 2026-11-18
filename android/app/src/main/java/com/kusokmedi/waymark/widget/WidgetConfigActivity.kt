package com.kusokmedi.waymark.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kusokmedi.waymark.data.local.WidgetConfigEntity
import com.kusokmedi.waymark.ui.theme.WaymarkTheme
import com.kusokmedi.waymark.waymarkContainer
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class WidgetConfigActivity : ComponentActivity() {
    private var widgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        widgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))

        val container = application.waymarkContainer()
        setContent {
            WaymarkTheme {
                WidgetSettingsScreen(
                    widgetId = widgetId,
                    load = {
                        container.database.widgetConfigDao().get(widgetId)
                            ?: WidgetConfigEntity(widgetId = widgetId)
                    },
                    onCancel = { finish() },
                    onSave = { config ->
                        container.database.widgetConfigDao().upsert(config)
                        WaymarkWidgetUpdater.updateAll(this@WidgetConfigActivity)
                        setResult(
                            RESULT_OK,
                            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId),
                        )
                        finish()
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetSettingsScreen(
    widgetId: Int,
    load: suspend () -> WidgetConfigEntity,
    onCancel: () -> Unit,
    onSave: suspend (WidgetConfigEntity) -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var base by remember { mutableStateOf<WidgetConfigEntity?>(null) }

    var showFlag by remember { mutableStateOf(true) }
    var showName by remember { mutableStateOf(true) }
    var showLevel by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(true) }
    var cornerRadius by remember { mutableFloatStateOf(12f) }
    var backgroundAlpha by remember { mutableFloatStateOf(100f) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(widgetId) {
        val cfg = load()
        base = cfg
        showFlag = cfg.showFlag
        showName = cfg.showCountryName
        showLevel = cfg.showLevel
        showDate = cfg.showDate
        cornerRadius = cfg.cornerRadius.coerceIn(0, 20).toFloat()
        backgroundAlpha = cfg.backgroundAlpha.coerceIn(0, 100).toFloat()
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Widget settings") },
                navigationIcon = {
                    IconButton(onClick = onCancel, enabled = !saving) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (loading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Loading settings…")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(
                "Customize how your Waymark watchlist widget looks.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))
            Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text("Corner radius: ${cornerRadius.roundToInt()} dp", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = cornerRadius,
                onValueChange = { cornerRadius = it },
                valueRange = 0f..20f,
                steps = 20,
            )

            Spacer(Modifier.height(12.dp))
            Text("Background opacity: ${backgroundAlpha.roundToInt()}%", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = backgroundAlpha,
                onValueChange = { backgroundAlpha = it },
                valueRange = 0f..100f,
                steps = 100,
            )

            Spacer(Modifier.height(16.dp))
            Text("Visible elements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            SettingToggle("Show flag", null, showFlag) { showFlag = it }
            SettingToggle("Show country name", null, showName) { showName = it }
            SettingToggle("Show advisory level", null, showLevel) { showLevel = it }
            SettingToggle("Show advisory date", null, showDate) { showDate = it }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (saving) return@Button
                    saving = true
                    val current = base ?: WidgetConfigEntity(widgetId = widgetId)
                    val updated = current.copy(
                        preset = "watchlist",
                        widgetType = "watchlist",
                        singleCountryId = null,
                        showFlag = showFlag,
                        showCountryName = showName,
                        showLevel = showLevel,
                        showDate = showDate,
                        backgroundStyle = "surface",
                        backgroundAlpha = backgroundAlpha.roundToInt().coerceIn(0, 100),
                        cornerRadius = cornerRadius.roundToInt().coerceIn(0, 20),
                        updatedAt = System.currentTimeMillis(),
                    )
                    scope.launch {
                        try {
                            onSave(updated)
                        } finally {
                            saving = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !saving,
            ) {
                Text(if (saving) "Saving…" else "Save changes")
            }
        }
    }
}

@Composable
private fun SettingToggle(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
