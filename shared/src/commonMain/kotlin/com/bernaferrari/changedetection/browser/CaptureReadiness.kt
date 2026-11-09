package com.bernaferrari.changedetection.browser

import kotlinx.serialization.Serializable

/** Fixed, privacy-safe causes; no URLs, selectors or document text. */
enum class CaptureIssue { PENDING_IMAGES, FONTS_LOADING, LAYOUT_CHANGED, VIEWPORT_NOT_READY, RESOURCE_FAILED, DOCUMENT_CHANGED }

@Serializable
enum class CaptureResourceKind { IMAGE, FONT, STYLESHEET, FRAME, OTHER }

/** A failed dependency described without its address or contents. */
@Serializable
data class CaptureResourceFailure(val kind: CaptureResourceKind, val httpStatus: Int? = null, val certificateRejected: Boolean = false) {
    internal val canCaptureText: Boolean
        get() = !certificateRejected && (kind == CaptureResourceKind.FONT || (kind == CaptureResourceKind.IMAGE && httpStatus != null))
}

/** Dimensions of the assembled capture, or zero when validation stopped before assembly. */
@Serializable
data class CaptureExtent(val width: Int, val height: Int) {
    internal companion object {
        val Empty = CaptureExtent(0, 0)
    }
}

/** A capture readiness failure is retryable; it must never be promoted as a page change. */
class CaptureNotReadyException(
    val issue: CaptureIssue,
    val resources: List<CaptureResourceFailure> = emptyList(),
    val extent: CaptureExtent? = null,
) : Exception("The page was not ready for a stable capture: ${issue.name}")
