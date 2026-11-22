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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Native RemoteViews widget with proper async data loading.
 * 
 * Key principles:
 * 1. onUpdate() NEVER blocks or calls suspend functions
 * 2. All DB access happens on IO dispatcher
 * 3. Three states: Loading → Data/Empty/Error
 * 4. Each state has a minimal, safe layout
 */
class WaymarkAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        Log.i(TAG, "onUpdate: ${appWidgetIds.size} widgets")
        
        for (appWidgetId in appWidgetIds) {
            // Show loading state immediately (non-blocking)
            showLoadingState(context, appWidgetManager, appWidgetId)
            
            // Load data asynchronously on IO thread
            loadAndUpdateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "onReceive: ${intent.action}")

        when (intent.action) {
            ACTION_WIDGET_REFRESH -> {
                val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                if (appWidgetId >= 0) {
                    Log.d(TAG, "Refresh pressed for widget $appWidgetId")
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    showLoadingState(context, appWidgetManager, appWidgetId)
                    
                    // Trigger sync and update widget
                    CoroutineScope(Dispatchers.Default).launch {
                        try {
                            SyncScheduler.syncNowFromWidget(context, forceSync = true)
                            // After sync, reload widget data
                            loadAndUpdateWidget(context, appWidgetManager, appWidgetId)
                        } catch (e: Exception) {
                            Log.e(TAG, "Sync failed", e)
                            showErrorState(context, appWidgetManager, appWidgetId)
                        }
                    }
                }
            }
            ACTION_WIDGET_COUNTRY_CLICK -> {
                val countryId = intent.getStringExtra(EXTRA_COUNTRY_ID)
                if (!countryId.isNullOrBlank()) {
                    val appIntent = Intent(context, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
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
        Log.d(TAG, "onDeleted: ${appWidgetIds.size} widgets")
    }

    companion object {
        private const val TAG = "WaymarkAppWidget"
        const val ACTION_WIDGET_REFRESH = "com.kusokmedi.waymark.WIDGET_REFRESH"
        const val ACTION_WIDGET_COUNTRY_CLICK = "com.kusokmedi.waymark.WIDGET_COUNTRY_CLICK"
        const val EXTRA_COUNTRY_ID = "countryId"
        
        // Fixed colors
        private const val COLOR_BG = 0xFF171717.toInt()
        private const val COLOR_ITEM = 0xFF303030.toInt()
        private const val COLOR_TEXT_PRIMARY = 0xFFFFFFFF.toInt()  // White
        private const val COLOR_TEXT_SECONDARY = 0xFFB0B0B0.toInt()  // Light gray
        
        private fun getFormattedUpdateTime(): String {
            val now = Date()
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            return "Updated ${format.format(now)}"
        }

        /**
         * Show loading state - fast, non-blocking, safe
         */
        private fun showLoadingState(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
        ) {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_watchlist_simple)
                
                views.setTextViewText(R.id.widget_last_updated, getFormattedUpdateTime())
                views.setTextColor(R.id.widget_title, COLOR_TEXT_PRIMARY)
                views.setTextColor(R.id.widget_last_updated, COLOR_TEXT_SECONDARY)
                
                // Clear any previous content
                views.removeAllViews(R.id.widget_countries_container)
                
                // Show loading message
                val loadingView = RemoteViews(context.packageName, R.layout.widget_state_loading)
                views.addView(R.id.widget_countries_container, loadingView)
                
                // Setup refresh button
                setupRefreshButton(context, views, appWidgetId)
                
                appWidgetManager.updateAppWidget(appWidgetId, views)
                Log.d(TAG, "Loading state shown for widget $appWidgetId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show loading state", e)
            }
        }

        /**
         * Load data from DB and update widget
         * This runs on IO dispatcher - safe for DB calls
         */
        private fun loadAndUpdateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = WaymarkDatabase.get(context)
                    
                    // Get watchlist
                    val watchlistEntities = db.watchlistDao().getAll()
                    if (watchlistEntities.isEmpty()) {
                        showEmptyState(context, appWidgetManager, appWidgetId)
                        return@launch
                    }
                    
                    val watchlistIds = watchlistEntities.map { it.countryId }
                    
                    // Get country cards
                    val repository = AdvisoryRepository(db)
                    val cards = repository.countryCardsByIds(watchlistIds)
                    
                    if (cards.isEmpty()) {
                        showEmptyState(context, appWidgetManager, appWidgetId)
                        return@launch
                    }
                    
                    // Build widget with data
                    val views = RemoteViews(context.packageName, R.layout.widget_watchlist_simple)
                    
                    views.setTextViewText(R.id.widget_last_updated, getFormattedUpdateTime())
                    views.setTextColor(R.id.widget_title, COLOR_TEXT_PRIMARY)
                    views.setTextColor(R.id.widget_last_updated, COLOR_TEXT_SECONDARY)
                    views.removeAllViews(R.id.widget_countries_container)
                    
                    // Add country items (limit to 10)
                    for (card in cards.take(10)) {
                        val itemView = buildCountryItemView(context, card, appWidgetId)
                        views.addView(R.id.widget_countries_container, itemView)
                    }
                    
                    setupRefreshButton(context, views, appWidgetId)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                    
                    Log.d(TAG, "Widget $appWidgetId updated with ${cards.size} countries")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load and update widget $appWidgetId", e)
                    showErrorState(context, appWidgetManager, appWidgetId)
                }
            }
        }

        /**
         * Build a single country item
         */
        private fun buildCountryItemView(
            context: Context,
            card: com.kusokmedi.waymark.domain.CountryCard,
            appWidgetId: Int,
        ): RemoteViews {
            val itemView = RemoteViews(context.packageName, R.layout.widget_country_item)
            
            itemView.setTextViewText(R.id.item_flag, flagEmoji(card.isoAlpha2))
            itemView.setTextViewText(R.id.item_name, card.name)
            itemView.setTextColor(R.id.item_name, COLOR_TEXT_PRIMARY)
            
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
            
            // Click handler
            val clickIntent = Intent(context, WaymarkAppWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_COUNTRY_CLICK
                putExtra(EXTRA_COUNTRY_ID, card.id)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val clickPending = android.app.PendingIntent.getBroadcast(
                context,
                card.id.hashCode(),
                clickIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) 
                    android.app.PendingIntent.FLAG_IMMUTABLE else 0)
            )
            itemView.setOnClickPendingIntent(R.id.item_root, clickPending)
            
            return itemView
        }

        /**
         * Show empty watchlist state
         */
        private fun showEmptyState(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
        ) {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_watchlist_simple)
                
                views.setTextViewText(R.id.widget_last_updated, getFormattedUpdateTime())
                views.setTextColor(R.id.widget_title, COLOR_TEXT_PRIMARY)
                views.setTextColor(R.id.widget_last_updated, COLOR_TEXT_SECONDARY)
                views.removeAllViews(R.id.widget_countries_container)
                
                val emptyView = RemoteViews(context.packageName, R.layout.widget_state_empty)
                views.addView(R.id.widget_countries_container, emptyView)
                
                setupRefreshButton(context, views, appWidgetId)
                appWidgetManager.updateAppWidget(appWidgetId, views)
                
                Log.d(TAG, "Empty state shown for widget $appWidgetId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show empty state", e)
            }
        }

        /**
         * Show error state
         */
        private fun showErrorState(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
        ) {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_watchlist_simple)
                
                views.setTextViewText(R.id.widget_last_updated, getFormattedUpdateTime())
                views.setTextColor(R.id.widget_title, COLOR_TEXT_PRIMARY)
                views.setTextColor(R.id.widget_last_updated, COLOR_TEXT_SECONDARY)
                views.removeAllViews(R.id.widget_countries_container)
                
                val errorView = RemoteViews(context.packageName, R.layout.widget_state_error)
                views.addView(R.id.widget_countries_container, errorView)
                
                setupRefreshButton(context, views, appWidgetId)
                appWidgetManager.updateAppWidget(appWidgetId, views)
                
                Log.d(TAG, "Error state shown for widget $appWidgetId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show error state", e)
            }
        }

        /**
         * Setup refresh button on any state
         */
        private fun setupRefreshButton(
            context: Context,
            views: RemoteViews,
            appWidgetId: Int,
        ) {
            try {
                val refreshIntent = Intent(context, WaymarkAppWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_REFRESH
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                val refreshPending = android.app.PendingIntent.getBroadcast(
                    context,
                    appWidgetId,
                    refreshIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) 
                        android.app.PendingIntent.FLAG_IMMUTABLE else 0)
                )
                views.setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPending)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to setup refresh button", e)
            }
        }
    }
}
