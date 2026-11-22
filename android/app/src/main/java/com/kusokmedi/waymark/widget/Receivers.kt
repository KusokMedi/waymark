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
 * Ensures WorkManager jobs resume after device boot or app update.
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
                        
                        // Trigger immediate sync
                        try {
                            SyncScheduler.syncNow(context, false)
                            Log.d("SyncRestoreReceiver", "Immediate sync triggered")
                        } catch (e: Exception) {
                            Log.e("SyncRestoreReceiver", "Failed to trigger immediate sync", e)
                        }
                        
                        // Update widgets
                        try {
                            WaymarkWidgetUpdater.updateAll(context)
                            Log.d("SyncRestoreReceiver", "Widgets updated")
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