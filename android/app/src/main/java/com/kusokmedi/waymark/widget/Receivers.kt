package com.kusokmedi.waymark.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.kusokmedi.waymark.SyncScheduler
import com.kusokmedi.waymark.data.local.WaymarkDatabase
import com.kusokmedi.waymark.waymarkContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WatchlistWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WatchlistWidget()

    override fun onReceive(context: Context, intent: Intent) {
        Log.i("WaymarkWidgetReceiver", "onReceive action=${intent.action} extras=${intent.extras}")
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        Log.i("WaymarkWidgetReceiver", "onUpdate ids=${appWidgetIds.toList()}")
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        // Use goAsync() to ensure database operations complete even if process is killed
        val pendingResult = goAsync()
        val db = WaymarkDatabase.get(context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                appWidgetIds.forEach { widgetId ->
                    db.widgetConfigDao().delete(widgetId)
                    Log.d("WaymarkWidgetReceiver", "Deleted config for widget $widgetId")
                }
            } catch (e: Exception) {
                Log.e("WaymarkWidgetReceiver", "Failed to delete widget configs", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/**
 * Handles widget refresh button click.
 * Triggered by widget refresh button without opening the app.
 */
class WidgetSyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.kusokmedi.waymark.WIDGET_SYNC_REQUEST") return
        
        Log.d("WidgetSyncReceiver", "Widget sync request received")
        
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // Trigger sync with forceSync=true to reload all countries
                SyncScheduler.syncNowFromWidget(context, forceSync = true)
                Log.d("WidgetSyncReceiver", "Widget sync triggered successfully")
            } catch (e: Exception) {
                Log.e("WidgetSyncReceiver", "Failed to trigger widget sync", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/**
 * Ensures WorkManager jobs resume after device boot or app update.
 * Also handles normal app lifecycle pauses - triggers sync on resume.
 * Triggered by ACTION_BOOT_COMPLETED and ACTION_MY_PACKAGE_REPLACED.
 */
class SyncRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("SyncRestoreReceiver", "onReceive action=$action")
        
        // Use goAsync() to prevent premature receiver termination
        val pendingResult = goAsync()
        
        CoroutineScope(Dispatchers.Default).launch {
            try {
                when (action) {
                    Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                        Log.d("SyncRestoreReceiver", "Restoring sync after $action")
                        // Re-schedule periodic sync
                        try {
                            val container = context.waymarkContainer()
                            val settings = container.settings.settings.first()
                            if (settings.automaticSync) {
                                SyncScheduler.schedulePeriodic(context, settings.syncIntervalHours, settings.allowMobileData)
                                Log.d("SyncRestoreReceiver", "Periodic sync scheduled")
                            }
                        } catch (e: Exception) {
                            Log.e("SyncRestoreReceiver", "Failed to restore periodic sync", e)
                        }
                        
                        // Trigger immediate sync - use forceSync to reload all data quickly
                        try {
                            SyncScheduler.syncNowFromWidget(context, forceSync = true)
                            Log.d("SyncRestoreReceiver", "Immediate sync triggered with forceSync=true")
                        } catch (e: Exception) {
                            Log.e("SyncRestoreReceiver", "Failed to trigger immediate sync", e)
                        }
                        
                        // Update widgets immediately (don't wait for sync to complete)
                        try {
                            WaymarkWidgetUpdater.updateAll(context)
                            Log.d("SyncRestoreReceiver", "Widgets updated immediately")
                        } catch (e: Exception) {
                            Log.e("SyncRestoreReceiver", "Failed to update widgets", e)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}