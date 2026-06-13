/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bernaferrari.changedetection.background.MonitoringManager
import com.bernaferrari.changedetection.platform.ShareManager
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import com.bernaferrari.changedetection.util.UnifiedDiffUtils
import com.bernaferrari.changedetection.util.decodeByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

data class ComparisonUiState(
    val isLoading: Boolean = true,
    val site: Site? = null,
    val snaps: List<Snap> = emptyList(),
    val selectedPairIndex: Int? = null,
    val snapContent1: String? = null,
    val snapContent2: String? = null,
    val snapBytes1: ByteArray? = null,
    val snapBytes2: ByteArray? = null,
    val singleSnapContent: String? = null,
    val singleSnapBytes: ByteArray? = null,
    val imageDiff: SnapshotImageDiff? = null,
    val isLoadingDiff: Boolean = false,
    val error: String? = null,
)

class ComparisonViewModel(
    private val sitesRepository: SitesRepository,
    private val snapsRepository: SnapsRepository,
    private val monitoringManager: MonitoringManager,
    private val shareManager: ShareManager,
) : ViewModel() {
    // ... (existing code)

    fun shareDiff(
        snap1: Snap,
        snap2: Snap,
        content1: String,
        content2: String,
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            val oldLines = content1.lines()
            val newLines = content2.lines()
            // Assuming DiffUtils is available in commonMain (it should be if fast-kotlin-diff-core is KMP)
            val patch =
                com.bernaferrari.difflib.DiffUtils
                    .diff(oldLines, newLines)
            val diff =
                UnifiedDiffUtils.generateUnifiedDiff(
                    originalFileName = "v${snap1.timestamp}",
                    revisedFileName = "v${snap2.timestamp}",
                    originalLines = oldLines,
                    patch = patch,
                    contextSize = 3,
                )
            val diffText = diff.joinToString("\n")
            val shareText = "Diff generated at ${kotlin.time.Clock.System.now().toEpochMilliseconds()}"
            val fileName = "diff_${kotlin.time.Clock.System.now().toEpochMilliseconds()}.diff"

            withContext(Dispatchers.Main) {
                shareManager.shareFile(fileName, diffText, shareText)
            }
        }
    }

    fun shareImageDiff(
        newerSnap: Snap,
        olderSnap: Snap,
        imageDiff: SnapshotImageDiff,
    ) {
        val summary =
            buildString {
                appendLine("Image diff")
                appendLine("Before: ${olderSnap.timestamp}")
                appendLine("After: ${newerSnap.timestamp}")
                appendLine("Changed: ${formatPercent(imageDiff.mismatchPercentage)}")
                appendLine("Changed pixels: ${imageDiff.differentPixels} / ${imageDiff.totalPixels}")
                appendLine("Before size: ${imageDiff.beforeWidth} x ${imageDiff.beforeHeight}")
                appendLine("After size: ${imageDiff.afterWidth} x ${imageDiff.afterHeight}")
                imageDiff.dimensionMismatch?.let {
                    appendLine(
                        "Dimension mismatch: ${it.beforeWidth} x ${it.beforeHeight} -> " +
                            "${it.afterWidth} x ${it.afterHeight}",
                    )
                }
                imageDiff.error?.let { appendLine("Error: $it") }
            }

        val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
        shareManager.shareFile(
            fileName = "image_diff_$now.txt",
            text = summary,
            title = "Image diff",
        )
    }

    private val _uiState = MutableStateFlow(ComparisonUiState())
    val uiState: StateFlow<ComparisonUiState> = _uiState.asStateFlow()

    fun loadSiteAndSnaps(siteId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val site = sitesRepository.getSiteById(siteId)
                val snaps =
                    snapsRepository
                        .getAllSnapsForSite(siteId)
                        .sortedByDescending { it.timestamp } // Most recent first

                _uiState.value =
                    ComparisonUiState(
                        isLoading = false,
                        site = site,
                        snaps = snaps,
                    )

                // If only one snapshot, load its content directly
                if (snaps.size == 1) {
                    loadSingleSnapContent(snaps.first())
                }

                // Auto-select the latest pair for immediate "Visual Diff"
                if (snaps.size >= 2) {
                    selectPairForComparison(0)
                }
            } catch (e: Exception) {
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error occurred",
                    )
            }
        }
    }

    fun selectPairForComparison(pairIndex: Int) {
        val currentState = _uiState.value
        val snaps = currentState.snaps

        if (pairIndex < 0 || pairIndex >= snaps.size - 1) return

        val snap1 = snaps[pairIndex]
        val snap2 = snaps[pairIndex + 1]

        _uiState.update { it.copy(selectedPairIndex = pairIndex, error = null) }
        loadSnapContents(snap1, snap2)
    }

    private fun loadSnapContents(
        snap1: Snap,
        snap2: Snap,
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingDiff = true,
                    error = null,
                    snapContent1 = null,
                    snapContent2 = null,
                    snapBytes1 = null,
                    snapBytes2 = null,
                    imageDiff = null,
                )
            }

            try {
                val (firstBytes, secondBytes) =
                    withContext(Dispatchers.IO) {
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
                                )
                            } else {
                                null
                            }
                        Triple(firstContent, secondContent, visualDiff)
                    }

                _uiState.update {
                    it.copy(
                        snapContent1 = content1,
                        snapContent2 = content2,
                        snapBytes1 = firstBytes,
                        snapBytes2 = secondBytes,
                        imageDiff = imageDiff,
                        isLoadingDiff = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingDiff = false,
                        error = "Failed to load content: ${e.message}",
                    )
                }
            }
        }
    }

    private fun loadSingleSnapContent(snap: Snap) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingDiff = true,
                    error = null,
                    singleSnapContent = null,
                    singleSnapBytes = null,
                )
            }

            try {
                val bytes =
                    withContext(Dispatchers.IO) {
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
                    it.copy(
                        singleSnapContent = content,
                        singleSnapBytes = bytes.takeIf { snap.isImageSnapshot() },
                        isLoadingDiff = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingDiff = false,
                        error = "Failed to load content: ${e.message}",
                    )
                }
            }
        }
    }

    fun reloadSite() {
        val site = _uiState.value.site ?: return
        viewModelScope.launch {
            monitoringManager.reloadSite(site)
        }
    }

    fun deleteSite() {
        val site = _uiState.value.site ?: return
        viewModelScope.launch {
            snapsRepository.deleteAllSnaps(site.id)
            sitesRepository.deleteSite(site.id)
        }
    }

    fun pruneSite() {
        val site = _uiState.value.site ?: return
        viewModelScope.launch {
            snapsRepository.pruneSnaps(site.id)
            // Refresh snaps list after pruning
            val snaps =
                snapsRepository
                    .getAllSnapsForSite(site.id)
                    .sortedByDescending { it.timestamp }
            _uiState.update { it.copy(snaps = snaps) }
        }
    }

    fun clearSelection() {
        _uiState.update {
            it.copy(
                selectedPairIndex = null,
                snapContent1 = null,
                snapContent2 = null,
                snapBytes1 = null,
                snapBytes2 = null,
                imageDiff = null,
            )
        }
    }
}

private fun formatPercent(value: Double): String = "${((value * 10).roundToInt() / 10.0)}%"
