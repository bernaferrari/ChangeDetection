package com.bernaferrari.changedetection.ui.addedit

import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bernaferrari.changedetection.WorkerHelper
import com.bernaferrari.changedetection.extensions.fixUrlIfNecessary
import com.bernaferrari.changedetection.extensions.isValidUrl
import com.bernaferrari.changedetection.repo.ColorGroup
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.source.local.SitesDao
import com.bernaferrari.changedetection.repo.source.local.SnapsDao
import com.bernaferrari.changedetection.util.GradientColors
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.abs

data class AddEditUiState(
    val title: String = "",
    val url: String = "",
    val notes: String = "",
    val selectedColor: ColorGroup = GradientColors.gradients.first(),
    val availableColors: List<ColorGroup> = GradientColors.gradients,
    val isBrowserMode: Boolean = false,
    val threshold: Int = 0,
    val lastDiffBytes: Int = 0,
    val showThresholdDialog: Boolean = false,
    val isLoading: Boolean = false,
    val urlError: String? = null,
    val errorMessage: String? = null,
    val shouldNavigateBack: Boolean = false,
    val canSave: Boolean = false
)

@HiltViewModel
class AddEditViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sitesDao: SitesDao,
    private val snapsDao: SnapsDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    private var currentSite: Site? = null

    init {
        // Try to get URL from clipboard
        val clipboardUrl = getUrlFromClipboard()
        if (clipboardUrl.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(url = clipboardUrl)
        }
        updateCanSave()
    }

    fun loadSite(siteId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                currentSite = withContext(Dispatchers.IO) {
                    sitesDao.getSiteById(siteId)
                }
                
                currentSite?.let { site ->
                    _uiState.value = _uiState.value.copy(
                        title = site.title ?: "",
                        url = site.url,
                        notes = site.notes ?: "",
                        selectedColor = site.colors,
                        isBrowserMode = site.isBrowser,
                        threshold = site.threshold ?: 0,
                        isLoading = false
                    )
                    updateCanSave()
                    
                    // Calculate last diff for threshold dialog
                    calculateLastDiff(siteId)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error loading site: ${e.message}"
                )
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
        updateCanSave()
    }

    fun updateUrl(url: String) {
        val fixedUrl = url.fixUrlIfNecessary()
        val error = if (fixedUrl.isNotBlank() && !fixedUrl.isValidUrl()) {
            "Invalid URL"
        } else null
        
        _uiState.value = _uiState.value.copy(
            url = fixedUrl,
            urlError = error
        )
        updateCanSave()
    }

    fun updateNotes(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    fun updateSelectedColor(color: ColorGroup) {
        _uiState.value = _uiState.value.copy(selectedColor = color)
    }

    fun updateBrowserMode(isBrowser: Boolean) {
        _uiState.value = _uiState.value.copy(isBrowserMode = isBrowser)
    }

    fun updateThreshold(threshold: Int) {
        _uiState.value = _uiState.value.copy(threshold = threshold)
    }

    fun showThresholdDialog() {
        _uiState.value = _uiState.value.copy(showThresholdDialog = true)
    }

    fun hideThresholdDialog() {
        _uiState.value = _uiState.value.copy(showThresholdDialog = false)
    }

    fun saveSite() {
        val state = _uiState.value
        
        if (!state.canSave) return
        
        viewModelScope.launch {
            try {
                val site = if (currentSite != null) {
                    // Update existing site
                    val updatedSite = currentSite!!.copy(
                        title = state.title,
                        url = state.url,
                        notes = state.notes,
                        isBrowser = state.isBrowserMode,
                        colors = state.selectedColor,
                        threshold = state.threshold
                    )
                    
                    withContext(Dispatchers.IO) {
                        sitesDao.updateSite(updatedSite)
                    }
                    
                    // Reload if URL changed
                    if (state.url != currentSite!!.url) {
                        WorkerHelper.reloadSite(updatedSite, context)
                    }
                    
                    updatedSite
                } else {
                    // Create new site
                    val newSite = Site(
                        title = state.title,
                        url = state.url,
                        tags = state.notes,
                        isBrowser = state.isBrowserMode,
                        timestamp = System.currentTimeMillis(),
                        colors = state.selectedColor
                    )
                    
                    withContext(Dispatchers.IO) {
                        sitesDao.insertSite(newSite)
                    }
                    
                    WorkerHelper.reloadSite(newSite, context)
                    newSite
                }
                
                _uiState.value = _uiState.value.copy(shouldNavigateBack = true)
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error saving site: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun updateCanSave() {
        val state = _uiState.value
        val canSave = state.url.isNotBlank() && 
                     state.url.isValidUrl() && 
                     state.urlError == null &&
                     !state.isLoading
        
        _uiState.value = state.copy(canSave = canSave)
    }

    private fun calculateLastDiff(siteId: String) {
        viewModelScope.launch {
            try {
                val items = withContext(Dispatchers.IO) {
                    snapsDao.getLastNSnapsForSiteId(siteId, 2)
                }
                
                if (items != null && items.size >= 2) {
                    val diff = abs(items[0].contentSize - items[1].contentSize)
                    _uiState.value = _uiState.value.copy(lastDiffBytes = diff)
                }
            } catch (e: Exception) {
                // Ignore errors in diff calculation
            }
        }
    }

    private fun getUrlFromClipboard(): String {
        return try {
            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clipData = clipboardManager?.primaryClip?.getItemAt(0)
            val text = clipData?.text?.toString()
            
            if (!text.isNullOrBlank() && text.isValidUrl()) {
                text
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }
}
