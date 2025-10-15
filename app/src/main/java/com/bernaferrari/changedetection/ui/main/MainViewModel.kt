package com.bernaferrari.changedetection.ui.main

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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
import com.bernaferrari.changedetection.ui.main.QuickMonitorState
import com.orhanobut.logger.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@HiltViewModel
class MainViewModel @Inject constructor(
    private val context: Application,
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

    internal fun removeSite(site: Site) = viewModelScope.launch {
        mSnapsRepository.deleteAllSnaps(site.id)
        mSitesRepository.deleteSite(site.id)
    }

    internal fun pruneSite(siteId: String) = viewModelScope.launch {
        mSnapsRepository.pruneSnaps(siteId)
    }

    suspend fun removeSnapsByType(siteId: String, contentType: String) {
        mSnapsRepository.deleteSnapsForSiteIdAndContentType(siteId, contentType)
    }

    internal fun saveSite(site: Site) = viewModelScope.launch {
        mSitesRepository.saveSite(site)
    }

    suspend fun getRecentContentTypes(siteId: String): List<ContentTypeInfo> {
        return mSnapsRepository.getContentTypeInfo(siteId)
    }

    internal fun updateSite(site: Site) = viewModelScope.launch {
        mSitesRepository.updateSite(site)
    }

    // StateFlow for tracking loading sites
    private val _loadingSites = MutableStateFlow<Set<String>>(emptySet())
    val loadingSites: StateFlow<Set<String>> = _loadingSites.asStateFlow()

    // Optimized StateFlow for Compose compatibility - combines sites and snaps efficiently
    val sitesFlow: StateFlow<List<SiteAndLastSnap>> = combine(
        mSitesRepository.getDataWithChanges(),
        _loadingSites.asStateFlow()
    ) { sites, loadingSiteIds ->
        coroutineScope {
            sites.map { site ->
                async(Dispatchers.IO) {
                    val snap = runCatching { mSnapsRepository.getMostRecentSnap(site.id) }.getOrNull()

                    SiteAndLastSnap(
                        site = site,
                        snap = snap,
                        isSyncing = loadingSiteIds.contains(site.id)
                    )
                }
            }.awaitAll()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Quick Monitor state
    private val QUICK_MONITOR_ACTIVE_KEY = booleanPreferencesKey("quick_monitor_active")
    private val QUICK_MONITOR_SITE_IDS_KEY = stringPreferencesKey("quick_monitor_site_ids")
    private val QUICK_MONITOR_START_TIME_KEY = longPreferencesKey("quick_monitor_start_time")
    private val QUICK_MONITOR_DEADLINE_TIME_KEY = longPreferencesKey("quick_monitor_deadline_time")
    private val QUICK_MONITOR_INTERVAL_KEY = intPreferencesKey("quick_monitor_interval_seconds")
    
    val quickMonitorState = dataStore.data.map { preferences ->
        val isActive = preferences[QUICK_MONITOR_ACTIVE_KEY] ?: false
        val siteIdsString = preferences[QUICK_MONITOR_SITE_IDS_KEY] ?: ""
        val siteIds = if (siteIdsString.isBlank()) emptySet()
        else siteIdsString.split(",").filter { it.isNotBlank() }.toSet()
        val startTime = preferences[QUICK_MONITOR_START_TIME_KEY] ?: 0L

        QuickMonitorState(
            isActive = isActive,
            siteIds = siteIds,
            startTime = startTime,
            deadlineTime = preferences[QUICK_MONITOR_DEADLINE_TIME_KEY] ?: 0L,
            intervalSeconds = preferences[QUICK_MONITOR_INTERVAL_KEY] ?: 15
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = QuickMonitorState()
    )

    // Initialize monitoring
    init {
        // Monitor work info changes to update loading states
        launch {
            workInfoFlow.collect { workInfoList ->
                val runningSiteIds = workInfoList
                    .filter { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
                    .flatMap { it.tags }
                    .filter { it != "singleEvent" && it != UNIQUEWORK }
                    .toSet()

                // Update loading sites state
                _loadingSites.value = runningSiteIds
            }
        }
    }


    private suspend fun getLastSnap(siteId: String): Snap? {
        return mSnapsRepository.getMostRecentSnap(siteId)
    }

    suspend fun getAllSites(): List<Site> = mSitesRepository.getSites()

    suspend fun isAlreadyMonitoringSite(id: String, url: String): Boolean =
        mSitesRepository.getSiteByUrl(url) != null || mSitesRepository.getSiteById(id) != null

    internal fun reloadSite(site: Site) {
        viewModelScope.launch {
            scheduleSiteReload(site)
        }
    }

    fun refreshAllSites() {
        viewModelScope.launch {
            val sites = runCatching { mSitesRepository.getSites() }.getOrDefault(emptyList())
            if (sites.isEmpty()) return@launch

            sites.forEach { site ->
                scheduleSiteReload(site)
            }
        }
    }

    private suspend fun scheduleSiteReload(site: Site) {
        try {
            if (_loadingSites.value.contains(site.id)) {
                return
            }

            _loadingSites.update { it + site.id }

            // Update site timestamp to indicate sync started
            mSitesRepository.updateSite(site.copy(timestamp = System.currentTimeMillis()))

            // Start sync work
            workerHelper.reloadSite(site)

            // Monitor work completion without blocking caller
            monitorSiteWorkCompletionAsync(site.id)

        } catch (e: Exception) {
            // Remove from loading state on error
            _loadingSites.update { it - site.id }
        }
    }

    private fun monitorSiteWorkCompletionAsync(siteId: String) {
        viewModelScope.launch {
            monitorSiteWorkCompletion(siteId)
        }
    }
    
    private suspend fun monitorSiteWorkCompletion(siteId: String) {
        try {
            // Wait a bit for work to start, then monitor completion
            delay(1000)
            
            // Observe work status until it finishes
            workInfoFlow
                .map { workInfoList ->
                    workInfoList.any { workInfo ->
                        workInfo.state in listOf(WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED) &&
                            workInfo.tags.contains(siteId)
                    }
                }
                .first { isStillRunning -> !isStillRunning }
        } catch (e: Exception) {
            Logger.e("Failed to monitor work for site $siteId: ${e.message}")
        } finally {
            _loadingSites.update { it - siteId }
        }
    }

    // Quick Monitor functions - simple, fast monitoring
    fun startQuickMonitor(siteIds: Set<String>, intervalSeconds: Int = 15) {
        viewModelScope.launch {
            Logger.d("🚀 MainViewModel: Starting Quick Monitor for ${siteIds.size} sites: $siteIds")
            
            dataStore.edit { preferences ->
                preferences[QUICK_MONITOR_ACTIVE_KEY] = true
                preferences[QUICK_MONITOR_SITE_IDS_KEY] = siteIds.joinToString(",")
                preferences[QUICK_MONITOR_START_TIME_KEY] = System.currentTimeMillis()
                preferences[QUICK_MONITOR_INTERVAL_KEY] = intervalSeconds
            }
            Logger.d("💾 MainViewModel: Quick Monitor state saved to DataStore")
            
            // Start quick monitoring with selected interval
            workerHelper.startQuickMonitor(siteIds.toList(), intervalSeconds = intervalSeconds)
            Logger.d("⚡ MainViewModel: WorkerHelper.startQuickMonitor called")
        }
    }

    fun stopQuickMonitor() {
        viewModelScope.launch {
            Logger.d("⏹️ MainViewModel: Stopping Quick Monitor")
            
            dataStore.edit { preferences ->
                preferences[QUICK_MONITOR_ACTIVE_KEY] = false
                preferences[QUICK_MONITOR_SITE_IDS_KEY] = ""
                preferences[QUICK_MONITOR_START_TIME_KEY] = 0L
                preferences[QUICK_MONITOR_INTERVAL_KEY] = 15
            }
            Logger.d("💾 MainViewModel: Quick Monitor state cleared from DataStore")
            
            workerHelper.stopQuickMonitor()
            Logger.d("⏹️ MainViewModel: WorkerHelper.stopQuickMonitor called")
        }
    }
}
