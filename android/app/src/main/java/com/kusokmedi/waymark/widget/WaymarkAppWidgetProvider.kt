package com.kusokmedi.waymark.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import com.kusokmedi.waymark.R
import com.kusokmedi.waymark.SyncScheduler
import com.kusokmedi.waymark.data.AdvisoryRepository
import com.kusokmedi.waymark.data.local.WaymarkDatabase
import com.kusokmedi.waymark.domain.flagEmoji
import com.kusokmedi.waymark.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Native RemoteViews AppWidget - lightweight, no Glance overhead
 * - Refresh button sends broadcast directly (no app open)
 * - Clicking country opens MainActivity with country detail
 * - WorkManager handles sync in background
 * - StackView displays countries list (swipeable)
 */
class WaymarkAppWidgetProvider : AppWidgetProvider() {
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        Log.i(TAG, "onUpdate appWidgetIds=${appWidgetIds.toList()}")
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "onReceive action=${intent.action}")
        
        when (intent.action) {
            ACTION_WIDGET_REFRESH -> {
                val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                Log.d(TAG, "Refresh requested for widget $appWidgetId")
                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        SyncScheduler.syncNowFromWidget(context, forceSync = true)
                        Log.d(TAG, "Sync triggered")
                    } catch (e: Exception) {
                        Log.e(TAG, "Sync failed", e)
                    }
                }
            }
            ACTION_WIDGET_COUNTRY_CLICK -> {
                val countryId = intent.getStringExtra(EXTRA_COUNTRY_ID)
                if (!countryId.isNullOrBlank()) {
                    val appIntent = Intent(context, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtra(MainActivity.EXTRA_COUNTRY_ID, countryId)
                    }
                    context.startActivity(appIntent)
                }
            }
        }
        
        super.onReceive(context, intent)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        Log.d(TAG, "Widgets deleted")
        val db = WaymarkDatabase.get(context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                appWidgetIds.forEach { db.widgetConfigDao().delete(it) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete widget configs", e)
            }
        }
    }

    companion object {
        private const val TAG = "WaymarkAppWidget"
        const val ACTION_WIDGET_REFRESH = "com.kusokmedi.waymark.WIDGET_REFRESH"
        const val ACTION_WIDGET_COUNTRY_CLICK = "com.kusokmedi.waymark.WIDGET_COUNTRY_CLICK"
        const val EXTRA_COUNTRY_ID = "countryId"
        
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
        ) {
            Log.d(TAG, "updateAppWidget id=$appWidgetId")
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val views = RemoteViews(context.packageName, R.layout.widget_watchlist)
                    
                    val isDark = isSystemDarkTheme(context)
                    val fgColor = if (isDark) 0xFFE8F0EB.toInt() else 0xFF16201B.toInt()
                    val mutedColor = if (isDark) 0xFFA4B4AB.toInt() else 0xFF5E6E66.toInt()
                    
                    views.setInt(R.id.widget_title, "setTextColor", fgColor)
                    views.setInt(R.id.widget_status, "setTextColor", mutedColor)
                    
                    // Refresh button
                    val refreshIntent = Intent(context, WaymarkAppWidgetProvider::class.java).apply {
                        action = ACTION_WIDGET_REFRESH
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    }
                    val refreshPending = android.app.PendingIntent.getBroadcast(
                        context, appWidgetId, refreshIntent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or 
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) android.app.PendingIntent.FLAG_IMMUTABLE else 0
                    )
                    views.setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPending)
                    
                    // Load countries
                    val db = WaymarkDatabase.get(context)
                    val watchlistIds = db.watchlistDao().getAll().map { it.countryId }
                    val cards = AdvisoryRepository(db).countryCardsByIds(watchlistIds)
                    
                    if (cards.isEmpty()) {
                        views.setTextViewText(R.id.widget_status, "Watchlist empty")
                    } else {
                        views.setTextViewText(R.id.widget_status, "Updated · ${cards.size} countries")
                        
                        views.removeAllViews(R.id.widget_stack)
                        
                        for (card in cards.take(10)) {
                            val itemView = RemoteViews(context.packageName, R.layout.widget_country_item)
                            itemView.setTextViewText(R.id.item_name, card.name)
                            itemView.setTextViewText(R.id.item_flag, flagEmoji(card.isoAlpha2))
                            
                            val levelText = "Level ${card.level?.number ?: "?"}" +
                                if (card.containsHigherRiskAreas) " · Higher risk" else ""
                            itemView.setTextViewText(R.id.item_level, levelText)
                            
                            val levelColor = when (card.level?.number) {
                                4 -> 0xFFB3261E.toInt()
                                3 -> 0xFFC77700.toInt()
                                2 -> 0xFF7A6A00.toInt()
                                1 -> 0xFF2E6B4F.toInt()
                                else -> 0xFF6E6E6E.toInt()
                            }
                            itemView.setTextColor(R.id.item_level, levelColor)
                            
                            val clickIntent = Intent(context, WaymarkAppWidgetProvider::class.java).apply {
                                action = ACTION_WIDGET_COUNTRY_CLICK
                                putExtra(EXTRA_COUNTRY_ID, card.id)
                            }
                            val clickPending = android.app.PendingIntent.getBroadcast(
                                context, card.id.hashCode(), clickIntent,
                                android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) android.app.PendingIntent.FLAG_IMMUTABLE else 0
                            )
                            itemView.setOnClickPendingIntent(R.id.item_root, clickPending)
                            views.addView(R.id.widget_stack, itemView)
                        }
                    }
                    
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                    Log.d(TAG, "Widget updated")
                } catch (e: Exception) {
                    Log.e(TAG, "Update failed", e)
                }
            }
        }
        
        private fun isSystemDarkTheme(context: Context): Boolean {
            val config = context.resources.configuration
            return (config.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
    }
}
