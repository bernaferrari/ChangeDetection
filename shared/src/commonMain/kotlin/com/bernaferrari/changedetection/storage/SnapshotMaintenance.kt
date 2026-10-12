package com.bernaferrari.changedetection.storage

import androidx.room3.withWriteTransaction
import com.bernaferrari.changedetection.repo.source.local.ChangeDatabase
import com.bernaferrari.changedetection.repo.source.local.SnapsDao
import com.bernaferrari.changedetection.util.FileStorage
import io.github.aakira.napier.Napier
import com.bernaferrari.changedetection.util.fileIoDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Clock

/** Rows commit before file deletion; a grace period protects files currently being written. */
class SnapshotMaintenance(
    private val database: ChangeDatabase,
    private val snaps: SnapsDao,
    private val files: FileStorage,
    private val policy: SnapshotRetention = SnapshotRetention(),
    private val cleanup: SnapshotFileCleanup = SnapshotFileCleanup(database.pendingFileDeletionDao(), snaps, files),
) {
    private val mutex = Mutex()
    private var lastSweep = 0L

    /** Runs even for unchanged or failed checks, so disk recovery needs no successful write. */
    suspend fun beforeCheck() = withContext(fileIoDispatcher) {
        mutex.withLock {
            cleanup.drain()
            sweepOrphansIfDue()
        }
    }

    suspend fun afterSavedVersion() = withContext(fileIoDispatcher) {
        mutex.withLock {
            while (true) {
                currentCoroutineContext().ensureActive()
                val obsolete = database.withWriteTransaction {
                    val ids = snaps.retentionBatch(policy.versionsPerPage, policy.byteBudget, 500)
                    if (ids.isNotEmpty()) snaps.deleteSnapsByIds(ids)
                    ids
                }
                if (obsolete.isEmpty()) break
                cleanup.drain()
            }
            sweepOrphansIfDue()
        }
    }

    private suspend fun sweepOrphansIfDue() {
        val now = Clock.System.now().toEpochMilliseconds()
        if (now - lastSweep < 3_600_000) return
        for (id in files.orphanCandidates(now - 24 * 3_600_000)) {
            currentCoroutineContext().ensureActive()
            cleanup.removeOrphan(id)
        }
        lastSweep = now
    }
}
