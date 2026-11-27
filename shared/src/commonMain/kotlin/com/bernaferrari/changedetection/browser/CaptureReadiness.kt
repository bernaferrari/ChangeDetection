package com.bernaferrari.changedetection.browser

import kotlinx.serialization.Serializable

/** Fixed, privacy-safe causes; no URLs, selectors or document text. */
enum class CaptureIssue { PENDING_IMAGES, FONTS_LOADING, LAYOUT_CHANGED, VIEWPORT_NOT_READY, RESOURCE_FAILED, DOCUMENT_CHANGED }

@Serializable
enum class CaptureResourceKind { IMAGE, FONT, STYLESHEET, FRAME, OTHER }

/** Fixed outcomes of the WebView font check; a false answer is not proof of an HTTP failure. */
@Serializable
enum class FontProbeFailureReason { MATCH_NOT_READY, INVALID_QUERY, CHECK_ERROR }

@Serializable
data class FontProbeFailure(val reason: FontProbeFailureReason, val rebuiltShorthand: Boolean)

/** A failed dependency described without its address or contents. */
@Serializable
data class CaptureResourceFailure(
    val kind: CaptureResourceKind,
    val httpStatus: Int? = null,
    val certificateRejected: Boolean = false,
    val fontProbe: FontProbeFailure? = null,
) {
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

/** KMPwright reports mid-screenshot viewport or document size drift with this message. */
internal fun Throwable.isScreenshotLayoutChange(): Boolean = this is IllegalStateException && message?.startsWith("The page changed size while taking its screenshot") == true
