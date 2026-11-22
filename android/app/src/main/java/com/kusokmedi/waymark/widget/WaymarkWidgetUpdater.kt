package com.kusokmedi.waymark.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

private sealed class UpdateState {
    object Idle : UpdateState()
    data class Pending(val force: Boolean) : UpdateState()
    data class Running(val dirty: Boolean, val force: Boolean) : UpdateState()
}

object WaymarkWidgetUpdater {
    private const val TAG = "WaymarkWidgetUpdater"
    private const val MIN_UPDATE_INTERVAL_MS = 500L
    private const val FORCE_THROTTLE_MS = 200L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private val updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    private val lastUpdateAt = AtomicLong(0L)
    private val lastForceAt = AtomicLong(0L)

    fun updateAll(context: Context) {
        requestUpdate(context.applicationContext, force = false)
    }

    fun forceUpdateAll(context: Context) {
        val app = context.applicationContext
        val now = System.currentTimeMillis()
        val timeSinceLastForce = now - lastForceAt.get()
        
        if (timeSinceLastForce < FORCE_THROTTLE_MS) {
            Log.d(TAG, "forceUpdateAll throttled")
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

    private fun requestUpdate(app: Context, force: Boolean) {
        val shouldStartLoop = updateState.value is UpdateState.Idle
        
        updateState.update { current ->
            when (current) {
                is UpdateState.Idle -> UpdateState.Pending(force)
                is UpdateState.Pending -> UpdateState.Pending(force || current.force)
                is UpdateState.Running -> {
                    Log.d(TAG, "update coalesced")
                    UpdateState.Running(dirty = true, force = force || current.force)
                }
            }
        }
        
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

                    delay(MIN_UPDATE_INTERVAL_MS)
                    
                    shouldContinue = updateState.value.let { state ->
                        when (state) {
                            is UpdateState.Running -> {
                                if (state.dirty) {
                                    Log.d(TAG, "trailing update")
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
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, WaymarkAppWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(component)
            
            Log.i(TAG, "doUpdate widgets=${ids.size}")
            
            for (id in ids) {
                try {
                    // Trigger widget update through onUpdate mechanism
                    val intent = android.content.Intent(context, WaymarkAppWidgetProvider::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(id))
                    }
                    context.sendBroadcast(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to trigger widget update $id", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "doUpdate failed", e)
        }
    }
}
