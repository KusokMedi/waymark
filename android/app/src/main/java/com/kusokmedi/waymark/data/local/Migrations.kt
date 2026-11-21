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

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE widget_configs ADD COLUMN cornerRadius INTEGER NOT NULL DEFAULT 24")
            // Single-country widgets are removed; drop orphan configs so they don't linger.
            db.execSQL("DELETE FROM widget_configs WHERE widgetType = 'single'")
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE widget_configs ADD COLUMN backgroundAlpha INTEGER NOT NULL DEFAULT 100")
            db.execSQL("UPDATE widget_configs SET showLevel = 0")
            db.execSQL("UPDATE widget_configs SET cornerRadius = MIN(cornerRadius, 20)")
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("UPDATE widget_configs SET showLevel = 1")
            db.execSQL("UPDATE widget_configs SET showDate = 0")
        }
    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add index on occurredAt for better query performance on history sorting
            db.execSQL("CREATE INDEX IF NOT EXISTS index_history_events_occurredAt ON history_events(occurredAt)")
        }
    }

    val ALL: Array<Migration> = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6
    )
}
