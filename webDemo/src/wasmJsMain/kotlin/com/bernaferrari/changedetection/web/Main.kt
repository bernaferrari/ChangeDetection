package com.bernaferrari.changedetection.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.bernaferrari.changedetection.ChangeDetectionDemoHost
import com.bernaferrari.changedetection.initChangeDetectionDemo

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initChangeDetectionDemo()
    ComposeViewport {
        ChangeDetectionDemoHost()
    }
}
