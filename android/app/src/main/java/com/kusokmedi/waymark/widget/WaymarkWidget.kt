package com.kusokmedi.waymark.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.kusokmedi.waymark.data.AdvisoryRepository
import com.kusokmedi.waymark.data.local.WaymarkDatabase
import com.kusokmedi.waymark.data.local.WidgetConfigEntity
import com.kusokmedi.waymark.domain.AdvisoryLevels
import com.kusokmedi.waymark.domain.CountryCard
import com.kusokmedi.waymark.ui.MainActivity
import java.util.Locale

private val COUNTRY_ID_KEY = ActionParameters.Key<String>("countryId")

class WaymarkWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(
        setOf(DpSize(110.dp, 80.dp), DpSize(180.dp, 110.dp), DpSize(250.dp, 150.dp), DpSize(320.dp, 240.dp))
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val data = loadWidgetData(context, widgetId, forcedPreset = null)
        provideContent {
            WidgetContent(context, data.config, data.cards)
        }
    }
}

class CompactWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(
        setOf(DpSize(160.dp, 60.dp), DpSize(220.dp, 80.dp), DpSize(300.dp, 110.dp))
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val data = loadWidgetData(context, widgetId, forcedPreset = "compact")
        provideContent {
            WidgetContent(context, data.config, data.cards.take(1))
        }
    }
}

private data class WidgetData(val config: WidgetConfigEntity, val cards: List<CountryCard>)

private suspend fun loadWidgetData(
    context: Context,
    widgetId: Int,
    forcedPreset: String?,
): WidgetData {
    val db = WaymarkDatabase.get(context)
    val stored = db.widgetConfigDao().get(widgetId) ?: WidgetConfigEntity(widgetId = widgetId)
    val config = if (forcedPreset == null) stored else stored.copy(preset = forcedPreset)
    val ids = config.countryIds.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    val cards = if (ids.isEmpty()) {
        // A fresh widget follows the watchlist until it is configured explicitly.
        val watchlistIds = db.watchlistDao().getAll().map { it.countryId }
        AdvisoryRepository(db).countryCardsByIds(watchlistIds)
    } else {
        AdvisoryRepository(db).countryCardsByIds(ids)
    }
    return WidgetData(config, cards)
}

@Composable
private fun WidgetContent(context: Context, config: WidgetConfigEntity, cards: List<CountryCard>) {
    val dark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES
    val background = if (config.backgroundStyle == "transparent") {
        ColorProvider(Color(if (dark) 0xCC121614 else 0xCCFBF9F4))
    } else {
        ColorProvider(Color(if (dark) 0xFF1B211E else 0xFFFBF9F4))
    }
    val foreground = ColorProvider(Color(if (dark) 0xFFE6EFEA else 0xFF1A2420))

    Box(
        modifier = GlanceModifier.fillMaxSize().background(background).padding(10.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        if (cards.isEmpty()) {
            EmptyWidget(foreground)
        } else {
            when (config.preset) {
                "compact" -> CompactContent(cards.first(), config, foreground)
                "grid" -> GridContent(context, cards, config, foreground)
                else -> ListContent(context, cards, config, foreground)
            }
        }
    }
}

@Composable
private fun EmptyWidget(foreground: ColorProvider) {
    Column(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Text("Waymark", style = TextStyle(color = foreground, fontWeight = FontWeight.Bold))
        Text(
            "Add countries in Waymark, or configure this widget.",
            style = TextStyle(color = foreground),
        )
    }
}

@Composable
private fun CompactContent(card: CountryCard, config: WidgetConfigEntity, foreground: ColorProvider) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FlagMonogram(card, foreground)
            Spacer(GlanceModifier.width(8.dp))
            Column {
                Text(
                    card.name,
                    style = TextStyle(color = foreground, fontWeight = FontWeight.Bold),
                )
                if (config.showLevel) {
                    Text(
                        levelLine(card),
                        style = TextStyle(color = levelColor(card), fontWeight = FontWeight.Medium),
                    )
                }
            }
        }
        if (config.showDate) {
            Spacer(GlanceModifier.height(4.dp))
            Text(formatDate(card.advisoryDate), style = TextStyle(color = foreground))
        }
    }
}

@Composable
private fun ListContent(
    context: Context,
    cards: List<CountryCard>,
    config: WidgetConfigEntity,
    foreground: ColorProvider,
) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        WidgetHeader(context, foreground)
        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
            items(cards) { card ->
                Row(
                    modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp)
                        .clickableToCountry(card.id),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (config.showFlag) {
                        FlagMonogram(card, foreground)
                        Spacer(GlanceModifier.width(8.dp))
                    }
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        if (config.showCountryName) {
                            Text(
                                card.name,
                                style = TextStyle(color = foreground, fontWeight = FontWeight.Bold),
                            )
                        }
                        if (config.showLevel) {
                            Text(levelLine(card), style = TextStyle(color = levelColor(card)))
                        }
                        if (config.showDate) {
                            Text(formatDate(card.advisoryDate), style = TextStyle(color = foreground))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GridContent(
    context: Context,
    cards: List<CountryCard>,
    config: WidgetConfigEntity,
    foreground: ColorProvider,
) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        WidgetHeader(context, foreground)
        cards.chunked(config.columns.coerceIn(1, 3)).forEach { row ->
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                row.forEach { card ->
                    Column(
                        modifier = GlanceModifier.defaultWeight().padding(4.dp)
                            .clickableToCountry(card.id),
                    ) {
                        if (config.showCountryName) {
                            Text(card.name, style = TextStyle(color = foreground, fontWeight = FontWeight.Bold))
                        }
                        if (config.showLevel) {
                            Text(levelLine(card), style = TextStyle(color = levelColor(card)))
                        }
                        if (config.showDate) {
                            Text(formatDate(card.advisoryDate), style = TextStyle(color = foreground))
                        }
                    }
                }
                if (row.size < config.columns.coerceIn(1, 3)) {
                    Spacer(GlanceModifier.defaultWeight())
                }
            }
        }
    }
}

@Composable
private fun WidgetHeader(context: Context, foreground: ColorProvider) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Waymark",
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(color = foreground, fontWeight = FontWeight.Bold),
        )
        Text(
            "↻",
            modifier = GlanceModifier.clickableRefresh(),
            style = TextStyle(color = foreground),
        )
    }
}

@Composable
private fun FlagMonogram(card: CountryCard, foreground: ColorProvider) {
    Box(
        modifier = GlanceModifier.size(28.dp).background(
            ColorProvider(Color(0x22335F52))
        ).padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            card.isoAlpha2 ?: card.isoAlpha3?.take(2) ?: "?",
            style = TextStyle(color = foreground, fontWeight = FontWeight.Bold),
        )
    }
}

private fun GlanceModifier.clickableToCountry(countryId: String): GlanceModifier =
    this.clickable(actionStartActivity<MainActivity>(actionParametersOf(COUNTRY_ID_KEY to countryId)))

private fun GlanceModifier.clickableRefresh(): GlanceModifier =
    this.clickable(actionRunCallback<RefreshWidgetsAction>())

class RefreshWidgetsAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        WaymarkWidgetUpdater.updateAll(context)
    }
}

private fun levelLine(card: CountryCard): String {
    val level = card.level
    return if (level == null) "Unknown" else "L${level.number} · ${AdvisoryLevels.label(level)}"
}

private fun levelColor(card: CountryCard): ColorProvider = when (card.level) {
    null -> ColorProvider(Color(0xFF6E6E6E))
    else -> when (card.level.number) {
        4 -> ColorProvider(Color(0xFFB3261E))
        3 -> ColorProvider(Color(0xFFB26A00))
        2 -> ColorProvider(Color(0xFF6A5B00))
        else -> ColorProvider(Color(0xFF2E6B4F))
    }
}

private fun formatDate(value: String?): String {
    if (value.isNullOrBlank()) return "Date unknown"
    return try {
        val parsed = java.time.LocalDate.parse(value)
        "Advisory " + parsed.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))
    } catch (_: Exception) {
        "Advisory date unknown"
    }
}
