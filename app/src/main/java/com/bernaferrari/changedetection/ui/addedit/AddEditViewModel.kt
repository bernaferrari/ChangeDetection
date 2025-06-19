package com.bernaferrari.changedetection.ui.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bernaferrari.changedetection.repo.ConstraintsRequired
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.source.SitesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class AddEditUiState(
    val site: Site = Site(
        id = "",
        title = "",
        url = "",
        timestamp = 0L,
        isSuccessful = true,
        isBrowser = false,
        threshold = 0,
        isRead = false,
        isSyncEnabled = true,
        isNotificationEnabled = true,
        notes = "",
        colors = Pair(0xFF2196F3.toInt(), 0xFF1976D2.toInt()),
        constraints = ConstraintsRequired(
            wifi = true,
            charging = false,
            batteryNotLow = false,
            deviceIdle = false,
        )
    ),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val urlError: String? = null,
    val saveSuccess: Boolean = false,
    val elementMonitoring: Boolean = false,
    val selectedElement: ElementInfo? = null
)

@HiltViewModel
class AddEditViewModel @Inject constructor(
    private val sitesRepository: SitesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val siteId: String = savedStateHandle.get<String>("siteId") ?: ""

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    init {
        if (siteId.isNotEmpty()) {
            loadSite(siteId)
        } else {
            // Initialize with a new site
            _uiState.value = _uiState.value.copy(
                site = _uiState.value.site.copy(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    private fun loadSite(siteId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val site = sitesRepository.getSiteById(siteId)
                if (site != null) {
                    _uiState.value = _uiState.value.copy(
                        site = site,
                        elementMonitoring = site.elementMonitoring,
                        selectedElement = if (site.cssSelector.isNotEmpty()) {
                            ElementInfo(
                                tagName = extractTagFromSelector(site.cssSelector),
                                cssSelector = site.cssSelector,
                                xpath = site.elementXPath,
                                text = "Selected element"
                            )
                        } else null,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        urlError = "Site not found"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    urlError = e.message ?: "Failed to load site"
                )
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(
            site = _uiState.value.site.copy(title = title)
        )
    }

    fun updateUrl(url: String) {
        _uiState.value = _uiState.value.copy(
            site = _uiState.value.site.copy(url = url),
            urlError = null
        )
    }

    fun updateNotes(notes: String) {
        _uiState.value = _uiState.value.copy(
            site = _uiState.value.site.copy(notes = notes)
        )
    }

    fun updateThreshold(threshold: Int) {
        _uiState.value = _uiState.value.copy(
            site = _uiState.value.site.copy(threshold = threshold)
        )
    }

    fun updateSyncEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            site = _uiState.value.site.copy(isSyncEnabled = enabled)
        )
    }

    fun updateNotificationEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            site = _uiState.value.site.copy(isNotificationEnabled = enabled)
        )
    }

    fun updateElementMonitoring(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            elementMonitoring = enabled,
            site = _uiState.value.site.copy(elementMonitoring = enabled)
        )
    }

    fun setSelectedElement(cssSelector: String, xpath: String, elementText: String) {
        val elementInfo = ElementInfo(
            tagName = extractTagFromSelector(cssSelector),
            cssSelector = cssSelector,
            xpath = xpath,
            text = elementText
        )

        _uiState.value = _uiState.value.copy(
            selectedElement = elementInfo,
            elementMonitoring = true,
            site = _uiState.value.site.copy(
                cssSelector = cssSelector,
                elementXPath = xpath,
                elementMonitoring = true
            )
        )
    }

    fun clearElementSelection() {
        _uiState.value = _uiState.value.copy(
            selectedElement = null,
            elementMonitoring = false,
            site = _uiState.value.site.copy(
                cssSelector = "",
                elementXPath = "",
                elementMonitoring = false
            )
        )
    }

    private fun extractTagFromSelector(cssSelector: String): String {
        return cssSelector.split(" ").lastOrNull()?.split(".")?.firstOrNull()?.split("#")
            ?.firstOrNull() ?: "div"
    }

    fun saveSite() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, urlError = null)

            try {
                val currentSite = _uiState.value.site

                // Validate URL
                if (currentSite.url.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        urlError = "URL is required"
                    )
                    return@launch
                }

                if (!isValidUrl(currentSite.url)) {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        urlError = "Please enter a valid URL"
                    )
                    return@launch
                }

                // Update timestamp
                val siteToSave = currentSite.copy(
                    timestamp = System.currentTimeMillis(),
                    title = if (currentSite.title?.isBlank() == true) generateTitleFromUrl(
                        currentSite.url
                    ) else currentSite.title
                )

                if (siteId.isNotEmpty()) {
                    sitesRepository.updateSite(siteToSave)
                } else {
                    sitesRepository.saveSite(siteToSave)
                }

                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveSuccess = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    urlError = e.message ?: "Failed to save site"
                )
            }
        }
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            val normalizedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else {
                url
            }
            java.net.URL(normalizedUrl)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun generateTitleFromUrl(url: String): String {
        return try {
            val normalizedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else {
                url
            }
            java.net.URL(normalizedUrl).host.removePrefix("www.")
        } catch (e: Exception) {
            url
        }
    }

    fun clearSaveSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }
}

data class ElementInfo(
    val tagName: String,
    val cssSelector: String,
    val xpath: String,
    val text: String,
    val className: String = "",
    val id: String = ""
)