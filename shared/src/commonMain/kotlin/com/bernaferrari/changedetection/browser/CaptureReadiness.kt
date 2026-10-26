package com.bernaferrari.changedetection.browser

/** Fixed, privacy-safe causes; no URLs, selectors or document text. */
enum class CaptureIssue { PENDING_IMAGES, FONTS_LOADING, LAYOUT_CHANGED, VIEWPORT_NOT_READY, RESOURCE_FAILED, DOCUMENT_CHANGED }

/** A capture readiness failure is retryable; it must never be promoted as a page change. */
class CaptureNotReadyException(val issue: CaptureIssue) : Exception("The page was not ready for a stable capture: ${issue.name}")
