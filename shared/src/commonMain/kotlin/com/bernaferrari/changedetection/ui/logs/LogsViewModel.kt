/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import com.bernaferrari.changedetection.util.FileStorage
import io.github.aakira.napier.Napier
import com.bernaferrari.changedetection.util.formatDate
import com.bernaferrari.changedetection.repo.source.local.SitesDao
import com.bernaferrari.changedetection.repo.source.local.SnapsDao

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.core.annotation.KoinViewModel

sealed class LogUiModel {
    data class Item(
        val snap: Snap,
    ) : LogUiModel()

    data class Header(
        val date: String,
    ) : LogUiModel()
}

@KoinViewModel
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class LogsViewModel(
    private val sitesDao: SitesDao,
    private val snapsDao: SnapsDao,
    private val snapsRepository: SnapsRepository,
    private val fileStorage: FileStorage,
    // Injected so host tests can run deletes on a deterministic test dispatcher.
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    /**
     * Snapshots held in memory between delete and Undo-expiry, keyed by snapId.
     * Each entry carries a [CompletableDeferred] that completes when the row/file
     * deletion has actually landed, so an Undo that arrives mid-delete can wait
     * instead of restoring into the delete's blind side (the old single-slot
     * version could delete the just-restored snapshot or silently drop Undo).
     */
    private class PendingSnapDeletion(
        val snap: Snap,
    ) {
        /**
         * Read before [deleted] completes; undo reads it only after awaiting
         * [deleted], so completion provides the necessary happens-before edge.
         */
        var contentBytes: ByteArray? = null

        /** Completes once read + deletion finished; cancelled if the delete fails. */
        val deleted = CompletableDeferred<Unit>()
    }

    // Mutated from both the caller (UI) thread and IO-dispatcher coroutines, so all
    // access goes through [MutableStateFlow.update]'s atomic compare-and-set loop.
    private val pendingDeletions = MutableStateFlow<Map<String, PendingSnapDeletion>>(emptyMap())
    val searchQuery = MutableStateFlow("")
    val siteMap: StateFlow<Map<String, Site>> =
        sitesDao
            .getDataWithChanges()
            .map { sites -> sites.associateBy { it.id } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyMap(),
            )

    val versionsCount: StateFlow<Int> =
        snapsDao
            .countVersionsInHistory()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0,
            )

    val changedSiteCount: StateFlow<Int> =
        snapsDao
            .countSitesWithChanges()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0,
            )

    val pagedSnaps: Flow<PagingData<LogUiModel>> = searchQuery
        .debounce(250)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            Pager(
                config = PagingConfig(
                    pageSize = 20,
                    prefetchDistance = 10,
                    enablePlaceholders = false,
                ),
                pagingSourceFactory = { snapsDao.getSnapsPagedSource(query.trim()) },
            ).flow
        }
        .map { pagingData ->
            pagingData
                .map { LogUiModel.Item(it) }
                .insertSeparators { before: LogUiModel.Item?, after: LogUiModel.Item? ->
                    val beforeDate = before?.snap?.timestamp?.let(::formatDate)
                    val afterDate = after?.snap?.timestamp?.let(::formatDate)

                    if (afterDate != null && beforeDate != afterDate) {
                        LogUiModel.Header(afterDate)
                    } else {
                        null
                    }
                }
        }.cachedIn(viewModelScope)

    // Only visible lazy-list entries request previews. Bound concurrent file reads and
    // skip oversized captures rather than retaining full documents in a scrolling list.
    private val previewPermits = kotlinx.coroutines.sync.Semaphore(2)

    internal suspend fun loadPreview(snap: Snap): HistoryPreviewContent = previewPermits.withPermit {
        loadHistoryPreview(snap, snapsRepository) { snapsDao.getPrecedingSnap(snap.siteId, snap.timestamp, snap.snapId) }
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    /**
     * Deletes a snapshot but keeps its row and content bytes in memory so the
     * snackbar's Undo action can restore it (see [undoRemoveSnap]). Call
     * [discardPendingDeletion] when the snackbar expires without Undo.
     */
    fun removeSnap(snap: Snap): Deferred<Boolean> {
        // Slot is registered synchronously so an immediately-following Undo always
        // finds its entry; the actual IO runs off the caller thread.
        val pending = PendingSnapDeletion(snap)
        pendingDeletions.update { it + (snap.snapId to pending) }
        return viewModelScope.async(ioDispatcher) {
            try {
                // Undo is only honest when the bytes are safely retained. If the
                // backup cannot be read, leave both the row and file untouched.
                pending.contentBytes = fileStorage.readBytes(snap.snapId)
                snapsRepository.deleteSnap(snap.snapId)
                pending.deleted.complete(Unit)
                // The screen releases this entry when its snackbar ends or leaves
                // composition. A fixed timer can expire before a queued snackbar
                // appears or while accessibility keeps its Undo action visible.
                true
            } catch (cancellation: CancellationException) {
                pendingDeletions.update { it - snap.snapId }
                pending.deleted.cancel()
                throw cancellation
            } catch (t: Throwable) {
                pendingDeletions.update { it - snap.snapId }
                pending.deleted.cancel()
                Napier.e("Failed to delete snapshot ${snap.snapId}", t)
                false
            }
        }
    }

    fun undoRemoveSnap(snap: Snap): Deferred<Boolean> {
        val pending = pendingDeletions.value[snap.snapId] ?: return CompletableDeferred(false)
        return viewModelScope.async(ioDispatcher) {
            // Wait for the delete to finish before restoring, otherwise the delete
            // would remove what this undo just re-inserted.
            try {
                pending.deleted.await()
            } catch (_: CancellationException) {
                // The delete failed or its owner was cancelled; the original row
                // was not safely removed, so there is nothing honest to restore.
                return@async false
            }
            try {
                val bytes = pending.contentBytes ?: return@async false
                fileStorage.writeBytes(snap.snapId, bytes)
                snapsDao.insertSnap(pending.snap)
                pendingDeletions.update { current ->
                    if (current[snap.snapId] === pending) current - snap.snapId else current
                }
                true
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                // Retain the backup while the UI offers Retry. Never re-insert a
                // row if its bytes could not be safely restored first.
                Napier.e("Failed to restore snapshot ${snap.snapId}", error)
                false
            }
        }
    }

    /**
     * Drops the retained bytes for a snapshot whose snackbar expired or was
     * dismissed without Undo; without this the blob stays pinned until the next
     * delete overwrites it. Safe to call even if no deletion is pending.
     */
    fun discardPendingDeletion(snap: Snap?) {
        if (snap == null) return
        pendingDeletions.update { current -> current - snap.snapId }
    }

    override fun onCleared() {
        // Release any retained snapshot bytes; nothing can restore them anymore.
        pendingDeletions.value = emptyMap()
        super.onCleared()
    }
}
