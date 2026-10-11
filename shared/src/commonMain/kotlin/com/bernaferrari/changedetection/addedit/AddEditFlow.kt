/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.addedit

import com.bernaferrari.changedetection.util.displayPageTitle

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bernaferrari.changedetection.ui.components.AppLoadingIndicator
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AddEditFlow(
    siteId: String? = null,
    selectedUrl: String? = null,
    onNavigateUp: () -> Unit,
    onSaveComplete: () -> Unit,
    onOpenSite: (String) -> Unit,
    onPageAdded: (siteId: String, name: String) -> Unit = { _, _ -> onSaveComplete() },
    viewModel: AddEditViewModel = koinViewModel { parametersOf(siteId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    androidx.compose.runtime.DisposableEffect(viewModel) {
        onDispose { viewModel.dismissPreview() }
    }
    LaunchedEffect(selectedUrl, siteId) {
        if (siteId == null && !selectedUrl.isNullOrBlank()) {
            if (viewModel.uiState.value.url.isBlank()) {
                viewModel.setUrl(selectedUrl)
            }
        }
    }

    // A save owns its route until the committed result is delivered.
    com.bernaferrari.changedetection.util.BackHandler(enabled = uiState.isSaving) {}

    // A new page goes straight back to Home, which shows it in the list and confirms with a message.
    val saved = uiState.savedSite
    LaunchedEffect(saved) {
        if (siteId == null && saved != null) onPageAdded(saved.id, saved.displayPageTitle())
    }

    LaunchedEffect(uiState.saveComplete) {
        if (uiState.saveComplete && siteId != null) {
            onSaveComplete()
            // Consume immediately so the flag can't re-fire on state restoration.
            viewModel.consumeSaveComplete()
        }
    }

    // Drain one-shot events so the channel never backs up; the dialog host has no
    // dedicated ack channel, so MonitorAdded is consumed without extra UI.
    LaunchedEffect(viewModel) {
        viewModel.events.collect { }
    }

    when {
        uiState.isLoading -> {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                AppLoadingIndicator()
            }
        }

        else -> {
            AddEditConfirmScreen(
                url = uiState.url,
                urlError = uiState.urlError,
                title = uiState.title,
                iconUrl = uiState.iconUrl,
                iconShape = uiState.iconShape,
                onIconShapeChange = viewModel::setIconShape,
                iconColors = uiState.selectedColors,
                onIconColorChange = viewModel::setIconColor,
                isEditing = siteId != null,
                isSaving = uiState.isSaving,
                saveError = uiState.saveError,
                duplicateSiteId = uiState.duplicateSiteId,
                onDismissDuplicate = viewModel::consumeDuplicate,
                onOpenDuplicate = onOpenSite,
                showUrlChangeDialog = uiState.showUrlChangeDialog,
                onCancel = viewModel::onUrlChangeCancel,
                onKeepHistory = viewModel::onUrlChangeKeepHistory,
                onStartFresh = viewModel::onUrlChangeStartFresh,
                onUrlChange = viewModel::setUrl,
                onTitleChange = viewModel::setTitle,
                captureMode = uiState.captureMode,
                textRegion = uiState.textRegion,
                regionError = uiState.regionError,
                onTextRegionChange = viewModel::setTextRegion,
                legacyThreshold = uiState.legacyThreshold,
                visualChangeThresholdBps = uiState.visualChangeThresholdBps,
                onVisualChangeThresholdChange = viewModel::setVisualChangeThreshold,
                onRemoveLegacyThreshold = viewModel::removeLegacyThreshold,
                onCaptureModeChange = viewModel::setCaptureMode,
                preview = uiState.preview,
                onPreview = viewModel::requestPreview,
                onClosePreview = viewModel::dismissPreview,
                onNavigateUp = onNavigateUp,
                onSave = viewModel::saveSite,
            )
        }
    }
}
