package com.bernaferrari.changedetection.repo.source.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap

/**
 * The Room Database that contains the Site table.
 * Inspired from Architecture Components MVVM sample app
 */
@Database(entities = [(Site::class), (Snap::class)], version = 2, exportSchema = false)
abstract class ChangeDatabase : RoomDatabase() {

    abstract fun siteDao(): SitesDao

    abstract fun snapsDao(): SnapsDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add all the new columns to the sites table
                database.execSQL("ALTER TABLE sites ADD COLUMN elementXPath TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE sites ADD COLUMN elementMonitoring INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE sites ADD COLUMN browserMode INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE sites ADD COLUMN cookieData TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE sites ADD COLUMN cssSelector TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE sites ADD COLUMN userAgent TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
