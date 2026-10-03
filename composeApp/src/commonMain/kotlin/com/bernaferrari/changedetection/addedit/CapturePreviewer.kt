/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.addedit

import com.bernaferrari.changedetection.repo.CaptureMode
import com.bernaferrari.changedetection.repo.ColorGroup
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.sync.SyncEngine

sealed interface CapturePreview {
    data class Html(val text: String, val totalBytes: Int, val selectableDocument: com.bernaferrari.changedetection.detection.SelectableTextDocument? = null) : CapturePreview
    data class Image(val bytes: ByteArray) : CapturePreview
}

/** A read-only sample: no monitor creation, baseline commit, scheduling or notification. */
fun interface CapturePreviewer {
    suspend fun capture(url: String, mode: CaptureMode): CapturePreview
    suspend fun capture(url: String, mode: CaptureMode, region: com.bernaferrari.changedetection.detection.TextRegion): CapturePreview {
        check(region.isEmpty) { "Region preview is unavailable" }
        return capture(url, mode)
    }
}

internal fun previewSite(url: String) = Site(null, url, 0L, "capture-preview", ColorGroup(0, 0), true, false)

class HtmlCapturePreviewer(private val engine: SyncEngine) : CapturePreviewer {
    override suspend fun capture(url: String, mode: CaptureMode): CapturePreview = capture(url, mode, com.bernaferrari.changedetection.detection.TextRegion())

    override suspend fun capture(url: String, mode: CaptureMode, region: com.bernaferrari.changedetection.detection.TextRegion): CapturePreview {
        check(mode != CaptureMode.SCREENSHOT) { "Screenshot monitoring is unavailable on this platform" }
        return when (val result = engine.fetchFromServer(previewSite(url))) {
            is SyncEngine.FetchResult.Error -> error(result.errorResult?.userMessage ?: result.message)

            is SyncEngine.FetchResult.Success -> {
                check(result.data.isNotEmpty()) { "The page returned no content" }
                check(mode != CaptureMode.TEXT || !result.contentType.startsWith("image/", true)) { "This address returns an image. Choose screenshots or source monitoring." }
                if (result.contentType.startsWith("image/", true)) {
                    CapturePreview.Image(result.data)
                } else {
                    CapturePreview.Html(
                        (
                            if (mode == CaptureMode.TEXT) {
                                com.bernaferrari.changedetection.util.readableSnapshotText(
                                    com.bernaferrari.changedetection.detection.prepareTextCapture(result.data, result.contentType, region).data.decodeToString(),
                                    result.contentType.contains("html", true),
                                )
                            } else if (mode == CaptureMode.SOURCE) {
                                result.data.decodeToString()
                            } else {
                                engine.parseHtml(result.data.decodeToString())
                            }
                            ).take(12_000),
                        result.data.size,
                        if (mode == CaptureMode.TEXT && result.contentType.contains("html", true)) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                                com.bernaferrari.changedetection.detection.selectableTextDocument(result.data.decodeToString())
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}
