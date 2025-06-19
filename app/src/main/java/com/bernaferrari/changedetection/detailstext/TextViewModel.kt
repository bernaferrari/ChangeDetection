package com.bernaferrari.changedetection.detailsText

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.core.net.toUri
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
import java.io.File
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

data class TextUiState(
    val snaps: List<Snap> = emptyList(),
    val currentSnap: Snap? = null,
    val previousSnap: Snap? = null,
    val diffHtml: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TextViewModel @Inject constructor(
    private val snapsRepository: SnapsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TextUiState())
    val uiState: StateFlow<TextUiState> = _uiState.asStateFlow()

    private val _diffViewMode = MutableStateFlow(DiffViewMode.VISUAL)
    val diffViewMode: StateFlow<DiffViewMode> = _diffViewMode.asStateFlow()

    private val _visualViewMode = MutableStateFlow(VisualViewMode.OVERLAY)
    val visualViewMode: StateFlow<VisualViewMode> = _visualViewMode.asStateFlow()

    private val _webViewContent = MutableStateFlow(WebViewContent("", ""))
    val webViewContent: StateFlow<WebViewContent> = _webViewContent.asStateFlow()

    // Remove showComparisonDialog, hideComparisonDialog, compareWithSnap functions

    // Update loadSnaps to not auto-load diff
    fun loadSnaps(siteId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val snapsLiveData = snapsRepository.getSnapsFiltered(siteId, "%")
                snapsLiveData.observeForever { snapsList ->
                    val filteredSnaps = snapsList?.filter { snap ->
                        snap.contentType.let { contentType ->
                            contentType.startsWith("text/") ||
                                    contentType == "application/json" ||
                                    contentType == "application/xml" ||
                                    contentType.contains("html") ||
                                    contentType.contains("xml") ||
                                    contentType.contains("json")
                        }
                    }?.sortedByDescending { it.timestamp } ?: emptyList()

                    _uiState.value = _uiState.value.copy(
                        snaps = filteredSnaps,
                        isLoading = false
                    )

                    // Auto-load diff only if we have exactly 2 snaps
                    if (filteredSnaps.size == 2) {
                        loadDiff(filteredSnaps[0], filteredSnaps[1])
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load content history"
                )
            }
        }
    }

    fun loadDiff(currentSnap: Snap, previousSnap: Snap?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                currentSnap = currentSnap,
                previousSnap = previousSnap,
                isLoading = true,
                error = null
            )
            
            try {
                val (diffHtml, currentContent, previousContent) = withContext(Dispatchers.IO) {
                    // Load and optimize HTML content for visual comparison
                    val currentContent = optimizeContentForWebView(
                        snapsRepository.getSnapContent(currentSnap.snapId).toString(Charsets.UTF_8)
                    )
                    val previousContent = previousSnap?.let {
                        optimizeContentForWebView(
                            snapsRepository.getSnapContent(it.snapId).toString(Charsets.UTF_8)
                        )
                    } ?: ""

                    // Generate traditional diff for the diff view mode
                    val diffHtml = if (previousSnap == null) {
                        generateSingleContentHtml(currentContent, currentSnap)
                    } else {
                        generateDiffHtmlContent(
                            previousContent,
                            currentContent,
                            previousSnap,
                            currentSnap
                        )
                    }

                    Triple(diffHtml, currentContent, previousContent)
                }

                // Store the optimized HTML content for visual comparison first
                _webViewContent.value = WebViewContent(
                    previousContent = previousContent,
                    currentContent = currentContent
                )
                
                _uiState.value = _uiState.value.copy(
                    diffHtml = diffHtml,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to generate diff"
                )
            }
        }
    }

    private fun optimizeContentForWebView(content: String): String {
        return if (content.trim().startsWith("<!DOCTYPE html") || content.trim()
                .startsWith("<html")
        ) {
            // Add mobile optimizations to existing HTML
            content.replace(
                "<head>",
                """<head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes">
                <style>
                    body { 
                        font-size: 16px; 
                        line-height: 1.6; 
                        -webkit-text-size-adjust: 100%;
                        -webkit-font-smoothing: antialiased;
                    }
                    img { max-width: 100%; height: auto; }
                    table { width: 100%; overflow-x: auto; display: block; white-space: nowrap; }
                    pre, code { overflow-x: auto; }
                </style>"""
            )
        } else {
            // Wrap plain text in optimized HTML
            generateOptimizedWebViewHtml(content)
        }
    }

    private fun generateOptimizedWebViewHtml(content: String): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes">
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Text', system-ui, sans-serif;
            background: #ffffff;
            color: #1d1d1f;
            line-height: 1.7;
            padding: 24px;
            font-size: 16px;
            -webkit-font-smoothing: antialiased;
            -webkit-text-size-adjust: 100%;
        }
        .content {
            white-space: pre-wrap;
            overflow-wrap: break-word;
            max-width: 100%;
            font-family: 'SF Mono', Monaco, Consolas, 'Courier New', monospace;
            font-size: 14px;
            line-height: 1.6;
            background: #f8f9fa;
            padding: 20px;
            border-radius: 12px;
            border: 1px solid #e9ecef;
            min-height: 200px;
        }
        .content:empty::after {
            content: "No content available";
            color: #8e8e93;
            font-style: italic;
        }
        @media (prefers-color-scheme: dark) {
            body { 
                background: #1c1c1e; 
                color: #ffffff; 
            }
            .content { 
                background: #2c2c2e; 
                border-color: #48484a; 
                color: #ffffff; 
            }
        }
        /* Smooth transitions for overlay mode */
        body {
            transition: opacity 0.3s ease;
        }
    </style>
</head>
<body>
    <div class="content">${escapeHtml(content)}</div>
</body>
</html>
        """.trimIndent()
    }

    private fun generateSingleContentHtml(content: String, snap: Snap): String {
        return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Text', system-ui, sans-serif;
            background: #fafbfc;
            color: #1d1d1f;
            line-height: 1.7;
            padding: 20px;
            font-size: 15px;
        }
        .content {
            white-space: pre-wrap;
            overflow-wrap: break-word;
            max-width: 100%;
            background: white;
            padding: 24px;
            border-radius: 12px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.1);
        }
    </style>
</head>
<body>
    <div class="content">${escapeHtml(content)}</div>
</body>
</html>
        """.trimIndent()
    }

    private fun generateDiffHtmlContent(
        oldContent: String, 
        newContent: String, 
        oldSnap: Snap, 
        newSnap: Snap
    ): String {
        val diff = generateVisualDiff(oldContent, newContent)
        
        return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Text', system-ui, sans-serif;
            background: #fafbfc;
            color: #1d1d1f;
            line-height: 1.6;
            padding: 16px;
            font-size: 14px;
        }
        .diff-container {
            background: white;
            border-radius: 12px;
            overflow: hidden;
            box-shadow: 0 1px 3px rgba(0,0,0,0.1);
        }
        .diff-header {
            background: #f8f9fa;
            padding: 16px;
            border-bottom: 1px solid #e9ecef;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .diff-stats {
            font-size: 12px;
            color: #6c757d;
        }
        .added-count { color: #28a745; font-weight: 600; }
        .removed-count { color: #dc3545; font-weight: 600; }
        .diff-content {
            padding: 12px 0;
        }
        .diff-line {
            padding: 6px 16px;
            font-family: 'SF Mono', Monaco, Consolas, monospace;
            font-size: 13px;
            position: relative;
            margin: 1px 0;
        }
        .diff-line.added {
            background: rgba(40, 167, 69, 0.1);
            border-left: 3px solid #28a745;
        }
        .diff-line.removed {
            background: rgba(220, 53, 69, 0.1);
            border-left: 3px solid #dc3545;
        }
        .diff-line.unchanged {
            background: transparent;
            opacity: 0.6;
        }
        .diff-line:hover {
            opacity: 1;
        }
        .line-prefix {
            display: inline-block;
            width: 20px;
            font-weight: bold;
            user-select: none;
        }
        .added .line-prefix { color: #28a745; }
        .removed .line-prefix { color: #dc3545; }
        .unchanged .line-prefix { color: #adb5bd; }
    </style>
</head>
<body>
    <div class="diff-container">
        <div class="diff-header">
            <div class="diff-stats">
                <span class="added-count">+${diff.addedLines}</span> 
                <span class="removed-count">-${diff.removedLines}</span> 
                <span>${diff.unchangedLines} unchanged</span>
            </div>
        </div>
        <div class="diff-content">
            ${diff.html}
        </div>
    </div>
</body>
</html>
        """.trimIndent()
    }

    private fun generateCleanContentHtml(content: String, title: String): String {
        return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Text', system-ui, sans-serif;
            background: #ffffff;
            color: #1d1d1f;
            line-height: 1.7;
            padding: 24px;
            font-size: 14px;
            height: 100vh;
            overflow-x: hidden;
        }
        .header {
            background: #f8f9fa;
            margin: -24px -24px 20px -24px;
            padding: 16px 24px;
            border-bottom: 1px solid #e9ecef;
            font-weight: 600;
            font-size: 13px;
            color: #495057;
            position: sticky;
            top: 0;
            z-index: 100;
        }
        .content {
            white-space: pre-wrap;
            overflow-wrap: break-word;
            max-width: 100%;
            font-family: 'SF Mono', Monaco, Consolas, monospace;
            font-size: 13px;
            line-height: 1.6;
        }
        .content:empty::after {
            content: "No content";
            color: #adb5bd;
            font-style: italic;
        }
    </style>
</head>
<body>
    <div class="header">${escapeHtml(title)}</div>
    <div class="content">${escapeHtml(content)}</div>
</body>
</html>
        """.trimIndent()
    }

    private fun generateVisualDiff(oldContent: String, newContent: String): DiffResult {
        val oldLines = oldContent.lines()
        val newLines = newContent.lines()
        val result = StringBuilder()

        var addedCount = 0
        var removedCount = 0
        var unchangedCount = 0

        // Enhanced diff algorithm with context
        val maxLines = maxOf(oldLines.size, newLines.size)
        var oldIndex = 0
        var newIndex = 0

        while (oldIndex < oldLines.size || newIndex < newLines.size) {
            val oldLine = oldLines.getOrNull(oldIndex)
            val newLine = newLines.getOrNull(newIndex)
            
            when {
                oldLine == null -> {
                    result.append(
                        "<div class=\"diff-line added\"><span class=\"line-prefix\">+</span>${
                            escapeHtml(
                                newLine ?: ""
                            )
                        }</div>"
                    )
                    addedCount++
                    newIndex++
                }
                newLine == null -> {
                    result.append(
                        "<div class=\"diff-line removed\"><span class=\"line-prefix\">−</span>${
                            escapeHtml(
                                oldLine
                            )
                        }</div>"
                    )
                    removedCount++
                    oldIndex++
                }
                oldLine == newLine -> {
                    result.append(
                        "<div class=\"diff-line unchanged\"><span class=\"line-prefix\"> </span>${
                            escapeHtml(
                                oldLine
                            )
                        }</div>"
                    )
                    unchangedCount++
                    oldIndex++
                    newIndex++
                }
                else -> {
                    result.append(
                        "<div class=\"diff-line removed\"><span class=\"line-prefix\">−</span>${
                            escapeHtml(
                                oldLine
                            )
                        }</div>"
                    )
                    result.append(
                        "<div class=\"diff-line added\"><span class=\"line-prefix\">+</span>${
                            escapeHtml(
                                newLine
                            )
                        }</div>"
                    )
                    addedCount++
                    removedCount++
                    oldIndex++
                    newIndex++
                }
            }
        }

        return DiffResult(result.toString(), addedCount, removedCount, unchangedCount)
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    fun deleteSnap(snapId: String) {
        viewModelScope.launch {
            try {
                snapsRepository.deleteSnap(snapId)
                val currentState = _uiState.value
                val updatedSnaps = currentState.snaps.filter { it.snapId != snapId }
                val newCurrentSnap = if (currentState.currentSnap?.snapId == snapId) {
                    updatedSnaps.firstOrNull()
                } else {
                    currentState.currentSnap
                }
                
                _uiState.value = currentState.copy(
                    snaps = updatedSnaps,
                    currentSnap = newCurrentSnap
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to delete snapshot"
                )
            }
        }
    }

    fun shareSnap(snap: Snap, context: Context) {
        try {
            val file = File(context.filesDir, snap.snapId)
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                context.startActivity(Intent.createChooser(shareIntent, "Share text content"))
            }
        } catch (e: Exception) {
            // Handle error
        }
    }

    fun openInBrowser(url: String, context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            context.startActivity(intent)
        } catch (e: Exception) {
            // Handle error
        }
    }

    companion object {
        fun formatTimestamp(timestamp: Long): String {
            val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            return formatter.format(Date(timestamp))
        }
        
        fun formatFileSize(bytes: Long): String {
            val units = arrayOf("B", "KB", "MB", "GB")
            var size = bytes.toDouble()
            var unitIndex = 0
            
            while (size >= 1024 && unitIndex < units.size - 1) {
                size /= 1024
                unitIndex++
            }
            
            return "%.1f %s".format(size, units[unitIndex])
        }
    }

    fun clearComparison() {
        _uiState.value = _uiState.value.copy(
            currentSnap = null,
            previousSnap = null,
            diffHtml = ""
        )
        _webViewContent.value = WebViewContent("", "")
    }

    fun setDiffViewMode(mode: DiffViewMode) {
        _diffViewMode.value = mode
    }

    fun setVisualViewMode(mode: VisualViewMode) {
        _visualViewMode.value = mode
    }
}

enum class DiffViewMode {
    TEXT, VISUAL
}

enum class VisualViewMode {
    OVERLAY, BEFORE, AFTER
}

data class WebViewContent(
    val previousContent: String,
    val currentContent: String
)

data class DiffResult(
    val html: String,
    val addedLines: Int,
    val removedLines: Int,
    val unchangedLines: Int
)

// Helper data class for the 5-tuple
data class Tuple5<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
