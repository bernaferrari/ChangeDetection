package com.bernaferrari.changedetection.ui.addedit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bernaferrari.changedetection.repo.ColorGroup
import com.bernaferrari.changedetection.repo.Site

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScreen(
    siteId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToWebView: (String) -> Unit,
    onNavigateToCompare: (String) -> Unit,
    viewModel: AddEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(siteId) {
        if (siteId != null) {
            viewModel.loadSite(siteId)
        }
    }

    LaunchedEffect(uiState.shouldNavigateBack) {
        if (uiState.shouldNavigateBack) {
            onNavigateBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = if (siteId == null) "Add Site" else "Edit Site",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                TextButton(
                    onClick = {
                        keyboardController?.hide()
                        viewModel.saveSite()
                    },
                    enabled = uiState.canSave
                ) {
                    Text("Save")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title field
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::updateTitle,
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            // URL field
            OutlinedTextField(
                value = uiState.url,
                onValueChange = viewModel::updateUrl,
                label = { Text("URL") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.urlError != null,
                supportingText = uiState.urlError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next
                ),
                trailingIcon = {
                    Row {
                        IconButton(
                            onClick = { onNavigateToWebView(uiState.url) },
                            enabled = uiState.url.isNotBlank() && uiState.urlError == null
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Open in browser"
                            )
                        }
                        IconButton(
                            onClick = { onNavigateToCompare(uiState.url) },
                            enabled = uiState.url.isNotBlank() && uiState.urlError == null
                        ) {
                            Icon(
                                imageVector = Icons.Default.Compare,
                                contentDescription = "Compare"
                            )
                        }
                    }
                }
            )

            // Notes field
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::updateNotes,
                label = { Text("Notes/Tags") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { keyboardController?.hide() }
                )
            )

            // Color selection
            Text(
                text = "Color",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(uiState.availableColors) { colorGroup ->
                    ColorPicker(
                        colorGroup = colorGroup,
                        isSelected = colorGroup == uiState.selectedColor,
                        onColorSelected = viewModel::updateSelectedColor
                    )
                }
            }

            // Browser mode toggle
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Browser Mode",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Enable JavaScript and advanced features",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.isBrowserMode,
                        onCheckedChange = viewModel::updateBrowserMode
                    )
                }
            }

            // Threshold setting (only for existing sites)
            if (siteId != null) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Diff Threshold",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Minimum change size to trigger notification (bytes)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${uiState.threshold} bytes",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            OutlinedButton(
                                onClick = viewModel::showThresholdDialog
                            ) {
                                Text("Change")
                            }
                        }
                    }
                }
            }
        }
    }

    // Threshold dialog
    if (uiState.showThresholdDialog) {
        var thresholdText by remember { mutableStateOf(uiState.threshold.toString()) }
        
        AlertDialog(
            onDismissRequest = viewModel::hideThresholdDialog,
            title = { Text("Diff Threshold") },
            text = {
                Column {
                    Text("The difference between last two items was ${uiState.lastDiffBytes} bytes. Put a value above this to ignore similar changes. Use 0 to allow all changes.")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = thresholdText,
                        onValueChange = { thresholdText = it },
                        label = { Text("Threshold (bytes)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        thresholdText.toIntOrNull()?.let { threshold ->
                            viewModel.updateThreshold(threshold)
                        }
                        viewModel.hideThresholdDialog()
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideThresholdDialog) {
                    Text("Cancel")
                }
            }
        )
    }

    // Error snackbar
    uiState.errorMessage?.let { message ->
        LaunchedEffect(message) {
            // Show snackbar here if using SnackbarHost
            viewModel.clearError()
        }
    }
}

@Composable
private fun ColorPicker(
    colorGroup: ColorGroup,
    isSelected: Boolean,
    onColorSelected: (ColorGroup) -> Unit
) {
    val brush = Brush.linearGradient(
        colors = listOf(
            Color(colorGroup.first),
            Color(colorGroup.second)
        )
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(brush)
            .selectable(
                selected = isSelected,
                onClick = { onColorSelected(colorGroup) }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AddEditScreenPreview() {
    MaterialTheme {
        AddEditScreen(
            onNavigateBack = {},
            onNavigateToWebView = {},
            onNavigateToCompare = {}
        )
    }
}
