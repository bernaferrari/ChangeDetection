package com.bernaferrari.changedetection.util

import androidx.compose.ui.platform.Clipboard
import platform.UIKit.UIPasteboard

actual suspend fun Clipboard.readText(): String? {
    return UIPasteboard.generalPasteboard.string
}
