package com.bernaferrari.changedetection.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.bernaferrari.changedetection.repo.ColorGroup
import com.bernaferrari.changedetection.repo.ContentTypeInfo
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class MainUiState(
    val listOfItems: List<SiteAndLastSnap> = emptyList(),
    val listOfColors: List<ColorGroup> = emptyList(),
    val listOfTags: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val workInfoList: List<WorkInfo> = emptyList(),
    val selectedColors: List<ColorGroup> = emptyList(),
    val selectedTags: List<String> = emptyList(),
    val interval: Long = 0
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val snapsRepository: SnapsRepository,
    private val sitesRepository: SitesRepository,
    private val workManager: WorkManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _selectedColors = MutableStateFlow<List<ColorGroup>>(emptyList())
    private val _selectedTags = MutableStateFlow<List<String>>(emptyList())

    var shouldSyncWhenAppOpen = true
    var sortAlphabetically = false

    init {
        fetchData()
        observeWorkManager()
    }

    private fun observeWorkManager() {
        viewModelScope.launch {
            // Observe work manager status
            // Note: This might need to be adapted based on your specific WorkManager setup
        }
    }

    private fun fetchData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val sites = withContext(Dispatchers.IO) {
                    sitesRepository.getSites()
                }
                
                val sitesWithLastSnap = withContext(Dispatchers.IO) {
                    sites.map { site ->
                        val lastSnap = snapsRepository.getMostRecentSnap(site.id)
                        SiteAndLastSnap(site, lastSnap, false)
                    }
                }

                val tags = pullOutTags(sites)
                val colors = sites.groupBy { it.colors }.keys.toList()

                _uiState.value = _uiState.value.copy(
                    listOfItems = sitesWithLastSnap,
                    listOfTags = tags,
                    listOfColors = colors,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun refreshData() {
        fetchData()
    }

    fun removeSite(site: Site) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                snapsRepository.deleteAllSnaps(site.id)
                sitesRepository.deleteSite(site.id)
            }
            fetchData() // Refresh the list
        }
    }

    fun pruneSite(siteId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                snapsRepository.pruneSnaps(siteId)
            }
            fetchData()
        }
    }

    suspend fun removeSnapsByType(siteId: String, contentType: String) {
        withContext(Dispatchers.IO) {
            snapsRepository.deleteSnapsForSiteIdAndContentType(siteId, contentType)
        }
        fetchData()
    }

    fun saveSite(site: Site) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                sitesRepository.saveSite(site)
            }
            fetchData()
        }
    }

    fun updateSite(site: Site) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                sitesRepository.updateSite(site)
            }
            fetchData()
        }
    }

    suspend fun getRecentContentTypes(siteId: String): List<ContentTypeInfo> {
        return withContext(Dispatchers.IO) {
            snapsRepository.getContentTypeInfo(siteId)
        }
    }

    suspend fun getAllSites(): List<Site> {
        return withContext(Dispatchers.IO) {
            sitesRepository.getSites()
        }
    }

    suspend fun isAlreadyMonitoringSite(id: String, url: String): Boolean {
        return withContext(Dispatchers.IO) {
            sitesRepository.getSiteByUrl(url) != null || sitesRepository.getSiteById(id) != null
        }
    }

    fun updateSelectedColors(colors: List<ColorGroup>) {
        _selectedColors.value = colors
        _uiState.value = _uiState.value.copy(selectedColors = colors)
        applyFilters()
    }

    fun updateSelectedTags(tags: List<String>) {
        _selectedTags.value = tags
        _uiState.value = _uiState.value.copy(selectedTags = tags)
        applyFilters()
    }

    private fun applyFilters() {
        viewModelScope.launch {
            val currentItems = _uiState.value.listOfItems
            val selectedColors = _uiState.value.selectedColors
            val selectedTags = _uiState.value.selectedTags

            val filteredItems = currentItems.filter { item ->
                val colorMatch = selectedColors.isEmpty() || item.site.colors in selectedColors
                val tagMatch = selectedTags.isEmpty() || selectedTags.any { tag ->
                    item.site.notes.lowercase().contains(tag.lowercase())
                }
                colorMatch && tagMatch
            }

            _uiState.value = _uiState.value.copy(listOfItems = filteredItems)
        }
    }

    private fun pullOutTags(sites: List<Site>): List<String> {
        return mutableListOf<String>().apply {
            for (site in sites) {
                val splitTags = site.notes.lowercase().split(',')
                splitTags
                    .filter { it.isNotEmpty() }
                    .forEach { tag ->
                        if (!this.contains(tag.trim())) {
                            this.add(tag.trim())
                        }
                    }
            }
            sort()
        }
    }
}
