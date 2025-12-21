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
                    // Log detailed error information but allow app to continue
                    android.util.Log.e("ChangeDatabase", "❌ Migration 1->2 failed: ${e.message}", e)
                    // Re-throw for crash reporting, but handled by Room's migration strategy
                    throw RuntimeException("Database migration 1->2 failed", e)
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
                    android.util.Log.e("ChangeDatabase", "❌ Migration 2->3 failed: ${e.message}", e)
                    throw RuntimeException("Database migration 2->3 failed", e)
                }
            }
        }
        
        val MIGRATION_3_4 = object : Migration(3, 4) {
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

                    // Browser and Request Configuration
                    safeAddColumn(database, existingColumns, "followRedirects", "INTEGER NOT NULL DEFAULT 1")
                    safeAddColumn(database, existingColumns, "requestTimeoutMs", "INTEGER NOT NULL DEFAULT 30000")
                    safeAddColumn(database, existingColumns, "customHeadersJson", "TEXT NOT NULL DEFAULT ''")

                    // Authentication Support
                    safeAddColumn(database, existingColumns, "requiresAuthentication", "INTEGER NOT NULL DEFAULT 0")
                    safeAddColumn(database, existingColumns, "authenticationMethod", "TEXT NOT NULL DEFAULT 'NONE'")
                    safeAddColumn(database, existingColumns, "encryptedCredentials", "TEXT NOT NULL DEFAULT ''")
                    safeAddColumn(database, existingColumns, "oauthTokenData", "TEXT NOT NULL DEFAULT ''")
                    safeAddColumn(database, existingColumns, "sessionPersistence", "INTEGER NOT NULL DEFAULT 1")

                    // Enhanced Element Monitoring
                    safeAddColumn(database, existingColumns, "multipleElementMonitoring", "INTEGER NOT NULL DEFAULT 0")
                    safeAddColumn(database, existingColumns, "elementHighlighting", "INTEGER NOT NULL DEFAULT 0")

                    // Smart Content Processing Enhancements
                    safeAddColumn(database, existingColumns, "noiseFiltering", "INTEGER NOT NULL DEFAULT 1")
                    safeAddColumn(database, existingColumns, "contentNormalization", "INTEGER NOT NULL DEFAULT 1")

                    // Sync and Priority Settings
                    safeAddColumn(database, existingColumns, "syncPriority", "TEXT NOT NULL DEFAULT 'NORMAL'")
                    safeAddColumn(database, existingColumns, "backgroundSyncEnabled", "INTEGER NOT NULL DEFAULT 1")
                    safeAddColumn(database, existingColumns, "syncFrequencyMinutes", "INTEGER NOT NULL DEFAULT 60")

                    // Proxy and Network Settings
                    safeAddColumn(database, existingColumns, "useProxy", "INTEGER NOT NULL DEFAULT 0")
                    safeAddColumn(database, existingColumns, "proxyConfigJson", "TEXT NOT NULL DEFAULT ''")
                    safeAddColumn(database, existingColumns, "retryAttempts", "INTEGER NOT NULL DEFAULT 3")
                    safeAddColumn(database, existingColumns, "retryDelayMs", "INTEGER NOT NULL DEFAULT 5000")

                    // Convert contentFocusMode from string to enum-compatible values
                    database.execSQL("UPDATE sites SET contentFocusMode = 'AUTO' WHERE contentFocusMode NOT IN ('AUTO', 'ECOMMERCE', 'NEWS', 'GENERAL', 'CUSTOM')")
                } catch (e: Exception) {
                    android.util.Log.e("ChangeDatabase", "❌ Migration 3->4 failed: ${e.message}", e)
                    throw RuntimeException("Database migration 3->4 failed", e)
                }
            }

            private fun safeAddColumn(database: SupportSQLiteDatabase, existingColumns: Set<String>, columnName: String, columnDef: String) {
                if (!existingColumns.contains(columnName)) {
                    try {
                        database.execSQL("ALTER TABLE sites ADD COLUMN $columnName $columnDef")
                    } catch (e: Exception) {
                        android.util.Log.w("ChangeDatabase", "⚠️ Could not add column $columnName: ${e.message}")
                    }
                }
            }
        }
    }
}
