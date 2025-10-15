package com.bernaferrari.changedetection.localBrowser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val isLoading: Boolean = false
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
                val content = withContext(Dispatchers.IO) {
                    snapsRepository.getSnapContent(snapId).toString(Charset.defaultCharset())
                }

                _state.value = BrowserState(
                    content = content,
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = BrowserState(
                    content = "",
                    isLoading = false
                )
            }
        }
    }
}
