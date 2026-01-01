package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-specific visual diff view for comparing HTML content.
 * On Android, uses WebView for rendering.
 * On iOS, provides a simple text-based fallback.
 */
@Composable
expect fun VisualDiffView(
    oldContent: String,
    newContent: String,
    modifier: Modifier = Modifier
)
