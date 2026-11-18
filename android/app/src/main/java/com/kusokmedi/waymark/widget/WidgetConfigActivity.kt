package com.kusokmedi.waymark.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.kusokmedi.waymark.data.local.WidgetConfigEntity
import com.kusokmedi.waymark.ui.theme.WaymarkTheme
import com.kusokmedi.waymark.waymarkContainer
import kotlinx.coroutines.launch

class WidgetConfigActivity : ComponentActivity() {
    private var widgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        val container = application.waymarkContainer()
        setContent {
            WaymarkTheme {
                WidgetSettingsScreen(
                    onCancel = { finish() },
                    onSave = { config ->
                        lifecycleScope.launch {
                            container.database.widgetConfigDao().upsert(config)
                            WaymarkWidgetUpdater.updateAll(this@WidgetConfigActivity)
                            setResult(
                                RESULT_OK,
                                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId),
                            )
                            finish()
                        }
                    },
                    loadConfig = {
                        container.database.widgetConfigDao().get(widgetId)
                            ?: WidgetConfigEntity(widgetId = widgetId, preset = "watchlist", widgetType = "watchlist")
                    },
                    widgetId = widgetId,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetSettingsScreen(
    widgetId: Int,
    loadConfig: suspend () -> WidgetConfigEntity,
    onCancel: () -> Unit,
    onSave: (WidgetConfigEntity) -> Unit,
) {
    var loaded by remember { mutableStateOf(false) }
    var showFlag by remember { mutableStateOf(true) }
    var showName by remember { mutableStateOf(true) }
    var showLevel by remember { mutableStateOf(true) }
    var showDate by remember { mutableStateOf(true) }
    var transparent by remember { mutableStateOf(false) }
    var cornerRadius by remember { mutableFloatStateOf(24f) }
    var base by remember { mutableStateOf<WidgetConfigEntity?>(null) }

    LaunchedEffect(widgetId) {
        val cfg = loadConfig()
        base = cfg
        showFlag = cfg.showFlag
        showName = cfg.showCountryName
        showLevel = cfg.showLevel
        showDate = cfg.showDate
        transparent = cfg.backgroundStyle == "transparent"
        cornerRadius = cfg.cornerRadius.coerceIn(8, 36).toFloat()
        loaded = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Widget settings") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val current = base ?: WidgetConfigEntity(widgetId = widgetId)
                            onSave(
                                current.copy(
                                    preset = "watchlist",
                                    widgetType = "watchlist",
                                    showFlag = showFlag,
                                    showCountryName = showName,
                                    showLevel = showLevel,
                                    showDate = showDate,
                                    backgroundStyle = if (transparent) "transparent" else "surface",
                                    cornerRadius = cornerRadius.toInt().coerceIn(8, 36),
                                    updatedAt = System.currentTimeMillis(),
                                )
                            )
                        },
                        enabled = loaded,
                    ) { Text("Save") }
                },
            )
        },
    ) { padding ->
        if (!loaded) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text("Loading…")
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
                "This widget shows countries from your watchlist.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text("Corner radius: ${cornerRadius.toInt()} dp")
            Slider(
                value = cornerRadius,
                onValueChange = { cornerRadius = it },
                valueRange = 8f..36f,
                steps = 27,
            )
            ToggleRow("Transparent background", "Transparency is off by default.", transparent) { transparent = it }

            Spacer(Modifier.height(16.dp))
            Text("Visible elements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            ToggleRow("Show country flag", null, showFlag) { showFlag = it }
            ToggleRow("Show country name", null, showName) { showName = it }
            ToggleRow("Show advisory level", null, showLevel) { showLevel = it }
            ToggleRow("Show advisory date", null, showDate) { showDate = it }
        }
    }
}

@Composable
private fun ToggleRow(title: String, description: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title)
            if (!description.isNullOrBlank()) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
