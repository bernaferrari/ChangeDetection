/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.addedit

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
fun AddPageDialog(
    sessionKey: Int,
    selectedUrl: String? = null,
    onDismiss: () -> Unit,
    onSaveComplete: () -> Unit,
    onOpenSite: (String) -> Unit = {},
) {
    val owner = androidx.compose.runtime.remember(sessionKey) {
        object : androidx.lifecycle.ViewModelStoreOwner {
            override val viewModelStore = androidx.lifecycle.ViewModelStore()
        }
    }
    androidx.compose.runtime.DisposableEffect(owner) {
        onDispose { owner.viewModelStore.clear() }
    }
    val viewModel: AddEditViewModel = koinViewModel(viewModelStoreOwner = owner) { parametersOf(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            // Keep the parent visible in compact browser windows. Phones still get a
            // full-screen task, while tablet-sized surfaces retain dialog context.
            val useDialogShape = maxWidth >= 520.dp && maxHeight >= 620.dp
            Surface(
                modifier = if (useDialogShape) {
                    Modifier
                        .padding(24.dp)
                        .widthIn(max = 640.dp)
                        .heightIn(max = 640.dp)
                        .fillMaxSize()
                } else {
                    Modifier.fillMaxSize()
                },
                shape = if (useDialogShape) {
                    MaterialTheme.shapes.extraLarge
                } else {
                    RoundedCornerShape(0.dp)
                },
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = if (useDialogShape) 6.dp else 0.dp,
                shadowElevation = if (useDialogShape) 12.dp else 0.dp,
            ) {
                AddEditFlow(
                    selectedUrl = selectedUrl,
                    onNavigateUp = onDismiss,
                    onOpenSite = onOpenSite,
                    onSaveComplete = onSaveComplete,
                    viewModel = viewModel,
                )
            }
        }
    }
}

@Composable
fun AddEditFlow(
    siteId: String? = null,
    selectedUrl: String? = null,
    onNavigateUp: () -> Unit,
    onSaveComplete: () -> Unit,
    onOpenSite: (String) -> Unit = {},
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
        siteId == null && uiState.savedSite != null -> {
            SetupReceiptScreen(
                savedSite = uiState.savedSite!!,
                onOpenPage = {
                    onSaveComplete()
                    onOpenSite(uiState.savedSite!!.id)
                },
                onDone = onSaveComplete,
            )
        }

        siteId != null && uiState.isLoading -> {
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
                isEditing = siteId != null,
                isSaving = uiState.isSaving,
                saveError = uiState.saveError,
                duplicateSiteId = uiState.duplicateSiteId,
                onDismissDuplicate = viewModel::consumeDuplicate,
                onOpenDuplicate = { id ->
                    onNavigateUp()
                    onOpenSite(id)
                },
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
