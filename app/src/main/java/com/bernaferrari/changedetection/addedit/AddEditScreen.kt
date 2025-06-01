package com.bernaferrari.changedetection.addedit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bernaferrari.changedetection.extensions.isValidUrl
import com.bernaferrari.changedetection.repo.ColorGroup
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.ui.compose.ColorPickerButton
import com.bernaferrari.changedetection.util.GradientColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScreen(
    site: Site? = null,
    onNavigateUp: () -> Unit,
    onNavigateToWebView: (String) -> Unit,
    onShowCompareDialog: (String) -> Unit,
    viewModel: AddEditViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    var title by remember { mutableStateOf(site?.title ?: "") }
    var url by remember { mutableStateOf(site?.url ?: "") }
    var tags by remember { mutableStateOf(site?.notes ?: "") }
    var useBrowser by remember { mutableStateOf(site?.isBrowser ?: false) }
    var threshold by remember { mutableStateOf(site?.threshold?.toString() ?: "0") }
    var selectedColors by remember { mutableStateOf(site?.colors ?: GradientColors.gradients.random()) }
    
    var showThresholdDialog by remember { mutableStateOf(false) }
    var urlError by remember { mutableStateOf<String?>(null) }
    
    // Initialize with clipboard URL if adding new site
    LaunchedEffect(Unit) {
        if (site == null) {
            clipboardManager.getText()?.text?.let { clipText ->
                if (clipText.isValidUrl()) {
                    url = clipText
                }
            }
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top bar
        TopAppBar(
            title = { Text(if (site == null) "Add Site" else "Edit Site") },
            navigationIcon = {
                IconButton(onClick = onNavigateUp) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                TextButton(
                    onClick = {
                        if (url.isValidUrl()) {
                            if (site == null) {
                                viewModel.addSite(
                                    title = title,
                                    url = url,
                                    tags = tags,
                                    useBrowser = useBrowser,
                                    colors = selectedColors,
                                    threshold = threshold.toIntOrNull() ?: 0
                                )
                            } else {
                                viewModel.updateSite(
                                    site = site,
                                    title = title,
                                    url = url,
                                    tags = tags,
                                    useBrowser = useBrowser,
                                    colors = selectedColors,
                                    threshold = threshold.toIntOrNull() ?: 0
                                )
                            }
                            onNavigateUp()
                        } else {
                            urlError = "Invalid URL"
                        }
                    }
                ) {
                    Text("Save")
                }
            }
        )
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // URL field
            OutlinedTextField(
                value = url,
                onValueChange = { 
                    url = it
                    urlError = null
                },
                label = { Text("URL") },
                isError = urlError != null,
                supportingText = urlError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    Row {
                        IconButton(
                            onClick = { onNavigateToWebView(url) },
                            enabled = url.isValidUrl()
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in browser")
                        }
                        IconButton(
                            onClick = { onShowCompareDialog(url) },
                            enabled = url.isValidUrl()
                        ) {
                            Icon(Icons.Default.Compare, contentDescription = "Compare")
                        }
                    }
                }
            )
            
            // Title field
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Tags field
            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text("Tags (optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
            
            // Browser option
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Use browser rendering",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Enable JavaScript and CSS rendering",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = useBrowser,
                    onCheckedChange = { useBrowser = it }
                )
            }
            
            // Threshold setting (only for existing sites)
            if (site != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Diff threshold",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Minimum size difference to detect changes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { showThresholdDialog = true }) {
                        Text("$threshold bytes")
                    }
                }
            }
            
            // Color picker
            Text(
                text = "Color scheme",
                style = MaterialTheme.typography.titleMedium
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                items(GradientColors.gradients) { colorGroup ->
                    ColorPickerButton(
                        colorGroup = colorGroup,
                        isSelected = selectedColors == colorGroup,
                        onClick = { selectedColors = colorGroup }
                    )
                }
            }
        }
    }
    
    // Threshold dialog
    if (showThresholdDialog) {
        AlertDialog(
            onDismissRequest = { showThresholdDialog = false },
            title = { Text("Diff Threshold") },
            text = {
                Column {
                    Text("Set minimum size difference in bytes to detect changes")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = threshold,
                        onValueChange = { threshold = it },
                        label = { Text("Bytes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showThresholdDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}
