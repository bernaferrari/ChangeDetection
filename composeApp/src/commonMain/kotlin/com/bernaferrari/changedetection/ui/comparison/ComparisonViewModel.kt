package com.bernaferrari.changedetection.ui.comparison

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bernaferrari.changedetection.background.MonitoringManager
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import com.bernaferrari.changedetection.util.decodeByteArray
import com.bernaferrari.changedetection.platform.ShareManager
import com.bernaferrari.changedetection.util.UnifiedDiffUtils
import kotlinx.datetime.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ComparisonUiState(
    val isLoading: Boolean = true,
    val site: Site? = null,
    val snaps: List<Snap> = emptyList(),
    val selectedPairIndex: Int? = null,
    val snapContent1: String? = null,
    val snapContent2: String? = null,
    val singleSnapContent: String? = null,
    val isLoadingDiff: Boolean = false,
    val error: String? = null
)

class ComparisonViewModel(
    private val sitesRepository: SitesRepository,
    private val snapsRepository: SnapsRepository,
    private val monitoringManager: MonitoringManager,
    private val shareManager: ShareManager
) : ViewModel() {
    
    // ... (existing code)

    fun shareDiff(snap1: Snap, snap2: Snap, content1: String, content2: String) {
        viewModelScope.launch(Dispatchers.Default) {
             val oldLines = content1.lines()
             val newLines = content2.lines()
             // Assuming DiffUtils is available in commonMain (it should be if fast-kotlin-diff-core is KMP)
             val patch = com.bernaferrari.difflib.DiffUtils.diff(oldLines, newLines)
             val diff = UnifiedDiffUtils.generateUnifiedDiff(
                 originalFileName = "v${snap1.timestamp}",
                 revisedFileName = "v${snap2.timestamp}",
                 originalLines = oldLines,
                 patch = patch,
                 contextSize = 3
             )
             val diffText = diff.joinToString("\n")
             val fileName = "diff_${com.bernaferrari.changedetection.util.platformCurrentTimeMillis()}.diff"
             
             withContext(Dispatchers.Main) {
                 shareManager.shareFile(fileName, diffText, "Share diff")
             }
        }
    }

    private val _uiState = MutableStateFlow(ComparisonUiState())
    val uiState: StateFlow<ComparisonUiState> = _uiState.asStateFlow()

    fun loadSiteAndSnaps(siteId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val site = sitesRepository.getSiteById(siteId)
                val snaps = snapsRepository.getAllSnapsForSite(siteId)
                    .sortedByDescending { it.timestamp } // Most recent first

                _uiState.value = ComparisonUiState(
                    isLoading = false,
                    site = site,
                    snaps = snaps
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
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
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

    private fun loadSnapContents(snap1: Snap, snap2: Snap) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDiff = true, error = null) }

            try {
                val (content1, content2) = withContext(Dispatchers.IO) {
                    val first = snapsRepository.getSnapContent(snap1.snapId)
                    val second = snapsRepository.getSnapContent(snap2.snapId)

                    // Use SharedStringUtils to decode with charset support
                    decodeByteArray(first, snap1.contentCharset) to
                            decodeByteArray(second, snap2.contentCharset)
                }

                _uiState.update {
                    it.copy(
                        snapContent1 = content1,
                        snapContent2 = content2,
                        isLoadingDiff = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingDiff = false,
                        error = "Failed to load content: ${e.message}"
                    )
                }
            }
        }
    }

    private fun loadSingleSnapContent(snap: Snap) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDiff = true, error = null) }

            try {
                val content = withContext(Dispatchers.IO) {
                    val bytes = snapsRepository.getSnapContent(snap.snapId)
                    decodeByteArray(bytes, snap.contentCharset)
                }

                _uiState.update {
                    it.copy(
                        singleSnapContent = content,
                        isLoadingDiff = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingDiff = false,
                        error = "Failed to load content: ${e.message}"
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
            val snaps = snapsRepository.getAllSnapsForSite(site.id)
                .sortedByDescending { it.timestamp }
            _uiState.update { it.copy(snaps = snaps) }
        }
    }

    fun clearSelection() {
        _uiState.update {
            it.copy(
                selectedPairIndex = null,
                snapContent1 = null,
                snapContent2 = null
            )
        }
    }
}
