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
import com.kusokmedi.waymark.data.local.SettingsRepository
import com.kusokmedi.waymark.data.local.SyncStateEntity
import com.kusokmedi.waymark.data.local.WaymarkDatabase
import com.kusokmedi.waymark.data.local.WidgetConfigEntity
import com.kusokmedi.waymark.domain.AdvisoryLevels
import com.kusokmedi.waymark.domain.CountryCard
import com.kusokmedi.waymark.domain.flagEmoji
import com.kusokmedi.waymark.ui.MainActivity
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
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
            DpSize(250.dp, 320.dp),
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val snapshot = WidgetSnapshot.load(context, widgetId)
        provideContent { WatchlistWidgetContent(snapshot) }
    }
}

internal data class WidgetSnapshot(
    val widgetId: Int,
    val config: WidgetConfigEntity,
    val cards: List<CountryCard>,
    val syncState: SyncStateEntity?,
    val dynamicColor: Boolean,
) {
    companion object {
        suspend fun load(context: Context, widgetId: Int): WidgetSnapshot {
            val db = WaymarkDatabase.get(context)
            val settings = SettingsRepository(context).settings.first()

            val existing = db.widgetConfigDao().get(widgetId)
            val config = normalizeConfig(existing, widgetId)
            if (existing == null || existing != config) {
                db.widgetConfigDao().upsert(config)
            }

            val watchlistIds = db.watchlistDao().getAll().map { it.countryId }
            val cards = AdvisoryRepository(db).countryCardsByIds(watchlistIds)
            val syncState = db.syncStateDao().get()

            return WidgetSnapshot(
                widgetId = widgetId,
                config = config,
                cards = cards,
                syncState = syncState,
                dynamicColor = settings.dynamicColor,
            )
        }

        private fun normalizeConfig(existing: WidgetConfigEntity?, widgetId: Int): WidgetConfigEntity {
            val base = existing ?: WidgetConfigEntity(
                widgetId = widgetId,
                preset = "watchlist",
                widgetType = "watchlist",
                updatedAt = System.currentTimeMillis(),
            )
            return base.copy(
                preset = "watchlist",
                widgetType = "watchlist",
                singleCountryId = null,
                cornerRadius = base.cornerRadius.coerceIn(8, 36),
                columns = 1,
                backgroundStyle = if (base.backgroundStyle == "transparent") "transparent" else "surface",
            )
        }
    }
}

@Composable
private fun WatchlistWidgetContent(snapshot: WidgetSnapshot) {
    val context = androidx.glance.LocalContext.current
    val colors = widgetColors(context, snapshot.dynamicColor, snapshot.config.backgroundStyle == "transparent")
    val radius = snapshot.config.cornerRadius.coerceIn(8, 36)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(radius.dp)
            .background(colors.background)
            .padding(10.dp),
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Header(snapshot, colors)

            if (snapshot.cards.isEmpty()) {
                Spacer(GlanceModifier.height(10.dp))
                Text(
                    text = "No countries in your watchlist",
                    style = TextStyle(
                        color = colors.foreground,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                    ),
                )
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    text = "Open Waymark to add countries.",
                    style = TextStyle(color = colors.muted, fontSize = 12.sp),
                )
            } else {
                Spacer(GlanceModifier.height(6.dp))
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(snapshot.cards, itemId = { it.id.hashCode().toLong() }) { card ->
                        CountryItem(card = card, config = snapshot.config, colors = colors)
                        Spacer(GlanceModifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(snapshot: WidgetSnapshot, colors: WidgetColors) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = "Waymark",
                style = TextStyle(
                    color = colors.foreground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                ),
                maxLines = 1,
            )
            Text(
                text = statusText(snapshot.syncState),
                style = TextStyle(color = colors.muted, fontSize = 11.sp),
                maxLines = 1,
            )
        }
        IconButton("↻", "Refresh", colors) { actionRunCallback<RefreshAction>() }
        Spacer(GlanceModifier.width(6.dp))
        IconButton("⚙", "Settings", colors) { actionRunCallback<OpenSettingsAction>() }
    }
}

@Composable
private fun IconButton(
    symbol: String,
    contentDescription: String,
    colors: WidgetColors,
    onClick: () -> androidx.glance.action.Action,
) {
    Box(
        modifier = GlanceModifier
            .size(30.dp)
            .cornerRadius(15.dp)
            .background(colors.chip)
            .clickable(onClick()),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = symbol, style = TextStyle(color = colors.foreground, fontSize = 14.sp))
    }
}

@Composable
private fun CountryItem(card: CountryCard, config: WidgetConfigEntity, colors: WidgetColors) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .cornerRadius(14.dp)
            .background(colors.row)
            .clickable(actionStartActivity<MainActivity>(actionParametersOf(COUNTRY_ID_KEY to card.id)))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (config.showFlag) {
            Box(
                modifier = GlanceModifier
                    .size(30.dp)
                    .cornerRadius(10.dp)
                    .background(colors.chip),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = flagEmoji(card.isoAlpha2),
                    style = TextStyle(fontSize = 16.sp),
                )
            }
            Spacer(GlanceModifier.width(8.dp))
        }

        Column(modifier = GlanceModifier.defaultWeight()) {
            if (config.showCountryName) {
                Text(
                    text = card.name,
                    style = TextStyle(
                        color = colors.foreground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    ),
                    maxLines = 1,
                )
            }
            if (config.showLevel) {
                Text(
                    text = levelText(card),
                    style = TextStyle(color = levelColor(card), fontWeight = FontWeight.Medium, fontSize = 12.sp),
                    maxLines = 1,
                )
            }
            if (config.showDate) {
                Text(
                    text = dateText(card.advisoryDate),
                    style = TextStyle(color = colors.muted, fontSize = 11.sp),
                    maxLines = 1,
                )
            }
        }
    }
}

internal data class WidgetColors(
    val background: ColorProvider,
    val foreground: ColorProvider,
    val muted: ColorProvider,
    val chip: ColorProvider,
    val row: ColorProvider,
)

private fun widgetColors(context: Context, dynamicColor: Boolean, transparent: Boolean): WidgetColors {
    val dark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES

    if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val bgRes = if (dark) android.R.color.system_neutral1_900 else android.R.color.system_neutral1_50
        val fgRes = if (dark) android.R.color.system_neutral1_50 else android.R.color.system_neutral1_900
        val mutedRes = if (dark) android.R.color.system_neutral2_200 else android.R.color.system_neutral2_700
        val chipRes = if (dark) android.R.color.system_neutral2_800 else android.R.color.system_neutral2_100
        val rowRes = if (dark) android.R.color.system_neutral2_700 else android.R.color.system_neutral1_100

        val bg = Color(context.getColor(bgRes)).let { if (transparent) it.copy(alpha = 0.86f) else it.copy(alpha = 0.97f) }
        val row = Color(context.getColor(rowRes)).let { if (transparent) it.copy(alpha = 0.55f) else it.copy(alpha = 0.78f) }
        return WidgetColors(
            background = ColorProvider(bg),
            foreground = ColorProvider(Color(context.getColor(fgRes))),
            muted = ColorProvider(Color(context.getColor(mutedRes))),
            chip = ColorProvider(Color(context.getColor(chipRes))),
            row = ColorProvider(row),
        )
    }

    return if (dark) {
        WidgetColors(
            background = ColorProvider(Color(if (transparent) 0xCC1A211E else 0xF21A211E)),
            foreground = ColorProvider(Color(0xFFE8F0EB)),
            muted = ColorProvider(Color(0xFFA4B4AB)),
            chip = ColorProvider(Color(0xFF27332D)),
            row = ColorProvider(Color(if (transparent) 0x99202925 else 0xFF212B26)),
        )
    } else {
        WidgetColors(
            background = ColorProvider(Color(if (transparent) 0xCCFBF9F4 else 0xF7FBF9F4)),
            foreground = ColorProvider(Color(0xFF16201B)),
            muted = ColorProvider(Color(0xFF5E6E66)),
            chip = ColorProvider(Color(0xFFE7EEE8)),
            row = ColorProvider(Color(if (transparent) 0x99F1F5F0 else 0xFFF1F5F0)),
        )
    }
}

class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val allowMobileData = SettingsRepository(context).settings.first().allowMobileData
        SyncScheduler.syncNow(context, allowMobileData)
        WaymarkWidgetUpdater.updateAll(context)
    }
}

class OpenSettingsAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val widgetId = runCatching { GlanceAppWidgetManager(context).getAppWidgetId(glanceId) }.getOrNull()
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        val intent = Intent(context, WidgetConfigActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        context.startActivity(intent)
    }
}

private fun statusText(state: SyncStateEntity?): String {
    if (state == null) return "Not synced yet"
    return when (effectiveStatus(state)) {
        "running" -> "Updating…"
        "failed", "offline" -> "Error, last update at ${timeText(state.lastSuccessAt)}"
        "success", "idle" -> if (state.lastSuccessAt != null) "Updated ${timeText(state.lastSuccessAt)}" else "Not synced yet"
        else -> if (state.lastSuccessAt != null) "Updated ${timeText(state.lastSuccessAt)}" else "Not synced yet"
    }
}

private fun effectiveStatus(state: SyncStateEntity): String {
    if (state.status == "running") {
        val age = System.currentTimeMillis() - (state.lastAttemptAt ?: 0L)
        if (age > TimeUnit.MINUTES.toMillis(3)) return "failed"
    }
    return state.status
}

private fun timeText(at: Long?): String {
    if (at == null) return "never"
    return try {
        val zoned = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault())
        val today = java.time.LocalDate.now(ZoneId.systemDefault())
        val pattern = if (zoned.toLocalDate() == today) "HH:mm" else "d MMM, HH:mm"
        DateTimeFormatter.ofPattern(pattern, Locale.getDefault()).format(zoned)
    } catch (_: Exception) {
        "recently"
    }
}

private fun levelText(card: CountryCard): String {
    val level = card.level ?: return "Unknown advisory"
    return "Level ${level.number} · ${AdvisoryLevels.label(level)}"
}

private fun levelColor(card: CountryCard): ColorProvider = when (card.level?.number) {
    4 -> ColorProvider(Color(0xFFB3261E))
    3 -> ColorProvider(Color(0xFFC77700))
    2 -> ColorProvider(Color(0xFF7A6A00))
    1 -> ColorProvider(Color(0xFF2E6B4F))
    else -> ColorProvider(Color(0xFF6E6E6E))
}

private fun dateText(value: String?): String {
    if (value.isNullOrBlank()) return "Advisory date unknown"
    return try {
        val parsed = LocalDate.parse(value)
        "Advisory " + parsed.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
    } catch (_: Exception) {
        "Advisory date unknown"
    }
}
