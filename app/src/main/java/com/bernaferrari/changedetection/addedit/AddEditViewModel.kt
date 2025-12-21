package com.bernaferrari.changedetection.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bernaferrari.changedetection.extensions.normalizeUrl
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.monitoring.ZapRule
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.browser.BrowserSessionData
import com.bernaferrari.changedetection.browser.SelectedElement
import com.bernaferrari.changedetection.browser.ZappedElement
import com.bernaferrari.changedetection.WorkerHelper
import com.bernaferrari.changedetection.repo.withCookies
import com.bernaferrari.changedetection.repo.withSmartMonitoring
import com.bernaferrari.changedetection.repo.withZapRules
import com.bernaferrari.changedetection.repo.zapRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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

@HiltViewModel
class AddEditViewModel @Inject constructor(
    private val sitesRepository: SitesRepository,
    private val workerHelper: WorkerHelper,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val siteId: String? = savedStateHandle.get<String>("siteId")
    
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
                    val selectors = site.cssSelector.split(',')
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .mapIndexed { index, selector ->
                            SelectedElement(
                                selector = selector,
                                description = selector,
                                timestamp = index.toLong()
                            )
                        }

                    val zapped = site.zapRules.mapIndexed { index, rule ->
                        when (rule) {
                            is ZapRule.CssSelector -> ZappedElement(
                                selector = rule.selector,
                                description = rule.selector,
                                timestamp = index.toLong()
                            )

                            is ZapRule.ElementType -> ZappedElement(
                                selector = rule.tagName,
                                description = rule.tagName,
                                timestamp = index.toLong()
                            )

                            is ZapRule.AttributePattern -> ZappedElement(
                                selector = "[${rule.attribute}~=${rule.pattern}]",
                                description = rule.pattern,
                                timestamp = index.toLong()
                            )

                            is ZapRule.TextPattern -> ZappedElement(
                                selector = rule.pattern,
                                description = rule.pattern,
                                timestamp = index.toLong()
                            )

                            is ZapRule.XPath -> ZappedElement(
                                selector = rule.xpath,
                                description = rule.xpath,
                                timestamp = index.toLong()
                            )
                        }
                    }

                    state.copy(
                        url = site.url,
                        title = site.title ?: "",
                        selectedColors = site.colors,
                        isLoading = false,
                        browserData = BrowserSessionData(
                            url = site.url,
                            title = site.title ?: "",
                            cookies = site.cookieData,
                            userAgent = site.userAgent,
                            selectedElements = selectors,
                            zappedElements = zapped
                        ),
                        useCookies = site.cookieData.isNotBlank(),
                        cookieBytes = site.cookieData?.toByteArray()?.size?.toLong() ?: 0L,
                        hasCapturedCookies = site.cookieData.isNotBlank()
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
        _uiState.update { 
            val cookies = data.cookies.orEmpty()
            val newCookieBytes = cookies.toByteArray().size.toLong()
            val previousBytes = it.cookieBytes
            val resolvedUseCookies = when {
                newCookieBytes == 0L -> false
                previousBytes == 0L -> true
                else -> it.useCookies
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
        _uiState.update { it.copy(useCookies = enabled) }
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
                val browserData = state.browserData

                val selectedSelectors: List<String> = browserData?.selectedElements?.map { it.selector }.orEmpty()
                val joinedCssSelector: String = selectedSelectors.joinToString(separator = ",")
                val hasElementMonitoring = selectedSelectors.isNotEmpty()
                val hasMultipleElements = selectedSelectors.size > 1

                val zapRules = browserData?.zappedElements
                    ?.map { element -> ZapRule.CssSelector(element.selector) }
                    .orEmpty()

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
                    cookieData = "",
                    userAgent = "",
                    zapRulesJson = "",
                    cssSelector = "",
                    elementMonitoring = false,
                    multipleElementMonitoring = false,
                    smartMonitoringEnabled = false,
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
                    updatedSite = updatedSite.copy(
                        cssSelector = joinedCssSelector,
                        elementMonitoring = hasElementMonitoring,
                        multipleElementMonitoring = hasMultipleElements,
                        userAgent = browserData.userAgent,
                        isBrowser = true
                    )

                    val cookieMap = parseCookieHeader(browserData.cookies)
                    updatedSite = if (state.useCookies && cookieMap.isNotEmpty()) {
                        updatedSite.withCookies(cookieMap)
                    } else {
                        updatedSite.copy(cookieData = "")
                    }

                    updatedSite = updatedSite.withZapRules(zapRules)
                    updatedSite = updatedSite.withSmartMonitoring(enabled = hasElementMonitoring || zapRules.isNotEmpty())
                } else if (!state.useCookies) {
                    updatedSite = updatedSite.copy(cookieData = "")
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

private fun parseCookieHeader(header: String): Map<String, String> {
    if (header.isBlank()) return emptyMap()

    val tokens = header.split(';')
    val map = mutableMapOf<String, String>()

    tokens.forEach { token ->
        val parts = token.trim().split('=', limit = 2)
        val name = parts.getOrNull(0)?.trim().orEmpty()
        if (name.isNotEmpty()) {
            val value = parts.getOrNull(1)?.trim() ?: ""
            map[name] = value
        }
    }

    return map
}
