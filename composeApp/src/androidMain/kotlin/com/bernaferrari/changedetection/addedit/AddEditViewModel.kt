package com.bernaferrari.changedetection.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bernaferrari.changedetection.extensions.normalizeUrl
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.browser.BrowserSessionData
import com.bernaferrari.changedetection.WorkerHelper
import com.bernaferrari.changedetection.util.FeatureFlags
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class AddEditUiState(
    val url: String = "",
    val title: String = "",
    val selectedColors: Pair<Int, Int> = Pair(0xFF2196F3.toInt(), 0xFF1976D2.toInt()),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val urlError: String? = null,
    val saveError: String? = null,
    val saveComplete: Boolean = false,
    val browserData: BrowserSessionData? = null,
    val useCookies: Boolean = false,
    val cookieBytes: Long = 0L,
    val hasCapturedCookies: Boolean = false
)

class AddEditViewModel(
    private val siteId: String?
) : ViewModel(), KoinComponent {

    private val sitesRepository: SitesRepository by inject()
    private val workerHelper: WorkerHelper by inject()
    
    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()
    
    init {
        // Load site if editing
        siteId?.let { id ->
            viewModelScope.launch {
                loadSite(id)
            }
        }
    }
    
    private suspend fun loadSite(id: String) {
        _uiState.update { it.copy(isLoading = true) }
        try {
            val site = sitesRepository.getSiteById(id)
            site?.let {
                _uiState.update { state ->
                    state.copy(
                        url = site.url,
                        title = site.title ?: "",
                        selectedColors = site.colors,
                        isLoading = false,
                        browserData = null,
                        useCookies = false,
                        cookieBytes = 0L,
                        hasCapturedCookies = false
                    )
                }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    saveError = "Failed to load site"
                )
            }
        }
    }
    
    fun setUrl(url: String) {
        val normalized = url.normalizeUrl()

        _uiState.update { current ->
            val updatedTitle = if (current.title.isEmpty() && normalized != null) {
                runCatching { java.net.URL(normalized).host.replace("www.", "") }
                    .getOrElse { current.title }
            } else {
                current.title
            }

            current.copy(
                url = url,
                urlError = if (url.isNotEmpty() && normalized == null) {
                    "Please enter a valid URL"
                } else null,
                title = updatedTitle
            )
        }
    }
    
    fun setTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }
    
    fun setColors(colors: Pair<Int, Int>) {
        _uiState.update { it.copy(selectedColors = colors) }
    }
    
    fun setBrowserData(data: BrowserSessionData) {
        if (!FeatureFlags.BROWSER_SESSION) return

        _uiState.update {
            val cookies = data.cookies.orEmpty()
            val newCookieBytes = if (FeatureFlags.COOKIES) {
                cookies.toByteArray().size.toLong()
            } else {
                0L
            }
            val previousBytes = it.cookieBytes
            val resolvedUseCookies = if (FeatureFlags.COOKIES) {
                when {
                    newCookieBytes == 0L -> false
                    previousBytes == 0L -> true
                    else -> it.useCookies
                }
            } else {
                false
            }
            it.copy(
                title = data.title ?: it.title,
                browserData = data,
                useCookies = resolvedUseCookies,
                cookieBytes = newCookieBytes,
                hasCapturedCookies = newCookieBytes > 0 || it.hasCapturedCookies
            )
        }
    }

    fun setUseCookies(enabled: Boolean) {
        if (FeatureFlags.COOKIES) {
            _uiState.update { it.copy(useCookies = enabled) }
        }
    }
    
    
    fun saveSite() {
        val state = _uiState.value
        val normalizedUrl = state.url.normalizeUrl()
        if (normalizedUrl == null) {
            _uiState.update { it.copy(urlError = "Please enter a valid URL") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            
            try {
                val browserData = if (FeatureFlags.BROWSER_SESSION) state.browserData else null

                val resolvedTitle = state.title.ifBlank {
                    try {
                        java.net.URL(normalizedUrl).host.replace("www.", "")
                    } catch (e: Exception) {
                        "Website"
                    }
                }

                val existingSite = siteId?.let { sitesRepository.getSiteById(it) }

                val baseSite = existingSite?.copy(
                    url = normalizedUrl,
                    title = resolvedTitle,
                    colors = state.selectedColors,
                    timestamp = System.currentTimeMillis()
                ) ?: Site(
                    id = java.util.UUID.randomUUID().toString(),
                    url = normalizedUrl,
                    title = resolvedTitle,
                    colors = state.selectedColors,
                    isBrowser = browserData != null,
                    isSyncEnabled = true,
                    isNotificationEnabled = true,
                    timestamp = System.currentTimeMillis(),
                    isSuccessful = true,
                    isRead = false,
                    threshold = 0,
                    notes = "",
                    constraints = com.bernaferrari.changedetection.repo.ConstraintsRequired(
                        false, false, false, false
                    )
                )

                var updatedSite = baseSite

                if (browserData != null) {
                    updatedSite = updatedSite.copy(isBrowser = true)
                }

                val site = updatedSite
                
                sitesRepository.saveSite(site)

                // Trigger initial sync to populate first snapshot
                workerHelper.reloadSite(site)

                _uiState.update { current ->
                    current.copy(isSaving = false, saveComplete = true)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveError = "Failed to save site: ${e.message}"
                    )
                }
            }
        }
    }
}
