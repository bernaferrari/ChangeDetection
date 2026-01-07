package com.bernaferrari.changedetection.browser

import androidx.compose.runtime.Composable

@Composable
expect fun ModernInteractiveBrowserScreen(
    initialUrl: String,
    initialSession: BrowserSessionData? = null,
    onNavigateUp: () -> Unit,
    onSaveComplete: (BrowserSessionData) -> Unit
)
