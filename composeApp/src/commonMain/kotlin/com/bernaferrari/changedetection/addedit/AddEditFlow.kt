/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.addedit

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bernaferrari.changedetection.util.BackHandler

enum class AddEditStep {
    LOADING,
    URL_INPUT,
    CONFIRM_DETAILS,
}

@Composable
fun AddEditFlow(
    siteId: String? = null,
    selectedUrl: String? = null,
    onNavigateUp: () -> Unit,
    onSaveComplete: () -> Unit,
    viewModel: AddEditViewModel = koinViewModel { parametersOf(siteId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentStep by remember {
        mutableStateOf(if (siteId != null) AddEditStep.LOADING else AddEditStep.URL_INPUT)
    }
    var hasAutoNavigatedToConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(selectedUrl, siteId) {
        if (siteId == null && !selectedUrl.isNullOrBlank()) {
            if (viewModel.uiState.value.url.isBlank()) {
                viewModel.setUrl(selectedUrl)
            }
        }
    }

    LaunchedEffect(siteId, uiState.isLoading) {
        if (siteId != null &&
            !uiState.isLoading &&
            !hasAutoNavigatedToConfirm
        ) {
            currentStep = AddEditStep.CONFIRM_DETAILS
            hasAutoNavigatedToConfirm = true
        }
    }

    // Handle save completion with success animation + snackbar
    LaunchedEffect(uiState.saveComplete) {
        if (uiState.saveComplete) {
            onSaveComplete()
        }
    }

    BackHandler(enabled = currentStep != AddEditStep.URL_INPUT) {
        when (currentStep) {
            AddEditStep.CONFIRM_DETAILS -> currentStep = AddEditStep.URL_INPUT
            else -> {}
        }
    }

    when (currentStep) {
        AddEditStep.URL_INPUT -> {
            UrlInputStep(
                url = uiState.url,
                urlError = uiState.urlError,
                onUrlChange = viewModel::setUrl,
                onNavigateUp = onNavigateUp,
                onContinue = { currentStep = AddEditStep.CONFIRM_DETAILS },
            )
        }

        AddEditStep.CONFIRM_DETAILS -> {
            AddEditConfirmScreen(
                url = uiState.url,
                title = uiState.title,
                isEditing = siteId != null,
                selectedColors = uiState.selectedColors,
                isSaving = uiState.isSaving,
                saveError = uiState.saveError,
                onTitleChange = viewModel::setTitle,
                onColorChange = viewModel::setColors,
                onNavigateUp = { currentStep = AddEditStep.URL_INPUT },
                onSave = viewModel::saveSite,
            )
        }

        AddEditStep.LOADING -> {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        }
    }
}
