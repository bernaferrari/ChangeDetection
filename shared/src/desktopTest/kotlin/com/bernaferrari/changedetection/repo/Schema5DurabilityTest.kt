package com.bernaferrari.changedetection.repo

import androidx.room3.Room
import androidx.room3.withWriteTransaction
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.bernaferrari.changedetection.repo.source.local.*
import com.bernaferrari.changedetection.sync.*
import com.bernaferrari.changedetection.util.ContentHasher
import com.bernaferrari.changedetection.util.DesktopFileStorage
import com.bernaferrari.changedetection.error.ErrorHandler
import io.ktor.client.HttpClient
import java.nio.file.Files
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class Schema5DurabilityTest {
    @Test fun versionFourUpgradeKeepsEveryHistoryRowAndAddsEmptyDeliveryQueue() = runBlocking {
        val dir = Files.createTempDirectory("schema-five").toFile();val path = dir.resolve("test.db").path
        val driver = BundledSQLiteDriver()
        val bytes = "Original saved content".encodeToByteArray()
        dir.resolve("saved").writeBytes(bytes)
        try {
            val schema = sequenceOf(File("schemas/com.bernaferrari.changedetection.repo.source.local.ChangeDatabase/4.json"), File("shared/schemas/com.bernaferrari.changedetection.repo.source.local.ChangeDatabase/4.json")).first { it.exists() }
            val database = Json.parseToJsonElement(schema.readText()).jsonObject.getValue("database").jsonObject
            driver.open(path).use { connection ->
                for (entity in database.getValue("entities").jsonArray) {
                    val table = entity.jsonObject;val name = table.getValue("tableName").jsonPrimitive.content
                    connection.exec(table.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", name))
                    for (index in table["indices"]?.jsonArray.orEmpty()) connection.exec(index.jsonObject.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", name))
                }
                for (query in database.getValue("setupQueries").jsonArray) connection.exec(query.jsonPrimitive.content)
                connection.exec("INSERT INTO sites (title,url,timestamp,siteId,isSuccessful,isBrowser,threshold,isRead,isSyncEnabled,isNotificationEnabled,notes,colors,constraints) VALUES ('Page','https://example.test',123,'page',1,0,0,0,1,1,'notes','0,0','false,false,false,false')")
                connection.exec("INSERT INTO snaps (snapId,siteId,timestamp,contentType,contentCharset,contentSize,contentHash,processedContentHash) VALUES ('saved','page',123,'text/plain','UTF-8',${bytes.size},'${ContentHasher.hashContent(bytes)}','')")
                connection.exec("PRAGMA user_version = 4")
            }
            val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = path), driver)
            try {
                val snap = requireNotNull(db.snapsDao().getSnapById("saved"));val site = requireNotNull(db.siteDao().getSiteById("page"))
                assertEquals(123L, snap.timestamp);assertEquals(ContentHasher.hashContent(bytes), snap.contentHash);assertEquals("", snap.payloadHash)
                assertFalse(site.isRead);assertEquals("notes", site.notes);assertTrue(db.pendingChangeAlertDao().firstBatch(50).isEmpty())
                assertArrayEquals(bytes, dir.resolve("saved").readBytes())
                db.pendingChangeAlertDao().enqueue(PendingChangeAlert(snap.snapId, site.id, snap.timestamp, ChangeAlertOutbox.identity(site)))
            } finally { db.close() }
            val reopened = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = path), driver)
            try {
                assertEquals("saved", reopened.pendingChangeAlertDao().firstBatch(50).single().snapId)
                reopened.snapsDao().deleteSnapById("saved")
                assertTrue(reopened.pendingChangeAlertDao().firstBatch(50).isEmpty())
            } finally { reopened.close() }
        } finally { dir.deleteRecursively() }
    }

    @Test fun queueAndHistoryRollBackTogether() = runBlocking {
        val dir = Files.createTempDirectory("outbox-rollback").toFile()
        val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = dir.resolve("test.db").path), BundledSQLiteDriver())
        try {
            val site = Site("Page", "https://example.test", 0L, "", "page", 0 to 0);db.siteDao().insertSite(site)
            val failed = runCatching {
                db.withWriteTransaction {
                    db.snapsDao().insertSnap(Snap("changed", site.id, 1L, "text/plain", "UTF-8", 5))
                    db.pendingChangeAlertDao().enqueue(PendingChangeAlert("changed", site.id, 1L, ChangeAlertOutbox.identity(site)))
                    error("Abort the same write transaction")
                }
            }
            assertTrue(failed.isFailure);assertNull(db.snapsDao().getSnapById("changed"));assertTrue(db.pendingChangeAlertDao().firstBatch(50).isEmpty())
        } finally { db.close();dir.deleteRecursively() }
    }

    @Test fun committedChangeRetriesAfterDatabaseReopenWithoutCreatingAnotherVersion() = runBlocking {
        val dir = Files.createTempDirectory("outbox-restart").toFile();val path = dir.resolve("test.db").path
        val files = DesktopFileStorage(dir.resolve("files").also { it.mkdirs() });val client = HttpClient()
        val site = Site("Page", "https://example.test", 0L, "", "page", 0 to 0).copy(captureMode = CaptureMode.SOURCE, isNotificationEnabled = true)
        val original = SyncEngine.FetchResult.Success("text/plain", "Original".encodeToByteArray());val changed = SyncEngine.FetchResult.Success("text/plain", "Changed".encodeToByteArray())
        var db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = path), BundledSQLiteDriver())
        try {
            db.siteDao().insertSite(site)
            val failing = ChangeAlertOutbox(db.pendingChangeAlertDao(), db.siteDao(), db.snapsDao(), ChangeNotifier { _, _ -> error("Temporary delivery failure") })
            val engine = SyncEngine(client, db, db.siteDao(), db.snapsDao(), files, ErrorHandler(), alertOutbox = failing)
            assertTrue(engine.establishFirstVersion(site, original) is SyncResult.Success)
            val baseline = requireNotNull(db.snapsDao().getLastSnapForSiteId(site.id))
            val saved = engine.processFetchResult(site, changed) as SyncResult.Success;assertTrue(saved.hadChanges)
            assertEquals(saved.snapId, db.pendingChangeAlertDao().firstBatch(50).single().snapId)
            assertArrayEquals(original.data, files.readBytes(baseline.snapId))
            db.close();db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = path), BundledSQLiteDriver())
            val delivered = mutableListOf<String>()
            val resumed = ChangeAlertOutbox(db.pendingChangeAlertDao(), db.siteDao(), db.snapsDao(), ChangeNotifier { _, id -> delivered += id })
            resumed.drain();resumed.drain()
            assertEquals(listOf(saved.snapId), delivered);assertTrue(db.pendingChangeAlertDao().firstBatch(50).isEmpty())
            val engine2 = SyncEngine(client, db, db.siteDao(), db.snapsDao(), files, ErrorHandler(), alertOutbox = resumed)
            val same = engine2.processFetchResult(site, changed) as SyncResult.Success
            assertFalse(same.hadChanges);assertEquals(2, db.snapsDao().getAllSnapsForSiteId(site.id).size)
            assertArrayEquals(original.data, files.readBytes(baseline.snapId));assertEquals(listOf(saved.snapId), delivered)
        } finally { client.close();db.close();dir.deleteRecursively() }
    }

    private fun SQLiteConnection.exec(sql: String) = prepare(sql).use { it.step() }
}
