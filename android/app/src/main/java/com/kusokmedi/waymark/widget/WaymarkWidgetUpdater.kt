package com.kusokmedi.waymark.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll

object WaymarkWidgetUpdater {
    private const val TAG = "WaymarkWidgetUpdater"

    suspend fun updateAll(context: Context) {
        runCatching { WatchlistWidget().updateAll(context) }
            .onFailure { Log.e(TAG, "updateAll failed", it) }
    }
}