package com.bernaferrari.changedetection.image

/** Browser screenshots have a white canvas; raw/source image alpha remains meaningful. */
internal fun compositeArgbOnWhite(argb: Int): Int {
    val alpha = argb ushr 24
    if (alpha == 255) return argb
    fun channel(shift: Int) = ((((argb ushr shift) and 255) * alpha + 255 * (255 - alpha) + 127) / 255)
    return (255 shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
}

internal fun IntArray.flattenOnWhite(checkCancelled: () -> Unit = {}) {
    for (i in indices) {
        if (i % 4096 == 0) checkCancelled()
        this[i] = compositeArgbOnWhite(this[i])
    }
}

internal fun renderedScreenshotPair(before: com.bernaferrari.changedetection.repo.Snap, after: com.bernaferrari.changedetection.repo.Snap) = before.contentHash.startsWith("image-v3:") && after.contentHash.startsWith("image-v3:")
