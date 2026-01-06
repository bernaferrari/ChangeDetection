package com.bernaferrari.changedetection.ui.setup

import androidx.compose.runtime.Composable
import androidx.compose.material3.Text

@Composable
actual fun ElementSelectorDialog(
    url: String,
    onDismiss: () -> Unit,
    onElementSelected: (cssSelector: String, elementPreview: String) -> Unit
) {
    Text("Not implemented on iOS")
}
