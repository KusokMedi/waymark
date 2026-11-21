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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.kusokmedi.waymark.data.local.SyncStateEntity
import com.kusokmedi.waymark.data.local.WaymarkDatabase
import com.kusokmedi.waymark.waymarkContainer
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

private sealed class UpdateState {
    object Idle : UpdateState()
    data class Pending(val force: Boolean) : UpdateState()
    data class Running(val dirty: Boolean, val force: Boolean) : UpdateState()
}

object WaymarkWidgetUpdater {
    private const val TAG = "WaymarkWidgetUpdater"
    private const val PREFS = "waymark_widget_prefs"
    private const val KEY_LAST_WIDGET_REFRESH_AT = "last_widget_refresh_at"
    private const val MIN_UPDATE_INTERVAL_MS = 500L
    private const val FORCE_THROTTLE_MS = 200L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private val updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    private val lastUpdateAt = AtomicLong(0L)
    private val lastForceAt = AtomicLong(0L)
    private val immediateSyncInFlight = AtomicBoolean(false)

    fun updateAll(context: Context) {
        requestUpdate(context.applicationContext, force = false)
    }

    fun forceUpdateAll(context: Context) {
        val app = context.applicationContext
        val now = System.currentTimeMillis()
        val timeSinceLastForce = now - lastForceAt.get()
        
        // Apply minimal throttling even for force updates to prevent excessive widget refreshes
        if (timeSinceLastForce < FORCE_THROTTLE_MS) {
            Log.d(TAG, "forceUpdateAll throttled, requesting coalesced update instead")
            requestUpdate(app, force = true)
            return
        }
        
        lastForceAt.set(now)
        scope.launch {
            try {
                mutex.withLock {
                    doUpdate(app)
                    lastUpdateAt.set(System.currentTimeMillis())
                }
            } catch (e: Exception) {
                Log.e(TAG, "forceUpdateAll failed", e)
            }
        }
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

    fun requestImmediateSync(context: Context) {
        val app = context.applicationContext
        scope.launch {
            if (!immediateSyncInFlight.compareAndSet(false, true)) {
                Log.d(TAG, "Immediate widget sync already in flight")
                forceUpdateAll(app)
                return@launch
            }
            try {
                val db = WaymarkDatabase.get(app)
                val now = System.currentTimeMillis()
                val currentState = db.syncStateDao().get() ?: SyncStateEntity()
                val lastAttempt = currentState.lastAttemptAt ?: 0L
                val age = now - lastAttempt
                val running = currentState.status == "running"
                val staleRunning = running && age > TimeUnit.SECONDS.toMillis(30)

                Log.i(TAG, "widget refresh tap age=${age}ms status=${currentState.status} staleRunning=$staleRunning")

                if (running && !staleRunning && age < 1_500) {
                    forceUpdateAll(app)
                    return@launch
                }

                db.syncStateDao().upsert(
                    currentState.copy(
                        lastAttemptAt = now,
                        status = "running",
                        message = if (staleRunning) "Retrying sync..." else null,
                    )
                )
                forceUpdateAll(app)

                try {
                    // Same path as in-app Watchlist refresh: SyncEngine.synchronize() with defaults.
                    app.waymarkContainer().syncEngine.synchronize()
                } catch (e: Exception) {
                    Log.e(TAG, "Immediate widget sync failed", e)
                }
            } finally {
                immediateSyncInFlight.set(false)
                forceUpdateAll(app)
            }
        }
    }

    private fun requestUpdate(app: Context, force: Boolean) {
        // Check current state and determine if we need to start update loop
        val shouldStartLoop = updateState.value is UpdateState.Idle
        
        // Update state without side effects
        updateState.update { current ->
            when (current) {
                is UpdateState.Idle -> UpdateState.Pending(force)
                is UpdateState.Pending -> UpdateState.Pending(force || current.force)
                is UpdateState.Running -> {
                    Log.d(TAG, "update coalesced during running force=${force || current.force}")
                    UpdateState.Running(dirty = true, force = force || current.force)
                }
            }
        }
        
        // Start update loop outside of state update to avoid CAS retry side effects
        if (shouldStartLoop) {
            startUpdateLoop(app, force)
        }
    }
    
    private fun startUpdateLoop(app: Context, initialForce: Boolean) {
        scope.launch {
            try {
                var shouldContinue = true
                while (shouldContinue) {
                    val (shouldForce, wasDirty) = updateState.value.let { state ->
                        when (state) {
                            is UpdateState.Pending -> {
                                updateState.value = UpdateState.Running(dirty = false, force = state.force)
                                state.force to false
                            }
                            is UpdateState.Running -> {
                                updateState.value = UpdateState.Running(dirty = false, force = state.force)
                                state.force to state.dirty
                            }
                            else -> false to false
                        }
                    }

                    if (!shouldForce) {
                        val now = System.currentTimeMillis()
                        val waitMs = (lastUpdateAt.get() + MIN_UPDATE_INTERVAL_MS) - now
                        if (waitMs > 0) delay(waitMs)
                    }

                    mutex.withLock {
                        doUpdate(app)
                        lastUpdateAt.set(System.currentTimeMillis())
                    }

                    // Absorb bursts into one trailing pass
                    delay(MIN_UPDATE_INTERVAL_MS)
                    
                    shouldContinue = updateState.value.let { state ->
                        when (state) {
                            is UpdateState.Running -> {
                                if (state.dirty) {
                                    Log.d(TAG, "trailing widget update dirty=true force=${state.force}")
                                    true
                                } else {
                                    updateState.value = UpdateState.Idle
                                    false
                                }
                            }
                            else -> {
                                updateState.value = UpdateState.Idle
                                false
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "updateLoop failed", e)
                updateState.value = UpdateState.Idle
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
                        val current = prefs[tickKey] ?: 0
                        val next = ((current.toLong() + 1) % Long.MAX_VALUE).toInt()
                        prefs[tickKey] = next
                    }
                    WatchlistWidget().update(context, glanceId)
                }.onFailure { Log.e(TAG, "state bump/update failed for $glanceId", it) }
            }

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
