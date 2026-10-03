/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.error

import org.junit.Assert.assertEquals
import org.junit.Test

class CheckFailureTest {
    private val handler = ErrorHandler()
    private fun classify(error: Throwable) = CheckFailure.from(handler.handleError(error), error)

    @Test fun httpFailuresKeepActionableReasons() {
        mapOf(
            401 to CheckFailure.LOGIN,
            403 to CheckFailure.ACCESS_DENIED,
            404 to CheckFailure.NOT_FOUND,
            410 to CheckFailure.NOT_FOUND,
            429 to CheckFailure.RATE_LIMITED,
            503 to CheckFailure.SERVER,
        ).forEach { (status, expected) ->
            assertEquals(expected, classify(NetworkError(status, "response contains private data")))
        }
    }

    @Test fun storageAndCaptureLimitsStayDistinct() {
        assertEquals(CheckFailure.STORAGE_FULL, classify(SnapshotStorageError(true)))
        assertEquals(CheckFailure.SAVE_FAILED, classify(SnapshotStorageError(false)))
        assertEquals(CheckFailure.CONTENT_TOO_LARGE, classify(SyncError(SyncError.Reason.CONTENT_TOO_LARGE)))
    }

    @Test fun unknownAndFutureCodesRemainHonest() {
        assertEquals(CheckFailure.UNKNOWN, CheckFailure.fromCode(null))
        assertEquals(CheckFailure.UNKNOWN, CheckFailure.fromCode("FUTURE_REASON"))
        assertEquals(CheckFailure.UNKNOWN, classify(IllegalStateException("unrecognized problem")))
    }
}
