package com.kusokmedi.waymark.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

object WaymarkWidgetUpdater {
    suspend fun updateAll(context: Context) {
        runCatching { WaymarkWidget().updateAll(context) }
        runCatching { CompactWidget().updateAll(context) }
    }
}
