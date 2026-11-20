package com.kusokmedi.waymark.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll

object WaymarkWidgetUpdater {
    private const val TAG = "WaymarkWidgetUpdater"

    suspend fun updateAll(context: Context) {
        try {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(WatchlistWidget::class.java)
            if (glanceIds.isEmpty()) {
                // Fallback to updateAll.
                WatchlistWidget().updateAll(context)
                return
            }
            glanceIds.forEach { glanceId ->
                runCatching { WatchlistWidget().update(context, glanceId) }
                    .onFailure { Log.e(TAG, "update failed for $glanceId", it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateAll failed", e)
            runCatching { WatchlistWidget().updateAll(context) }
        }
    }
}