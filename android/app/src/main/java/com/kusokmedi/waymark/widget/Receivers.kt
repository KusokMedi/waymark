package com.kusokmedi.waymark.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.kusokmedi.waymark.data.local.WaymarkDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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