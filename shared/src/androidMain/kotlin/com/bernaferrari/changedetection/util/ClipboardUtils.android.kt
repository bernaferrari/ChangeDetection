package com.bernaferrari.changedetection.util

import androidx.compose.ui.platform.Clipboard

actual suspend fun Clipboard.readText(): String? {
    val clipEntry = getClipEntry()
    return clipEntry
        ?.clipData
        ?.getItemAt(0)
        ?.text
        ?.toString()
}
