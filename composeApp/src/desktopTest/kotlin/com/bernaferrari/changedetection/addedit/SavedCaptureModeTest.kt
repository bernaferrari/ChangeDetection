/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.addedit

import androidx.lifecycle.viewModelScope
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.bernaferrari.changedetection.background.*
import com.bernaferrari.changedetection.error.ErrorHandler
import com.bernaferrari.changedetection.repo.*
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.repo.source.local.*
import com.bernaferrari.changedetection.sync.*
import com.bernaferrari.changedetection.util.DesktopFileStorage
import com.sun.net.httpserver.HttpServer
import io.ktor.client.HttpClient
import java.net.InetSocketAddress
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SavedCaptureModeTest {
    @Test(timeout = 20_000)
    fun editingPreservesSavedModesAndUnsupportedChecksKeepTheImageHistory() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val directory = Files.createTempDirectory("saved-capture-mode").toFile()
        val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = directory.resolve("test.db").path), BundledSQLiteDriver())
        val files = DesktopFileStorage(directory.resolve("snapshots").also { it.mkdirs() })
        val repository = SitesRepository(SitesLocalDataSource(AppExecutors(), db.siteDao(), com.bernaferrari.changedetection.storage.SnapshotFileCleanup(db.pendingFileDeletionDao(), db.snapsDao(), files)))
        val requests = AtomicInteger()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            requests.incrementAndGet()
            val body = "<p>Unwanted HTML fallback</p>".encodeToByteArray()
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()
        val client = HttpClient()
        val manager = object : MonitoringManager {
            override val quickMonitorState = MutableStateFlow(TurboState())
            override fun getSyncStatusFlow() = flowOf(emptyList<SyncJobInfo>())
            override fun reloadSite(site: Site) = Unit
            override fun startQuickMonitor(siteIds: List<String>, intervalSeconds: Int, durationMinutes: Int) = Unit
            override fun stopQuickMonitor() = Unit
            override suspend fun updateSchedule() = Unit
            override fun cancelWork() = Unit
        }
        val editors = mutableListOf<AddEditViewModel>()
        try {
            assertFalse(supportsVisualMonitoring)
            for (mode in listOf(CaptureMode.SCREENSHOT, CaptureMode.DEFAULT)) {
                val id = mode.name
                val site = Site("Original", "http://127.0.0.1:${server.address.port}/$id", 123L, "", id, 0 to 0).copy(captureMode = mode, lastSuccessfulCheck = 123L)
                repository.saveSite(site)
                files.writeBytes(id, byteArrayOf(1, 2, 3))
                val baseline = Snap(id, id, 123L, "image/png", "", 3)
                db.snapsDao().insertSnap(baseline)
                val editor = AddEditViewModel(id, repository, CapturePreviewer { _, _ -> error("Rename must not preview") }, manager).also(editors::add)
                withTimeout(5_000) { editor.uiState.first { !it.isLoading } }
                assertEquals(mode, editor.uiState.value.captureMode)
                editor.setVisualChangeThreshold(100)
                assertEquals(0, repository.getSiteById(id)?.visualChangeThresholdBps)
                val cancelled = AddEditViewModel(id, repository, CapturePreviewer { _, _ -> error("No preview") }, manager).also(editors::add)
                withTimeout(5_000) { cancelled.uiState.first { !it.isLoading } }
                assertEquals(0, cancelled.uiState.value.visualChangeThresholdBps)
                editor.setTitle("Renamed")
                editor.saveSite()
                withTimeout(5_000) { editor.uiState.first { it.saveComplete || it.saveError != null } }
                assertTrue(editor.uiState.value.saveComplete)
                assertEquals(mode, repository.getSiteById(id)?.captureMode)
                assertEquals("Renamed", repository.getSiteById(id)?.title)
                assertEquals(100, repository.getSiteById(id)?.visualChangeThresholdBps)
                val reopened = AddEditViewModel(id, repository, CapturePreviewer { _, _ -> error("No preview") }, manager).also(editors::add)
                withTimeout(5_000) { reopened.uiState.first { !it.isLoading } }
                assertEquals(100, reopened.uiState.value.visualChangeThresholdBps)
                assertEquals(baseline, db.snapsDao().getLastSnapForSiteId(id))
                if (mode == CaptureMode.SCREENSHOT) {
                    val engine = SyncEngine(client, db, db.siteDao(), db.snapsDao(), files, ErrorHandler(), ChangeNotifier { _, _ -> error("Unavailable capture must not notify") })
                    val result = engine.syncSite(id)
                    assertTrue(result is SyncResult.Failed)
                    assertEquals("CAPTURE_UNAVAILABLE", repository.getSiteById(id)?.lastFailureCode)
                    assertEquals(123L, repository.getSiteById(id)?.lastSuccessfulCheck)
                    assertEquals(0, requests.get())
                    assertEquals(baseline, db.snapsDao().getLastSnapForSiteId(id))
                    assertArrayEquals(byteArrayOf(1, 2, 3), files.readBytes(id))
                }
                editor.setCaptureMode(CaptureMode.SOURCE)
                assertEquals(mode, repository.getSiteById(id)?.captureMode)
                editor.consumeSaveComplete()
                editor.saveSite()
                withTimeout(5_000) { editor.uiState.first { it.saveComplete || it.saveError != null } }
                assertEquals(CaptureMode.SOURCE, repository.getSiteById(id)?.captureMode)
            }
        } finally {
            editors.forEach { it.viewModelScope.cancel() }
            client.close()
            server.stop(0)
            db.close()
            directory.deleteRecursively()
            Dispatchers.resetMain()
        }
    }
}
