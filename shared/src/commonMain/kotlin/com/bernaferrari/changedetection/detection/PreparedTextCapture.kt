/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.detection

import com.bernaferrari.changedetection.repo.CaptureMode
import com.bernaferrari.changedetection.util.ContentHasher

internal data class PreparedTextCapture(val data: ByteArray, val contentHash: String)

/** One transformation shared by preview and persistence; stored markup is the monitored region. */
internal fun prepareTextCapture(content: ByteArray, contentType: String, region: TextRegion): PreparedTextCapture {
    val data = if (region.isEmpty) content else region.extractHtml(content.decodeToString(), contentType.contains("html", true)).encodeToByteArray()
    val textHash = ContentHasher.detectionHash(data, contentType, CaptureMode.TEXT)
    if (region.isEmpty) return PreparedTextCapture(data, textHash)
    val include = region.include.trim()
    val exclude = region.exclude.trim()
    // Length-prefix both values so distinct rules cannot share an ambiguous concatenation.
    val ruleId = ContentHasher.hashContent("${include.length}:$include${exclude.length}:$exclude".encodeToByteArray())
    return PreparedTextCapture(data, "text-region-v2:$ruleId:${textHash.substringAfter(':')}")
}
