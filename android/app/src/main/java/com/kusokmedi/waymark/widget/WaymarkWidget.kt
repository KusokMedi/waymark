package com.kusokmedi.waymark.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
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
import com.kusokmedi.waymark.data.local.AppSettings
import com.kusokmedi.waymark.data.local.SyncStateEntity
import com.kusokmedi.waymark.data.local.WaymarkDatabase
import com.kusokmedi.waymark.data.local.WidgetConfigEntity
import com.kusokmedi.waymark.domain.AdvisoryLevels
import com.kusokmedi.waymark.domain.CountryCard
import com.kusokmedi.waymark.domain.flagEmoji
import com.kusokmedi.waymark.ui.MainActivity
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

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
        val data = loadWidgetData(context, widgetId)
        provideContent { WatchlistWidgetContent(data) }
    }
}

internal data class WidgetData(
    val widgetId: Int,
    val config: WidgetConfigEntity,
    val cards: List<CountryCard>,
    val syncState: SyncStateEntity?,
    val dynamicColor: Boolean,
)

internal suspend fun loadWidgetData(context: Context, widgetId: Int): WidgetData {
    val db = WaymarkDatabase.get(context)
    val appSettings = com.kusokmedi.waymark.data.local.SettingsRepository(context).settings.first()
    val existing = db.widgetConfigDao().get(widgetId)
    val config = (existing ?: WidgetConfigEntity(
        widgetId = widgetId,
        preset = "watchlist",
        widgetType = "watchlist",
        updatedAt = System.currentTimeMillis(),
    )).let { cfg ->
        val normalized = cfg.copy(
            widgetType = "watchlist",
            singleCountryId = null,
            cornerRadius = cfg.cornerRadius.coerceIn(8, 36),
            backgroundStyle = if (cfg.backgroundStyle == "transparent") "transparent" else "surface",
        )
        if (existing == null || existing != normalized) db.widgetConfigDao().upsert(normalized)
        normalized
    }

    val ids = db.watchlistDao().getAll().map { it.countryId }
    val cards = AdvisoryRepository(db).countryCardsByIds(ids)
    val syncState = normalizeSyncState(db.syncStateDao().get())
    if (syncState != null && syncState != db.syncStateDao().get()) {
        db.syncStateDao().upsert(syncState)
    }
    return WidgetData(widgetId, config, cards, syncState, appSettings.dynamicColor)
}

private suspend fun normalizeSyncState(state: SyncStateEntity?): SyncStateEntity? {
    if (state == null) return null
    if (state.status != "running") return state
    val age = System.currentTimeMillis() - (state.lastAttemptAt ?: 0L)
    return if (age > TimeUnit.MINUTES.toMillis(3)) {
        state.copy(status = "failed", message = state.message ?: "Synchronization was interrupted")
    } else state
}

@Composable
internal fun WatchlistWidgetContent(data: WidgetData) {
    val context = androidx.glance.LocalContext.current
    val palette = widgetPalette(context, data.dynamicColor, data.config.backgroundStyle == "transparent")
    val radius = data.config.cornerRadius.coerceIn(8, 36)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(radius.dp)
            .background(palette.background)
            .padding(12.dp),
    ) {
        if (data.cards.isEmpty()) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                WidgetHeader(data.widgetId, palette, data.syncState)
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
                WidgetHeader(data.widgetId, palette, data.syncState)
                Spacer(GlanceModifier.height(6.dp))
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(data.cards, itemId = { it.id.hashCode().toLong() }) { card ->
                        CountryRow(card, data.config, palette)
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetHeader(widgetId: Int, palette: WidgetPalette, syncState: SyncStateEntity?) {
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            SmallIconButton("↻", palette) { actionRunCallback<RefreshWidgetsAction>() }
            Spacer(GlanceModifier.width(6.dp))
            SmallIconButton("⚙", palette) { actionRunCallback<OpenWidgetSettingsAction>() }
        }
    }
}

@Composable
private fun SmallIconButton(symbol: String, palette: WidgetPalette, onClick: () -> androidx.glance.action.Action) {
    Box(
        modifier = GlanceModifier
            .size(30.dp)
            .cornerRadius(15.dp)
            .background(palette.chip)
            .clickable(onClick()),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, style = TextStyle(color = palette.foreground, fontSize = 14.sp))
    }
}

@Composable
private fun CountryRow(card: CountryCard, config: WidgetConfigEntity, palette: WidgetPalette) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .cornerRadius(14.dp)
            .background(palette.row)
            .padding(horizontal = 8.dp, vertical = 7.dp)
            .clickable(actionStartActivity<MainActivity>(actionParametersOf(COUNTRY_ID_KEY to card.id))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (config.showFlag) {
            FlagBadge(card, palette, size = 32)
            Spacer(GlanceModifier.width(8.dp))
        }
        Column(modifier = GlanceModifier.defaultWeight()) {
            if (config.showCountryName) {
                Text(
                    card.name,
                    style = TextStyle(color = palette.foreground, fontWeight = FontWeight.Bold, fontSize = 14.sp),
                    maxLines = 1,
                )
            }
            if (config.showLevel) {
                Text(
                    levelLine(card),
                    style = TextStyle(color = levelColor(card), fontWeight = FontWeight.Medium, fontSize = 12.sp),
                    maxLines = 1,
                )
            }
            if (config.showDate) {
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

internal fun widgetPalette(context: Context, dynamicColor: Boolean, transparent: Boolean): WidgetPalette {
    val dark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES

    if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val bgRes = if (dark) android.R.color.system_neutral1_900 else android.R.color.system_neutral1_50
        val fgRes = if (dark) android.R.color.system_neutral1_50 else android.R.color.system_neutral1_900
        val mutedRes = if (dark) android.R.color.system_neutral2_200 else android.R.color.system_neutral2_700
        val chipRes = if (dark) android.R.color.system_neutral2_800 else android.R.color.system_neutral2_100
        val rowRes = if (dark) android.R.color.system_neutral2_700 else android.R.color.system_neutral1_100
        val bgColor = Color(context.getColor(bgRes)).let { if (transparent) it.copy(alpha = 0.85f) else it.copy(alpha = 0.96f) }
        return WidgetPalette(
            background = ColorProvider(bgColor),
            foreground = ColorProvider(Color(context.getColor(fgRes))),
            muted = ColorProvider(Color(context.getColor(mutedRes))),
            chip = ColorProvider(Color(context.getColor(chipRes))),
            row = ColorProvider(Color(context.getColor(rowRes)).copy(alpha = if (transparent) 0.55f else 0.75f)),
        )
    }

    return if (dark) {
        WidgetPalette(
            background = ColorProvider(Color(if (transparent) 0xCC1A211E else 0xF21A211E)),
            foreground = ColorProvider(Color(0xFFE8F0EB)),
            muted = ColorProvider(Color(0xFF9FB0A7)),
            chip = ColorProvider(Color(0xFF27322D)),
            row = ColorProvider(Color(if (transparent) 0x99202925 else 0xFF202925)),
        )
    } else {
        WidgetPalette(
            background = ColorProvider(Color(if (transparent) 0xCCFBF9F4 else 0xF7FBF9F4)),
            foreground = ColorProvider(Color(0xFF16201B)),
            muted = ColorProvider(Color(0xFF5E6E66)),
            chip = ColorProvider(Color(0xFFE7EEE8)),
            row = ColorProvider(Color(if (transparent) 0x99F1F5F0 else 0xFFF1F5F0)),
        )
    }
}

class RefreshWidgetsAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        SyncScheduler.syncNow(context)
        WaymarkWidgetUpdater.updateAll(context)
    }
}

class OpenWidgetSettingsAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        val intent = Intent(context, WidgetConfigActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        context.startActivity(intent)
    }
}

internal fun syncStatusText(state: SyncStateEntity?): String {
    if (state == null) return "Not synced yet"
    val status = effectiveStatus(state)
    if (status == "running") {
        val dots = ((System.currentTimeMillis() / 700) % 3).toInt() + 1
        return "Updating" + ".".repeat(dots)
    }
    val last = state.lastSuccessAt
    val time = if (last != null) formatSyncTime(last) else "never"
    return when (status) {
        "failed", "offline" -> "Error, last update at $time"
        "success", "idle" -> if (last != null) "Updated $time" else "Not synced yet"
        else -> if (last != null) "Updated $time" else "Not synced yet"
    }
}

private fun effectiveStatus(state: SyncStateEntity): String {
    if (state.status == "running") {
        val age = System.currentTimeMillis() - (state.lastAttemptAt ?: 0L)
        if (age > TimeUnit.MINUTES.toMillis(3)) return "failed"
    }
    return state.status
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
