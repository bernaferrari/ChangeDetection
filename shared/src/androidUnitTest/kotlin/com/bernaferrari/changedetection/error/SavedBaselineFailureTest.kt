package com.bernaferrari.changedetection.error

import com.bernaferrari.changedetection.shared.generated.resources.*
import org.junit.Assert.*
import org.junit.Test

class SavedBaselineFailureTest {
    @Test fun `saved baseline integrity failure keeps distinct copy and permits a transient retry`() {
        val error = BaselineIntegrityError()
        val result = ErrorHandler().handleError(error)
        assertEquals(CheckFailure.SAVED_CONTENT_UNAVAILABLE, CheckFailure.from(result, error))
        assertTrue(result.isRetryable)
        assertEquals(Res.string.check_failure_saved_content_unavailable, result.presentation?.title)
        assertEquals(Res.string.check_failure_saved_content_unavailable_detail, result.presentation?.body)
    }

    @Test fun `ordinary database read failure is not falsely called damaged saved content`() {
        val error = DatabaseError(DatabaseError.Operation.READ, "Database read unavailable")
        val result = ErrorHandler().handleError(error)
        assertNotEquals(CheckFailure.SAVED_CONTENT_UNAVAILABLE, CheckFailure.from(result, error))
    }
}
