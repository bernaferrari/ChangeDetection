/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
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
