package com.bernaferrari.changedetection.util

import androidx.compose.ui.platform.Clipboard

expect suspend fun Clipboard.readText(): String?
