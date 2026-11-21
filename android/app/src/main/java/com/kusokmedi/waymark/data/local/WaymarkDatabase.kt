package com.kusokmedi.waymark.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CountryEntity::class,
        AdvisoryEntity::class,
        WatchlistEntity::class,
        HistoryEntity::class,
        SyncStateEntity::class,
        WidgetConfigEntity::class,
    ],
    version = 6,
    exportSchema = true,
)
abstract class WaymarkDatabase : RoomDatabase() {
    abstract fun countryDao(): CountryDao
    abstract fun advisoryDao(): AdvisoryDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun historyDao(): HistoryDao
    abstract fun syncStateDao(): SyncStateDao
    abstract fun widgetConfigDao(): WidgetConfigDao

    companion object {
        private val instance: WaymarkDatabase by lazy {
            throw IllegalStateException(
                "Database must be initialized with get(context) before first access"
            )
        }
        
        @Volatile
        private var INSTANCE: WaymarkDatabase? = null

        fun get(context: Context): WaymarkDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WaymarkDatabase::class.java,
                    "waymark.db",
                )
                    .addMigrations(*Migrations.ALL)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}