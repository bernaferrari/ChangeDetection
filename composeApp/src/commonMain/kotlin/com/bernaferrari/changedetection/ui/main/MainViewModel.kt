/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
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
import com.bernaferrari.changedetection.error.ErrorEvent
import com.bernaferrari.changedetection.error.ErrorHandler
import com.bernaferrari.changedetection.error.ErrorContext
import com.bernaferrari.changedetection.repo.ContentTypeInfo
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import io.github.aakira.napier.Napier
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import org.koin.core.annotation.KoinViewModel

sealed class UiMessage {
    data object SnapshotsPruned : UiMessage()
}

@OptIn(ExperimentalUuidApi::class)
@KoinViewModel
class MainViewModel(
    private val mSnapsRepository: SnapsRepository,
    private val mSitesRepository: SitesRepository,
    private val monitoringManager: MonitoringManager,
    private val dataStore: DataStore<Preferences>,
    private val errorHandler: ErrorHandler,
) : ViewModel() {

    val syncStatusFlow = monitoringManager.getSyncStatusFlow()

    // Error state exposed to UI
    private val _currentError = MutableStateFlow<ErrorEvent?>(null)
    val currentError: StateFlow<ErrorEvent?> = _currentError.asStateFlow()

    // Success/error messages for snackbar display
    private val _uiMessage = MutableSharedFlow<UiMessage>(extraBufferCapacity = 1)
    val uiMessage: SharedFlow<UiMessage> = _uiMessage.asSharedFlow()

    // Pending retry action for error recovery
    private var pendingRetryAction: (() -> Unit)? = null

    fun onDeleteSite(site: Site) {
        _pendingDeletionSiteIds.update { it + site.id }
    }

    fun onUndoDeleteSite(site: Site) {
        _pendingDeletionSiteIds.update { it - site.id }
    }

    fun onConfirmDeleteSite(site: Site) {
        viewModelScope.launch {
            _pendingDeletionSiteIds.update { it - site.id }
            runCatching {
                mSnapsRepository.deleteAllSnaps(site.id)
                mSitesRepository.deleteSite(site.id)
            }.onFailure { error ->
                showError(
                    error = error,
                    context = ErrorContext.DELETE_SITE,
                    siteId = site.id,
                    retryAction = { onConfirmDeleteSite(site) },
                )
            }
        }
    }

    fun pruneSite(siteId: String) {
        viewModelScope.launch {
            runCatching {
                mSnapsRepository.pruneSnaps(siteId)
                _uiMessage.tryEmit(UiMessage.SnapshotsPruned)
            }.onFailure { error ->
                showError(
                    error = error,
                    context = ErrorContext.GENERAL,
                    siteId = siteId,
                    retryAction = { pruneSite(siteId) },
                )
            }
        }
    }

    suspend fun removeSnapsByType(siteId: String, contentType: String) {
        runCatching {
            mSnapsRepository.deleteSnapsForSiteIdAndContentType(siteId, contentType)
        }.onFailure { error ->
            showError(error, ErrorContext.GENERAL, siteId)
        }
    }

    fun saveSite(site: Site) {
        viewModelScope.launch {
            runCatching {
                mSitesRepository.saveSite(site)
            }.onFailure { error ->
                showError(
                    error = error,
                    context = ErrorContext.ADD_SITE,
                    siteId = site.id,
                    retryAction = { saveSite(site) },
                )
            }
        }
    }

    suspend fun getRecentContentTypes(siteId: String): List<ContentTypeInfo> = runCatching {
        mSnapsRepository.getContentTypeInfo(siteId)
    }.getOrElse { error ->
        showError(error, ErrorContext.GENERAL, siteId)
        emptyList()
    }

    fun updateSite(site: Site) {
        viewModelScope.launch {
            runCatching {
                mSitesRepository.updateSite(site)
            }.onFailure { error ->
                showError(
                    error = error,
                    context = ErrorContext.EDIT_SITE,
                    siteId = site.id,
                    retryAction = { updateSite(site) },
                )
            }
        }
    }

    // StateFlow for tracking loading sites
    private val _loadingSites = MutableStateFlow<Set<String>>(emptySet())
    val loadingSites: StateFlow<Set<String>> = _loadingSites.asStateFlow()

    // StateFlow for tracking sites pending deletion (Undo feature)
    private val _pendingDeletionSiteIds = MutableStateFlow<Set<String>>(emptySet())

    val sitesFlow: StateFlow<List<SiteAndLastSnap>?> = combine(
        mSitesRepository.getDataWithChanges(),
        _loadingSites.asStateFlow(),
        _pendingDeletionSiteIds.asStateFlow(),
    ) { sites, loadingSiteIds, pendingDeletionIds ->
        // Load all snapshots in a single optimized query
        val snapsBySite = runCatching { mSnapsRepository.getAllMostRecentSnaps() }.getOrNull() ?: emptyMap()

        // Map sites to SiteAndLastSnap using the pre-loaded snapshots
        sites
            .filter { !pendingDeletionIds.contains(it.id) } // Filter out pending deletions
            .map { site ->
                SiteAndLastSnap(
                    site = site,
                    snap = snapsBySite[site.id], // Fast map lookup instead of separate query
                    isSyncing = loadingSiteIds.contains(site.id),
                )
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null,
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
        val siteIds = if (siteIdsString.isBlank()) {
            emptySet()
        } else {
            siteIdsString.split(",").filter { it.isNotBlank() }.toSet()
        }
        val startTime = preferences[QUICK_MONITOR_START_TIME_KEY] ?: 0L

        QuickMonitorState(
            isActive = isActive,
            siteIds = siteIds,
            startTime = startTime,
            deadlineTime = preferences[QUICK_MONITOR_DEADLINE_TIME_KEY] ?: 0L,
            intervalSeconds = preferences[QUICK_MONITOR_INTERVAL_KEY] ?: 15,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = QuickMonitorState(),
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

            if (deadline > 0L && getNow() > deadline) {
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

    fun reloadSite(site: Site) {
        viewModelScope.launch {
            scheduleSiteReload(site)
        }
    }

    fun refreshAllSites() {
        viewModelScope.launch {
            val sites = runCatching { mSitesRepository.getSites() }
                .onFailure { error ->
                    showError(
                        error = error,
                        context = ErrorContext.GENERAL,
                        retryAction = ::refreshAllSites,
                    )
                }.getOrNull()
                ?: return@launch
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
            mSitesRepository.updateSite(site.copy(timestamp = getNow()))

            // Start sync work
            monitoringManager.reloadSite(site)

            // Monitor work completion without blocking caller
            monitorSiteWorkCompletionAsync(site.id)
        } catch (e: Exception) {
            _loadingSites.update { it - site.id }
            showError(
                error = e,
                context = ErrorContext.GENERAL,
                siteId = site.id,
                retryAction = { reloadSite(site) },
            )
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

    fun startQuickMonitor(siteIds: Set<String>, intervalSeconds: Int = 15) {
        viewModelScope.launch {
            Napier.d("Starting quick monitor for ${siteIds.size} sites")

            dataStore.edit { preferences ->
                preferences[QUICK_MONITOR_ACTIVE_KEY] = true
                preferences[QUICK_MONITOR_SITE_IDS_KEY] = siteIds.joinToString(",")
                preferences[QUICK_MONITOR_START_TIME_KEY] = getNow()
                preferences[QUICK_MONITOR_INTERVAL_KEY] = intervalSeconds
            }
            monitoringManager.startQuickMonitor(siteIds.toList(), intervalSeconds = intervalSeconds)
        }
    }

    fun stopQuickMonitor() {
        viewModelScope.launch {
            Napier.d("Stopping quick monitor")

            dataStore.edit { preferences ->
                preferences[QUICK_MONITOR_ACTIVE_KEY] = false
                preferences[QUICK_MONITOR_SITE_IDS_KEY] = ""
                preferences[QUICK_MONITOR_START_TIME_KEY] = 0L
                preferences[QUICK_MONITOR_INTERVAL_KEY] = 15
            }
            monitoringManager.stopQuickMonitor()
        }
    }

    // Error handling methods

    /**
     * Show an error to the user.
     */
    private fun showError(
        error: Throwable,
        context: ErrorContext,
        siteId: String? = null,
        retryAction: (() -> Unit)? = null,
    ) {
        val result = errorHandler.handleError(error, context, siteId = siteId)
        pendingRetryAction = retryAction?.takeIf { result.isRetryable }
        _currentError.value = ErrorEvent(
            id = kotlin.uuid.Uuid.random().toString(),
            type = result.type,
            severity = result.severity,
            message = result.userMessage,
            description = result.description,
            context = context,
            userAction = null,
            siteId = siteId,
            timestamp = Clock.System.now().toEpochMilliseconds(),
            isRetryable = pendingRetryAction != null,
            recoveryAction = result.recoveryAction,
            suggestedActions = result.suggestedActions,
        )
    }

    /**
     * Dismiss the current error.
     */
    fun dismissError() {
        _currentError.value = null
        pendingRetryAction = null
    }

    /**
     * Retry the last failed action.
     */
    fun retryLastAction() {
        val retryAction = pendingRetryAction ?: return
        dismissError()
        retryAction()
    }
}

private fun getNow() = kotlin.time.Clock.System.now().toEpochMilliseconds()
