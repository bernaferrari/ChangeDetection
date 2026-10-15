package com.bernaferrari.changedetection.localBrowser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import com.bernaferrari.changedetection.util.decodeByteArray
import com.bernaferrari.changedetection.util.isTextContentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

data class BrowserState(
    val content: String = "",
    val bytes: ByteArray? = null,
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val mimeType: String = "text/html",
    val charset: String = "UTF-8",
    val timestamp: Long? = null,
    val contentSize: Int = 0,
)

@KoinViewModel
class LocalBrowserViewModel(
    private val snapsRepository: SnapsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BrowserState())
    val state: StateFlow<BrowserState> = _state.asStateFlow()

    private var fetchJob: Job? = null
    private var fetchGeneration = 0L

    fun reset() {
        ++fetchGeneration
        fetchJob?.cancel()
        fetchJob = null
        _state.value = BrowserState()
    }

    fun fetchData(snapId: String) {
        // IO can fail after cancellation. Only the current request owns screen state.
        val generation = ++fetchGeneration
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            _state.value = BrowserState(isLoading = true)

            try {
                val loaded = withContext(Dispatchers.Default) {
                    val snap = snapsRepository.getSnapById(snapId)
                    val bytes = snapsRepository.getSnapContent(snapId)
                    val (mimeType, charsetName) = resolveEncoding(snap)
                    val decodedContent = if (mimeType.isTextContentType()) decodeByteArray(bytes, charsetName) else ""
                    BrowserState(
                        content = decodedContent,
                        bytes = bytes,
                        isLoading = false,
                        mimeType = mimeType,
                        charset = charsetName,
                        timestamp = snap?.timestamp,
                        contentSize = snap?.contentSize ?: bytes.size,
                    )
                }
                if (generation == fetchGeneration) _state.value = loaded
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                if (generation != fetchGeneration) return@launch
                _state.value =
                    BrowserState(
                        content = "",
                        isLoading = false,
                        hasError = true,
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

        val mimeType =
            snap.contentType
                .substringBefore(';')
                .ifBlank { fallbackMime }

        val charsetFromType =
            snap.contentType
                .split(';')
                .drop(1)
                .firstOrNull { it.trim().startsWith("charset", ignoreCase = true) }
                ?.substringAfter('=', fallbackCharset)
                ?.trim()
                ?.trim('"', '\'')
                ?.ifBlank { null }

        val charset =
            snap.contentCharset
                .takeIf { it.isNotBlank() }
                ?: charsetFromType
                ?: fallbackCharset

        return mimeType to charset
    }
}
