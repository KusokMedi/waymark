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
    version = 5,
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
        @Volatile
        private var instance: WaymarkDatabase? = null

        fun get(context: Context): WaymarkDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    WaymarkDatabase::class.java,
                    "waymark.db",
                )
                    .addMigrations(*Migrations.ALL)
                    .build()
                    .also { instance = it }
            }
    }
}
