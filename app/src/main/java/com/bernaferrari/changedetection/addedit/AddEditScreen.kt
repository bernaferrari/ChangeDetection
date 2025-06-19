package com.bernaferrari.changedetection.addedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.bernaferrari.changedetection.util.GradientColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScreen(
    siteId: String? = null,
    selectedUrl: String? = null,
    onNavigateUp: () -> Unit,
    onNavigateToWebView: (String) -> Unit,
    onNavigateToVisualComparison: (String) -> Unit = {},
    onShowCompareDialog: (String) -> Unit,
    viewModel: AddEditViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboard.current
    val text = remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val site by viewModel.site.collectAsStateWithLifecycle()

    var formData by remember {
        mutableStateOf(
            FormData(
                title = "",
                url = "",
                tags = "",
                useBrowser = false,
                threshold = "0",
                selectedColors = GradientColors.gradients.random()
            )
        )
    }

    val isFormValid = formData.url.isValidUrl()

    // Initialize site data when editing
    LaunchedEffect(siteId) {
        if (siteId != null) {
            viewModel.loadSite(siteId)
        }
    }

    // Update form data when site is loaded
    LaunchedEffect(site) {
        site?.let { siteData ->
            formData = FormData(
                title = siteData.title ?: "",
                url = siteData.url,
                tags = siteData.notes,
                useBrowser = siteData.isBrowser,
                threshold = siteData.threshold.toString(),
                selectedColors = siteData.colors
            )
        }
    }

    // Initialize with clipboard URL if adding new site (not editing)
    LaunchedEffect(Unit) {
        if (siteId == null) {
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

    // Handle navigation after successful save
    LaunchedEffect(uiState) {
        if (uiState.isSuccess) {
            onNavigateUp()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (siteId == null) "Add Site" else "Edit Site",
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
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding(),
                shadowElevation = 8.dp,
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    FilledTonalButton(
                        onClick = {
                            if (isFormValid) {
                                if (siteId == null) {
                                    viewModel.addSite(
                                        title = formData.title,
                                        url = formData.url,
                                        tags = formData.tags,
                                        useBrowser = formData.useBrowser,
                                        colors = formData.selectedColors,
                                        threshold = formData.threshold.toIntOrNull() ?: 0
                                    )
                                } else {
                                    site?.let { currentSite ->
                                        viewModel.updateSite(
                                            site = currentSite,
                                            title = formData.title,
                                            url = formData.url,
                                            tags = formData.tags,
                                            useBrowser = formData.useBrowser,
                                            colors = formData.selectedColors,
                                            threshold = formData.threshold.toIntOrNull() ?: 0
                                        )
                                    }
                                }
                            } else {
                                formData = formData.copy(urlError = "Please enter a valid URL")
                            }
                        },
                        enabled = isFormValid
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    ) { paddingValues ->
        AddEditForm(
            formData = formData,
            isEditing = siteId != null,
            onFormDataChange = { formData = it },
            onNavigateToWebView = onNavigateToWebView,
            onShowCompareDialog = onShowCompareDialog,
            onShowComparison = {
                // Navigate to visual comparison when user clicks compare
                if (siteId != null) {
                    onNavigateToVisualComparison(siteId)
                }
            },
            modifier = Modifier.padding(paddingValues)
        )
    }
}
