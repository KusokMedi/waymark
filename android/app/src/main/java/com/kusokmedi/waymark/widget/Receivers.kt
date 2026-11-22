package com.kusokmedi.waymark.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.kusokmedi.waymark.SyncScheduler
import com.kusokmedi.waymark.data.local.WaymarkDatabase
import com.kusokmedi.waymark.waymarkContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WatchlistWidgetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.i("WaymarkWidgetReceiver", "onReceive action=${intent.action}")
        // Widgets are updated via WaymarkWidgetUpdater when sync completes
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
        
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                when (action) {
                    Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                        Log.d("SyncRestoreReceiver", "Restoring sync after $action")
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
                        
                        try {
                            SyncScheduler.syncNowFromWidget(context, forceSync = true)
                            Log.d("SyncRestoreReceiver", "Immediate sync triggered with forceSync=true")
                        } catch (e: Exception) {
                            Log.e("SyncRestoreReceiver", "Failed to trigger immediate sync", e)
                        }
                        
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