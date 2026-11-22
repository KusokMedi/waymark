package com.kusokmedi.waymark.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.kusokmedi.waymark.R
import com.kusokmedi.waymark.data.AdvisoryRepository
import com.kusokmedi.waymark.data.local.WaymarkDatabase
import com.kusokmedi.waymark.domain.CountryCard
import com.kusokmedi.waymark.domain.flagEmoji
import kotlinx.coroutines.runBlocking

/**
 * RemoteViewsService for providing country items to widget ListView.
 * 
 * This service loads countries from the watchlist and provides them
 * to the widget's ListView for scrolling support.
 */
class WaymarkRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return WaymarkRemoteViewsFactory(applicationContext)
    }

    companion object {
        private const val TAG = "WaymarkRemoteViewsService"
    }
}

/**
 * Factory that provides RemoteViews for each country in the watchlist.
 * 
 * Key principles:
 * 1. Must be thread-safe (called from background thread)
 * 2. All DB access happens synchronously (runBlocking OK here)
 * 3. Each item is a RemoteViews with country info
 * 4. Clicks trigger broadcast with country ID
 */
class WaymarkRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    
    private var countries: List<CountryCard> = emptyList()
    
    companion object {
        private const val TAG = "WaymarkRemoteViewsFactory"
        
        // Fixed colors (must match widget provider)
        private const val COLOR_TEXT_PRIMARY = 0xFFFFFFFF.toInt()
        private const val COLOR_TEXT_SECONDARY = 0xFFB0B0B0.toInt()
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate")
    }

    override fun onDataSetChanged() {
        Log.d(TAG, "onDataSetChanged")
        try {
            // Load countries from watchlist (synchronous, runs on background thread)
            countries = runBlocking {
                val db = WaymarkDatabase.get(context)
                val watchlistEntities = db.watchlistDao().getAll()
                
                if (watchlistEntities.isEmpty()) {
                    emptyList()
                } else {
                    val watchlistIds = watchlistEntities.map { it.countryId }
                    val repository = AdvisoryRepository(db)
                    repository.countryCardsByIds(watchlistIds)
                }
            }
            Log.d(TAG, "Loaded ${countries.size} countries from watchlist")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load countries", e)
            countries = emptyList()
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
    }

    override fun getCount(): Int = countries.size

    override fun getViewAt(position: Int): RemoteViews? {
        return try {
            if (position < 0 || position >= countries.size) {
                null
            } else {
                buildCountryItemView(position)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build view at position $position", e)
            null
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true

    /**
     * Build a single country item view for ListView
     */
    private fun buildCountryItemView(position: Int): RemoteViews {
        val card = countries[position]
        val itemView = RemoteViews(context.packageName, R.layout.widget_country_item)
        
        try {
            itemView.setTextViewText(R.id.item_flag, flagEmoji(card.isoAlpha2))
            itemView.setTextViewText(R.id.item_name, card.name)
            itemView.setTextColor(R.id.item_name, COLOR_TEXT_PRIMARY)
            
            val levelText = "Level ${card.level?.number ?: "?"}" +
                if (card.containsHigherRiskAreas && card.level?.number in 1..3) " Higher risk" else ""
            itemView.setTextViewText(R.id.item_level, levelText)
            
            val levelColor = when (card.level?.number) {
                4 -> 0xFFB3261E.toInt()
                3 -> 0xFFC77700.toInt()
                2 -> 0xFF7A6A00.toInt()
                1 -> 0xFF2E6B4F.toInt()
                else -> 0xFF6E6E6E.toInt()
            }
            itemView.setTextColor(R.id.item_level, levelColor)
            
            // Click handler - broadcast with country ID
            val clickIntent = Intent(context, WaymarkAppWidgetProvider::class.java).apply {
                action = WaymarkAppWidgetProvider.ACTION_WIDGET_COUNTRY_CLICK
                putExtra(WaymarkAppWidgetProvider.EXTRA_COUNTRY_ID, card.id)
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
        } catch (e: Exception) {
            Log.e(TAG, "Error building country item view at position $position", e)
            // Return partially initialized view rather than crashing
        }
        
        return itemView
    }
}
