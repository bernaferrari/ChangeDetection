package com.bernaferrari.changedetection.browser

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Native Android viewport shared by interactive setup and fresh checks. */
internal object WatchCaptureViewport {
    const val WIDTH = 1080
    const val HEIGHT = 1920
}

/** Fits the interactive renderer on screen without changing its capture/layout viewport. */
@Composable internal expect fun watchCaptureViewportModifier(): Modifier
