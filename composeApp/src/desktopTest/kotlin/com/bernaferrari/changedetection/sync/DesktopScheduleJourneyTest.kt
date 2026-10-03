/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.bernaferrari.changedetection.background.*
import com.bernaferrari.changedetection.error.ErrorHandler
import com.bernaferrari.changedetection.repo.*
import com.bernaferrari.changedetection.repo.source.local.*
import com.bernaferrari.changedetection.util.DesktopFileStorage
import com.sun.net.httpserver.HttpServer
import io.ktor.client.HttpClient
import java.net.InetSocketAddress
import java.nio.file.Files
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DesktopScheduleJourneyTest {
    @Test(timeout = 30_000)
    fun realHttpCapturesFollowMinimumAndMaximumSchedulesAndKeepComparisonFiles() = runBlocking {
        val scheduler = TestCoroutineScheduler()
        Dispatchers.setMain(UnconfinedTestDispatcher(scheduler))
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val directory = Files.createTempDirectory("desktop-schedule-journey").toFile()
        val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = directory.resolve("test.db").path), BundledSQLiteDriver())
        val files = DesktopFileStorage(directory.resolve("snapshots").apply { mkdirs() })
        val content = AtomicReference("<html><body><p>Opening 10:00</p></body></html>")
        val requests = AtomicInteger()
        val responseStatus = AtomicInteger(200)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/schedule") { exchange ->
            requests.incrementAndGet()
            val bytes = content.get().encodeToByteArray()
            exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
            exchange.sendResponseHeaders(responseStatus.get(), bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        val persisted = MutableStateFlow<Preferences>(mutablePreferencesOf(MonitoringSchedule.INTERVAL to 15L))
        val preferences = object : DataStore<Preferences> {
            override val data = persisted
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = transform(persisted.value).also { persisted.value = it }
        }
        val notices = CopyOnWriteArrayList<String>()
        val client = HttpClient()
        val engine = SyncEngine(client, db, db.siteDao(), db.snapsDao(), files, ErrorHandler(), ChangeNotifier { _, id -> notices.add(id) })
        val manager = DesktopMonitoringManager(engine, preferences, appScope)
        suspend fun awaitCondition(condition: suspend () -> Boolean) = withTimeout(10_000) {
            while (true) {
                scheduler.runCurrent()
                if (condition()) break
                delay(10)
            }
        }
        suspend fun awaitScheduled() = awaitCondition { manager.getSyncStatusFlow().first().any { it.isPeriodic && it.state == SyncState.ENQUEUED } }
        try {
            val page = Site("Schedule", "http://127.0.0.1:${server.address.port}/schedule", 0L, "", "page", 0 to 0).copy(captureMode = CaptureMode.TEXT)
            db.siteDao().insertSite(page)
            val baseline = engine.syncSite(page.id) as SyncResult.Success
            assertFalse(baseline.hadChanges)
            assertTrue(notices.isEmpty())
            manager.updateSchedule()
            awaitScheduled()
            content.set("<html><body><p>Opening 10:30</p></body></html>")
            scheduler.advanceTimeBy(15 * 60_000L - 1)
            scheduler.runCurrent()
            assertEquals(1, requests.get())
            scheduler.advanceTimeBy(1)
            awaitCondition { notices.size == 1 }
            awaitScheduled()
            val source = SnapsLocalDataSource(AppExecutors(), db.snapsDao(), db.siteDao(), files, com.bernaferrari.changedetection.storage.SnapshotFileCleanup(db.pendingFileDeletionDao(), db.snapsDao(), files))
            val pair = requireNotNull(source.getSnapPair(requireNotNull(baseline.snapId), notices.single()))
            assertTrue(pair.first.second.decodeToString().contains("10:00"))
            assertTrue(pair.second.second.decodeToString().contains("10:30"))
            assertFalse(requireNotNull(db.siteDao().getSiteById(page.id)).isRead)

            val lastSuccessfulCheck = requireNotNull(db.siteDao().getSiteById(page.id)).lastSuccessfulCheck
            val savedIds = db.snapsDao().getAllSnapsForSiteId(page.id).map { it.snapId }.toSet()
            responseStatus.set(503)
            content.set("<html><body>Temporarily unavailable</body></html>")
            scheduler.advanceTimeBy(15 * 60_000L)
            awaitCondition { db.siteDao().getSiteById(page.id)?.isSuccessful == false }
            awaitScheduled()
            val failed = requireNotNull(db.siteDao().getSiteById(page.id))
            assertEquals("SERVER", failed.lastFailureCode)
            assertEquals(lastSuccessfulCheck, failed.lastSuccessfulCheck)
            assertFalse(failed.isRead)
            assertEquals(savedIds, db.snapsDao().getAllSnapsForSiteId(page.id).map { it.snapId }.toSet())
            assertEquals(savedIds, directory.resolve("snapshots").listFiles()!!.map { it.name }.toSet())
            assertArrayEquals(pair.first.second, files.readBytes(pair.first.first.snapId))
            assertArrayEquals(pair.second.second, files.readBytes(pair.second.first.snapId))
            assertEquals(1, notices.size)

            persisted.value = persisted.value.toMutablePreferences().apply { this[MonitoringSchedule.INTERVAL] = 10080L }
            manager.updateSchedule()
            awaitScheduled()
            content.set("<html><body><p>Opening 11:00</p></body></html>")
            responseStatus.set(200)
            val before = requests.get()
            scheduler.advanceTimeBy(7 * 24 * 60 * 60_000L - 1)
            scheduler.runCurrent()
            assertEquals(before, requests.get())
            scheduler.advanceTimeBy(1)
            awaitCondition { notices.size == 2 }
            awaitScheduled()
            assertEquals(3, db.snapsDao().getAllSnapsForSiteId(page.id).size)
            val recovered = requireNotNull(db.siteDao().getSiteById(page.id))
            assertTrue(recovered.isSuccessful)
            assertNull(recovered.lastFailureCode)
            assertFalse(recovered.isRead)
            val recoveryPair = requireNotNull(source.getSnapPair(notices.first(), notices.last()))
            assertTrue(recoveryPair.first.second.decodeToString().contains("10:30"))
            assertTrue(recoveryPair.second.second.decodeToString().contains("11:00"))
            // A later unchanged successful check must not duplicate the recovery notice/version.
            scheduler.advanceTimeBy(7 * 24 * 60 * 60_000L)
            awaitCondition { requests.get() == before + 2 }
            awaitScheduled()
            assertEquals(2, notices.size)
            assertEquals(3, db.snapsDao().getAllSnapsForSiteId(page.id).size)
            persisted.value = persisted.value.toMutablePreferences().apply { this[MonitoringSchedule.ENABLED] = false }
            manager.updateSchedule()
            assertFalse(manager.getSyncStatusFlow().first().any { it.isPeriodic })
            val stoppedCount = requests.get()
            scheduler.advanceTimeBy(7 * 24 * 60 * 60_000L)
            scheduler.runCurrent()
            assertEquals(stoppedCount, requests.get())
        } finally {
            manager.cancelWork()
            appScope.cancel()
            scheduler.runCurrent()
            client.close()
            server.stop(0)
            db.close()
            directory.deleteRecursively()
            Dispatchers.resetMain()
        }
    }
}
