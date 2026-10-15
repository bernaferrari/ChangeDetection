package com.bernaferrari.changedetection.ui.comparison

import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.util.isTextContentType

internal fun Snap.isImageSnapshot(): Boolean = contentType.startsWith("image/", ignoreCase = true)

internal fun Snap.isHtmlSnapshot(): Boolean = contentType.contains("html", ignoreCase = true)

internal fun Snap.isTextSnapshot(): Boolean = contentType.isTextContentType()
