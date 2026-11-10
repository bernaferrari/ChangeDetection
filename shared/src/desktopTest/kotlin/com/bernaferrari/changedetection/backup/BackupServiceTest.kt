package com.bernaferrari.changedetection.backup

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.bernaferrari.changedetection.error.ErrorHandler
import com.bernaferrari.changedetection.repo.CaptureMode
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.local.ChangeDatabase
import com.bernaferrari.changedetection.repo.source.local.getRoomDatabase
import com.bernaferrari.changedetection.storage.SnapshotFileCleanup
import com.bernaferrari.changedetection.sync.SyncEngine
import com.bernaferrari.changedetection.util.DesktopFileStorage
import io.ktor.client.HttpClient
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test

class BackupServiceTest {
    /** A backup held in memory, so the engine is tested without zip files. */
    private class MemoryArchive(val entries: MutableList<Pair<String, ByteArray>> = mutableListOf()) : BackupSink {
        override fun write(name: String, bytes: ByteArray) {
            entries += name to bytes
        }

        fun source() = object : BackupSource {
            private var index = -1
            override fun nextName(): String? = entries.getOrNull(++index)?.first
            override fun readCurrent(maxBytes: Int): ByteArray? = entries[index].second.takeIf { it.size <= maxBytes }
        }

        fun manifest(): BackupManifest = backupJson.decodeFromString(entries.first().second.decodeToString())
    }

    private class Device(name: String) : AutoCloseable {
        private val directory = Files.createTempDirectory("backup-$name").toFile()
        val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = directory.resolve("test.db").path), BundledSQLiteDriver())
        val files = DesktopFileStorage(directory.resolve("files").also { it.mkdirs() })
        private val client = HttpClient()
        val engine = SyncEngine(client, db, db.siteDao(), db.snapsDao(), files, ErrorHandler())
        var settings = mapOf("sync_interval" to "120", "theme_mode" to "DARK")
        val service = BackupService(
            db,
            db.siteDao(),
            db.snapsDao(),
            files,
            SnapshotFileCleanup(db.pendingFileDeletionDao(), db.snapsDao(), files),
            readSettings = { settings },
            writeSettings = { settings = it },
            now = { 1_794_268_800_000 },
        )

        suspend fun watch(id: String, url: String, vararg versions: String): Site {
            val site = Site("Page $id", url, 0L, "", id, 0 to 0).copy(captureMode = CaptureMode.SOURCE)
            db.siteDao().insertSite(site)
            versions.forEach { text -> assertTrue(engine.processFetchResult(requireNotNull(db.siteDao().getSiteById(id)), SyncEngine.FetchResult.Success("text/plain", text.encodeToByteArray())) is com.bernaferrari.changedetection.sync.SyncResult.Success) }
            return requireNotNull(db.siteDao().getSiteById(id))
        }

        suspend fun versions(id: String): List<Snap> = db.snapsDao().getAllSnapsForSiteId(id)

        override fun close() {
            client.close()
            db.close()
            directory.deleteRecursively()
        }
    }

    @Test fun aPagesBackupHoldsPagesAndSettingsButNoVersions() = runBlocking {
        Device("a").use { phone ->
            phone.watch("one", "https://example.test/one", "First", "Second")

            val manifest: BackupManifest = backupJson.decodeFromString(phone.service.exportPages("3.2").decodeToString())

            assertEquals(BACKUP_FORMAT, manifest.format)
            assertEquals(1, manifest.version)
            assertEquals("3.2", manifest.appVersion)
            assertEquals(1_794_268_800_000, manifest.exportedAt)
            assertEquals(listOf("https://example.test/one"), manifest.sites.map { it.url })
            assertTrue(manifest.snaps.isEmpty())
            assertEquals(phone.settings, manifest.settings)
        }
    }

    @Test fun aFullBackupStartsWithTheManifestAndCarriesEveryVersionUnchanged() = runBlocking {
        Device("a").use { phone ->
            phone.watch("one", "https://example.test/one", "First", "Second")
            val archive = MemoryArchive()

            val summary = phone.service.exportEverything(archive, "3.2")

            assertEquals(BackupSummary(pages = 1, versions = 2), summary)
            assertEquals(MANIFEST_ENTRY, archive.entries.first().first)
            val stored = phone.versions("one")
            assertEquals(stored.map { it.snapId }.toSet(), archive.manifest().snaps.map { it.snapId }.toSet())
            stored.forEach { snap ->
                val entry = archive.entries.single { it.first == SNAPSHOT_PREFIX + snap.snapId }
                assertArrayEquals(phone.files.readBytes(snap.snapId), entry.second)
            }
        }
    }

    @Test fun aVersionWhoseFileIsMissingIsLeftOutOfAFullBackupWithoutFailingIt() = runBlocking {
        Device("a").use { phone ->
            phone.watch("one", "https://example.test/one", "First", "Second")
            val lost = phone.versions("one").first()
            phone.files.deleteFile(lost.snapId)
            val archive = MemoryArchive()

            val summary = phone.service.exportEverything(archive, "3.2")

            assertEquals(1, summary.versions)
            assertFalse(archive.entries.any { it.first == SNAPSHOT_PREFIX + lost.snapId })
        }
    }

    @Test fun aFullBackupRestoresPagesVersionsAndTheirContentOnAnotherDevice() = runBlocking {
        Device("old").use { old ->
            Device("new").use { new ->
                val page = old.watch("one", "https://example.test/one", "First", "Second")
                val archive = MemoryArchive().also { old.service.exportEverything(it, "3.2") }

                val summary = new.service.import(archive.source(), restoreSettings = false)

                assertEquals(ImportSummary(pagesAdded = 1, pagesAlreadyHere = 0, pagesRejected = 0, versionsAdded = 2, settingsRestored = false), summary)
                val restored = requireNotNull(new.db.siteDao().getSiteById("one"))
                assertEquals(page.url, restored.url)
                assertEquals(page.title, restored.title)
                assertEquals(page.captureMode, restored.captureMode)
                assertEquals(old.versions("one").toSet(), new.versions("one").toSet())
                old.versions("one").forEach { assertArrayEquals(old.files.readBytes(it.snapId), new.files.readBytes(it.snapId)) }
            }
        }
    }

    @Test fun aPagesBackupAddsPagesThatStartFresh() = runBlocking {
        Device("old").use { old ->
            Device("new").use { new ->
                old.watch("one", "https://example.test/one", "First")
                // A page that was failing when it was exported must not arrive marked as failing.
                old.db.siteDao().insertSite(requireNotNull(old.db.siteDao().getSiteById("one")).copy(isSuccessful = false, lastFailureCode = "SERVER", failureCount = 4, failingSince = 1L, failureAlerted = true))

                val summary = new.service.import(ManifestOnlySource(old.service.exportPages("3.2")), restoreSettings = false)

                assertEquals(1, summary.pagesAdded)
                assertEquals(0, summary.versionsAdded)
                val restored = requireNotNull(new.db.siteDao().getSiteById("one"))
                assertTrue(restored.isSuccessful)
                assertNull(restored.lastFailureCode)
                assertEquals(0L, restored.timestamp)
                assertNull(restored.lastSuccessfulCheck)
                assertEquals(0, restored.failureCount)
                assertNull(restored.failingSince)
                assertFalse(restored.failureAlerted)
                assertTrue(new.versions("one").isEmpty())
            }
        }
    }

    @Test fun importingTheSameBackupTwiceChangesNothing() = runBlocking {
        Device("old").use { old ->
            Device("new").use { new ->
                old.watch("one", "https://example.test/one", "First", "Second")
                val archive = MemoryArchive().also { old.service.exportEverything(it, "3.2") }
                new.service.import(archive.source(), restoreSettings = false)

                val again = new.service.import(archive.source(), restoreSettings = false)

                assertEquals(ImportSummary(pagesAdded = 0, pagesAlreadyHere = 1, pagesRejected = 0, versionsAdded = 0, settingsRestored = false), again)
                assertEquals(2, new.versions("one").size)
            }
        }
    }

    @Test fun aPageAlreadyOnTheDeviceKeepsItsOwnOptionsAndHistory() = runBlocking {
        Device("old").use { old ->
            old.watch("one", "https://example.test/one", "First")
            val archive = MemoryArchive().also { old.service.exportEverything(it, "3.2") }
            old.watch("one", "https://example.test/one", "Second", "Third")
            val before = old.versions("one")

            val summary = old.service.import(archive.source(), restoreSettings = false)

            assertEquals(1, summary.pagesAlreadyHere)
            assertEquals(before, old.versions("one"))
        }
    }

    @Test fun aRecentlyDeletedPageCanBeRestoredWithItsVersions() = runBlocking {
        Device("phone").use { phone ->
            val site = phone.watch("one", "https://example.test/one", "First")
            val archive = MemoryArchive()
            phone.service.exportEverything(archive, "3.2")
            val saved = phone.versions(site.id).single()
            phone.db.siteDao().deleteSiteById(site.id)
            assertTrue(phone.files.fileExists(saved.snapId))

            val restored = phone.service.import(archive.source(), restoreSettings = false)

            assertEquals(1, restored.pagesAdded)
            assertEquals(1, restored.versionsAdded)
            assertEquals(saved.snapId, phone.versions(site.id).single().snapId)
        }
    }

    @Test fun settingsAreRestoredOnlyWhenAsked() = runBlocking {
        Device("old").use { old ->
            Device("new").use { new ->
                new.settings = mapOf("theme_mode" to "LIGHT")
                val file = old.service.exportPages("3.2")

                assertFalse(new.service.import(ManifestOnlySource(file), restoreSettings = false).settingsRestored)
                assertEquals(mapOf("theme_mode" to "LIGHT"), new.settings)

                assertTrue(new.service.import(ManifestOnlySource(file), restoreSettings = true).settingsRestored)
                assertEquals(old.settings, new.settings)
            }
        }
    }

    private fun page(id: String, url: String) = Site("Page", url, 0L, "", id, 0 to 0)

    private fun file(manifest: BackupManifest, vararg entries: Pair<String, ByteArray>) = MemoryArchive(mutableListOf(MANIFEST_ENTRY to backupJson.encodeToString(manifest).encodeToByteArray(), *entries))

    @Test fun onlyWebAddressesAreImported() = runBlocking {
        Device("new").use { new ->
            val hostile = file(BackupManifest(sites = listOf(page("a", "file:///data/data/private"), page("b", "javascript:alert(1)"), page("c", "content://settings/secure"), page("d", "https://example.test/ok"))))

            val summary = new.service.import(hostile.source(), restoreSettings = false)

            assertEquals(1, summary.pagesAdded)
            assertEquals(3, summary.pagesRejected)
            assertEquals(listOf("d"), new.db.siteDao().getSitesList().map { it.id })
        }
    }

    @Test fun aVersionNameThatIsNotAnIdRefusesTheFileAndLeavesNothingBehind() = runBlocking {
        Device("new").use { new ->
            val body = "x".encodeToByteArray()
            val hostile = file(
                BackupManifest(sites = listOf(page("a", "https://example.test/a")), snaps = listOf(Snap("../escape", "a", 1L, "text/plain", "UTF-8", body.size))),
                SNAPSHOT_PREFIX + "../escape" to body,
            )

            try {
                new.service.import(hostile.source(), restoreSettings = false)
                fail("a file with an undeclared or unsafe entry must be refused")
            } catch (expected: InvalidBackupException) {
            }
            assertTrue("the page added before the bad entry must be removed again", new.db.siteDao().getSitesList().isEmpty())
            assertFalse(new.files.fileExists("escape"))
        }
    }

    @Test fun anEntryLargerThanItDeclaresRefusesTheFile() = runBlocking {
        Device("new").use { new ->
            val id = "11111111-2222-3333-4444-555555555555"
            val hostile = file(
                BackupManifest(sites = listOf(page("a", "https://example.test/a")), snaps = listOf(Snap(id, "a", 1L, "text/plain", "UTF-8", 4))),
                SNAPSHOT_PREFIX + id to ByteArray(4096),
            )

            try {
                new.service.import(hostile.source(), restoreSettings = false)
                fail("an entry that inflates past its declared size must be refused")
            } catch (expected: InvalidBackupException) {
            }
            assertTrue(new.db.siteDao().getSitesList().isEmpty())
            assertFalse(new.files.fileExists(id))
        }
    }

    @Test fun aRejectedArchiveRollsBackContentAlreadyWritten() = runBlocking {
        Device("new").use { new ->
            val id = "11111111-2222-3333-4444-555555555555"
            val archive = file(
                BackupManifest(sites = listOf(page("a", "https://example.test/a")), snaps = listOf(Snap(id, "a", 1L, "text/plain", "UTF-8", 1))),
                SNAPSHOT_PREFIX + id to byteArrayOf(65),
                "unexpected" to byteArrayOf(),
            )
            try {
                new.service.import(archive.source(), restoreSettings = false)
                fail("The archive should be refused")
            } catch (_: InvalidBackupException) {
            }
            assertTrue(new.db.siteDao().getSitesList().isEmpty())
            assertNull(new.db.snapsDao().getSnapById(id))
            assertFalse(new.files.fileExists(id))
        }
    }

    @Test fun aDamagedVersionIsSkippedAndTheRestIsKept() = runBlocking {
        Device("old").use { old ->
            Device("new").use { new ->
                old.watch("one", "https://example.test/one", "First", "Second")
                val archive = MemoryArchive().also { old.service.exportEverything(it, "3.2") }
                val damaged = archive.entries.indexOfFirst { it.first.startsWith(SNAPSHOT_PREFIX) }
                val (name, bytes) = archive.entries[damaged]
                archive.entries[damaged] = name to ByteArray(bytes.size) { '?'.code.toByte() }

                val summary = new.service.import(archive.source(), restoreSettings = false)

                assertEquals(1, summary.pagesAdded)
                assertEquals(1, summary.versionsAdded)
                assertFalse(new.files.fileExists(name.removePrefix(SNAPSHOT_PREFIX)))
            }
        }
    }

    @Test fun aFileFromANewerAppOrOfAnotherKindIsRefusedUntouched() = runBlocking {
        Device("new").use { new ->
            val pages = listOf(page("a", "https://example.test/a"))
            for ((hostile, expected) in listOf(
                file(BackupManifest(version = BACKUP_VERSION + 1, sites = pages)) to BackupTooNewException::class,
                file(BackupManifest(format = "something-else", sites = pages)) to InvalidBackupException::class,
                MemoryArchive(mutableListOf(MANIFEST_ENTRY to "not json".encodeToByteArray())) to InvalidBackupException::class,
                MemoryArchive(mutableListOf("other.txt" to "hello".encodeToByteArray())) to InvalidBackupException::class,
                MemoryArchive() to InvalidBackupException::class,
            )) {
                try {
                    new.service.import(hostile.source(), restoreSettings = true)
                    fail("must be refused")
                } catch (error: Exception) {
                    assertEquals(expected, error::class)
                }
            }
            assertTrue(new.db.siteDao().getSitesList().isEmpty())
            assertEquals("settings must not change when a file is refused", mapOf("sync_interval" to "120", "theme_mode" to "DARK"), new.settings)
        }
    }

    @Test fun aVersion1FileWrittenByHandStillReads() = runBlocking {
        Device("new").use { new ->
            val handwritten = """
                {"format":"change-detection-backup","version":1,"exportedAt":1,"appVersion":"3.2","settings":{},
                 "sites":[{"title":"Docs","url":"https://example.test/docs","timestamp":0,"id":"hand","isSuccessful":true,"isBrowser":false,
                           "threshold":0,"isRead":true,"isSyncEnabled":true,"isNotificationEnabled":true,"notes":"",
                           "colors":{"first":1,"second":2},"constraints":{"wifi":false,"charging":false,"batteryNotLow":false,"deviceIdle":false},
                           "aFieldFromTheFuture":123}],
                 "snaps":[]}
            """.trimIndent()

            val summary = new.service.import(ManifestOnlySource(handwritten.encodeToByteArray()), restoreSettings = false)

            assertEquals(1, summary.pagesAdded)
            assertEquals("https://example.test/docs", requireNotNull(new.db.siteDao().getSiteById("hand")).url)
        }
    }
}
