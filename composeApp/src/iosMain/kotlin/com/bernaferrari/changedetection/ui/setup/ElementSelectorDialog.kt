/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.setup

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * iOS implementation of ElementSelectorDialog.
 *
 * Note: Full element selection (tapping elements on a page) requires
 * JavaScript injection which is limited on iOS. This implementation
 * provides a manual selector input as an alternative.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun ElementSelectorDialog(
    url: String,
    onDismiss: () -> Unit,
    onElementSelected: (cssSelector: String, elementPreview: String) -> Unit,
) {
    var manualSelector by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Element") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "Element selection is not available on iOS. " +
                        "Enter a CSS selector manually:",
                    style = MaterialTheme.typography.bodyMedium,
                )

                OutlinedTextField(
                    value = manualSelector,
                    onValueChange = { manualSelector = it },
                    label = { Text("CSS Selector") },
                    placeholder = { Text("e.g., .price, #content, article") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Text(
                    "Examples:\n" +
                        "• .class-name - Select by class\n" +
                        "• #id - Select by ID\n" +
                        "• tag - Select by tag name",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (manualSelector.isNotBlank()) {
                        onElementSelected(manualSelector, manualSelector)
                    }
                },
                enabled = manualSelector.isNotBlank(),
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
