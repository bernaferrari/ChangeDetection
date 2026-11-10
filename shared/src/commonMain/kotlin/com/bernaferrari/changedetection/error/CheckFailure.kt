package com.bernaferrari.changedetection.error

import io.ktor.client.plugins.ResponseException

/** Who can end a run of failed checks, which decides how soon the person is told about it. */
enum class FailureUrgency {
    /** Only the person can fix it (sign in, pick the part again, correct the address); waiting changes nothing. */
    NEEDS_YOU,

    /** The site or the network is misbehaving and often recovers by itself. */
    MAY_PASS,

    /** The device, not the page, is the cause; every page fails together, so no page is singled out. */
    DEVICE,
}

/** Stable persisted categories; never store raw exception text or response bodies as UI copy. */
enum class CheckFailure(val urgency: FailureUrgency = FailureUrgency.MAY_PASS) {
    CONNECTION,
    BACKGROUND_RESTRICTED(FailureUrgency.DEVICE),
    TLS,
    TIMEOUT,
    LOGIN(FailureUrgency.NEEDS_YOU),
    ACCESS_DENIED,
    NOT_FOUND(FailureUrgency.NEEDS_YOU),
    RATE_LIMITED,
    SERVER,
    STORAGE_FULL(FailureUrgency.DEVICE),
    SAVE_FAILED(FailureUrgency.DEVICE),
    SAVED_CONTENT_UNAVAILABLE,
    CONTENT_TOO_LARGE(FailureUrgency.NEEDS_YOU),
    CONTENT(FailureUrgency.NEEDS_YOU),
    CAPTURE_UNAVAILABLE(FailureUrgency.NEEDS_YOU),
    CAPTURE_INCOMPLETE,
    REGION(FailureUrgency.NEEDS_YOU),
    MULTIPLE_VISUAL_REGIONS(FailureUrgency.NEEDS_YOU),
    UNKNOWN,
    DOCUMENT_CHANGED,

    /** The page answered but never held still (images, fonts or layout kept arriving); a slow site is [TIMEOUT]. */
    PAGE_UNSETTLED,
    ;

    companion object {
        fun fromCode(code: String?): CheckFailure = entries.firstOrNull { it.name == code } ?: UNKNOWN

        fun from(result: ErrorResult, error: Throwable?): CheckFailure {
            if (error is BaselineIntegrityError) return SAVED_CONTENT_UNAVAILABLE
            if (error is BackgroundExecutionUnavailable) return BACKGROUND_RESTRICTED
            if (error is kotlinx.coroutines.TimeoutCancellationException) return TIMEOUT
            if (error is com.bernaferrari.changedetection.browser.CaptureNotReadyException) {
                return when (error.issue) {
                    com.bernaferrari.changedetection.browser.CaptureIssue.RESOURCE_FAILED -> CAPTURE_INCOMPLETE

                    com.bernaferrari.changedetection.browser.CaptureIssue.DOCUMENT_CHANGED -> DOCUMENT_CHANGED

                    com.bernaferrari.changedetection.browser.CaptureIssue.PENDING_IMAGES,
                    com.bernaferrari.changedetection.browser.CaptureIssue.FONTS_LOADING,
                    com.bernaferrari.changedetection.browser.CaptureIssue.LAYOUT_CHANGED,
                    com.bernaferrari.changedetection.browser.CaptureIssue.VIEWPORT_NOT_READY,
                    -> PAGE_UNSETTLED
                }
            }
            if (error is ContentError && error.reason == ContentError.Reason.CAPTURE_UNAVAILABLE) return CAPTURE_UNAVAILABLE
            if (error is com.bernaferrari.changedetection.detection.TextRegionException) return if (error.issue == com.bernaferrari.changedetection.detection.TextRegionIssue.MULTIPLE_MATCHES) MULTIPLE_VISUAL_REGIONS else REGION
            if (isTlsFailure(error)) return TLS
            val responseCode = when (error) {
                is NetworkError -> error.code
                is ResponseException -> error.response.status.value
                else -> null
            }
            if (responseCode != null) {
                return when (responseCode) {
                    401 -> LOGIN

                    403 -> ACCESS_DENIED

                    404, 410 -> NOT_FOUND

                    429 -> RATE_LIMITED

                    in 500..599 -> SERVER

                    // The site answered and said no (bad request, gone for legal reasons, method refused): not a connection problem.
                    in 400..499 -> ACCESS_DENIED

                    else -> CONNECTION
                }
            }
            if (error is SnapshotStorageError) return if (error.lowSpace) STORAGE_FULL else SAVE_FAILED
            // SQLite reports a full disk as a database error; say what it is.
            if (generateSequence(error) { it.cause }.take(6).any(::isDiskFull)) return STORAGE_FULL
            if (error is SyncError && error.reason == SyncError.Reason.CONTENT_TOO_LARGE) return CONTENT_TOO_LARGE
            return when (result.type) {
                ErrorType.CONNECTIVITY, ErrorType.NETWORK -> CONNECTION
                ErrorType.TIMEOUT -> TIMEOUT
                ErrorType.AUTHENTICATION -> LOGIN
                ErrorType.PERMISSIONS -> ACCESS_DENIED
                ErrorType.RATE_LIMITED -> RATE_LIMITED
                ErrorType.DATABASE -> SAVE_FAILED
                ErrorType.CONTENT -> CONTENT
                else -> UNKNOWN
            }
        }
    }
}

private fun isDiskFull(error: Throwable): Boolean {
    if (error::class.simpleName == "SQLiteFullException") return true
    val message = error.message ?: return false
    return listOf("disk is full", "SQLITE_FULL", "ENOSPC", "No space left").any { message.contains(it, ignoreCase = true) }
}

/** JVM type checks cover every framework TLS/certificate subtype, including release builds. */
internal expect fun isPlatformTlsFailure(error: Throwable): Boolean

private val tlsClassNames = setOf("SSLException", "SSLHandshakeException", "SSLPeerUnverifiedException", "SSLProtocolException", "SSLKeyException", "CertificateException", "CertificateExpiredException", "CertificateNotYetValidException", "CertificateParsingException", "CertPathValidatorException", "CertPathBuilderException", "TLSException", "TlsException")
internal fun isDirectTlsFailure(error: Throwable): Boolean = isPlatformTlsFailure(error) || error::class.simpleName in tlsClassNames
internal fun isTlsFailure(error: Throwable?): Boolean = generateSequence(error) { it.cause }.take(6).any(::isDirectTlsFailure)
