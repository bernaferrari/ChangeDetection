package com.bernaferrari.changedetection.repo

import com.bernaferrari.changedetection.detection.TextRegion
import com.bernaferrari.changedetection.util.ContentHasher

/** The settings that define captured content, independent of presentation and alert filters. */
internal data class CapturePolicy(val mode: CaptureMode, val region: TextRegion) {
    val regionIdentity: String get() = ContentHasher.hashContent("${region.include.length}:${region.include}${region.exclude.length}:${region.exclude}".encodeToByteArray())
    fun imageIdentity(browserCapture: Boolean): String = "${mode.name}:${if (browserCapture || mode == CaptureMode.SCREENSHOT) "browser" else "source"}:$regionIdentity"
}

internal fun Site.capturePolicy(): CapturePolicy = CapturePolicy(
    captureMode,
    when (captureMode) {
        CaptureMode.TEXT, CaptureMode.SCREENSHOT, CaptureMode.DEFAULT -> TextRegion(textIncludeSelector.trim(), textExcludeSelector.trim())
        else -> TextRegion()
    },
)

/** Browser fragments can identify distinct client-side routes. HTTP source captures never send them. Preserve Text routes consistently for creation/editing on every platform. */
internal fun CaptureMode.usesBrowserIdentity(): Boolean = this == CaptureMode.SCREENSHOT || this == CaptureMode.DEFAULT || (this == CaptureMode.TEXT && supportsRenderedTextMonitoring)
