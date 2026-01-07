package com.bernaferrari.changedetection.addedit

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScreen(
    siteId: String? = null,
    selectedUrl: String? = null,
    onNavigateUp: () -> Unit,
    onSaveComplete: () -> Unit,
    viewModel: AddEditViewModel = koinViewModel { parametersOf(siteId) }
) {
    // Forward to the new, fully-featured flow for consistency
    AddEditFlow(
        siteId = siteId,
        selectedUrl = selectedUrl,
        onNavigateUp = onNavigateUp,
        onSaveComplete = onSaveComplete,
        viewModel = viewModel
    )
}
