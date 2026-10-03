/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.storage

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

/** Durable, idempotent file cleanup. Snapshot IDs are immutable capture identities, never reused. */
class SnapshotFileCleanup(
    private val pending: PendingFileDeletionDao,
    private val snaps: SnapsDao,
    private val files: FileStorage,
) {
    private val mutex = Mutex()

    /** A fixed high-water mark and keyset batches avoid archive-sized allocations and retry loops. */
    suspend fun drain() = withContext(fileIoDispatcher) {
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
