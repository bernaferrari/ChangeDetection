package com.bernaferrari.changedetection.ui.main

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bernaferrari.changedetection.background.MonitoringManager
import com.bernaferrari.changedetection.background.SyncState
import com.bernaferrari.changedetection.repo.ContentTypeInfo
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import io.github.aakira.napier.Napier
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

class MainViewModel(
    private val mSnapsRepository: SnapsRepository,
    private val mSitesRepository: SitesRepository,
    private val monitoringManager: MonitoringManager,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    internal var shouldSyncWhenAppOpen = true

    val syncStatusFlow = monitoringManager.getSyncStatusFlow()

    fun removeSite(site: Site) = viewModelScope.launch {
        mSnapsRepository.deleteAllSnaps(site.id)
        mSitesRepository.deleteSite(site.id)
    }

    fun pruneSite(siteId: String) = viewModelScope.launch {
        mSnapsRepository.pruneSnaps(siteId)
    }

    suspend fun removeSnapsByType(siteId: String, contentType: String) {
        mSnapsRepository.deleteSnapsForSiteIdAndContentType(siteId, contentType)
    }

    fun saveSite(site: Site) = viewModelScope.launch {
        mSitesRepository.saveSite(site)
    }

    suspend fun getRecentContentTypes(siteId: String): List<ContentTypeInfo> {
        return mSnapsRepository.getContentTypeInfo(siteId)
    }

    fun updateSite(site: Site) = viewModelScope.launch {
        mSitesRepository.updateSite(site)
    }

    // StateFlow for tracking loading sites
    private val _loadingSites = MutableStateFlow<Set<String>>(emptySet())
    val loadingSites: StateFlow<Set<String>> = _loadingSites.asStateFlow()

    /**
     * OPTIMIZATION: Efficiently combines sites with their most recent snapshots.
     */
    val sitesFlow: StateFlow<List<SiteAndLastSnap>?> = combine(
        mSitesRepository.getDataWithChanges(),
        _loadingSites.asStateFlow()
    ) { sites, loadingSiteIds ->
        // Load all snapshots in a single optimized query
        val snapsBySite = runCatching { mSnapsRepository.getAllMostRecentSnaps() }.getOrNull() ?: emptyMap()

        // Map sites to SiteAndLastSnap using the pre-loaded snapshots (O(1) lookup)
        sites.map { site ->
            SiteAndLastSnap(
                site = site,
                snap = snapsBySite[site.id],  // Fast map lookup instead of separate query
                isSyncing = loadingSiteIds.contains(site.id)
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
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
        viewModelScope.launch {
            syncStatusFlow.collect { syncList ->
                val runningSiteIds = syncList
                    .filter { it.state == SyncState.RUNNING || it.state == SyncState.ENQUEUED }
                    .flatMap { it.tags }
                    .filter { it != "singleEvent" && it != "work" }
                    .toSet()

                // Update loading sites state
                _loadingSites.value = runningSiteIds
            }
        }

        restoreQuickMonitorIfNeeded()
    }

    private fun restoreQuickMonitorIfNeeded() {
        viewModelScope.launch {
            val prefs = dataStore.data.first()
            val isActive = prefs[QUICK_MONITOR_ACTIVE_KEY] ?: false
            val siteIdsString = prefs[QUICK_MONITOR_SITE_IDS_KEY].orEmpty()
            val siteIds = siteIdsString.split(",").filter { it.isNotBlank() }
            val interval = prefs[QUICK_MONITOR_INTERVAL_KEY] ?: 15
            val deadline = prefs[QUICK_MONITOR_DEADLINE_TIME_KEY] ?: 0L

            if (!isActive || siteIds.isEmpty()) {
                return@launch
            }

            if (deadline > 0L && System.currentTimeMillis() > deadline) {
                dataStore.edit { preferences ->
                    preferences[QUICK_MONITOR_ACTIVE_KEY] = false
                    preferences[QUICK_MONITOR_SITE_IDS_KEY] = ""
                    preferences[QUICK_MONITOR_START_TIME_KEY] = 0L
                    preferences[QUICK_MONITOR_DEADLINE_TIME_KEY] = 0L
                    preferences[QUICK_MONITOR_INTERVAL_KEY] = 15
                }
                return@launch
            }

            monitoringManager.startQuickMonitor(siteIds, interval)
        }
    }

    private suspend fun getLastSnap(siteId: String): Snap? {
        return mSnapsRepository.getMostRecentSnap(siteId)
    }

    suspend fun getAllSites(): List<Site> = mSitesRepository.getSites()

    suspend fun isAlreadyMonitoringSite(id: String, url: String): Boolean =
        mSitesRepository.getSiteByUrl(url) != null || mSitesRepository.getSiteById(id) != null

    fun reloadSite(site: Site) {
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
            monitoringManager.reloadSite(site)

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
            // Note: In KMP wait usage is limited but delay is fine
            kotlinx.coroutines.delay(1000)

            // Observe work status until it finishes
            syncStatusFlow
                .map { syncList ->
                    syncList.any { syncInfo ->
                        syncInfo.state.isRunningOrEnqueued() &&
                                syncInfo.tags.contains(siteId)
                    }
                }
                .first { isStillRunning -> !isStillRunning }
        } catch (e: Exception) {
            Napier.e("Failed to monitor work for site $siteId: ${e.message}")
        } finally {
            _loadingSites.update { it - siteId }
        }
    }

    // Quick Monitor functions - simple, fast monitoring
    fun startQuickMonitor(siteIds: Set<String>, intervalSeconds: Int = 15) {
        viewModelScope.launch {
            Napier.d("🚀 MainViewModel: Starting Quick Monitor for ${siteIds.size} sites: $siteIds")

            dataStore.edit { preferences ->
                preferences[QUICK_MONITOR_ACTIVE_KEY] = true
                preferences[QUICK_MONITOR_SITE_IDS_KEY] = siteIds.joinToString(",")
                preferences[QUICK_MONITOR_START_TIME_KEY] = System.currentTimeMillis()
                preferences[QUICK_MONITOR_INTERVAL_KEY] = intervalSeconds
            }
            Napier.d("💾 MainViewModel: Quick Monitor state saved to DataStore")

            // Start quick monitoring with selected interval
            monitoringManager.startQuickMonitor(siteIds.toList(), intervalSeconds = intervalSeconds)
            Napier.d("⚡ MainViewModel: monitoringManager.startQuickMonitor called")
        }
    }

    fun stopQuickMonitor() {
        viewModelScope.launch {
            Napier.d("⏹️ MainViewModel: Stopping Quick Monitor")

            dataStore.edit { preferences ->
                preferences[QUICK_MONITOR_ACTIVE_KEY] = false
                preferences[QUICK_MONITOR_SITE_IDS_KEY] = ""
                preferences[QUICK_MONITOR_START_TIME_KEY] = 0L
                preferences[QUICK_MONITOR_INTERVAL_KEY] = 15
            }
            Napier.d("💾 MainViewModel: Quick Monitor state cleared from DataStore")

            monitoringManager.stopQuickMonitor()
            Napier.d("⏹️ MainViewModel: monitoringManager.stopQuickMonitor called")
        }
    }
}
