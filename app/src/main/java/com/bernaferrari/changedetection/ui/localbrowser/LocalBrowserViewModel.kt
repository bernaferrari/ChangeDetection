package com.bernaferrari.changedetection.ui.localbrowser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bernaferrari.changedetection.extensions.replaceRelativePathWithAbsolute
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

data class LocalBrowserUiState(
    val content: String = "",
    val processedContent: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LocalBrowserViewModel @Inject constructor(
    private val snapsRepository: SnapsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocalBrowserUiState())
    val uiState: StateFlow<LocalBrowserUiState> = _uiState.asStateFlow()

    fun loadContent(snapId: String, url: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )
            
            try {
                val content = withContext(Dispatchers.IO) {
                    snapsRepository.getSnapContent(snapId).toString(Charset.defaultCharset())
                }
                
                val processedContent = content.replaceRelativePathWithAbsolute(url)
                
                _uiState.value = _uiState.value.copy(
                    content = content,
                    processedContent = processedContent,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load snapshot: ${e.message}"
                )
            }
        }
    }
}
