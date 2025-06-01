package com.bernaferrari.changedetection.addedit

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bernaferrari.changedetection.extensions.isValidUrl
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.util.GradientColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScreen(
    site: Site? = null,
    selectedUrl: String? = null,
    onNavigateUp: () -> Unit,
    onNavigateToWebView: (String) -> Unit,
    onShowCompareDialog: (String) -> Unit,
    viewModel: AddEditViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboard.current
    val text = remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var formData by remember {
        mutableStateOf(
            FormData(
                title = site?.title ?: "",
                url = site?.url ?: "",
                tags = site?.notes ?: "",
                useBrowser = site?.isBrowser ?: false,
                threshold = site?.threshold?.toString() ?: "0",
                selectedColors = site?.colors ?: GradientColors.gradients.random()
            )
        )
    }

    val isFormValid = formData.url.isValidUrl()

    // Initialize with clipboard URL if adding new site
    LaunchedEffect(Unit) {
        if (site == null) {
            clipboardManager.getClipEntry()?.toString()?.let { clipText ->
                if (clipText.isValidUrl()) {
                    formData = formData.copy(url = clipText)
                }
            }
        }
    }

    // Handle selected URL from WebView
    LaunchedEffect(selectedUrl) {
        selectedUrl?.let {
            formData = formData.copy(url = it)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (site == null) "Add Site" else "Edit Site",
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = {
                            if (isFormValid) {
                                if (site == null) {
                                    viewModel.addSite(
                                        title = formData.title,
                                        url = formData.url,
                                        tags = formData.tags,
                                        useBrowser = formData.useBrowser,
                                        colors = formData.selectedColors,
                                        threshold = formData.threshold.toIntOrNull() ?: 0
                                    )
                                } else {
                                    viewModel.updateSite(
                                        site = site,
                                        title = formData.title,
                                        url = formData.url,
                                        tags = formData.tags,
                                        useBrowser = formData.useBrowser,
                                        colors = formData.selectedColors,
                                        threshold = formData.threshold.toIntOrNull() ?: 0
                                    )
                                }
                                onNavigateUp()
                            } else {
                                formData = formData.copy(urlError = "Please enter a valid URL")
                            }
                        },
                        enabled = isFormValid,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        AddEditForm(
            formData = formData,
            isEditing = site != null,
            onFormDataChange = { formData = it },
            onNavigateToWebView = onNavigateToWebView,
            onShowCompareDialog = onShowCompareDialog,
            modifier = Modifier.padding(paddingValues)
        )
    }
}
