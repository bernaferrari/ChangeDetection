package com.bernaferrari.changedetection.addedit

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.browser.BrowserSessionData
import com.bernaferrari.changedetection.browser.ModernInteractiveBrowserScreen
import kotlinx.coroutines.delay

enum class AddEditStep {
    URL_INPUT,
    BROWSER_SETUP,
    CONFIRM_DETAILS
}

@Composable
fun AddEditFlow(
    siteId: String? = null,
    selectedUrl: String? = null,
    onNavigateUp: () -> Unit,
    onSaveComplete: () -> Unit,
    viewModel: AddEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var currentStep by remember { mutableStateOf(AddEditStep.URL_INPUT) }
    var browserData by remember { mutableStateOf<BrowserSessionData?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    val successMessage = stringResource(id = R.string.site_added_successfully)
    var hasAutoNavigatedToConfirm by remember { mutableStateOf(false) }

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
            uiState.browserData != null &&
            !hasAutoNavigatedToConfirm
        ) {
            browserData = uiState.browserData
            currentStep = AddEditStep.CONFIRM_DETAILS
            hasAutoNavigatedToConfirm = true
        }
    }
    
    // Handle save completion with success animation + snackbar
    LaunchedEffect(uiState.saveComplete) {
        if (uiState.saveComplete) {
            snackbarHostState.showSnackbar(successMessage)
            showSuccessDialog = true
            delay(1500)
            showSuccessDialog = false
            onSaveComplete()
        }
    }
    
    BackHandler(enabled = currentStep != AddEditStep.URL_INPUT) {
        when (currentStep) {
            AddEditStep.BROWSER_SETUP -> currentStep = AddEditStep.URL_INPUT
            AddEditStep.CONFIRM_DETAILS -> currentStep = AddEditStep.BROWSER_SETUP
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
            label = "step_transition"
        ) { step ->
            when (step) {
                AddEditStep.URL_INPUT -> {
                    BeautifulUrlInputStep(
                        url = uiState.url,
                        urlError = uiState.urlError,
                        onUrlChange = viewModel::setUrl,
                        onNavigateUp = onNavigateUp,
                        onContinue = {
                            currentStep = AddEditStep.BROWSER_SETUP
                        }
                    )
                }
                
                AddEditStep.BROWSER_SETUP -> {
                    ModernInteractiveBrowserScreen(
                        initialUrl = uiState.url,
                        initialSession = uiState.browserData,
                        onNavigateUp = { currentStep = AddEditStep.URL_INPUT },
                        onSaveComplete = { data ->
                            browserData = data
                            viewModel.setTitle(data.title ?: "")
                            viewModel.setBrowserData(data)
                            currentStep = AddEditStep.CONFIRM_DETAILS
                        }
                    )
                }
                
                AddEditStep.CONFIRM_DETAILS -> {
                    AddEditConfirmScreen(
                        url = uiState.url,
                        title = uiState.title,
                        selectedColors = uiState.selectedColors,
                        isSaving = uiState.isSaving,
                        saveError = uiState.saveError,
                        browserData = browserData,
                        useCookies = uiState.useCookies,
                        cookieBytes = uiState.cookieBytes,
                        onTitleChange = viewModel::setTitle,
                        onColorChange = viewModel::setColors,
                        onUseCookiesChange = viewModel::setUseCookies,
                        onNavigateUp = { currentStep = AddEditStep.BROWSER_SETUP },
                        onSave = viewModel::saveSite
                    )
                }
            }
        }

        // Success Dialog
        if (showSuccessDialog) {
            SuccessDialog(
                accentColors = uiState.selectedColors,
                onDismiss = { showSuccessDialog = false }
            )
        }

        // Snackbar host (global for this flow)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
