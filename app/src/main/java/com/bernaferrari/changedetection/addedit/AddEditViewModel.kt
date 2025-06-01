package com.bernaferrari.changedetection.addedit

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bernaferrari.changedetection.WorkerHelper
import com.bernaferrari.changedetection.repo.ColorGroup
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.source.local.SitesDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AddEditUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AddEditViewModel @Inject constructor(
    private val sitesDao: SitesDao,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    fun addSite(
        title: String,
        url: String,
        tags: String,
        useBrowser: Boolean,
        colors: ColorGroup,
        threshold: Int
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val site = Site(
                    title = title.takeIf { it.isNotBlank() },
                    url = url,
                    notes = tags.takeIf { it.isNotBlank() } ?: "",
                    isBrowser = useBrowser,
                    timestamp = System.currentTimeMillis(),
                    colors = colors,
                    threshold = threshold
                )
                
                withContext(Dispatchers.IO) {
                    sitesDao.insertSite(site)
                }
                
                // Trigger initial sync using DataStore-based WorkerHelper
                WorkerHelper.reloadSite(site, dataStore)
                
                _uiState.value = AddEditUiState(isSuccess = true)
                
            } catch (e: Exception) {
                _uiState.value = AddEditUiState(error = e.message)
            }
        }
    }

    fun updateSite(
        site: Site,
        title: String,
        url: String,
        tags: String,
        useBrowser: Boolean,
        colors: ColorGroup,
        threshold: Int
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val updatedSite = site.copy(
                    title = title.takeIf { it.isNotBlank() },
                    url = url,
                    notes = tags.takeIf { it.isNotBlank() } ?: "",
                    isBrowser = useBrowser,
                    colors = colors,
                    threshold = threshold
                )
                
                withContext(Dispatchers.IO) {
                    sitesDao.updateSite(updatedSite)
                }
                
                // Reload if URL changed
                if (site.url != url) {
                    WorkerHelper.reloadSite(updatedSite, dataStore)
                }
                
                _uiState.value = AddEditUiState(isSuccess = true)
                
            } catch (e: Exception) {
                _uiState.value = AddEditUiState(error = e.message)
            }
        }
    }

    fun clearState() {
        _uiState.value = AddEditUiState()
    }
}
