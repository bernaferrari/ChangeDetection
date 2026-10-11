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
import com.bernaferrari.changedetection.sync.SyncProgressTracker
import com.bernaferrari.changedetection.util.ApplicationScope
import io.github.aakira.napier.Napier
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import org.koin.core.annotation.KoinViewModel

sealed class UiMessage {
    data object SnapshotsPruned : UiMessage()
    data object TurboEnded : UiMessage()
    data class PageAdded(val siteId: String, val name: String) : UiMessage()
}

@OptIn(ExperimentalUuidApi::class)
@KoinViewModel
class MainViewModel(
    private val mSnapsRepository: SnapsRepository,
    private val mSitesRepository: SitesRepository,
    private val monitoringManager: MonitoringManager,
    private val dataStore: DataStore<Preferences>,
    private val errorHandler: ErrorHandler,
    private val applicationScope: ApplicationScope,
    syncProgressTracker: SyncProgressTracker,
    private val syncEngine: com.bernaferrari.changedetection.sync.SyncEngine,
) : ViewModel() {

    val syncStatusFlow = monitoringManager.getSyncStatusFlow()

    // Error state exposed to UI
    private val _currentError = MutableStateFlow<ErrorEvent?>(null)
    val currentError: StateFlow<ErrorEvent?> = _currentError.asStateFlow()

    // Success/error messages for snackbar display
    // A channel keeps a message until Home is on screen to show it (e.g. right after adding a page).
    private val _uiMessage = kotlinx.coroutines.channels.Channel<UiMessage>(kotlinx.coroutines.channels.Channel.BUFFERED)
    val uiMessage: kotlinx.coroutines.flow.Flow<UiMessage> = _uiMessage.receiveAsFlow()

    fun announcePageAdded(siteId: String, name: String) {
        _uiMessage.trySend(UiMessage.PageAdded(siteId, name))
    }

    // Pending retry action for error recovery
    private var pendingRetryAction: (() -> Unit)? = null

    private val deletions: UndoableDeletion<Site> = UndoableDeletion(
        scope = applicationScope.scope,
        delete = { site ->
            mSnapsRepository.deleteSiteWithSnaps(site.id)
            removeSiteFromQuickMonitor(site.id)
        },
        onHidden = { site -> _pendingDeletionSiteIds.update { it + site.id } },
        onRestored = { site -> _pendingDeletionSiteIds.update { it - site.id } },
        onDeleted = { site -> _pendingDeletionSiteIds.update { it - site.id } },
        onFailure = { site, error ->
            showError(error, ErrorContext.DELETE_SITE, site.id, retryAction = { retryDeletion(site) })
        },
    )

    internal fun onDeleteSite(site: Site) = deletions.stage(site)

    internal suspend fun resolveDeletion(ticket: UndoableDeletion.Ticket<Site>, chooseUndo: suspend () -> Boolean) = deletions.resolve(ticket, chooseUndo)

    /** Deletes right away; used when the user already confirmed and no undo is offered. */
    internal fun commitDeletion(ticket: UndoableDeletion.Ticket<Site>) {
        applicationScope.scope.launch { deletions.resolve(ticket) { false } }
    }

    private fun retryDeletion(site: Site) = deletions.retry(site)

    fun pruneSite(siteId: String) {
        viewModelScope.launch {
            runCatching {
                mSnapsRepository.pruneSnaps(siteId)
                _uiMessage.trySend(UiMessage.SnapshotsPruned)
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
                if (!site.isSyncEnabled) removeSiteFromQuickMonitor(site.id)
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

    // WorkManager describes queued requests; the process tracker describes the
    // page currently doing real work. Keep optimistic taps separately so one
    // source can never erase another source's truthful loading state.
    private val _workLoadingSites = MutableStateFlow<Set<String>>(emptySet())
    private val _optimisticLoadingSites = MutableStateFlow<Set<String>>(emptySet())
    val loadingSites: StateFlow<Set<String>> = combine(
        _workLoadingSites,
        _optimisticLoadingSites,
        syncProgressTracker.activeSiteIds,
    ) { workIds, optimisticIds, activeIds ->
        workIds + optimisticIds + activeIds
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptySet(),
    )

    // StateFlow for tracking sites pending deletion (Undo feature)
    private val _pendingDeletionSiteIds = MutableStateFlow<Set<String>>(emptySet())

    private var watchlistErrorId: String? = null
    private val watchlistData = watchlistReader(
        sites = mSitesRepository,
        onFailure = { error ->
            showError(error, ErrorContext.GENERAL)
            watchlistErrorId = _currentError.value?.id
        },
        onLoaded = {
            if (watchlistErrorId != null && _currentError.value?.id == watchlistErrorId) dismissError()
            watchlistErrorId = null
        },
    )

    val sitesFlow: StateFlow<List<SiteAndLastSnap>?> = combine(
        watchlistData,
        loadingSites,
        _pendingDeletionSiteIds.asStateFlow(),
    ) { rows, loadingSiteIds, pendingDeletionIds ->
        rows.filter { it.site.id !in pendingDeletionIds }
            .map { it.copy(isSyncing = it.site.id in loadingSiteIds) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null,
    )

    val monitoringSchedule = dataStore.data.map { com.bernaferrari.changedetection.background.MonitoringSchedule.from(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.bernaferrari.changedetection.background.MonitoringSchedule())

    fun resumeScheduledChecks() {
        applicationScope.scope.launch {
            runCatching {
                dataStore.edit { it[com.bernaferrari.changedetection.background.MonitoringSchedule.ENABLED] = true }
                monitoringManager.updateSchedule()
            }.onFailure { showError(it, ErrorContext.GENERAL) }
        }
    }

    val quickMonitorState = monitoringManager.quickMonitorState

    // Initialize monitoring
    init {
        applicationScope.scope.launch { runCatching { syncEngine.resolveMissingIcons() } }
        // Monitor work info changes to update loading states
        viewModelScope.launch {
            syncStatusFlow.collect { syncList ->
                val runningSiteIds = syncList
                    .filter { it.state == SyncState.RUNNING }
                    .flatMap { it.tags }
                    .filter { it != "singleEvent" && it != "work" }
                    .toSet()

                // Update loading sites state
                _workLoadingSites.value = runningSiteIds
            }
        }

        viewModelScope.launch {
            runCatching { monitoringManager.updateSchedule() }
                .onFailure { showError(it, ErrorContext.GENERAL) }
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
            if (loadingSites.value.contains(site.id)) {
                return
            }

            _optimisticLoadingSites.update { it + site.id }

            // Update site timestamp to indicate sync started

            // Start sync work
            monitoringManager.reloadSite(site)

            // Monitor work completion without blocking caller
            monitorSiteWorkCompletionAsync(site.id)
        } catch (e: Exception) {
            _optimisticLoadingSites.update { it - site.id }
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
            // Hand off tap feedback to actual execution tracking as soon as Android
            // acknowledges the request. ENQUEUED retries are not active checks.
            com.bernaferrari.changedetection.background.awaitSiteRequest(syncStatusFlow, siteId)
        } catch (e: Exception) {
            Napier.e("Failed to monitor work for site $siteId: ${e.message}")
        } finally {
            _optimisticLoadingSites.update { it - siteId }
        }
    }

    fun startQuickMonitor(siteIds: Set<String>, intervalSeconds: Int = 30, durationMinutes: Int = 180) {
        if (siteIds.isEmpty()) {
            stopQuickMonitor()
            return
        }
        try {
            monitoringManager.startQuickMonitor(siteIds.toList(), intervalSeconds, durationMinutes)
        } catch (error: Exception) {
            showError(error, ErrorContext.GENERAL, retryAction = { startQuickMonitor(siteIds, intervalSeconds, durationMinutes) })
        }
    }

    private suspend fun removeSiteFromQuickMonitor(siteId: String) {
        val current = quickMonitorState.value
        if (!current.isActive || siteId !in current.siteIds) return
        startQuickMonitor(current.siteIds - siteId, current.intervalSeconds)
    }

    fun stopQuickMonitor() = monitoringManager.stopQuickMonitor()

    /** Clears the "new" state for exactly the version shown, so a newer change stays unread. */
    fun markRead(row: SiteAndLastSnap) {
        val snapId = row.snap?.snapId ?: return
        viewModelScope.launch {
            runCatching { mSitesRepository.markSiteRead(row.site.id, snapId) }
                .onFailure { showError(it, ErrorContext.EDIT_SITE, row.site.id) }
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
