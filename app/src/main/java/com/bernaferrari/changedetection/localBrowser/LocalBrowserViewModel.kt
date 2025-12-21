package com.bernaferrari.changedetection.localBrowser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.charset.Charset
import javax.inject.Inject

data class BrowserState(
    val content: String = "",
    val isLoading: Boolean = false,
    val mimeType: String = "text/html",
    val charset: String = "UTF-8"
)

@HiltViewModel
class LocalBrowserViewModel @Inject constructor(
    private val snapsRepository: SnapsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BrowserState())
    val state: StateFlow<BrowserState> = _state.asStateFlow()

    fun reset() {
        _state.value = BrowserState()
    }

    fun fetchData(snapId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            try {
                val snapshotInfo = withContext(Dispatchers.IO) {
                    val snap = snapsRepository.getSnapById(snapId)
                    val bytes = snapsRepository.getSnapContent(snapId)
                    Triple(snap, bytes, resolveEncoding(snap))
                }

                val (snap, bytes, encoding) = snapshotInfo
                val (mimeType, charsetName) = encoding

                val decodedContent = runCatching {
                    String(bytes, Charset.forName(charsetName))
                }.getOrElse {
                    String(bytes, Charsets.UTF_8)
                }

                _state.value = BrowserState(
                    content = decodedContent,
                    isLoading = false,
                    mimeType = mimeType,
                    charset = charsetName
                )
            } catch (e: Exception) {
                _state.value = BrowserState(
                    content = "",
                    isLoading = false
                )
            }
        }
    }

    private fun resolveEncoding(snap: Snap?): Pair<String, String> {
        val fallbackMime = "text/html"
        val fallbackCharset = "UTF-8"

        if (snap == null) {
            return fallbackMime to fallbackCharset
        }

        val mimeType = snap.contentType
            .substringBefore(';')
            .ifBlank { fallbackMime }

        val charsetFromType = snap.contentType
            .split(';')
            .drop(1)
            .firstOrNull { it.trim().startsWith("charset", ignoreCase = true) }
            ?.substringAfter('=', fallbackCharset)
            ?.trim()
            ?.trim('"', '\'')
            ?.ifBlank { null }

        val charset = snap.contentCharset
            .takeIf { it.isNotBlank() }
            ?: charsetFromType
            ?: fallbackCharset

        return mimeType to charset
    }
}
