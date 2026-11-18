package com.kusokmedi.waymark.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Explicit migrations live here. Version 1 is the initial released schema.
 *
 * Future versions must add a Migration here and a migration test under
 * `src/androidTest`. Destructive fallbacks are intentionally not configured.
 */
object Migrations {
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE widget_configs ADD COLUMN widgetType TEXT NOT NULL DEFAULT 'watchlist'")
            db.execSQL("ALTER TABLE widget_configs ADD COLUMN singleCountryId TEXT")
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
