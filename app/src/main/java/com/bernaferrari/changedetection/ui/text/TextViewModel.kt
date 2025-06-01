package com.bernaferrari.changedetection.ui.text

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bernaferrari.changedetection.repo.Repository
import com.bernaferrari.changedetection.repo.Snap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

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
    private val repository: Repository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TextUiState())
    val uiState: StateFlow<TextUiState> = _uiState.asStateFlow()

    fun loadSnaps(siteId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val snapsList = repository.getAllSnapsFromSite(siteId)
                    .filter { snap ->
                        // Filter for text-based content types
                        snap.contentType?.let { contentType ->
                            contentType.startsWith("text/") ||
                            contentType == "application/json" ||
                            contentType == "application/xml" ||
                            contentType.contains("html") ||
                            contentType.contains("xml") ||
                            contentType.contains("json")
                        } ?: false
                    }
                    .sortedByDescending { it.timestamp }

                _uiState.value = _uiState.value.copy(
                    snaps = snapsList,
                    currentSnap = snapsList.firstOrNull(),
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load text snapshots"
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
                val diffHtml = withContext(Dispatchers.IO) {
                    generateDiffHtml(currentSnap, previousSnap)
                }
                
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

    private suspend fun generateDiffHtml(currentSnap: Snap, previousSnap: Snap?): String {
        val currentContent = repository.getSnapContent(currentSnap.snapId).toString(Charsets.UTF_8)
        val previousContent = previousSnap?.let { 
            repository.getSnapContent(it.snapId).toString(Charsets.UTF_8) 
        } ?: ""

        return if (previousSnap == null) {
            // No previous content, show current content
            generateSingleContentHtml(currentContent, currentSnap)
        } else {
            // Generate diff HTML
            generateDiffHtmlContent(previousContent, currentContent, previousSnap, currentSnap)
        }
    }

    private fun generateSingleContentHtml(content: String, snap: Snap): String {
        return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Content View</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            margin: 0;
            padding: 16px;
            background-color: #f5f5f5;
            line-height: 1.6;
        }
        .header {
            background: white;
            padding: 16px;
            border-radius: 8px;
            margin-bottom: 16px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        .content {
            background: white;
            padding: 16px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            white-space: pre-wrap;
            overflow-wrap: break-word;
        }
        .timestamp {
            color: #666;
            font-size: 14px;
        }
        .size {
            color: #888;
            font-size: 12px;
            margin-top: 4px;
        }
    </style>
</head>
<body>
    <div class="header">
        <div class="timestamp">${formatTimestamp(snap.timestamp)}</div>
        <div class="size">Size: ${formatFileSize(snap.contentSize)}</div>
    </div>
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
        val diff = generateSimpleDiff(oldContent, newContent)
        
        return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Text Diff</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            margin: 0;
            padding: 16px;
            background-color: #f5f5f5;
            line-height: 1.6;
        }
        .header {
            background: white;
            padding: 16px;
            border-radius: 8px;
            margin-bottom: 16px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        .comparison {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 16px;
            margin-bottom: 16px;
        }
        .snap-info {
            background: white;
            padding: 12px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        .snap-info.old {
            border-left: 4px solid #ff6b6b;
        }
        .snap-info.new {
            border-left: 4px solid #4ecdc4;
        }
        .timestamp {
            font-weight: 600;
            margin-bottom: 4px;
        }
        .size {
            color: #666;
            font-size: 12px;
        }
        .diff-content {
            background: white;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            overflow: hidden;
        }
        .diff-line {
            padding: 4px 16px;
            border-left: 4px solid transparent;
            font-family: 'Monaco', 'Consolas', monospace;
            font-size: 13px;
            white-space: pre-wrap;
            overflow-wrap: break-word;
        }
        .diff-line.removed {
            background-color: #ffe6e6;
            border-left-color: #ff6b6b;
        }
        .diff-line.added {
            background-color: #e6fff6;
            border-left-color: #4ecdc4;
        }
        .diff-line.unchanged {
            background-color: #f8f9fa;
            color: #666;
        }
        .legend {
            display: flex;
            gap: 16px;
            margin-bottom: 16px;
            font-size: 12px;
        }
        .legend-item {
            display: flex;
            align-items: center;
            gap: 8px;
        }
        .legend-color {
            width: 16px;
            height: 16px;
            border-radius: 2px;
        }
    </style>
</head>
<body>
    <div class="header">
        <h2 style="margin: 0 0 16px 0;">Text Comparison</h2>
        <div class="comparison">
            <div class="snap-info old">
                <div class="timestamp">Previous: ${formatTimestamp(oldSnap.timestamp)}</div>
                <div class="size">Size: ${formatFileSize(oldSnap.contentSize)}</div>
            </div>
            <div class="snap-info new">
                <div class="timestamp">Current: ${formatTimestamp(newSnap.timestamp)}</div>
                <div class="size">Size: ${formatFileSize(newSnap.contentSize)}</div>
            </div>
        </div>
        <div class="legend">
            <div class="legend-item">
                <div class="legend-color" style="background: #ffe6e6; border: 1px solid #ff6b6b;"></div>
                <span>Removed</span>
            </div>
            <div class="legend-item">
                <div class="legend-color" style="background: #e6fff6; border: 1px solid #4ecdc4;"></div>
                <span>Added</span>
            </div>
            <div class="legend-item">
                <div class="legend-color" style="background: #f8f9fa; border: 1px solid #ccc;"></div>
                <span>Unchanged</span>
            </div>
        </div>
    </div>
    <div class="diff-content">
        $diff
    </div>
</body>
</html>
        """.trimIndent()
    }

    private fun generateSimpleDiff(oldContent: String, newContent: String): String {
        val oldLines = oldContent.lines()
        val newLines = newContent.lines()
        val result = StringBuilder()

        val maxLines = maxOf(oldLines.size, newLines.size)
        
        for (i in 0 until maxLines) {
            val oldLine = oldLines.getOrNull(i)
            val newLine = newLines.getOrNull(i)
            
            when {
                oldLine == null -> {
                    // Line was added
                    result.append("<div class=\"diff-line added\">+ ${escapeHtml(newLine ?: "")}</div>")
                }
                newLine == null -> {
                    // Line was removed
                    result.append("<div class=\"diff-line removed\">- ${escapeHtml(oldLine)}</div>")
                }
                oldLine == newLine -> {
                    // Line unchanged
                    result.append("<div class=\"diff-line unchanged\">  ${escapeHtml(oldLine)}</div>")
                }
                else -> {
                    // Line changed
                    result.append("<div class=\"diff-line removed\">- ${escapeHtml(oldLine)}</div>")
                    result.append("<div class=\"diff-line added\">+ ${escapeHtml(newLine)}</div>")
                }
            }
        }
        
        return result.toString()
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
                repository.deleteSnap(snapId)
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
        
        private fun formatFileSize(bytes: Long): String {
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
}
