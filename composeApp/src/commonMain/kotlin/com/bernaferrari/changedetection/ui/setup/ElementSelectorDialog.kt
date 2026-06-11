/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.setup

import androidx.compose.runtime.Composable

@Composable
expect fun ElementSelectorDialog(
    url: String,
    onDismiss: () -> Unit,
    onElementSelected: (cssSelector: String, elementPreview: String) -> Unit,
)
