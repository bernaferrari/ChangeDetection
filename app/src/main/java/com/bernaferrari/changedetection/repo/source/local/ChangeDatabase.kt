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
@Database(entities = [(Site::class), (Snap::class)], version = 3, exportSchema = false)
abstract class ChangeDatabase : RoomDatabase() {

    abstract fun siteDao(): SitesDao

    abstract fun snapsDao(): SnapsDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // No schema changes; keep for historical versioning.
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `sites_new` (
                            `title` TEXT,
                            `url` TEXT NOT NULL,
                            `timestamp` INTEGER NOT NULL,
                            `siteId` TEXT NOT NULL,
                            `isSuccessful` INTEGER NOT NULL,
                            `isBrowser` INTEGER NOT NULL,
                            `threshold` INTEGER NOT NULL,
                            `isRead` INTEGER NOT NULL,
                            `isSyncEnabled` INTEGER NOT NULL,
                            `isNotificationEnabled` INTEGER NOT NULL,
                            `notes` TEXT NOT NULL,
                            `colors` TEXT NOT NULL,
                            `constraints` TEXT NOT NULL,
                            PRIMARY KEY(`siteId`)
                        )
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        INSERT INTO `sites_new` (
                            `title`, `url`, `timestamp`, `siteId`, `isSuccessful`, `isBrowser`,
                            `threshold`, `isRead`, `isSyncEnabled`, `isNotificationEnabled`,
                            `notes`, `colors`, `constraints`
                        )
                        SELECT
                            `title`, `url`, `timestamp`, `siteId`, `isSuccessful`, `isBrowser`,
                            `threshold`, `isRead`, `isSyncEnabled`, `isNotificationEnabled`,
                            `notes`, `colors`, `constraints`
                        FROM `sites`
                        """.trimIndent()
                    )

                    database.execSQL("DROP TABLE `sites`")
                    database.execSQL("ALTER TABLE `sites_new` RENAME TO `sites`")
                    database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_sites_siteId` ON `sites` (`siteId`)")
                } catch (e: Exception) {
                    android.util.Log.e("ChangeDatabase", "❌ Migration 2->3 failed: ${e.message}", e)
                    throw RuntimeException("Database migration 2->3 failed", e)
                }
            }
        }
    }
}
