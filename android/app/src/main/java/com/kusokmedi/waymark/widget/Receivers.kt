package com.kusokmedi.waymark.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.kusokmedi.waymark.data.local.WaymarkDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WaymarkWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WaymarkWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val db = WaymarkDatabase.get(context)
        CoroutineScope(Dispatchers.IO).launch {
            appWidgetIds.forEach { db.widgetConfigDao().delete(it) }
        }
    }
}

class CompactWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CompactWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val db = WaymarkDatabase.get(context)
        CoroutineScope(Dispatchers.IO).launch {
            appWidgetIds.forEach { db.widgetConfigDao().delete(it) }
        }
    }
}
