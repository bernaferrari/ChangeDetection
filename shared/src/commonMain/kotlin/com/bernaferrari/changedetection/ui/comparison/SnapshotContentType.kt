package com.bernaferrari.changedetection.ui.comparison

import com.bernaferrari.changedetection.repo.Snap

internal fun Snap.isImageSnapshot(): Boolean = contentType.startsWith("image/", ignoreCase = true)

internal fun Snap.isHtmlSnapshot(): Boolean = contentType.contains("html", ignoreCase = true)

internal fun Snap.isTextSnapshot(): Boolean {
    val type = contentType.lowercase()
    return type.startsWith("text/") ||
        "html" in type ||
        "xml" in type ||
        "json" in type ||
        "javascript" in type ||
        "ecmascript" in type
}
