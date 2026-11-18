package com.kusokmedi.waymark.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

object WaymarkWidgetUpdater {
    suspend fun updateAll(context: Context) {
        runCatching { WatchlistWidget().updateAll(context) }
        runCatching { SingleCountryWidget().updateAll(context) }
    }
}
