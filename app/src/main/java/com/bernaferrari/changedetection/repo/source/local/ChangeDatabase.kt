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
@Database(entities = [(Site::class), (Snap::class)], version = 4, exportSchema = false)
abstract class ChangeDatabase : RoomDatabase() {

    abstract fun siteDao(): SitesDao

    abstract fun snapsDao(): SnapsDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Check if columns already exist to prevent errors
                    val cursor = database.query("PRAGMA table_info(sites)")
                    val existingColumns = mutableSetOf<String>()
                    while (cursor.moveToNext()) {
                        val columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                        existingColumns.add(columnName)
                    }
                    cursor.close()
                    
                    // Add columns only if they don't exist
                    val columnsToAdd = mapOf(
                        "elementXPath" to "TEXT NOT NULL DEFAULT ''",
                        "elementMonitoring" to "INTEGER NOT NULL DEFAULT 0",
                        "browserMode" to "INTEGER NOT NULL DEFAULT 0",
                        "cookieData" to "TEXT NOT NULL DEFAULT ''",
                        "cssSelector" to "TEXT NOT NULL DEFAULT ''",
                        "userAgent" to "TEXT NOT NULL DEFAULT ''"
                    )
                    
                    columnsToAdd.forEach { (columnName, columnDef) ->
                        if (!existingColumns.contains(columnName)) {
                            database.execSQL("ALTER TABLE sites ADD COLUMN $columnName $columnDef")
                        }
                    }
                } catch (e: Exception) {
                    // Log error but don't crash - allow app to continue with existing schema
                    android.util.Log.e("ChangeDatabase", "Migration 1->2 failed: ${e.message}")
                }
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Check existing columns
                    val cursor = database.query("PRAGMA table_info(sites)")
                    val existingColumns = mutableSetOf<String>()
                    while (cursor.moveToNext()) {
                        val columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                        existingColumns.add(columnName)
                    }
                    cursor.close()
                    
                    // Add columns only if they don't exist
                    val columnsToAdd = mapOf(
                        "smartMonitoringEnabled" to "INTEGER NOT NULL DEFAULT 0",
                        "zapRulesJson" to "TEXT NOT NULL DEFAULT ''",
                        "customFiltersJson" to "TEXT NOT NULL DEFAULT ''",
                        "contentFocusMode" to "TEXT NOT NULL DEFAULT 'AUTO'"
                    )
                    
                    columnsToAdd.forEach { (columnName, columnDef) ->
                        if (!existingColumns.contains(columnName)) {
                            database.execSQL("ALTER TABLE sites ADD COLUMN $columnName $columnDef")
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ChangeDatabase", "Migration 2->3 failed: ${e.message}")
                }
            }
        }
        
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Browser and Request Configuration
                database.execSQL("ALTER TABLE sites ADD COLUMN followRedirects INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE sites ADD COLUMN requestTimeoutMs INTEGER NOT NULL DEFAULT 30000")
                database.execSQL("ALTER TABLE sites ADD COLUMN customHeadersJson TEXT NOT NULL DEFAULT ''")
                
                // Authentication Support
                database.execSQL("ALTER TABLE sites ADD COLUMN requiresAuthentication INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE sites ADD COLUMN authenticationMethod TEXT NOT NULL DEFAULT 'NONE'")
                database.execSQL("ALTER TABLE sites ADD COLUMN encryptedCredentials TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE sites ADD COLUMN oauthTokenData TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE sites ADD COLUMN sessionPersistence INTEGER NOT NULL DEFAULT 1")
                
                // Enhanced Element Monitoring
                database.execSQL("ALTER TABLE sites ADD COLUMN multipleElementMonitoring INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE sites ADD COLUMN elementHighlighting INTEGER NOT NULL DEFAULT 0")
                
                // Smart Content Processing Enhancements
                database.execSQL("ALTER TABLE sites ADD COLUMN noiseFiltering INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE sites ADD COLUMN contentNormalization INTEGER NOT NULL DEFAULT 1")
                
                // Sync and Priority Settings
                database.execSQL("ALTER TABLE sites ADD COLUMN syncPriority TEXT NOT NULL DEFAULT 'NORMAL'")
                database.execSQL("ALTER TABLE sites ADD COLUMN backgroundSyncEnabled INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE sites ADD COLUMN syncFrequencyMinutes INTEGER NOT NULL DEFAULT 60")
                
                // Proxy and Network Settings
                database.execSQL("ALTER TABLE sites ADD COLUMN useProxy INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE sites ADD COLUMN proxyConfigJson TEXT NOT NULL DEFAULT ''") 
                database.execSQL("ALTER TABLE sites ADD COLUMN retryAttempts INTEGER NOT NULL DEFAULT 3")
                database.execSQL("ALTER TABLE sites ADD COLUMN retryDelayMs INTEGER NOT NULL DEFAULT 5000")
                
                // Convert contentFocusMode from string to enum-compatible values
                database.execSQL("UPDATE sites SET contentFocusMode = 'AUTO' WHERE contentFocusMode NOT IN ('AUTO', 'ECOMMERCE', 'NEWS', 'GENERAL', 'CUSTOM')")
            }
        }
    }
}
