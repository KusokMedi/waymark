package com.kusokmedi.waymark.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.glance.appwidget.cornerRadius
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
import com.kusokmedi.waymark.SyncScheduler
import com.kusokmedi.waymark.data.AdvisoryRepository
import com.kusokmedi.waymark.data.local.SyncStateEntity
import com.kusokmedi.waymark.data.local.WaymarkDatabase
import com.kusokmedi.waymark.data.local.WidgetConfigEntity
import com.kusokmedi.waymark.domain.AdvisoryLevels
import com.kusokmedi.waymark.domain.CountryCard
import com.kusokmedi.waymark.domain.flagEmoji
import com.kusokmedi.waymark.ui.MainActivity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val COUNTRY_ID_KEY = ActionParameters.Key<String>("countryId")

class WatchlistWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 110.dp),
            DpSize(180.dp, 110.dp),
            DpSize(250.dp, 180.dp),
            DpSize(320.dp, 280.dp),
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val data = loadWidgetData(context, widgetId, WidgetKind.WATCHLIST)
        provideContent { WatchlistWidgetContent(context, data) }
    }
}

class SingleCountryWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 110.dp),
            DpSize(250.dp, 110.dp),
            DpSize(250.dp, 180.dp),
            DpSize(320.dp, 240.dp),
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val data = loadWidgetData(context, widgetId, WidgetKind.SINGLE)
        provideContent { SingleCountryWidgetContent(context, data) }
    }
}

internal enum class WidgetKind(val type: String) {
    WATCHLIST("watchlist"),
    SINGLE("single"),
}

internal data class WidgetData(
    val config: WidgetConfigEntity,
    val cards: List<CountryCard>,
    val syncState: SyncStateEntity?,
)

internal suspend fun loadWidgetData(context: Context, widgetId: Int, kind: WidgetKind): WidgetData {
    val db = WaymarkDatabase.get(context)
    val existing = db.widgetConfigDao().get(widgetId)
    val config = when {
        existing != null -> existing.copy(widgetType = kind.type)
        kind == WidgetKind.WATCHLIST -> WidgetConfigEntity(
            widgetId = widgetId,
            preset = "watchlist",
            widgetType = WidgetKind.WATCHLIST.type,
            updatedAt = System.currentTimeMillis(),
        ).also { db.widgetConfigDao().upsert(it) }
        else -> WidgetConfigEntity(
            widgetId = widgetId,
            preset = "single",
            widgetType = WidgetKind.SINGLE.type,
            updatedAt = System.currentTimeMillis(),
        )
    }

    val repo = AdvisoryRepository(db)
    val cards = when (kind) {
        WidgetKind.WATCHLIST -> {
            val ids = db.watchlistDao().getAll().map { it.countryId }
            repo.countryCardsByIds(ids)
        }
        WidgetKind.SINGLE -> {
            val id = config.singleCountryId
            if (id.isNullOrBlank()) emptyList() else repo.countryCardsByIds(listOf(id))
        }
    }
    val syncState = db.syncStateDao().get()
    return WidgetData(config, cards, syncState)
}

@Composable
internal fun WatchlistWidgetContent(context: Context, data: WidgetData) {
    val palette = widgetPalette(context)
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(palette.background)
            .padding(12.dp),
    ) {
        if (data.cards.isEmpty()) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                WidgetHeader(palette, data.syncState)
                Spacer(GlanceModifier.height(8.dp))
                Text(
                    "No countries in your watchlist",
                    style = TextStyle(color = palette.foreground, fontWeight = FontWeight.Medium),
                )
                Text(
                    "Open Waymark and add countries to see them here.",
                    style = TextStyle(color = palette.muted, fontSize = 12.sp),
                )
            }
        } else {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                WidgetHeader(palette, data.syncState)
                Spacer(GlanceModifier.height(6.dp))
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(data.cards, itemId = { it.id.hashCode().toLong() }) { card ->
                        CountryRow(card, palette, compact = false)
                    }
                }
            }
        }
    }
}

@Composable
internal fun SingleCountryWidgetContent(context: Context, data: WidgetData) {
    val palette = widgetPalette(context)
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(palette.background)
            .padding(14.dp),
    ) {
        val card = data.cards.firstOrNull()
        if (card == null) {
            Column(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                WidgetHeader(palette, data.syncState)
                Spacer(GlanceModifier.height(8.dp))
                Text(
                    "Select a country",
                    style = TextStyle(color = palette.foreground, fontWeight = FontWeight.Medium),
                )
                Text(
                    "Open the widget settings to choose a country.",
                    style = TextStyle(color = palette.muted, fontSize = 12.sp),
                )
            }
        } else {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .clickable(actionStartActivity<MainActivity>(actionParametersOf(COUNTRY_ID_KEY to card.id))),
            ) {
                WidgetHeader(palette, data.syncState)
                Spacer(GlanceModifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FlagBadge(card, palette, size = 44)
                    Spacer(GlanceModifier.width(10.dp))
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            card.name,
                            style = TextStyle(color = palette.foreground, fontWeight = FontWeight.Bold, fontSize = 18.sp),
                            maxLines = 2,
                        )
                        Spacer(GlanceModifier.height(2.dp))
                        Text(
                            levelLine(card),
                            style = TextStyle(color = levelColor(card), fontWeight = FontWeight.Medium, fontSize = 14.sp),
                        )
                    }
                }
                Spacer(GlanceModifier.defaultWeight())
                Column {
                    Text(
                        advisoryDateLine(card),
                        style = TextStyle(color = palette.muted, fontSize = 12.sp),
                    )
                    if (card.regionCount > 0) {
                        Text(
                            "Regional advisories: ${card.regionCount}",
                            style = TextStyle(color = palette.muted, fontSize = 12.sp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetHeader(palette: WidgetPalette, syncState: SyncStateEntity?) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                "Waymark",
                style = TextStyle(color = palette.foreground, fontWeight = FontWeight.Bold, fontSize = 13.sp),
            )
            Text(
                syncStatusText(syncState),
                style = TextStyle(color = palette.muted, fontSize = 11.sp),
                maxLines = 1,
            )
        }
        Box(
            modifier = GlanceModifier
                .size(30.dp)
                .cornerRadius(15.dp)
                .background(palette.chip)
                .clickable(actionRunCallback<RefreshWidgetsAction>()),
            contentAlignment = Alignment.Center,
        ) {
            Text("↻", style = TextStyle(color = palette.foreground, fontSize = 14.sp))
        }
    }
}

@Composable
private fun CountryRow(card: CountryCard, palette: WidgetPalette, compact: Boolean) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .cornerRadius(14.dp)
            .background(palette.row)
            .padding(horizontal = 8.dp, vertical = 7.dp)
            .clickable(actionStartActivity<MainActivity>(actionParametersOf(COUNTRY_ID_KEY to card.id))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FlagBadge(card, palette, size = 34)
        Spacer(GlanceModifier.width(8.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                card.name,
                style = TextStyle(color = palette.foreground, fontWeight = FontWeight.Bold, fontSize = 14.sp),
                maxLines = 1,
            )
            Text(
                levelLine(card),
                style = TextStyle(color = levelColor(card), fontWeight = FontWeight.Medium, fontSize = 12.sp),
                maxLines = 1,
            )
            if (!compact) {
                Text(
                    advisoryDateLine(card),
                    style = TextStyle(color = palette.muted, fontSize = 11.sp),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun FlagBadge(card: CountryCard, palette: WidgetPalette, size: Int) {
    Box(
        modifier = GlanceModifier
            .size(size.dp)
            .cornerRadius((size / 3).dp)
            .background(palette.chip),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            flagEmoji(card.isoAlpha2),
            style = TextStyle(fontSize = (size * 0.55f).sp),
        )
    }
}

internal data class WidgetPalette(
    val background: ColorProvider,
    val foreground: ColorProvider,
    val muted: ColorProvider,
    val chip: ColorProvider,
    val row: ColorProvider,
)

internal fun widgetPalette(context: Context): WidgetPalette {
    val dark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES
    return if (dark) {
        WidgetPalette(
            background = ColorProvider(Color(0xF21A211E)),
            foreground = ColorProvider(Color(0xFFE8F0EB)),
            muted = ColorProvider(Color(0xFF9FB0A7)),
            chip = ColorProvider(Color(0xFF27322D)),
            row = ColorProvider(Color(0xFF202925)),
        )
    } else {
        WidgetPalette(
            background = ColorProvider(Color(0xF7FBF9F4)),
            foreground = ColorProvider(Color(0xFF16201B)),
            muted = ColorProvider(Color(0xFF5E6E66)),
            chip = ColorProvider(Color(0xFFE7EEE8)),
            row = ColorProvider(Color(0xFFF1F5F0)),
        )
    }
}

class RefreshWidgetsAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        SyncScheduler.syncNow(context)
        WaymarkWidgetUpdater.updateAll(context)
    }
}

internal fun syncStatusText(state: SyncStateEntity?): String {
    if (state == null) return "Not synced yet"
    if (state.status == "running") return "Updating…"
    val at = state.lastSuccessAt ?: return when (state.status) {
        "failed" -> "Update failed"
        "offline" -> "Offline"
        else -> "Not synced yet"
    }
    return "Updated " + formatSyncTime(at)
}

private fun formatSyncTime(at: Long): String = try {
    val instant = Instant.ofEpochMilli(at)
    val zoned = instant.atZone(ZoneId.systemDefault())
    val today = java.time.LocalDate.now(ZoneId.systemDefault())
    val pattern = if (zoned.toLocalDate() == today) "HH:mm" else "d MMM, HH:mm"
    DateTimeFormatter.ofPattern(pattern, Locale.getDefault()).format(zoned)
} catch (_: Exception) {
    "recently"
}

internal fun levelLine(card: CountryCard): String {
    val level = card.level ?: return "Unknown advisory"
    return "Level ${level.number} · ${AdvisoryLevels.label(level)}"
}

internal fun levelColor(card: CountryCard): ColorProvider = when (card.level?.number) {
    4 -> ColorProvider(Color(0xFFB3261E))
    3 -> ColorProvider(Color(0xFFC77700))
    2 -> ColorProvider(Color(0xFF7A6A00))
    1 -> ColorProvider(Color(0xFF2E6B4F))
    else -> ColorProvider(Color(0xFF6E6E6E))
}

internal fun advisoryDateLine(card: CountryCard): String {
    val value = card.advisoryDate
    if (value.isNullOrBlank()) return "Advisory date unknown"
    return try {
        val parsed = java.time.LocalDate.parse(value)
        "Advisory " + parsed.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))
    } catch (_: Exception) {
        "Advisory date unknown"
    }
}
