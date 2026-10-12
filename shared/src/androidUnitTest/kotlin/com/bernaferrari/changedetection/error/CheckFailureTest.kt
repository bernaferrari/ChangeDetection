package com.bernaferrari.changedetection.error

import com.bernaferrari.changedetection.shared.generated.resources.*
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

    @Test fun errorsCarryTranslatedTitlesWithoutChangingRecoveryActions() {
        val expected = listOf(
            NetworkError(401, "private") to Res.string.check_failure_login,
            NetworkError(403, "private") to Res.string.check_failure_access_denied,
            NetworkError(404, "private") to Res.string.check_failure_not_found,
            NetworkError(429, "private") to Res.string.check_failure_rate_limited,
            NetworkError(503, "private") to Res.string.check_failure_server,
            SnapshotStorageError(true) to Res.string.check_failure_storage_full,
            SnapshotStorageError(false) to Res.string.check_failure_save_failed,
            ValidationError("url", "private") to Res.string.error_invalid_input,
            DatabaseError(DatabaseError.Operation.READ, "private") to Res.string.comparison_load_failed,
            DatabaseError(DatabaseError.Operation.DELETE, "private") to Res.string.delete_site_failed,
        )
        expected.forEach { (error, title) -> assertEquals(title, handler.handleError(error).presentation?.title) }
        assertEquals(RecoveryAction.LOGIN_REQUIRED, handler.handleError(NetworkError(401, "private")).recoveryAction)
        assertEquals(RecoveryAction.RETRY_LATER, handler.handleError(NetworkError(503, "private")).recoveryAction)
        assertEquals(CheckFailure.ACCESS_DENIED, CheckFailure.from(ErrorResult(ErrorType.PERMISSIONS, "private"), null))
    }

    @Test fun unknownAndFutureCodesRemainHonest() {
        assertEquals(CheckFailure.UNKNOWN, CheckFailure.fromCode(null))
        assertEquals(CheckFailure.UNKNOWN, CheckFailure.fromCode("FUTURE_REASON"))
        assertEquals(CheckFailure.UNKNOWN, classify(IllegalStateException("unrecognized problem")))
    }
}
