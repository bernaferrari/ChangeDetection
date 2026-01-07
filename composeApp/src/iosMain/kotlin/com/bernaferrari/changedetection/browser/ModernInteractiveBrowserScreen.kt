package com.bernaferrari.changedetection.browser

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
actual fun ModernInteractiveBrowserScreen(
    initialUrl: String,
    initialSession: BrowserSessionData?,
    onNavigateUp: () -> Unit,
    onSaveComplete: (BrowserSessionData) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Browser not implemented on iOS yet")
    }
}
