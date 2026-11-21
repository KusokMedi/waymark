package com.kusokmedi.waymark.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object WaymarkWidgetUpdater {
    private const val TAG = "WaymarkWidgetUpdater"
    private const val PREFS = "waymark_widget_prefs"
    private const val KEY_LAST_WIDGET_REFRESH_AT = "last_widget_refresh_at"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()

    @Volatile private var updateInFlight: Boolean = false
    @Volatile private var dirty: Boolean = false
    @Volatile private var forceRequested: Boolean = false
    @Volatile private var lastUpdateAt: Long = 0L

    fun updateAll(context: Context) {
        requestUpdate(context.applicationContext, force = false)
    }

    fun forceUpdateAll(context: Context) {
        requestUpdate(context.applicationContext, force = true)
    }

    fun markWidgetRefreshed(context: Context, at: Long = System.currentTimeMillis()) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_WIDGET_REFRESH_AT, at)
            .apply()
    }

    fun lastWidgetRefreshAt(context: Context): Long? {
        val value = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_WIDGET_REFRESH_AT, 0L)
        return value.takeIf { it > 0L }
    }

    private fun requestUpdate(app: Context, force: Boolean) {
        if (force) forceRequested = true
        dirty = true
        if (updateInFlight) {
            Log.d(TAG, "update coalesced force=$force")
            return
        }
        updateInFlight = true
        scope.launch {
            try {
                while (true) {
                    val shouldForce = forceRequested
                    dirty = false
                    forceRequested = false

                    if (!shouldForce) {
                        val now = System.currentTimeMillis()
                        val waitMs = (lastUpdateAt + 500L) - now
                        if (waitMs > 0) delay(waitMs)
                    }

                    mutex.withLock {
                        doUpdate(app)
                        lastUpdateAt = System.currentTimeMillis()
                    }

                    // Absorb bursts into one trailing pass.
                    delay(500)
                    if (!dirty) break
                    Log.d(TAG, "trailing widget update dirty=$dirty force=$forceRequested")
                }
            } catch (e: Exception) {
                Log.e(TAG, "requestUpdate failed", e)
            } finally {
                updateInFlight = false
                if (dirty) {
                    updateInFlight = true
                    scope.launch {
                        delay(300)
                        updateInFlight = false
                        requestUpdate(app, force = forceRequested)
                    }
                }
            }
        }
    }

    private suspend fun doUpdate(context: Context) {
        try {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(WatchlistWidget::class.java)
            Log.i(TAG, "doUpdate glanceIds=${glanceIds.size}")

            // Bump Glance state to force recomposition even when only our DB changed.
            val tickKey = intPreferencesKey("refresh_tick")
            glanceIds.forEach { glanceId ->
                runCatching {
                    updateAppWidgetState(context, glanceId) { prefs ->
                        val next = ((prefs[tickKey] ?: 0) + 1) % 1_000_000
                        prefs[tickKey] = next
                    }
                    WatchlistWidget().update(context, glanceId)
                }.onFailure { Log.e(TAG, "state bump/update failed for $glanceId", it) }
            }

            runCatching { WatchlistWidget().updateAll(context) }
                .onFailure { Log.e(TAG, "updateAll failed", it) }

            sendAppWidgetUpdateBroadcast(context)
        } catch (e: Exception) {
            Log.e(TAG, "doUpdate failed", e)
            runCatching { WatchlistWidget().updateAll(context) }
                .onFailure { Log.e(TAG, "fallback updateAll failed", it) }
        }
    }

    private fun sendAppWidgetUpdateBroadcast(context: Context) {
        runCatching {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, WatchlistWidgetReceiver::class.java)
            val ids = appWidgetManager.getAppWidgetIds(component)
            if (ids.isEmpty()) return
            val intent = Intent(context, WatchlistWidgetReceiver::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
            Log.i(TAG, "sent APPWIDGET_UPDATE broadcast ids=${ids.toList()}")
        }.onFailure { Log.e(TAG, "broadcast update failed", it) }
    }
}
