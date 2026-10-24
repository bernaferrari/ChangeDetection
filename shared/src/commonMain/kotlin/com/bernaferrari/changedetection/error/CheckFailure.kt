package com.bernaferrari.changedetection.error

import io.ktor.client.plugins.ResponseException

/** Stable persisted categories; never store raw exception text or response bodies as UI copy. */
enum class CheckFailure {
    CONNECTION,
    TLS,
    TIMEOUT,
    LOGIN,
    ACCESS_DENIED,
    NOT_FOUND,
    RATE_LIMITED,
    SERVER,
    STORAGE_FULL,
    SAVE_FAILED,
    CONTENT_TOO_LARGE,
    CONTENT,
    CAPTURE_UNAVAILABLE,
    REGION,
    UNKNOWN,
    ;

    companion object {
        fun fromCode(code: String?): CheckFailure = entries.firstOrNull { it.name == code } ?: UNKNOWN

        fun from(result: ErrorResult, error: Throwable?): CheckFailure {
            if (error is com.bernaferrari.changedetection.browser.CaptureNotReadyException) return TIMEOUT
            if (error is ContentError && error.reason == ContentError.Reason.CAPTURE_UNAVAILABLE) return CAPTURE_UNAVAILABLE
            if (error is com.bernaferrari.changedetection.detection.TextRegionException) return REGION
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
