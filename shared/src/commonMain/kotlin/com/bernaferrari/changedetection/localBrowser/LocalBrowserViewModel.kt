/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.localBrowser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import com.bernaferrari.changedetection.util.decodeByteArray
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

    fun reset() {
        fetchJob?.cancel()
        _state.value = BrowserState()
    }

    fun fetchData(snapId: String) {
        // Cancel any in-flight load so a stale response can never overwrite
        // newer state (rapid snapshot switching, or reset() during a load).
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            try {
                val (snap, bytes) = withContext(Dispatchers.Default) {
                    snapsRepository.getSnapById(snapId) to snapsRepository.getSnapContent(snapId)
                }
                val (mimeType, charsetName) = resolveEncoding(snap)
                val decodedContent = if (mimeType.startsWith("text/", ignoreCase = true)) {
                    decodeByteArray(bytes, charsetName)
                } else {
                    ""
                }

                _state.value =
                    BrowserState(
                        content = decodedContent,
                        bytes = bytes,
                        isLoading = false,
                        mimeType = mimeType,
                        charset = charsetName,
                        timestamp = snap?.timestamp,
                        contentSize = snap?.contentSize ?: bytes.size,
                    )
            } catch (e: Exception) {
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
