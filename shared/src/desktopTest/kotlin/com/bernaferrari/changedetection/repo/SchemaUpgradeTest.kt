package com.bernaferrari.changedetection.repo

import androidx.room3.Room
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.bernaferrari.changedetection.repo.source.local.MIGRATION_1_4
import com.bernaferrari.changedetection.repo.source.local.ChangeDatabase
import com.bernaferrari.changedetection.repo.source.local.getRoomDatabase
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

/**
 * The public releases (database version 1, in its 2019 and 2019–2024 shapes) upgrade straight to today's schema
 * with every page and its history intact. Room validates the final tables and indexes on open, so a migration
 * that drifts from the entities fails here.
 */
class SchemaUpgradeTest {
    @Test fun release22DatabaseUpgrades() = runBlocking { upgradeVersionOne(withBrowserColumns = false) }

    @Test fun releases2019To2024DatabaseUpgrades() = runBlocking { upgradeVersionOne(withBrowserColumns = true) }

    @Test fun retiredDevelopmentSchemasStartFresh() = runBlocking {
        for (version in listOf(2, 3, 10)) {
            withDatabaseFile { path, driver ->
                getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = path), driver).apply {
                    siteDao().getSitesList()
                    close()
                }
                driver.open(path).use { connection ->
                    connection.exec("INSERT INTO sites (title,url,timestamp,siteId,isSuccessful,isBrowser,threshold,isRead,isSyncEnabled,isNotificationEnabled,notes,colors,constraints) VALUES ('Page','https://example.test',123,'page',1,0,0,0,1,1,'notes','0,0','false,false,false,false')")
                    connection.exec("INSERT INTO snaps (snapId,siteId,timestamp,contentType,contentCharset,contentSize,contentHash,processedContentHash) VALUES ('saved','page',123,'text/plain','UTF-8',5,'hash','processed')")
                    connection.exec("ALTER TABLE sites DROP COLUMN iconShape")
                    connection.exec("PRAGMA user_version = $version")
                }
                val reset = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = path), driver)
                try {
                    assertTrue("retired schema $version must start fresh", reset.siteDao().getSitesList().isEmpty())
                    assertTrue(reset.snapsDao().getAllSnapsForSiteId("page").isEmpty())
                    reset.siteDao().insertSite(Site(title = "New page", url = "https://example.test", timestamp = 0, tags = "", id = "new", colors = 0 to 0))
                    assertEquals(PageIconShape.AUTOMATIC, reset.siteDao().getSiteById("new")?.iconShape)
                } finally {
                    reset.close()
                }
                driver.open(path).use { assertEquals("6", it.queryAll("PRAGMA user_version").trim()) }
            }
        }
    }

    /** Version 5 is on people's phones: a page that was already failing keeps its history and starts its streak at zero. */
    @Test fun aPublicSchemaWithoutAMigrationPathIsNeverErased() = runBlocking {
        withDatabaseFile { path, driver ->
            getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = path), driver).apply {
                siteDao().getSitesList()
                close()
            }
            driver.open(path).use { connection ->
                connection.exec("INSERT INTO sites (title,url,timestamp,siteId,isSuccessful,isBrowser,threshold,isRead,isSyncEnabled,isNotificationEnabled,notes,colors,constraints) VALUES ('Page','https://example.test',123,'page',1,0,0,0,1,1,'notes','0,0','false,false,false,false')")
                // Pretend this file is the public version 4, then open it with no migration from version 4 registered.
                connection.exec("PRAGMA user_version = 4")
            }
            val failure = runCatching {
                getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = path), driver, migrations = arrayOf(MIGRATION_1_4)).apply {
                    try {
                        siteDao().getSitesList()
                    } finally {
                        close()
                    }
                }
            }.exceptionOrNull()
            assertNotNull("a public schema with no migration path must refuse to open, not start fresh", failure)
            driver.open(path).use { connection ->
                assertEquals("the page must still be on disk", "1", connection.queryAll("SELECT count(*) FROM sites").trim())
                assertEquals("4", connection.queryAll("PRAGMA user_version").trim())
            }
        }
    }

    @Test fun versionFiveUpgradeKeepsPagesAndStartsFailureStreaksEmpty() = runBlocking {
        withDatabaseFile { path, driver ->
            val schema = sequenceOf("schemas", "shared/schemas").map { File("$it/com.bernaferrari.changedetection.repo.source.local.ChangeDatabase/5.json") }.first { it.exists() }
            val database = Json.parseToJsonElement(schema.readText()).jsonObject.getValue("database").jsonObject
            driver.open(path).use { connection ->
                for (entity in database.getValue("entities").jsonArray) {
                    val table = entity.jsonObject
                    val name = table.getValue("tableName").jsonPrimitive.content
                    connection.exec(table.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", name))
                    for (index in table["indices"]?.jsonArray.orEmpty()) connection.exec(index.jsonObject.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", name))
                }
                for (query in database.getValue("setupQueries").jsonArray) connection.exec(query.jsonPrimitive.content)
                connection.exec("INSERT INTO sites (title,url,timestamp,siteId,isSuccessful,isBrowser,threshold,isRead,isSyncEnabled,isNotificationEnabled,notes,colors,constraints,lastFailureCode) VALUES ('Page','https://example.test',123,'page',0,0,0,1,1,1,'notes','0,0','false, false, false, false','LOGIN')")
                connection.exec("INSERT INTO snaps (snapId,siteId,timestamp,contentType,contentCharset,contentSize,contentHash,processedContentHash) VALUES ('saved','page',100,'text/plain','UTF-8',5,'hash','processed')")
                connection.exec("PRAGMA user_version = 5")
            }
            val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = path), driver)
            try {
                val site = requireNotNull(db.siteDao().getSiteById("page")) { "the page was lost" }
                assertEquals("notes", site.notes)
                assertEquals("LOGIN", site.lastFailureCode)
                assertEquals(0, site.failureCount)
                assertNull(site.failingSince)
                assertFalse(site.failureAlerted)
                assertEquals("saved", db.snapsDao().getLastSnapForSiteId("page")?.snapId)
                // The streak begins with the first check after the upgrade.
                db.siteDao().updateSyncMetadata("page", 500, successful = false, isBrowser = false, isRead = null, failureCode = "LOGIN")
                val failed = requireNotNull(db.siteDao().getSiteById("page"))
                assertEquals(1, failed.failureCount)
                assertEquals(500L, failed.failingSince)
            } finally {
                db.close()
            }
            driver.open(path).use { assertEquals("6", it.queryAll("PRAGMA user_version").trim()) }
        }
    }

    @Test fun historyQueriesUseTheirIndexWithoutSorting() = runBlocking {
        withDatabaseFile { path, driver ->
            getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = path), driver).apply {
                siteDao().getSitesList()
                close()
            }
            driver.open(path).use { connection ->
                val plan = connection.queryAll("EXPLAIN QUERY PLAN SELECT * FROM snaps WHERE siteId = 'page' ORDER BY timestamp DESC, snapId DESC LIMIT 1")
                assertTrue(plan, "index_snaps_siteId_timestamp_snapId" in plan)
                assertFalse(plan, "TEMP B-TREE" in plan)
            }
        }
    }

    private suspend fun upgradeVersionOne(withBrowserColumns: Boolean) = withDatabaseFile { path, driver ->
        driver.open(path).use { connection ->
            // Exactly what Room 2 created for the old entities.
            connection.exec(
                "CREATE TABLE IF NOT EXISTS `sites` (`title` TEXT, `url` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `siteId` TEXT NOT NULL, " +
                    "`isSuccessful` INTEGER NOT NULL, " + (if (withBrowserColumns) "`isBrowser` INTEGER NOT NULL, `threshold` INTEGER NOT NULL, " else "") +
                    "`isRead` INTEGER NOT NULL, `isSyncEnabled` INTEGER NOT NULL, `isNotificationEnabled` INTEGER NOT NULL, `notes` TEXT NOT NULL, " +
                    "`colors` TEXT NOT NULL, `constraints` TEXT NOT NULL, PRIMARY KEY(`siteId`))",
            )
            connection.exec("CREATE UNIQUE INDEX IF NOT EXISTS `index_sites_siteId` ON `sites` (`siteId`)")
            connection.exec(
                "CREATE TABLE IF NOT EXISTS `snaps` (`snapId` TEXT NOT NULL, `siteId` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `contentType` TEXT NOT NULL, " +
                    "`contentCharset` TEXT NOT NULL, `contentSize` INTEGER NOT NULL, PRIMARY KEY(`snapId`), " +
                    "FOREIGN KEY(`siteId`) REFERENCES `sites`(`siteId`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            connection.exec("CREATE UNIQUE INDEX IF NOT EXISTS `index_snaps_siteId_snapId` ON `snaps` (`siteId`, `snapId`)")
            connection.exec("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
            connection.exec("INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, 'legacy')")

            val browser = if (withBrowserColumns) ", isBrowser, threshold" else ""
            val browserValues = if (withBrowserColumns) ", 1, 512" else ""
            connection.exec(
                "INSERT INTO sites (title, url, timestamp, siteId, isSuccessful$browser, isRead, isSyncEnabled, isNotificationEnabled, notes, colors, constraints) " +
                    "VALUES ('Fellowship', 'https://example.test', 1560000000000, 'page', 1$browserValues, 0, 1, 1, 'grants', '-16728876,-12627531', 'false, true, false, false')",
            )
            connection.exec(
                "INSERT INTO sites (title, url, timestamp, siteId, isSuccessful$browser, isRead, isSyncEnabled, isNotificationEnabled, notes, colors, constraints) " +
                    "VALUES (NULL, 'https://broken.test', 1560000000000, 'broken', 0${if (withBrowserColumns) ", 0, 0" else ""}, 0, 0, 1, '', '0,0', 'false, false, false, false')",
            )
            connection.exec("INSERT INTO snaps VALUES ('old', 'page', 1550000000000, 'text/html', 'utf-8', 1200)")
            connection.exec("INSERT INTO snaps VALUES ('new', 'page', 1560000000000, 'text/html', 'utf-8', 1250)")
            connection.exec("PRAGMA user_version = 1")
        }

        val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = path), driver)
        try {
            val site = requireNotNull(db.siteDao().getSiteById("page")) { "the page was lost" }
            assertEquals("Fellowship", site.title)
            assertEquals("grants", site.notes)
            assertEquals(-16728876 to -12627531, site.colors)
            assertTrue(site.constraints.wifi)
            assertEquals(if (withBrowserColumns) 512 else 0, site.threshold)
            assertEquals(withBrowserColumns, site.isBrowser)
            assertEquals(CaptureMode.DEFAULT, site.captureMode)
            assertEquals(1560000000000, site.lastSuccessfulCheck)
            assertTrue("upgraded pages must not all look new", site.isRead)
            assertEquals("", site.iconUrl)
            assertEquals(PageIconShape.AUTOMATIC, site.iconShape)
            assertEquals(0, site.visualChangeThresholdBps)

            val broken = requireNotNull(db.siteDao().getSiteById("broken"))
            assertNull("a failed check never becomes a success", broken.lastSuccessfulCheck)
            assertFalse(broken.isSyncEnabled)

            assertEquals("new", db.snapsDao().getLastSnapForSiteId("page")?.snapId)
            db.siteDao().updateIcon("page", "https://example.test/icon.png")
            assertEquals("https://example.test/icon.png", db.siteDao().getSiteById("page")?.iconUrl)
            db.siteDao().updateIconShape("page", PageIconShape.CLOVER)
            db.siteDao().updateIconColor("page", com.bernaferrari.changedetection.repo.PageIconColor.PINK.colors)
            assertEquals(Json.encodeToString(site.copy(colors = com.bernaferrari.changedetection.repo.PageIconColor.PINK.colors, iconUrl = "https://example.test/icon.png", iconShape = PageIconShape.CLOVER)), Json.encodeToString(requireNotNull(db.siteDao().getSiteById("page"))))
        } finally {
            db.close()
        }
        val reopened = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = path), driver)
        try {
            assertEquals(PageIconShape.CLOVER, reopened.siteDao().getSiteById("page")?.iconShape)
            assertEquals(com.bernaferrari.changedetection.repo.PageIconColor.PINK.colors, reopened.siteDao().getSiteById("page")?.colors)
            assertEquals("new", reopened.snapsDao().getLastSnapForSiteId("page")?.snapId)
        } finally {
            reopened.close()
        }
        driver.open(path).use { connection ->
            assertEquals("6", connection.queryAll("PRAGMA user_version").trim())
            val indexes = connection.queryAll("SELECT name FROM sqlite_master WHERE type = 'index' AND name LIKE 'index_%'")
            assertFalse(indexes, "index_sites_siteId" in indexes)
            assertTrue(indexes, "index_snaps_siteId_timestamp_snapId" in indexes)
        }
    }

    private suspend fun withDatabaseFile(block: suspend (path: String, driver: BundledSQLiteDriver) -> Unit) {
        val directory = Files.createTempDirectory("schema").toFile()
        try {
            block(directory.resolve("test.db").path, BundledSQLiteDriver())
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun SQLiteConnection.exec(sql: String) = prepare(sql).use { it.step() }

    private fun SQLiteConnection.queryAll(sql: String): String = prepare(sql).use { statement ->
        buildString {
            while (statement.step()) {
                for (i in 0 until statement.getColumnCount()) append(statement.getText(i)).append(' ')
                append('\n')
            }
        }
    }
}
