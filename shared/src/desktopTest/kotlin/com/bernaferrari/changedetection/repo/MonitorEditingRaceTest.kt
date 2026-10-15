package com.bernaferrari.changedetection.repo

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.viewModelScope
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.bernaferrari.changedetection.addedit.*
import com.bernaferrari.changedetection.background.MonitoringManager
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.repo.source.local.*
import com.bernaferrari.changedetection.storage.SnapshotFileCleanup
import com.bernaferrari.changedetection.util.DesktopFileStorage
import java.nio.file.Files
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class MonitorEditingRaceTest {
    @Test fun pauseAndNotificationChangesDoNotUndoEachOtherUsingStaleScreenState() = runBlocking {
        Fixture().use { fixture ->
            for (notificationsFirst in listOf(true, false)) {
                val original = fixture.site
                fixture.db.siteDao().insertSite(original)
                if (notificationsFirst) {
                    fixture.sites.setNotificationsEnabled(original.id, false)
                    fixture.sites.setMonitoringEnabled(original.id, false)
                } else {
                    fixture.sites.setMonitoringEnabled(original.id, false)
                    fixture.sites.setNotificationsEnabled(original.id, false)
                }
                val saved = requireNotNull(fixture.sites.getSiteById(original.id))
                assertFalse("A notification toggle must not resume a paused page", saved.isSyncEnabled)
                assertFalse("Pausing must not re-enable notifications", saved.isNotificationEnabled)
                assertEquals(original.title, saved.title)
                assertEquals(original.timestamp, saved.timestamp)
                assertEquals(original.isRead, saved.isRead)
            }
        }
    }

    @Test fun deletingAPageDuringSaveDoesNotResurrectIt() = editingRace(delete = true)

    @Test fun editingATitlePreservesMonitoringChangesMadeDuringSave() = editingRace(delete = false)

    @Test fun startingFreshCannotRecreateAPageDeletedDuringSave() = editingRace(delete = true, startFresh = true)

    @Test fun startingFreshPreservesConcurrentMonitoringChanges() = editingRace(delete = false, startFresh = true)

    private fun editingRace(delete: Boolean, startFresh: Boolean = false) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val release = CompletableDeferred<Unit>()
        Fixture().use { fixture ->
            fixture.db.siteDao().insertSite(fixture.site)
            val saving = CompletableDeferred<Unit>()
            val monitor = mock<MonitoringManager>()
            doSuspendableAnswer {
                saving.complete(Unit)
                release.await()
                Unit
            }.whenever(monitor).updateSchedule()
            val preferences = mock<DataStore<Preferences>>()
            whenever(preferences.data).thenReturn(flowOf(emptyPreferences()))
            val editor = AddEditViewModel(fixture.site.id, fixture.sites, CapturePreviewer { _, _ -> error("No preview required") }, monitor, preferences)
            try {
                editor.uiState.first { it.loadedSiteUrl != null }
                editor.setTitle("New title")
                if (startFresh) editor.setUrl("https://other.test")
                editor.saveSite()
                if (startFresh) editor.onUrlChangeStartFresh()
                saving.await()
                if (delete) {
                    fixture.db.siteDao().deleteSiteById(fixture.site.id)
                } else {
                    fixture.db.siteDao().updateMonitoringOptions(fixture.site.id, false, false)
                }
                release.complete(Unit)
                editor.uiState.first { !it.isSaving }
                val saved = fixture.sites.getSiteById(fixture.site.id)
                if (delete) {
                    assertNull("A stale editor must not recreate a deleted monitor", saved)
                    assertFalse(editor.uiState.value.saveComplete)
                    assertEquals(AddEditError.SiteNotFound, editor.uiState.value.saveError)
                } else {
                    assertEquals("New title", saved?.title)
                    assertFalse(requireNotNull(saved).isSyncEnabled)
                    assertFalse(saved.isNotificationEnabled)
                }
            } finally {
                release.complete(Unit)
                editor.viewModelScope.coroutineContext.job.cancelAndJoin()
                Dispatchers.resetMain()
            }
        }
    }

    private class Fixture : AutoCloseable {
        val directory = Files.createTempDirectory("monitor-edit-races").toFile()
        val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = directory.resolve("test.db").path), BundledSQLiteDriver())
        val files = DesktopFileStorage(directory.resolve("files").also { it.mkdirs() })
        val cleanup = SnapshotFileCleanup(db.pendingFileDeletionDao(), db.snapsDao(), files)
        val sites = SitesRepository(SitesLocalDataSource(AppExecutors(), db.siteDao(), cleanup))
        val site = Site("Page", "https://example.test", 42L, "", "page", 0 to 0).copy(captureMode = CaptureMode.SOURCE, isSyncEnabled = true, isNotificationEnabled = true, isRead = false)
        override fun close() {
            db.close()
            directory.deleteRecursively()
        }
    }
}
