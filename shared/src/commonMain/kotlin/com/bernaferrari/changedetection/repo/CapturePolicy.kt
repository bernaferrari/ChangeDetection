/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.repo

import com.bernaferrari.changedetection.detection.TextRegion
import com.bernaferrari.changedetection.util.ContentHasher

/** The settings that define captured content, independent of presentation and alert filters. */
internal data class CapturePolicy(val mode: CaptureMode, val region: TextRegion) {
    val regionIdentity: String get() = ContentHasher.hashContent("${region.include.length}:${region.include}${region.exclude.length}:${region.exclude}".encodeToByteArray())
}

internal fun Site.capturePolicy(): CapturePolicy = CapturePolicy(
    captureMode,
    when (captureMode) {
        CaptureMode.TEXT, CaptureMode.SCREENSHOT, CaptureMode.DEFAULT -> TextRegion(textIncludeSelector.trim(), textExcludeSelector.trim())
        else -> TextRegion()
    },
)

/** Browser fragments can identify distinct client-side routes. HTTP captures never send them. */
internal fun CaptureMode.usesBrowserIdentity(): Boolean = this == CaptureMode.SCREENSHOT || this == CaptureMode.DEFAULT
