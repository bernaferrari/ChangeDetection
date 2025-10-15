package com.bernaferrari.changedetection.addedit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.extensions.isValidUrl
import com.bernaferrari.changedetection.repo.ColorGroup
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
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier // This modifier from AddEditScreen already has paddingValues applied
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        SectionCard(
            icon = Icons.Outlined.Language,
            title = stringResource(id = R.string.website_url),
            description = stringResource(id = R.string.add_edit_url_description)
        ) {
            OutlinedTextField(
                value = formData.url,
                onValueChange = {
                    onFormDataChange(formData.copy(url = it, urlError = null))
                },
                label = { Text(stringResource(id = R.string.enter_website_url)) },
                placeholder = { Text(stringResource(id = R.string.website_url_placeholder)) },
                isError = formData.urlError != null,
                supportingText = formData.urlError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {}),
                trailingIcon = if (formData.url.isValidUrl()) {
                    {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (isEditing) {
                                IconButton(onClick = { onShowComparison(formData.url) }) {
                                    Icon(
                                        Icons.AutoMirrored.Outlined.CompareArrows,
                                        contentDescription = stringResource(id = R.string.add_edit_compare_versions)
                                    )
                                }
                            }
                            IconButton(onClick = { onNavigateToWebView(formData.url) }) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.OpenInNew,
                                    contentDescription = stringResource(id = R.string.open_in_browser)
                                )
                            }
                        }
                    }
                } else null,
                shape = RoundedCornerShape(12.dp)
            )
        }

        SectionCard(
            icon = Icons.Outlined.Edit,
            title = stringResource(id = R.string.add_edit_details_title),
            description = stringResource(id = R.string.add_edit_details_description)
        ) {
            OutlinedTextField(
                value = formData.title,
                onValueChange = { onFormDataChange(formData.copy(title = it)) },
                label = { Text(stringResource(id = R.string.site_name)) },
                placeholder = { Text(stringResource(id = R.string.site_name_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = formData.tags,
                onValueChange = { onFormDataChange(formData.copy(tags = it)) },
                label = { Text(stringResource(id = R.string.add_edit_tags_label)) },
                placeholder = { Text(stringResource(id = R.string.add_edit_tags_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                shape = RoundedCornerShape(12.dp)
            )
        }

        SectionCard(
            icon = Icons.Outlined.Settings,
            title = stringResource(id = R.string.add_edit_monitoring_title),
            description = stringResource(id = R.string.add_edit_monitoring_description)
        ) {
            PreferenceItem(
                title = stringResource(id = R.string.add_edit_browser_rendering),
                subtitle = stringResource(id = R.string.add_edit_browser_rendering_description),
                leadingIcon = Icons.Outlined.Language,
                trailingContent = {
                    Switch(
                        checked = formData.useBrowser,
                        onCheckedChange = { onFormDataChange(formData.copy(useBrowser = it)) }
                    )
                },
                onClick = { onFormDataChange(formData.copy(useBrowser = !formData.useBrowser)) }
            )

            if (isEditing) {
                PreferenceItem(
                    title = stringResource(id = R.string.add_edit_change_threshold),
                    subtitle = stringResource(id = R.string.add_edit_change_threshold_description),
                    leadingIcon = Icons.Outlined.Tune,
                    trailingContent = {
                        val thresholdLabel = stringResource(
                            id = R.string.add_edit_threshold_value_bytes,
                            formData.threshold.ifBlank { "0" }
                        )
                        AssistChip(
                            onClick = { showThresholdDialog = true },
                            label = { Text(thresholdLabel) }
                        )
                    },
                    onClick = { showThresholdDialog = true }
                )
            }
        }

        SectionCard(
            icon = Icons.Outlined.Palette,
            title = stringResource(id = R.string.color_theme),
            description = stringResource(id = R.string.add_edit_color_theme_description)
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(GradientColors.gradients) { colorGroup ->
                    ColorPickerButton(
                        colorGroup = colorGroup,
                        isSelected = formData.selectedColors == colorGroup,
                        onClick = { onFormDataChange(formData.copy(selectedColors = colorGroup)) },
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        }
    }

    // Threshold dialog
    if (showThresholdDialog) {
        AlertDialog(
            onDismissRequest = { showThresholdDialog = false },
            title = { Text(stringResource(id = R.string.add_edit_threshold_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(id = R.string.add_edit_threshold_dialog_body),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = formData.threshold,
                        onValueChange = {
                            val sanitized = it.filter { char -> char.isDigit() }
                            onFormDataChange(formData.copy(threshold = sanitized))
                        },
                        label = { Text(stringResource(id = R.string.add_edit_threshold_label)) },
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
                    Text(stringResource(id = R.string.add_edit_threshold_apply))
                }
            },
            dismissButton = {
                TextButton(onClick = { showThresholdDialog = false }) {
                    Text(stringResource(id = R.string.cancel))
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

@Composable
private fun SectionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 6.dp,
        shadowElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.07f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    tonalElevation = 6.dp,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            content()
        }
    }
}

@Composable
private fun PreferenceItem(
    title: String,
    subtitle: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    trailingContent: @Composable () -> Unit,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.06f))
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            },
            supportingContent = {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingContent = {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    tonalElevation = 3.dp,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(
                        leadingIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(18.dp)
                    )
                }
            },
            trailingContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.heightIn(min = 24.dp)
                ) {
                    trailingContent()
                }
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            ),
            modifier = if (onClick != null) {
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
            } else {
                Modifier.fillMaxWidth()
            }
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
