package com.bernaferrari.changedetection.error

import io.ktor.client.plugins.ResponseException

/** Stable persisted categories; never store raw exception text or response bodies as UI copy. */
enum class CheckFailure {
    CONNECTION,
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
            if (error is ContentError && error.reason == ContentError.Reason.CAPTURE_UNAVAILABLE) return CAPTURE_UNAVAILABLE
            if (error is com.bernaferrari.changedetection.detection.TextRegionException) return REGION
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
