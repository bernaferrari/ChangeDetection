/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bernaferrari.changedetection.background.MonitoringManager
import com.bernaferrari.changedetection.extensions.normalizeUrl
import com.bernaferrari.changedetection.extensions.canonicalUrlKey
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.source.SitesRepository
import io.github.aakira.napier.Napier
import io.ktor.http.Url
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

enum class PreviewFailure { TIMEOUT, REGION, UNAVAILABLE }

sealed interface CapturePreviewState {
    data object Idle : CapturePreviewState
    data object Loading : CapturePreviewState
    data class Ready(val content: CapturePreview) : CapturePreviewState
    data class Failed(val reason: PreviewFailure) : CapturePreviewState
}

data class AddEditUiState(
    val preview: CapturePreviewState = CapturePreviewState.Idle,
    val legacyThreshold: Int = 0,
    val visualChangeThresholdBps: Int = 0,
    val textRegion: com.bernaferrari.changedetection.detection.TextRegion = com.bernaferrari.changedetection.detection.TextRegion(),
    val regionError: Boolean = false,
    // Screenshots are how most pages are watched; text is the light alternative.
    val captureMode: com.bernaferrari.changedetection.repo.CaptureMode = if (com.bernaferrari.changedetection.repo.supportsVisualMonitoring) com.bernaferrari.changedetection.repo.CaptureMode.SCREENSHOT else com.bernaferrari.changedetection.repo.CaptureMode.TEXT,
    val url: String = "",
    val title: String = "",
    val selectedColors: Pair<Int, Int> = Pair(0xFF2196F3.toInt(), 0xFF1976D2.toInt()),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val urlError: AddEditError? = null,
    val saveError: AddEditError? = null,
    val saveComplete: Boolean = false,
    val savedSite: Site? = null,
    val duplicateSiteId: String? = null,
    /** URL of the site as loaded in edit mode, used to detect URL changes. */
    val loadedSiteUrl: String? = null,
    val showUrlChangeDialog: Boolean = false,
)

/** One-shot events consumed by the dialog host (e.g. snackbars on MainScreen). */
sealed interface AddEditEvent {
    data object MonitorAdded : AddEditEvent
}

enum class AddEditError {
    InvalidUrl,
    SiteNotFound,
    LoadFailed,
    SaveFailed,
}

@OptIn(ExperimentalUuidApi::class)
@KoinViewModel
class AddEditViewModel(
    @InjectedParam private val siteId: String?,
    private val sitesRepository: SitesRepository,
    private val previewer: CapturePreviewer,
    private val monitoringManager: MonitoringManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddEditUiState(isLoading = siteId != null))
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()
    private val _events = Channel<AddEditEvent>(Channel.BUFFERED)
    val events: Flow<AddEditEvent> = _events.receiveAsFlow()
    private var titleEditedByUser = false
    private var previewJob: kotlinx.coroutines.Job? = null
    private var previewGeneration = 0L

    fun dismissPreview() {
        previewGeneration++
        previewJob?.cancel()
        previewJob = null
        _uiState.update { it.copy(preview = CapturePreviewState.Idle) }
    }

    fun requestPreview() {
        val state = _uiState.value
        if (!validateRegion(state)) return
        val url = state.url.normalizeUrl()
        if (url == null) {
            _uiState.update { it.copy(urlError = AddEditError.InvalidUrl) }
            return
        }
        dismissPreview()
        val generation = previewGeneration
        _uiState.update { it.copy(preview = CapturePreviewState.Loading) }
        previewJob = viewModelScope.launch {
            try {
                val content = previewer.capture(url, state.captureMode, state.textRegion)
                if (generation == previewGeneration) {
                    // A page's own first heading names it better than its host; never override a typed name.
                    val heading = (content as? CapturePreview.Html)?.pageTitle ?: (content as? CapturePreview.Image)?.pageTitle
                    _uiState.update {
                        it.copy(
                            preview = CapturePreviewState.Ready(content),
                            title = if (!titleEditedByUser && heading != null) heading else it.title,
                        )
                    }
                }
            } catch (timeout: kotlinx.coroutines.TimeoutCancellationException) {
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                if (generation == previewGeneration) _uiState.update { it.copy(preview = CapturePreviewState.Failed(PreviewFailure.TIMEOUT)) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (generation == previewGeneration) {
                    _uiState.update {
                        it.copy(preview = CapturePreviewState.Failed(if (error is com.bernaferrari.changedetection.detection.TextRegionException) PreviewFailure.REGION else PreviewFailure.UNAVAILABLE))
                    }
                }
            }
        }
    }

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
            if (site != null) {
                titleEditedByUser = true
                _uiState.update { state ->
                    state.copy(
                        url = site.url,
                        title = site.title ?: "",
                        selectedColors = site.colors,
                        loadedSiteUrl = site.url,
                        legacyThreshold = site.threshold.coerceAtLeast(0),
                        textRegion = com.bernaferrari.changedetection.detection.TextRegion(site.textIncludeSelector, site.textExcludeSelector),
                        captureMode = site.captureMode,
                        visualChangeThresholdBps = site.visualChangeThresholdBps,
                        isLoading = false,
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        saveError = AddEditError.SiteNotFound,
                    )
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Napier.e("Failed to load monitor", error)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    saveError = AddEditError.LoadFailed,
                )
            }
        }
    }

    fun setUrl(url: String) {
        if (url != _uiState.value.url) dismissPreview()
        val normalized = url.normalizeUrl()

        _uiState.update { current ->
            val updatedTitle =
                if (!titleEditedByUser && normalized != null) {
                    runCatching {
                        // Until the page's own title arrives, the readable host is the honest name.
                        Url(normalized).host.removePrefix("www.")
                    }.getOrElse { current.title }
                } else if (!titleEditedByUser) {
                    ""
                } else {
                    current.title
                }
            current.copy(
                url = url,
                urlError =
                if (url.isNotEmpty() && normalized == null) {
                    AddEditError.InvalidUrl
                } else {
                    null
                },
                title = updatedTitle,
            )
        }
    }

    fun consumeDuplicate() {
        _uiState.update { it.copy(duplicateSiteId = null) }
    }

    /** Clears the one-shot saveComplete flag once the UI has handled it. */
    fun consumeSaveComplete() {
        _uiState.update { it.copy(saveComplete = false) }
    }

    fun setCaptureMode(mode: com.bernaferrari.changedetection.repo.CaptureMode) {
        if (!com.bernaferrari.changedetection.repo.supportsVisualMonitoring && mode in setOf(com.bernaferrari.changedetection.repo.CaptureMode.DEFAULT, com.bernaferrari.changedetection.repo.CaptureMode.SCREENSHOT)) return
        if (mode != _uiState.value.captureMode) dismissPreview()
        _uiState.update { it.copy(captureMode = mode, regionError = false) }
    }

    fun setVisualChangeThreshold(basisPoints: Int) {
        require(basisPoints in 0..10_000)
        _uiState.update { it.copy(visualChangeThresholdBps = basisPoints) }
    }

    fun setTextRegion(region: com.bernaferrari.changedetection.detection.TextRegion) {
        dismissPreview()
        _uiState.update { it.copy(textRegion = region, regionError = false) }
    }

    private fun validateRegion(state: AddEditUiState): Boolean {
        return try {
            state.textRegion.validate()
            true
        } catch (invalid: com.bernaferrari.changedetection.detection.TextRegionException) {
            _uiState.update { it.copy(regionError = true) }
            false
        }
    }

    fun removeLegacyThreshold() {
        _uiState.update { it.copy(legacyThreshold = 0) }
    }

    fun setTitle(title: String) {
        titleEditedByUser = true
        _uiState.update { it.copy(title = title) }
    }

    fun saveSite() {
        val state = _uiState.value
        if (!validateRegion(state)) return
        val normalizedUrl = state.url.normalizeUrl()
        if (normalizedUrl == null) {
            _uiState.update { it.copy(urlError = AddEditError.InvalidUrl) }
            return
        }

        // Edit-mode URL-change trap: confirm what happens with existing snapshots first.
        val currentUrl = if (siteId != null) _uiState.value.loadedSiteUrl else null
        if (currentUrl != null && normalizedUrl.canonicalUrlKey() != currentUrl.canonicalUrlKey()) {
            _uiState.update { it.copy(showUrlChangeDialog = true) }
            return
        }

        performSave(startFresh = false)
    }

    /** User chose "Keep history": save without touching existing snapshots. */
    fun onUrlChangeKeepHistory() {
        consumeUrlChangeDialog()
        performSave(startFresh = false)
    }

    /** User chose "Start fresh": remove old versions so the next check is a new baseline. */
    fun onUrlChangeStartFresh() {
        consumeUrlChangeDialog()
        performSave(startFresh = true)
    }

    /** Dialog dismissed without choosing: stay on the form unsaved. */
    fun onUrlChangeCancel() {
        consumeUrlChangeDialog()
    }

    private fun consumeUrlChangeDialog() {
        _uiState.update { it.copy(showUrlChangeDialog = false) }
    }

    private fun performSave(startFresh: Boolean) {
        val state = _uiState.value
        if (!validateRegion(state)) return
        val normalizedUrl = state.url.normalizeUrl()
        if (normalizedUrl == null) {
            _uiState.update { it.copy(urlError = AddEditError.InvalidUrl) }
            return
        }

        // Guard against double-submit: mark saving synchronously BEFORE launching,
        // so a rapid second tap observes isSaving and bails out.
        if (_uiState.value.isSaving || _uiState.value.isLoading) return
        _uiState.update { it.copy(isSaving = true, saveError = null) }

        viewModelScope.launch {
            try {
                val existingSite = siteId?.let { sitesRepository.getSiteById(it) }

                // Check before any destructive "Start fresh" work. This also
                // protects edit mode from being changed to another saved page.
                val duplicate = sitesRepository.getSiteByUrl(normalizedUrl)
                if (duplicate != null && duplicate.id != siteId) {
                    _uiState.update {
                        it.copy(isSaving = false, duplicateSiteId = duplicate.id)
                    }
                    return@launch
                }

                val resolvedTitle =
                    state.title.ifBlank {
                        try {
                            Url(normalizedUrl).host.replace("www.", "")
                        } catch (e: Exception) {
                            "Website"
                        }
                    }

                val baseSite =
                    existingSite?.copy(
                        url = normalizedUrl,
                        captureMode = state.captureMode,
                        visualChangeThresholdBps = state.visualChangeThresholdBps,
                        textIncludeSelector = state.textRegion.include.trim(),
                        textExcludeSelector = state.textRegion.exclude.trim(),
                        title = resolvedTitle,
                        colors = state.selectedColors,
                        threshold = if ((state.visualChangeThresholdBps > 0 && state.captureMode in setOf(com.bernaferrari.changedetection.repo.CaptureMode.SCREENSHOT, com.bernaferrari.changedetection.repo.CaptureMode.DEFAULT)) || state.captureMode in setOf(com.bernaferrari.changedetection.repo.CaptureMode.TEXT, com.bernaferrari.changedetection.repo.CaptureMode.SOURCE)) 0 else state.legacyThreshold,
                        timestamp = if (startFresh) 0L else existingSite.timestamp,
                    ) ?: Site(
                        id = Uuid.random().toString(),
                        url = normalizedUrl,
                        captureMode = state.captureMode,
                        visualChangeThresholdBps = state.visualChangeThresholdBps,
                        textIncludeSelector = state.textRegion.include.trim(),
                        textExcludeSelector = state.textRegion.exclude.trim(),
                        title = resolvedTitle,
                        colors = state.selectedColors,
                        isBrowser = true,
                        isSyncEnabled = true,
                        isNotificationEnabled = true,
                        timestamp = 0L,
                        isSuccessful = true,
                        isRead = false,
                        threshold = 0,
                        notes = "",
                        constraints =
                        com.bernaferrari.changedetection.repo.ConstraintsRequired(
                            false,
                            false,
                            false,
                            false,
                        ),
                    )

                monitoringManager.updateSchedule()
                sitesRepository.saveSite(baseSite, resetHistory = startFresh && existingSite != null)

                if (siteId == null) {
                    _events.trySend(AddEditEvent.MonitorAdded)
                }

                _uiState.update { current ->
                    current.copy(isSaving = false, saveComplete = true, savedSite = baseSite)
                }

                // Saving succeeded even if the best-effort initial check cannot be scheduled.
                runCatching { monitoringManager.reloadSite(baseSite) }
                    .onFailure { Napier.w("Monitor saved, but its initial check could not be scheduled", it) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Napier.e("Failed to save monitor", error)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveError = AddEditError.SaveFailed,
                    )
                }
            }
        }
    }
}
