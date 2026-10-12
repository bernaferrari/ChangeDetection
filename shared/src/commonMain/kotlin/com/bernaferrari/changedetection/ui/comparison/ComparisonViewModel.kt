package com.bernaferrari.changedetection.ui.comparison

import com.bernaferrari.changedetection.image.PlatformImageDiff

import com.bernaferrari.changedetection.image.SnapshotImageDiff

import com.bernaferrari.changedetection.util.readableSnapshotText

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bernaferrari.changedetection.background.MonitoringManager
import com.bernaferrari.changedetection.error.ErrorContext
import com.bernaferrari.changedetection.error.ErrorHandler
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import com.bernaferrari.changedetection.platform.ShareManager
import com.bernaferrari.changedetection.util.ApplicationScope
import com.bernaferrari.changedetection.util.UnifiedDiffUtils
import com.bernaferrari.changedetection.util.decodeByteArray
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import org.koin.core.annotation.KoinViewModel

enum class ComparisonLoadFailure { Page, Content }

data class ComparisonUiState(
    val isLoading: Boolean = true,
    val site: Site? = null,
    val snaps: List<Snap> = emptyList(),
    val selectedPairIndex: Int? = null,
    val snapContent1: String? = null,
    val snapContent2: String? = null,
    val snapBytes1: ByteArray? = null,
    val snapBytes2: ByteArray? = null,
    /** A saved baseline has no earlier version, but remains readable from history. */
    val singleSnap: Snap? = null,
    val singleSnapContent: String? = null,
    val singleSnapBytes: ByteArray? = null,
    val imageDiff: SnapshotImageDiff? = null,
    val isLoadingDiff: Boolean = false,
    /** True when the selected pair spans a capture-method change (text snapshot vs screenshot). */
    val mixedTypePair: Boolean = false,
    val error: ComparisonLoadFailure? = null,
    val requestedSnapshotUnavailable: Boolean = false,
    val isUpdatingReadState: Boolean = false,
    val newVersionAvailable: Boolean = false,
) {
    val showsLatestComparison: Boolean
        get() = !isLoading && !isLoadingDiff && error == null && imageDiff?.error == null &&
            !requestedSnapshotUnavailable && snaps.size > 1 && selectedPairIndex == 0
    val canMarkRead: Boolean
        get() = showsLatestComparison && site?.isRead == false && !isUpdatingReadState
}

@KoinViewModel
class ComparisonViewModel(
    private val sitesRepository: SitesRepository,
    private val snapsRepository: SnapsRepository,
    private val monitoringManager: MonitoringManager,
    private val shareManager: ShareManager,
    private val errorHandler: ErrorHandler,
    private val applicationScope: ApplicationScope,
) : ViewModel() {
    private var contentLoadJob: Job? = null
    private var siteLoadJob: Job? = null
    private var siteObservationJob: Job? = null
    private var snapshotObservationJob: Job? = null

    /** One-shot messages for the snackbar host (e.g. prune success). */
    private val _actionMessages = Channel<String>(Channel.BUFFERED)
    val actionMessages: Flow<String> = _actionMessages.receiveAsFlow()

    /** What changed, written for a person: the page, when, and the edits as "old → new". */
    fun shareDiff(
        snap1: Snap,
        snap2: Snap,
        content1: String,
        content2: String,
        readableText: Boolean = false,
    ) {
        val site = _uiState.value.site ?: return
        viewModelScope.launch {
            val text = withContext(Dispatchers.Default) {
                val oldLines = readableSnapshotText(content1, readableText && snap1.isHtmlSnapshot()).lines().map { it.trim() }.filter { it.isNotEmpty() }
                val newLines = readableSnapshotText(content2, readableText && snap2.isHtmlSnapshot()).lines().map { it.trim() }.filter { it.isNotEmpty() }
                val deltas = com.bernaferrari.difflib.DiffUtils.diff(oldLines, newLines).deltas
                buildString {
                    appendLine("${site.title ?: site.url} changed")
                    deltas.take(MAX_SHARED_EDITS).forEach { delta ->
                        val before = delta.source.lines.joinToString(" ").take(160)
                        val after = delta.target.lines.joinToString(" ").take(160)
                        when {
                            before.isNotEmpty() && after.isNotEmpty() -> appendLine("• $before → $after")
                            after.isNotEmpty() -> appendLine("+ $after")
                            else -> appendLine("− $before")
                        }
                    }
                    if (deltas.size > MAX_SHARED_EDITS) appendLine("…and ${deltas.size - MAX_SHARED_EDITS} more")
                    append(site.url)
                }
            }
            shareManager.shareText(text, site.title ?: site.url)
        }
    }

    fun shareImageDiff(
        newerSnap: Snap,
        olderSnap: Snap,
        imageDiff: SnapshotImageDiff,
    ) {
        val site = _uiState.value.site ?: return
        val areas = imageDiff.visualChange?.regions?.size ?: 0
        val text = buildString {
            append("${site.title ?: site.url} changed")
            if (areas > 0) append(" · $areas ${if (areas == 1) "area" else "areas"}")
            appendLine()
            append(site.url)
        }
        shareManager.shareText(text, site.title ?: site.url)
    }

    private companion object {
        const val MAX_SHARED_EDITS = 6
    }

    private val _uiState = MutableStateFlow(ComparisonUiState())
    val uiState: StateFlow<ComparisonUiState> = _uiState.asStateFlow()

    private var entryRequest: Pair<String, String?>? = null
    private var lastLoadRequest: Pair<String, String?>? = null

    /** Re-entering a retained route must preserve the user's historical selection. */
    fun openComparison(siteId: String, initialSnapId: String? = null) {
        val request = siteId to initialSnapId
        if (entryRequest == request) return
        entryRequest = request
        loadSiteAndSnaps(siteId, initialSnapId)
    }

    /** Retry the visible/requested identity; only explicit View latest may change it. */
    fun retryComparison() {
        val request = lastLoadRequest ?: return
        val state = _uiState.value
        val selected = state.selectedPairIndex?.let { state.snaps.getOrNull(it)?.snapId }
        loadSiteAndSnaps(request.first, selected ?: state.singleSnap?.snapId ?: request.second)
    }

    fun loadSiteAndSnaps(
        siteId: String,
        initialSnapId: String? = null,
    ) {
        lastLoadRequest = siteId to initialSnapId
        siteLoadJob?.cancel()
        siteObservationJob?.cancel()
        snapshotObservationJob?.cancel()
        contentLoadJob?.cancel()
        siteLoadJob = viewModelScope.launch {
            _uiState.value = ComparisonUiState()

            try {
                val site = sitesRepository.getSiteById(siteId)
                val snaps =
                    snapsRepository
                        .getAllSnapsForSite(siteId)
                        .sortedWith(compareByDescending<Snap> { it.timestamp }.thenByDescending { it.snapId })

                _uiState.value =
                    ComparisonUiState(
                        isLoading = false,
                        site = site,
                        snaps = snaps,
                    )

                observeCheckStatus(siteId)

                if (initialSnapId != null && snaps.none { it.snapId == initialSnapId }) {
                    _uiState.update { it.copy(requestedSnapshotUnavailable = true) }
                    observeHistory(siteId)
                    return@launch
                }

                // If only one snapshot, load its content directly
                if (snaps.size == 1) {
                    loadSingleSnapContent(snaps.first())
                }
                // Auto-select the alerted snapshot's pair when opened from a notification,
                // else the latest pair for immediate "Visual Diff"
                if (snaps.size >= 2) {
                    selectPairForComparison(initialSnapId)
                }
                observeHistory(siteId)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                Napier.w("Could not load saved comparison", e)
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        error = ComparisonLoadFailure.Page,
                    )
            }
        }
    }

    /** Check health is live even while the user reviews a specific historical pair. */
    private fun observeCheckStatus(siteId: String) {
        siteObservationJob = viewModelScope.launch {
            try {
                sitesRepository.getDataWithChanges().collect { sites ->
                    val latest = sites.firstOrNull { it.id == siteId }
                    _uiState.update { current ->
                        if (current.site?.id == siteId) current.copy(site = latest) else current
                    }
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Napier.w("Could not refresh page check status", error)
            }
        }
    }

    /** Metadata stays live; saved content is never silently switched beneath the reader. */
    private fun observeHistory(siteId: String) {
        snapshotObservationJob = viewModelScope.launch {
            try {
                snapsRepository.getSnaps(siteId).collect { rows ->
                    val snaps = rows.sortedWith(compareByDescending<Snap> { it.timestamp }.thenByDescending { it.snapId })
                    val before = _uiState.value
                    if (before.site?.id != siteId || before.snaps == snaps) return@collect
                    val selectedId = before.singleSnap?.snapId ?: before.selectedPairIndex?.let { before.snaps.getOrNull(it)?.snapId }
                    val olderId = before.selectedPairIndex?.let { before.snaps.getOrNull(it + 1)?.snapId }
                    val index = snaps.indexOfFirst { it.snapId == selectedId }
                    val available = selectedId == null || (index >= 0 && (olderId == null || snaps.getOrNull(index + 1)?.snapId == olderId))
                    _uiState.update { current ->
                        current.copy(
                            snaps = snaps,
                            selectedPairIndex = index.takeIf { available && olderId != null },
                            singleSnap = current.singleSnap?.takeIf { available },
                            requestedSnapshotUnavailable = current.requestedSnapshotUnavailable || !available,
                            newVersionAvailable = current.newVersionAvailable || snaps.firstOrNull()?.snapId != before.snaps.firstOrNull()?.snapId,
                            isLoadingDiff = current.isLoadingDiff && available,
                        )
                    }
                    if (!available) {
                        contentLoadJob?.cancel()
                    } else if (selectedId == null && !before.requestedSnapshotUnavailable && snaps.isNotEmpty()) {
                        selectPairForComparison(0)
                    }
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Napier.w("Could not refresh saved versions", error)
            }
        }
    }

    fun selectPairForComparison(pairIndex: Int) {
        val currentState = _uiState.value
        val snaps = currentState.snaps

        if (pairIndex !in snaps.indices) return
        if (pairIndex == snaps.lastIndex) {
            loadSingleSnapContent(snaps[pairIndex])
            return
        }

        val snap1 = snaps[pairIndex]
        val snap2 = snaps[pairIndex + 1]

        _uiState.update { it.copy(selectedPairIndex = pairIndex, singleSnap = null, singleSnapContent = null, singleSnapBytes = null, error = null, requestedSnapshotUnavailable = false, newVersionAvailable = if (pairIndex == 0) false else it.newVersionAvailable) }
        loadSnapContents(pairIndex, snap1, snap2)
    }

    /**
     * Selects the requested snapshot's pair. Missing history never silently opens another change.
     */
    fun selectPairForComparison(snapId: String?) {
        if (snapId == null) {
            selectPairForComparison(0)
            return
        }
        val index = _uiState.value.snaps.indexOfFirst { it.snapId == snapId }
        // The baseline opens as a saved version; every later version opens its actual pair.
        if (index in _uiState.value.snaps.indices) {
            selectPairForComparison(index)
        } else {
            _uiState.update { it.copy(requestedSnapshotUnavailable = true) }
        }
    }

    private fun loadSnapContents(
        pairIndex: Int,
        snap1: Snap,
        snap2: Snap,
    ) {
        contentLoadJob?.cancel()
        contentLoadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingDiff = true,
                    error = null,
                    snapContent1 = null,
                    snapContent2 = null,
                    snapBytes1 = null,
                    snapBytes2 = null,
                    imageDiff = null,
                    mixedTypePair = snap1.isTextSnapshot() != snap2.isTextSnapshot(),
                )
            }

            try {
                val (firstBytes, secondBytes) =
                    withContext(Dispatchers.Default) {
                        val first = snapsRepository.getSnapContent(snap1.snapId)
                        val second = snapsRepository.getSnapContent(snap2.snapId)
                        first to second
                    }

                val (content1, content2, imageDiff) =
                    withContext(Dispatchers.Default) {
                        val firstContent =
                            if (snap1.isTextSnapshot()) {
                                decodeByteArray(firstBytes, snap1.contentCharset)
                            } else {
                                null
                            }
                        val secondContent =
                            if (snap2.isTextSnapshot()) {
                                decodeByteArray(secondBytes, snap2.contentCharset)
                            } else {
                                null
                            }
                        val visualDiff =
                            if (snap1.isImageSnapshot() && snap2.isImageSnapshot()) {
                                PlatformImageDiff.compare(
                                    beforeBytes = secondBytes,
                                    afterBytes = firstBytes,
                                ).let { diff ->
                                    // Row-aligned areas, so a shifted page reads as "a story was added" instead of "everything changed".
                                    if (diff.visualChange != null) diff else diff.copy(visualChange = com.bernaferrari.changedetection.image.computeVisualChange(secondBytes, firstBytes))
                                }
                            } else {
                                null
                            }
                        Triple(firstContent, secondContent, visualDiff)
                    }

                _uiState.update { current ->
                    if (current.snaps.getOrNull(current.selectedPairIndex ?: -1)?.snapId != snap1.snapId || current.snaps.getOrNull((current.selectedPairIndex ?: -2) + 1)?.snapId != snap2.snapId) return@update current
                    current.copy(
                        snapContent1 = content1,
                        snapContent2 = content2,
                        snapBytes1 = firstBytes,
                        snapBytes2 = secondBytes,
                        imageDiff = imageDiff,
                        isLoadingDiff = false,
                    )
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                Napier.w("Could not load saved comparison", e)
                _uiState.update { current ->
                    if (current.snaps.getOrNull(current.selectedPairIndex ?: -1)?.snapId != snap1.snapId || current.snaps.getOrNull((current.selectedPairIndex ?: -2) + 1)?.snapId != snap2.snapId) return@update current
                    current.copy(
                        isLoadingDiff = false,
                        error = ComparisonLoadFailure.Content,
                    )
                }
            }
        }
    }

    private fun loadSingleSnapContent(snap: Snap) {
        contentLoadJob?.cancel()
        contentLoadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingDiff = true,
                    error = null,
                    singleSnap = snap,
                    selectedPairIndex = null,
                    snapContent1 = null,
                    snapContent2 = null,
                    snapBytes1 = null,
                    snapBytes2 = null,
                    imageDiff = null,
                    requestedSnapshotUnavailable = false,
                    newVersionAvailable = if (it.snaps.firstOrNull()?.snapId == snap.snapId) false else it.newVersionAvailable,
                    singleSnapContent = null,
                    singleSnapBytes = null,
                )
            }

            try {
                val bytes =
                    withContext(Dispatchers.Default) {
                        snapsRepository.getSnapContent(snap.snapId)
                    }
                val content =
                    withContext(Dispatchers.Default) {
                        if (snap.isTextSnapshot()) {
                            decodeByteArray(bytes, snap.contentCharset)
                        } else {
                            null
                        }
                    }

                _uiState.update {
                    if (it.singleSnap?.snapId != snap.snapId) return@update it
                    it.copy(
                        singleSnapContent = content,
                        singleSnapBytes = bytes.takeIf { snap.isImageSnapshot() },
                        isLoadingDiff = false,
                    )
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                Napier.w("Could not load saved comparison", e)
                _uiState.update {
                    it.copy(
                        isLoadingDiff = false,
                        error = ComparisonLoadFailure.Content,
                    )
                }
            }
        }
    }

    /** The returned concrete capture is the undo receipt; loading content never acknowledges it. */
    suspend fun markVisibleComparisonRead(): Snap? {
        val visible = _uiState.value
        if (!visible.canMarkRead) return null
        val snap = visible.snaps.first()
        _uiState.update { it.copy(isUpdatingReadState = true) }
        return try {
            if (sitesRepository.markSiteRead(snap.siteId, snap.snapId)) {
                _uiState.update { current ->
                    if (current.site == visible.site) current.copy(site = current.site?.copy(isRead = true)) else current
                }
                snap
            } else {
                _actionMessages.send("read_changed")
                null
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Napier.w("Could not acknowledge saved comparison", error)
            _actionMessages.send("read_failed")
            null
        } finally {
            _uiState.update { it.copy(isUpdatingReadState = false) }
        }
    }

    suspend fun undoRead(receipt: Snap) {
        val visible = _uiState.value
        try {
            if (sitesRepository.undoSiteRead(receipt.siteId, receipt.snapId)) {
                _uiState.update { current ->
                    if (current.site?.id == receipt.siteId && current.site == visible.site) {
                        current.copy(site = current.site.copy(isRead = false))
                    } else {
                        current
                    }
                }
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Napier.w("Could not undo acknowledgement", error)
            _actionMessages.send("read_failed")
        }
    }

    fun reloadSite() {
        val site = _uiState.value.site ?: return
        viewModelScope.launch {
            monitoringManager.reloadSite(site)
        }
    }

    /** Result flag for the last delete attempt: null = none, true = success, false = failed. */
    private val _deleteResult = MutableStateFlow<Boolean?>(null)
    val deleteResult: StateFlow<Boolean?> = _deleteResult.asStateFlow()

    fun deleteSite() {
        val site = _uiState.value.site ?: return
        _deleteResult.value = null
        // Application-scoped: deletion survives navigating back (which cancels
        // viewModelScope). Rows go atomically via FK cascade; files swept after.
        applicationScope.scope.launch {
            runCatching {
                snapsRepository.deleteSiteWithSnaps(site.id)
            }.onSuccess {
                _deleteResult.value = true
            }.onFailure { e ->
                Napier.e(tag = "ComparisonViewModel") { "Failed to delete site ${site.id}: ${e.message}" }
                errorHandler.handleError(e, ErrorContext.DELETE_SITE, userAction = "delete site")
                _deleteResult.value = false
            }
        }
    }

    fun pruneSite() {
        val site = _uiState.value.site ?: return
        val selectedSnapId = _uiState.value.singleSnap?.snapId ?: _uiState.value.snaps.getOrNull(_uiState.value.selectedPairIndex ?: 0)?.snapId
        viewModelScope.launch {
            try {
                snapsRepository.pruneSnaps(site.id)
                // Reload the selected capture by identity: pruning changes list indices and
                // may remove the older half of the comparison currently on screen.
                if (_uiState.value.site?.id == site.id) loadSiteAndSnaps(site.id, selectedSnapId)
                _actionMessages.trySend("pruned")
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                Napier.e(tag = "ComparisonViewModel") { "Failed to prune site ${site.id}: ${e.message}" }
                errorHandler.handleError(e, ErrorContext.DATABASE_OPERATION, userAction = "prune snapshots")
                _actionMessages.trySend("prune_failed")
            }
        }
    }

    fun clearSelection() {
        _uiState.update {
            it.copy(
                selectedPairIndex = null,
                singleSnap = null,
                singleSnapContent = null,
                singleSnapBytes = null,
                snapContent1 = null,
                snapContent2 = null,
                snapBytes1 = null,
                snapBytes2 = null,
                imageDiff = null,
            )
        }
    }
}
