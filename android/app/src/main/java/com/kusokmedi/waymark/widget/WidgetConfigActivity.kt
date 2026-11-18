package com.kusokmedi.waymark.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.kusokmedi.waymark.data.local.WidgetConfigEntity
import com.kusokmedi.waymark.domain.CountryCard
import com.kusokmedi.waymark.ui.theme.WaymarkTheme
import com.kusokmedi.waymark.waymarkContainer
import kotlinx.coroutines.launch

class WidgetConfigActivity : ComponentActivity() {
    private var widgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        widgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        setResult(RESULT_CANCELED)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val container = application.waymarkContainer()
        setContent {
            WaymarkTheme {
                WidgetConfigScreen(
                    widgetId = widgetId,
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
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetConfigScreen(
    widgetId: Int,
    onCancel: () -> Unit,
    onSave: (WidgetConfigEntity) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val container = remember { context.applicationContext.waymarkContainer() }
    val countries by container.repository.allCountries.collectAsState(initial = emptyList())
    val selected = remember { mutableStateListOf<String>() }
    var loaded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var preset by remember { mutableStateOf("list") }
    var showDate by remember { mutableStateOf(true) }
    var showLevel by remember { mutableStateOf(true) }
    var showFlag by remember { mutableStateOf(true) }
    var showName by remember { mutableStateOf(true) }
    var transparent by remember { mutableStateOf(false) }
    var columns by remember { mutableStateOf(1f) }

    LaunchedEffect(Unit) {
        val existing = container.database.widgetConfigDao().get(widgetId)
        if (existing != null) {
            selected.addAll(existing.countryIds.split(',').map { it.trim() }.filter { it.isNotEmpty() })
            preset = existing.preset
            showDate = existing.showDate
            showLevel = existing.showLevel
            showFlag = existing.showFlag
            showName = existing.showCountryName
            transparent = existing.backgroundStyle == "transparent"
            columns = existing.columns.toFloat().coerceIn(1f, 3f)
        }
        loaded = true
    }

    val filtered = remember(countries, query) {
        if (query.isBlank()) countries
        else countries.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.isoAlpha2?.equals(query, ignoreCase = true) == true ||
                it.isoAlpha3?.equals(query, ignoreCase = true) == true
        }
    }
    val byId = remember(countries) { countries.associateBy { it.id } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Widget settings") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            onSave(
                                WidgetConfigEntity(
                                    widgetId = widgetId,
                                    preset = preset,
                                    countryIds = selected.joinToString(","),
                                    showDate = showDate,
                                    showLevel = showLevel,
                                    showFlag = showFlag,
                                    showCountryName = showName,
                                    backgroundStyle = if (transparent) "transparent" else "surface",
                                    columns = columns.toInt().coerceIn(1, 3),
                                    updatedAt = System.currentTimeMillis(),
                                )
                            )
                        },
                    ) { Text("Save") }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            if (!loaded) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading countries…")
                }
                return@Column
            }
            Text("Layout", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("compact" to "Compact", "list" to "List", "grid" to "Grid").forEach { (id, label) ->
                    FilterChip(selected = preset == id, onClick = { preset = id }, label = { Text(label) })
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Columns", modifier = Modifier.width(90.dp))
                Slider(
                    value = columns,
                    onValueChange = { columns = it },
                    valueRange = 1f..3f,
                    steps = 1,
                    modifier = Modifier.weight(1f),
                )
                Text(columns.toInt().toString(), modifier = Modifier.width(24.dp))
            }
            ToggleRow("Show advisory date", showDate) { showDate = it }
            ToggleRow("Show advisory level", showLevel) { showLevel = it }
            ToggleRow("Show flag monogram", showFlag) { showFlag = it }
            ToggleRow("Show country name", showName) { showName = it }
            ToggleRow("Transparent background", transparent) { transparent = it }

            Spacer(Modifier.height(8.dp))
            Text(
                "Selected countries (${selected.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (selected.isEmpty()) {
                Text(
                    "No countries selected. The widget will follow your watchlist.",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    selected.take(6).forEachIndexed { index, id ->
                        val card = byId[id]
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                card?.name ?: id,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            IconButton(
                                onClick = {
                                    if (index > 0) {
                                        val item = selected.removeAt(index)
                                        selected.add(index - 1, item)
                                    }
                                },
                            ) { Icon(Icons.Default.ArrowUpward, contentDescription = "Move up") }
                            IconButton(
                                onClick = {
                                    if (index < selected.lastIndex) {
                                        val item = selected.removeAt(index)
                                        selected.add(index + 1, item)
                                    }
                                },
                            ) { Icon(Icons.Default.ArrowDownward, contentDescription = "Move down") }
                        }
                    }
                    if (selected.size > 6) {
                        Text("…and ${selected.size - 6} more", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search countries") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
            )
            LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                items(filtered, key = { it.id }) { card ->
                    CountrySelectionRow(
                        card = card,
                        checked = card.id in selected,
                        count = selected.size,
                        onToggle = {
                            if (card.id in selected) selected.remove(card.id) else selected.add(card.id)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun CountrySelectionRow(
    card: CountryCard,
    checked: Boolean,
    count: Int,
    onToggle: () -> Unit,
) {
    Surface(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
            Text(card.name, modifier = Modifier.weight(1f))
            Text(
                card.isoAlpha2 ?: card.isoAlpha3 ?: "",
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
