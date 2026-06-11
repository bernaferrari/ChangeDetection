/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.addedit

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
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
import com.bernaferrari.changedetection.browser.BrowserSessionData
import com.bernaferrari.changedetection.browser.ModernInteractiveBrowserScreen
import com.bernaferrari.changedetection.util.FeatureFlags
import com.bernaferrari.changedetection.util.BackHandler

enum class AddEditStep {
    LOADING,
    URL_INPUT,
    BROWSER_SETUP,
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
    var browserData by remember { mutableStateOf<BrowserSessionData?>(null) }
    var hasAutoNavigatedToConfirm by remember { mutableStateOf(false) }
    val browserSessionEnabled = FeatureFlags.BROWSER_SESSION

    LaunchedEffect(selectedUrl, siteId) {
        if (siteId == null && !selectedUrl.isNullOrBlank()) {
            if (viewModel.uiState.value.url.isBlank()) {
                viewModel.setUrl(selectedUrl)
            }
        }
    }

    LaunchedEffect(siteId, uiState.isLoading, uiState.browserData) {
        if (siteId != null &&
            !uiState.isLoading &&
            !hasAutoNavigatedToConfirm
        ) {
            if (uiState.browserData != null) {
                browserData = uiState.browserData
            }
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
            AddEditStep.BROWSER_SETUP -> currentStep = AddEditStep.URL_INPUT
            AddEditStep.CONFIRM_DETAILS -> {
                currentStep = if (browserSessionEnabled) {
                    AddEditStep.BROWSER_SETUP
                } else {
                    AddEditStep.URL_INPUT
                }
            }

            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState.ordinal > initialState.ordinal) {
                    (slideInHorizontally { it } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it } + fadeOut())
                } else {
                    (slideInHorizontally { -it } + fadeIn()) togetherWith
                        (slideOutHorizontally { it } + fadeOut())
                }
            },
            label = "step_transition",
        ) { step ->
            when (step) {
                AddEditStep.URL_INPUT -> {
                    BeautifulUrlInputStep(
                        url = uiState.url,
                        urlError = uiState.urlError,
                        onUrlChange = viewModel::setUrl,
                        onNavigateUp = onNavigateUp,
                        onContinue = {
                            currentStep = if (browserSessionEnabled) {
                                AddEditStep.BROWSER_SETUP
                            } else {
                                AddEditStep.CONFIRM_DETAILS
                            }
                        },
                    )
                }

                AddEditStep.BROWSER_SETUP -> {
                    if (browserSessionEnabled) {
                        ModernInteractiveBrowserScreen(
                            initialUrl = uiState.url,
                            initialSession = uiState.browserData,
                            onNavigateUp = { currentStep = AddEditStep.URL_INPUT },
                            onSaveComplete = { data ->
                                browserData = data
                                viewModel.setTitle(data.title)
                                viewModel.setBrowserData(data)
                                currentStep = AddEditStep.CONFIRM_DETAILS
                            },
                        )
                    }
                }

                AddEditStep.CONFIRM_DETAILS -> {
                    AddEditConfirmScreen(
                        url = uiState.url,
                        title = uiState.title,
                        isEditing = siteId != null,
                        selectedColors = uiState.selectedColors,
                        isSaving = uiState.isSaving,
                        saveError = uiState.saveError,
                        browserData = if (browserSessionEnabled) browserData else null,
                        useCookies = if (FeatureFlags.COOKIES) uiState.useCookies else false,
                        cookieBytes = if (FeatureFlags.COOKIES) uiState.cookieBytes else 0L,
                        onTitleChange = viewModel::setTitle,
                        onColorChange = viewModel::setColors,
                        onUseCookiesChange = if (FeatureFlags.COOKIES) viewModel::setUseCookies else { _: Boolean -> },
                        onNavigateUp = {
                            currentStep = if (browserSessionEnabled) {
                                AddEditStep.BROWSER_SETUP
                            } else {
                                AddEditStep.URL_INPUT
                            }
                        },
                        onSave = viewModel::saveSite,
                    )
                }

                AddEditStep.LOADING -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center,
                    ) {
                        androidx.compose.material3.CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
