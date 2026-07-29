/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/**
 * Lets previews and offline UI tests render the stable generated favicon without scheduling a
 * network image request. Production keeps remote favicon loading enabled by default.
 */
val LocalRemoteFaviconLoading = staticCompositionLocalOf { true }

@Composable
internal expect fun PlatformFaviconImage(
    model: String,
    contentDescription: String?,
    modifier: Modifier,
)
