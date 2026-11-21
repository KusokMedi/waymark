package com.kusokmedi.waymark.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.ColorFilter
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import kotlinx.coroutines.flow.first
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
import com.kusokmedi.waymark.data.local.SettingsRepository
import com.kusokmedi.waymark.data.local.SyncStateEntity
import com.kusokmedi.waymark.data.local.WaymarkDatabase
import com.kusokmedi.waymark.data.local.WidgetConfigEntity
import com.kusokmedi.waymark.domain.AdvisoryLevels
import com.kusokmedi.waymark.domain.CountryCard
import com.kusokmedi.waymark.domain.flagEmoji
import com.kusokmedi.waymark.R
import com.kusokmedi.waymark.ui.MainActivity
import com.kusokmedi.waymark.waymarkContainer
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
            DpSize(110.dp, 50.dp),
            DpSize(180.dp, 50.dp),
            DpSize(250.dp, 50.dp),
            DpSize(110.dp, 110.dp),
            DpSize(180.dp, 110.dp),
            DpSize(250.dp, 110.dp),
            DpSize(250.dp, 180.dp),
            DpSize(320.dp, 180.dp),
            DpSize(320.dp, 280.dp),
            DpSize(250.dp, 320.dp),
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        try {
            val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
            val snapshot = WidgetSnapshot.load(context, widgetId)
            Log.i(TAG_WIDGET, "provideGlance widgetId=$widgetId status=${snapshot.syncState?.status} lastSuccess=${snapshot.syncState?.lastSuccessAt} cards=${snapshot.cards.size}")
            provideContent { WatchlistWidgetContent(snapshot) }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG_WIDGET, "provideGlance failed", e)
            try {
                provideContent {
                    Box(
                        modifier = GlanceModifier.fillMaxSize().padding(12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Waymark widget error. Tap refresh.",
                            style = TextStyle(color = ColorProvider(Color(0xFFB3261E)), fontSize = 12.sp),
                        )
                    }
                }
            } catch (_: Exception) {
                // Ignore secondary failure while reporting an error state.
            }
        }
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
            val syncState = recoverStaleRunningSyncState(db)

            return WidgetSnapshot(
                widgetId = widgetId,
                config = config,
                cards = cards,
                syncState = syncState,
                dynamicColor = settings.dynamicColor,
            )
        }

        private fun normalizeConfig(existing: WidgetConfigEntity?, widgetId: Int): WidgetConfigEntity {
            // Fixed defaults: no settings UI. Keep updatedAt stable unless values change.
            val base = WidgetConfigEntity(
                widgetId = widgetId,
                preset = "watchlist",
                widgetType = "watchlist",
                showFlag = true,
                showCountryName = true,
                showLevel = true,
                showDate = false,
                backgroundStyle = "surface",
                backgroundAlpha = 100,
                cornerRadius = 12,
                columns = 1,
                updatedAt = existing?.updatedAt ?: 0L,
            )
            return if (existing == base) existing else base.copy(updatedAt = System.currentTimeMillis())
        }
    }
}

@Composable
private fun WatchlistWidgetContent(snapshot: WidgetSnapshot) {
    val context = androidx.glance.LocalContext.current
    val colors = widgetColors(context, snapshot.dynamicColor, snapshot.config.backgroundAlpha)
    val radius = snapshot.config.cornerRadius.coerceIn(0, 20)
    val size = LocalSize.current
    val compact = size.height < 80.dp || size.width < 150.dp
    val outerPadding = if (compact) 6.dp else 10.dp
    val cardSpacing = 4.dp

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(radius.dp)
            .background(colors.background)
            .padding(outerPadding),
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Header(snapshot, colors, compact)

            if (snapshot.cards.isEmpty()) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight()
                        .clickable(actionStartActivity<MainActivity>()),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Your watchlist is empty",
                            style = TextStyle(
                                color = colors.foreground,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (compact) 12.sp else 14.sp,
                            ),
                        )
                        Spacer(GlanceModifier.height(4.dp))
                        Text(
                            text = "Tap to open Waymark and add countries.",
                            style = TextStyle(color = colors.muted, fontSize = if (compact) 10.sp else 12.sp),
                        )
                    }
                }
            } else {
                Spacer(GlanceModifier.height(if (compact) 4.dp else 6.dp))
                val cardCount = snapshot.cards.size
                LazyColumn(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                    items(
                        count = cardCount,
                        itemId = { index -> snapshot.cards[index].id.hashCode().toLong() },
                    ) { index ->
                        val card = snapshot.cards[index]
                        Column(modifier = GlanceModifier.fillMaxWidth()) {
                            CountryItem(
                                card = card,
                                config = snapshot.config,
                                colors = colors,
                                compact = compact,
                            )
                            if (index < cardCount - 1) {
                                Spacer(GlanceModifier.height(cardSpacing))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(snapshot: WidgetSnapshot, colors: WidgetColors, compact: Boolean = false) {
    val titleSize = if (compact) 11.sp else 13.sp
    val statusSize = if (compact) 9.sp else 11.sp
    val progressSize = if (compact) 16.dp else 20.dp
    val isSyncing = snapshot.syncState?.let { effectiveStatus(it) == "running" } ?: false
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
                    fontSize = titleSize,
                ),
                maxLines = 1,
            )
            Text(
                text = statusText(snapshot.syncState),
                style = TextStyle(color = colors.muted, fontSize = statusSize),
                maxLines = 1,
            )
        }
        if (isSyncing) {
            CircularProgressIndicator(
                GlanceModifier.size(progressSize),
                colors.foreground,
            )
            Spacer(GlanceModifier.width(if (compact) 4.dp else 6.dp))
        }
        IconButton(iconRes = R.drawable.ic_widget_open, contentDescription = "Open app", colors = colors, compact = compact) { actionStartActivity<MainActivity>() }
    }
}

@Composable
private fun IconButton(
    iconRes: Int,
    contentDescription: String,
    colors: WidgetColors,
    compact: Boolean = false,
    onClick: () -> androidx.glance.action.Action,
) {
    val buttonSize = if (compact) 28.dp else 34.dp
    val iconSize = if (compact) 16.dp else 20.dp
    Box(
        modifier = GlanceModifier
            .size(buttonSize)
            .cornerRadius((buttonSize.value / 2).dp)
            .background(colors.chip)
            .clickable(onClick()),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = contentDescription,
            modifier = GlanceModifier.size(iconSize),
            colorFilter = ColorFilter.tint(colors.foreground),
        )
    }
}

@Composable
private fun CountryItemContent(card: CountryCard, config: WidgetConfigEntity, colors: WidgetColors, compact: Boolean = false, columnModifier: GlanceModifier = GlanceModifier.fillMaxWidth()) {
    val flagSize = if (compact) 24.dp else 30.dp
    val flagCorner = if (compact) 8.dp else 10.dp
    val flagTextSize = if (compact) 13.sp else 16.sp
    val nameSize = if (compact) 11.sp else 13.sp
    val levelSize = if (compact) 10.sp else 12.sp
    val dateSize = if (compact) 9.sp else 11.sp
    val spacerWidth = if (compact) 6.dp else 8.dp

    if (config.showFlag) {
        Box(
            modifier = GlanceModifier
                .size(flagSize)
                .cornerRadius(flagCorner)
                .background(colors.chip),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = flagEmoji(card.isoAlpha2),
                style = TextStyle(fontSize = flagTextSize),
            )
        }
        Spacer(GlanceModifier.width(spacerWidth))
    }

    Column(modifier = columnModifier) {
        if (config.showCountryName) {
            Text(
                text = card.name,
                style = TextStyle(
                    color = colors.foreground,
                    fontWeight = FontWeight.Bold,
                    fontSize = nameSize,
                ),
                maxLines = 1,
            )
        }
        if (config.showLevel) {
            Text(
                text = levelText(card),
                style = TextStyle(color = levelColor(card), fontWeight = FontWeight.Medium, fontSize = levelSize),
                maxLines = 1,
            )
        }
        if (config.showDate) {
            Text(
                text = dateText(card.advisoryDate),
                style = TextStyle(color = colors.muted, fontSize = dateSize),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CountryItem(
    card: CountryCard,
    config: WidgetConfigEntity,
    colors: WidgetColors,
    compact: Boolean = false,
) {
    val r = config.cornerRadius.coerceIn(0, 20)
    val horizontalPadding = if (compact) 8.dp else 10.dp
    val verticalPadding = if (compact) 6.dp else 10.dp
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .let { m ->
                if (r > 0) m.cornerRadius(r.dp).background(colors.row)
                else m.background(colors.row)
            }
            .clickable(actionStartActivity<MainActivity>(actionParametersOf(COUNTRY_ID_KEY to card.id)))
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CountryItemContent(card, config, colors, compact, GlanceModifier.defaultWeight())
    }
}

internal data class WidgetColors(
    val background: ColorProvider,
    val foreground: ColorProvider,
    val muted: ColorProvider,
    val chip: ColorProvider,
    val row: ColorProvider,
)

private fun widgetColors(context: Context, dynamicColor: Boolean, backgroundAlpha: Int): WidgetColors {
    val dark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES

    val alpha = (backgroundAlpha.coerceIn(0, 100) / 100f)

    if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val bgRes = if (dark) android.R.color.system_neutral1_900 else android.R.color.system_neutral1_50
        val fgRes = if (dark) android.R.color.system_neutral1_50 else android.R.color.system_neutral1_900
        val mutedRes = if (dark) android.R.color.system_neutral2_200 else android.R.color.system_neutral2_700
        val chipRes = if (dark) android.R.color.system_neutral2_800 else android.R.color.system_neutral2_100
        val rowRes = if (dark) android.R.color.system_neutral2_700 else android.R.color.system_neutral1_100

        val bg = Color(context.getColor(bgRes)).copy(alpha = alpha)
        val row = Color(context.getColor(rowRes)).copy(alpha = 0.92f)
        return WidgetColors(
            background = ColorProvider(bg),
            foreground = ColorProvider(Color(context.getColor(fgRes))),
            muted = ColorProvider(Color(context.getColor(mutedRes))),
            chip = ColorProvider(Color(context.getColor(chipRes))),
            row = ColorProvider(row),
        )
    }

    val bgBase = if (dark) 0x1A211E else 0xFBFBF4
    val rowBase = if (dark) 0x212B26 else 0xF1F5F0
    return if (dark) {
        WidgetColors(
            background = ColorProvider(Color(bgBase).copy(alpha = alpha)),
            foreground = ColorProvider(Color(0xFFE8F0EB)),
            muted = ColorProvider(Color(0xFFA4B4AB)),
            chip = ColorProvider(Color(0xFF27332D)),
            row = ColorProvider(Color(rowBase).copy(alpha = 0.92f)),
        )
    } else {
        WidgetColors(
            background = ColorProvider(Color(bgBase).copy(alpha = alpha)),
            foreground = ColorProvider(Color(0xFF16201B)),
            muted = ColorProvider(Color(0xFF5E6E66)),
            chip = ColorProvider(Color(0xFFE7EEE8)),
            row = ColorProvider(Color(rowBase).copy(alpha = 0.92f)),
        )
    }
}

private const val TAG_WIDGET = "WaymarkWidget"


private fun statusText(state: SyncStateEntity?): String {
    if (state == null) return "Not synced yet"
    return when (effectiveStatus(state)) {
        "running" -> "Syncing…"
        "offline" -> if (state.lastSuccessAt != null) "Offline · Updated ${timeText(state.lastSuccessAt)}" else "Offline · Not synced yet"
        "failed" -> if (state.lastSuccessAt != null) "Update failed · ${timeText(state.lastSuccessAt)}" else "Update failed"
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
    val level = card.level ?: return "Unknown"
    val base = "Level ${level.number}"
    return if (card.containsHigherRiskAreas) "$base · Higher risk" else base
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

private suspend fun recoverStaleRunningSyncState(db: WaymarkDatabase): SyncStateEntity? {
    return db.syncStateDao().recoverStaleRunning(staleThresholdMs = 30_000L)
}
