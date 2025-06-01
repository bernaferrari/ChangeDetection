package com.bernaferrari.changedetection.ui.webview

import androidx.lifecycle.ViewModel
import com.bernaferrari.changedetection.extensions.fixUrlIfNecessary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class WebViewUiState(
    val currentUrl: String = "",
    val urlInput: String = "",
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val shouldLoadUrl: Boolean = false,
    val shouldGoBack: Boolean = false,
    val shouldNavigateBack: Boolean = false
)

@HiltViewModel
class WebViewViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(WebViewUiState())
    val uiState: StateFlow<WebViewUiState> = _uiState.asStateFlow()

    fun loadUrl(url: String) {
        val fixedUrl = url.fixUrlIfNecessary()
        _uiState.value = _uiState.value.copy(
            currentUrl = fixedUrl,
            urlInput = fixedUrl,
            shouldLoadUrl = true
        )
    }

    fun updateUrlInput(input: String) {
        _uiState.value = _uiState.value.copy(urlInput = input)
    }

    fun navigateToUrl() {
        val fixedUrl = _uiState.value.urlInput.fixUrlIfNecessary()
        _uiState.value = _uiState.value.copy(
            currentUrl = fixedUrl,
            shouldLoadUrl = true
        )
    }

    fun goBack() {
        _uiState.value = _uiState.value.copy(shouldGoBack = true)
    }

    fun navigateBack() {
        _uiState.value = _uiState.value.copy(shouldNavigateBack = true)
    }

    fun onPageStarted(url: String, canGoBack: Boolean) {
        _uiState.value = _uiState.value.copy(
            currentUrl = url,
            urlInput = url,
            isLoading = true,
            canGoBack = canGoBack
        )
    }

    fun onPageFinished(url: String, canGoBack: Boolean) {
        _uiState.value = _uiState.value.copy(
            currentUrl = url,
            urlInput = url,
            isLoading = false,
            canGoBack = canGoBack
        )
    }

    fun onUrlLoaded() {
        _uiState.value = _uiState.value.copy(shouldLoadUrl = false)
    }

    fun onBackExecuted() {
        _uiState.value = _uiState.value.copy(shouldGoBack = false)
    }
}
