/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

import androidx.lifecycle.viewModelScope
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.bernaferrari.changedetection.repo.*
import com.bernaferrari.changedetection.repo.source.*
import com.bernaferrari.changedetection.repo.source.local.*
import com.bernaferrari.changedetection.util.*
import com.bernaferrari.changedetection.error.ErrorHandler
import java.nio.file.Files
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.mock

class MissingSnapshotRecoveryTest {
    @Test fun missingComparisonFileCanRecoverWithoutAcknowledgingOrReplacingHistory() = verify(false)

    @Test fun missingBaselineIsAnErrorAndRetryRestoresItsActualContent() = verify(true)

    private fun verify(single: Boolean) = runBlocking {
        val directory = Files.createTempDirectory("missing-snapshot-recovery").toFile()
        val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = directory.resolve("test.db").path), BundledSQLiteDriver())
        val storage = DesktopFileStorage(directory.resolve("snapshots").apply { mkdirs() })
        val executors = AppExecutors()
        val sites = SitesRepository(SitesLocalDataSource(executors, db.siteDao(), com.bernaferrari.changedetection.storage.SnapshotFileCleanup(db.pendingFileDeletionDao(), db.snapsDao(), storage)))
        val snapshots = SnapsRepository(SnapsLocalDataSource(executors, db.snapsDao(), db.siteDao(), storage, com.bernaferrari.changedetection.storage.SnapshotFileCleanup(db.pendingFileDeletionDao(), db.snapsDao(), storage)))
        val application = ApplicationScope()
        val model = ComparisonViewModel(sites, snapshots, mock(), mock(), ErrorHandler(), application)
        try {
            db.siteDao().insertSite(Site("Schedule", "https://example.test", 2L, "", "page", 0 to 0).copy(isRead = false))
            db.snapsDao().insertSnap(Snap("older", "page", 1L, "text/plain", "UTF-8", 13))
            if (!single) {
                db.snapsDao().insertSnap(Snap("newer", "page", 2L, "text/plain", "UTF-8", 13))
                storage.writeBytes("newer", "Opening 10:30".encodeToByteArray())
            }
            withContext(Dispatchers.Main) { model.openComparison("page", if (single) "older" else "newer") }
            val failed = withTimeout(5_000) { model.uiState.first { it.error != null } }
            assertEquals(ComparisonLoadFailure.Content, failed.error)
            assertNull(failed.singleSnapContent)
            assertNull(failed.snapContent1)
            assertFalse(failed.canMarkRead)
            assertEquals(if (single) 1 else 2, snapshots.getAllSnapsForSite("page").size)
            storage.writeBytes("older", "Opening 10:00".encodeToByteArray())
            withContext(Dispatchers.Main) { model.retryComparison() }
            val recovered = withTimeout(5_000) { model.uiState.first { !it.isLoading && !it.isLoadingDiff && it.error == null && (it.singleSnapContent != null || it.snapContent1 != null) } }
            if (single) {
                assertEquals("Opening 10:00", recovered.singleSnapContent)
            } else {
                assertEquals("Opening 10:30", recovered.snapContent1)
                assertEquals("Opening 10:00", recovered.snapContent2)
            }
            assertFalse(requireNotNull(db.siteDao().getSiteById("page")).isRead)
            assertEquals(if (single) listOf("older") else listOf("newer", "older"), recovered.snaps.map { it.snapId })
        } finally {
            model.viewModelScope.coroutineContext[Job]?.cancelAndJoin()
            application.scope.cancel()
            db.close()
            directory.deleteRecursively()
        }
    }
}
