/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.browser

import androidx.compose.runtime.Composable

@Composable
expect fun ModernInteractiveBrowserScreen(
    initialUrl: String,
    initialSession: BrowserSessionData? = null,
    onNavigateUp: () -> Unit,
    onSaveComplete: (BrowserSessionData) -> Unit,
)
