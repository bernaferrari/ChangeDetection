package com.bernaferrari.changedetection.addedit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.extensions.isValidUrl
import com.bernaferrari.changedetection.repo.ColorGroup
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.ui.compose.ColorPickerButton
import com.bernaferrari.changedetection.util.GradientColors

data class FormData(
    val title: String = "",
    val url: String = "",
    val tags: String = "",
    val useBrowser: Boolean = false,
    val threshold: String = "0",
    val selectedColors: ColorGroup = GradientColors.gradients.first(),
    val urlError: String? = null
)

@Composable
fun AddEditForm(
    modifier: Modifier = Modifier, // This modifier will include paddingValues from Scaffold
    formData: FormData,
    isEditing: Boolean = false,
    onFormDataChange: (FormData) -> Unit,
    onNavigateToWebView: (String) -> Unit = {},
    onShowCompareDialog: (String) -> Unit = {},
    onShowComparison: (String) -> Unit = {},
    onShowThresholdDialog: () -> Unit = {},
) {
    var showThresholdDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier // This modifier from AddEditScreen already has paddingValues applied
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp), // This is for inner content padding
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // URL Section
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Website URL",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                OutlinedTextField(
                    value = formData.url,
                    onValueChange = {
                        onFormDataChange(formData.copy(url = it, urlError = null))
                    },
                    label = { Text("Enter website URL") },
                    placeholder = { Text("https://example.com") },
                    isError = formData.urlError != null,
                    supportingText = formData.urlError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
//                            LocalFocusManager.current.clearFocus()
                        }
                    ),
                    trailingIcon = if (formData.url.isValidUrl()) {
                        {
                            Row {
                                // Only show comparison for editing existing sites
                                if (isEditing) {
                                    IconButton(
                                        onClick = { onShowComparison(formData.url) }
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Outlined.CompareArrows,
                                            contentDescription = "Compare versions"
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onNavigateToWebView(formData.url) }
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Outlined.OpenInNew,
                                        contentDescription = "Test webpage"
                                    )
                                }
                            }
                        }
                    } else null,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // Site Details Section
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Site Details",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                OutlinedTextField(
                    value = formData.title,
                    onValueChange = { onFormDataChange(formData.copy(title = it)) },
                    label = { Text("Site title (optional)") },
                    placeholder = { Text("My Website") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = formData.tags,
                    onValueChange = { onFormDataChange(formData.copy(tags = it)) },
                    label = { Text("Tags or notes (optional)") },
                    placeholder = { Text("work, important") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // Monitoring Settings Section
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Monitoring Settings",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Browser rendering option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onFormDataChange(formData.copy(useBrowser = !formData.useBrowser)) },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Browser rendering",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Enable JavaScript and CSS",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = formData.useBrowser,
                        onCheckedChange = { onFormDataChange(formData.copy(useBrowser = it)) }
                    )
                }

                // Threshold setting (only for editing)
                if (isEditing) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showThresholdDialog = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Change threshold",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Minimum difference to detect",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${formData.threshold} bytes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Color Scheme Section
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Color Theme",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(GradientColors.gradients) { colorGroup ->
                        ColorPickerButton(
                            colorGroup = colorGroup,
                            isSelected = formData.selectedColors == colorGroup,
                            onClick = { onFormDataChange(formData.copy(selectedColors = colorGroup)) },
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }
            }
        }
    }

    // Threshold dialog
    if (showThresholdDialog) {
        AlertDialog(
            onDismissRequest = { showThresholdDialog = false },
            title = { Text("Change Threshold") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Set the minimum size difference in bytes to detect changes.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = formData.threshold,
                        onValueChange = { onFormDataChange(formData.copy(threshold = it)) },
                        label = { Text("Threshold (bytes)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                FilledTonalButton(onClick = {
                    showThresholdDialog = false
                    onShowThresholdDialog()
                }) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showThresholdDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AddEditFormPreview() {
    MaterialTheme {
        var formData by remember {
            mutableStateOf(
                FormData(
                    url = "https://example.com",
                    title = "Example Website",
                    tags = "work, important",
                    useBrowser = true
                )
            )
        }

        AddEditForm(
            formData = formData,
            isEditing = false,
            onFormDataChange = { formData = it }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AddEditFormEditingPreview() {
    MaterialTheme {
        var formData by remember {
            mutableStateOf(
                FormData(
                    url = "https://github.com",
                    title = "GitHub",
                    tags = "development, code",
                    useBrowser = false,
                    threshold = "100"
                )
            )
        }

        AddEditForm(
            formData = formData,
            isEditing = true,
            onFormDataChange = { formData = it }
        )
    }
}
