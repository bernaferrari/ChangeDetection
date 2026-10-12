package com.bernaferrari.changedetection.storage

import com.bernaferrari.changedetection.repo.Snap
import kotlinx.coroutines.NonCancellable
import com.bernaferrari.changedetection.repo.source.local.PendingFileDeletionDao
import com.bernaferrari.changedetection.repo.source.local.SnapsDao
import com.bernaferrari.changedetection.util.FileStorage
import com.bernaferrari.changedetection.util.fileIoDispatcher
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Durable, idempotent file cleanup and coordinated restoration of an existing capture identity. */
class SnapshotFileCleanup(
    private val pending: PendingFileDeletionDao,
    private val snaps: SnapsDao,
    private val files: FileStorage,
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = fileIoDispatcher,
) {
    private val mutex = Mutex()

    /** A fixed high-water mark and keyset batches avoid archive-sized allocations and retry loops. */
    suspend fun drain() = withContext(ioDispatcher) {
        mutex.withLock {
            val through = pending.lastId() ?: return@withLock
            var after: String? = null
            while (true) {
                currentCoroutineContext().ensureActive()
                val batch = if (after == null) pending.firstBatch(through) else pending.batch(after, through)
                if (batch.isEmpty()) break
                for (id in batch) {
                    currentCoroutineContext().ensureActive()
                    // Protect a restored/replaced live identity; never delete its content.
                    if (snaps.getSnapById(id) != null || removeFile(id)) pending.complete(id)
                }
                after = batch.last()
            }
        }
    }

    /** Undo owns the same lock as deletion, including retries already past their row-existence check. */
    suspend fun restore(snap: Snap, bytes: ByteArray) = withContext(ioDispatcher) {
        mutex.withLock {
            withContext(NonCancellable) {
                files.writeBytes(snap.snapId, bytes)
                snaps.insertSnap(snap)
                pending.complete(snap.snapId)
            }
        }
    }

    /** Orphan discovery must use the same ownership boundary as durable deletion and Undo. */
    suspend fun removeOrphan(id: String): Boolean = withContext(ioDispatcher) {
        mutex.withLock { snaps.getSnapById(id) != null || removeFile(id) }
    }

    /** Starts at process initialization, even with no pages or scheduled monitoring disabled. */
    suspend fun run() {
        while (currentCoroutineContext().isActive) {
            try {
                drain()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Napier.w("Could not drain pending snapshot cleanup; retrying later", error)
            }
            delay(60_000)
        }
    }

    private fun removeFile(id: String): Boolean = try {
        files.deleteFile(id) || !files.fileExists(id)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Napier.w("Snapshot cleanup remains pending", error)
        false
    }
}
