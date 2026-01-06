package com.bernaferrari.changedetection.ui.setup

import androidx.compose.runtime.Composable

@Composable
expect fun ElementSelectorDialog(
    url: String,
    onDismiss: () -> Unit,
    onElementSelected: (cssSelector: String, elementPreview: String) -> Unit
)
