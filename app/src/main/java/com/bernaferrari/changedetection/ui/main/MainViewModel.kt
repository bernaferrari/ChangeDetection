package com.bernaferrari.changedetection.ui.main

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.bernaferrari.changedetection.WorkerHelper
import com.bernaferrari.changedetection.WorkerHelper.Companion.UNIQUEWORK
import com.bernaferrari.changedetection.repo.ContentTypeInfo
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import com.bernaferrari.changedetection.settings.SettingsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@HiltViewModel
class MainViewModel @Inject constructor(
    context: Application,
    private val mSnapsRepository: SnapsRepository,
    private val mSitesRepository: SitesRepository,
    private val workerHelper: WorkerHelper,
    private val dataStore: DataStore<Preferences>
) : AndroidViewModel(context), CoroutineScope {

    internal var shouldSyncWhenAppOpen = true

    val workInfoFlow = workerHelper.getWorkInfoFlow()

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCleared() {
        job.cancel()
        super.onCleared()
    }

    internal fun removeSite(site: Site) = GlobalScope.launch {
        mSnapsRepository.deleteAllSnaps(site.id)
        mSitesRepository.deleteSite(site.id)
    }

    internal fun pruneSite(siteId: String) = GlobalScope.launch {
        mSnapsRepository.pruneSnaps(siteId)
    }

    suspend fun removeSnapsByType(siteId: String, contentType: String) {
        mSnapsRepository.deleteSnapsForSiteIdAndContentType(siteId, contentType)
    }

    internal fun saveSite(site: Site) = GlobalScope.launch {
        mSitesRepository.saveSite(site)
    }

    suspend fun getRecentContentTypes(siteId: String): List<ContentTypeInfo> {
        return mSnapsRepository.getContentTypeInfo(siteId)
    }

    internal fun updateSite(site: Site) = GlobalScope.launch {
        mSitesRepository.updateSite(site)
    }

    // StateFlow for Compose compatibility
    private val _sitesFlow = MutableStateFlow<List<SiteAndLastSnap>>(emptyList())
    val sitesFlow: StateFlow<List<SiteAndLastSnap>> = _sitesFlow.asStateFlow()

    // StateFlow for tracking loading sites
    private val _loadingSites = MutableStateFlow<Set<String>>(emptySet())
    val loadingSites: StateFlow<Set<String>> = _loadingSites.asStateFlow()

    // Turbo mode related flows
    val turboModeState = dataStore.data.map { preferences ->
        val isActive = preferences[SettingsViewModel.TURBO_MODE_KEY] ?: false
        val siteIdsString = preferences[SettingsViewModel.TURBO_SITE_IDS_KEY] ?: ""
        val siteIds = if (siteIdsString.isBlank()) emptyList()
        else siteIdsString.split(",").filter { it.isNotBlank() }
        val startTime = preferences[SettingsViewModel.TURBO_START_TIME_KEY] ?: 0L

        TurboModeState(
            isActive = isActive,
            siteIds = siteIds,
            startTime = startTime
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TurboModeState()
    )

    // Initialize data loading
    init {
        updateItems()

        // Monitor work info changes to update loading states
        launch {
            workInfoFlow.collect { workInfoList ->
                val runningSiteIds = workInfoList
                    .filter { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
                    .flatMap { it.tags }
                    .filter { it != "singleEvent" && it != UNIQUEWORK }
                    .toSet()

                val completedSites = _loadingSites.value - runningSiteIds
                if (completedSites.isNotEmpty()) {
                    _loadingSites.value = runningSiteIds
                    updateItems()
                }
            }
        }
    }

    internal fun updateItems() = launch(Dispatchers.Main) {
        val list = mutableListOf<SiteAndLastSnap>()
        withContext(Dispatchers.IO) {
            mSitesRepository.getSites()
                .mapTo(list) { SiteAndLastSnap(it, getLastSnap(it.id), false) }
        }
        _sitesFlow.value = list
    }

    private suspend fun getLastSnap(siteId: String): Snap? {
        return mSnapsRepository.getMostRecentSnap(siteId)
    }

    suspend fun getAllSites(): List<Site> = mSitesRepository.getSites()

    suspend fun isAlreadyMonitoringSite(id: String, url: String): Boolean =
        mSitesRepository.getSiteByUrl(url) != null || mSitesRepository.getSiteById(id) != null

    internal fun reloadSite(site: Site) {
        // Add to loading sites
        _loadingSites.value = _loadingSites.value + site.id
        workerHelper.reloadSite(site)
        launch {
            mSitesRepository.updateSite(site.copy(timestamp = System.currentTimeMillis()))
            updateItems()
        }
    }

    suspend fun getLastTurboSiteIds(): List<String> {
        val preferences = dataStore.data.first()
        val siteIdsString = preferences[SettingsViewModel.TURBO_SITE_IDS_KEY] ?: ""
        return if (siteIdsString.isBlank()) emptyList()
        else siteIdsString.split(",").filter { it.isNotBlank() }
    }

    fun startTurboMode(siteIds: List<String>) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[SettingsViewModel.TURBO_MODE_KEY] = true
                preferences[SettingsViewModel.TURBO_SITE_IDS_KEY] = siteIds.joinToString(",")
                preferences[SettingsViewModel.TURBO_START_TIME_KEY] = System.currentTimeMillis()
            }
            workerHelper.startTurboMode(siteIds)
        }
    }

    fun stopTurboMode() {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[SettingsViewModel.TURBO_MODE_KEY] = false
                preferences[SettingsViewModel.TURBO_SITE_IDS_KEY] = ""
                preferences[SettingsViewModel.TURBO_START_TIME_KEY] = 0L
            }
            workerHelper.stopTurboMode()
        }
    }
}

data class TurboModeState(
    val isActive: Boolean = false,
    val siteIds: List<String> = emptyList(),
    val startTime: Long = 0L
)
