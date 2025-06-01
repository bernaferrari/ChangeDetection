package com.bernaferrari.changedetection.detailstext

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.charset.Charset
import javax.inject.Inject

@HiltViewModel
class TextViewModel @Inject constructor(
    private val snapsRepository: SnapsRepository
) : ViewModel() {

    private val _selectedSnaps = MutableStateFlow<List<Snap>>(emptyList())
    val selectedSnaps: StateFlow<List<Snap>> = _selectedSnaps.asStateFlow()

    private val _viewMode = MutableStateFlow(TextViewMode.DIFF)
    val viewMode: StateFlow<TextViewMode> = _viewMode.asStateFlow()

    private val _diffLines = MutableStateFlow<List<String>>(emptyList())
    val diffLines: StateFlow<List<String>> = _diffLines.asStateFlow()

    private val _webContent = MutableStateFlow("")
    val webContent: StateFlow<String> = _webContent.asStateFlow()

    fun getSnapsPaged(siteId: String, contentType: String): Flow<PagingData<Snap>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = true
            ),
            pagingSourceFactory = { snapsRepository.getSnapForPaging(siteId, contentType) }
        ).flow.cachedIn(viewModelScope)
    }

    fun selectSnap(snap: Snap) {
        val currentSelected = _selectedSnaps.value.toMutableList()

        when {
            snap in currentSelected -> {
                // Deselect
                currentSelected.remove(snap)
            }
            currentSelected.size < 2 -> {
                // Add to selection
                currentSelected.add(0, snap) // Add to front (most recent)
            }
            else -> {
                // Replace oldest selection
                currentSelected.removeAt(1)
                currentSelected.add(0, snap)
            }
        }

        _selectedSnaps.value = currentSelected

        // Generate diff if we have 2 selected
        if (currentSelected.size == 2) {
            generateDiff(currentSelected[1].snapId, currentSelected[0].snapId)
        }
    }

    fun setViewMode(mode: TextViewMode) {
        _viewMode.value = mode

        when (mode) {
            TextViewMode.ORIGINAL -> loadSnapContent(_selectedSnaps.value.getOrNull(1)?.snapId)
            TextViewMode.REVISED -> loadSnapContent(_selectedSnaps.value.getOrNull(0)?.snapId)
            TextViewMode.DIFF -> {
                if (_selectedSnaps.value.size == 2) {
                    generateDiff(_selectedSnaps.value[1].snapId, _selectedSnaps.value[0].snapId)
                }
            }
        }
    }

    private fun loadSnapContent(snapId: String?) {
        if (snapId == null) return

        viewModelScope.launch {
            try {
                val content = withContext(Dispatchers.IO) {
                    snapsRepository.getSnapContent(snapId).toString(Charset.defaultCharset())
                }
                _webContent.value = content
            } catch (e: Exception) {
                _webContent.value = ""
            }
        }
    }

    private fun generateDiff(originalId: String, revisedId: String) {
        viewModelScope.launch {
            try {
                val (originalContent, revisedContent) = withContext(Dispatchers.IO) {
                    val original = snapsRepository.getSnapContent(originalId).toString(Charset.defaultCharset())
                    val revised = snapsRepository.getSnapContent(revisedId).toString(Charset.defaultCharset())
                    Pair(original, revised)
                }

                // Generate diff lines
                val diffResult = withContext(Dispatchers.Default) {
                    generateDiffLines(originalContent, revisedContent)
                }

                if (_viewMode.value == TextViewMode.DIFF) {
                    _diffLines.value = diffResult.first
                    _webContent.value = diffResult.second
                }

            } catch (e: Exception) {
                _diffLines.value = emptyList()
                _webContent.value = ""
            }
        }
    }

    private fun generateDiffLines(original: String, revised: String): Pair<List<String>, String> {
        // Simplified diff generation - in real implementation you'd use a proper diff library
        val originalLines = original.lines()
        val revisedLines = revised.lines()

        val diffLines = mutableListOf<String>()
        val htmlBuilder = StringBuilder()

        // Simple line-by-line comparison
        val maxLines = maxOf(originalLines.size, revisedLines.size)

        for (i in 0 until maxLines) {
            val origLine = originalLines.getOrNull(i)
            val revLine = revisedLines.getOrNull(i)

            when {
                origLine == null -> {
                    // Added line
                    diffLines.add("+TEXTADDED$revLine")
                    htmlBuilder.append("<div style='background-color: #e6ffed;'>+ $revLine</div>")
                }
                revLine == null -> {
                    // Removed line
                    diffLines.add("-TEXTREMOVED$origLine")
                    htmlBuilder.append("<div style='background-color: #ffeef0;'>- $origLine</div>")
                }
                origLine != revLine -> {
                    // Changed line
                    diffLines.add("-TEXTREMOVED$origLine")
                    diffLines.add("+TEXTADDED$revLine")
                    htmlBuilder.append("<div style='background-color: #ffeef0;'>- $origLine</div>")
                    htmlBuilder.append("<div style='background-color: #e6ffed;'>+ $revLine</div>")
                }
                else -> {
                    // Unchanged line
                    diffLines.add("  $origLine")
                    htmlBuilder.append("<div>  $origLine</div>")
                }
            }
        }

        return Pair(diffLines, htmlBuilder.toString())
    }

    fun removeSnap(snapId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            snapsRepository.deleteSnap(snapId)
        }
    }
}
