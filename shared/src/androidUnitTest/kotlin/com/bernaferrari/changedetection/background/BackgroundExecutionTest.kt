package com.bernaferrari.changedetection.background

import com.bernaferrari.changedetection.error.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class BackgroundExecutionTest {
    @Test fun `denied promotion skips capture and records actionable failure`() = runTest {
        var captures = 0
        var reported: BackgroundExecutionUnavailable? = null
        val answer = checkAfterForeground(prepare = { throw IllegalStateException("promotion denied") }, check = { captures++; "captured" }, denied = { reported = it; "failure" })
        assertEquals("failure", answer)
        assertEquals(0, captures)
        val error = ErrorHandler().handleError(reported!!)
        assertEquals(CheckFailure.BACKGROUND_RESTRICTED, CheckFailure.from(error, reported))
        assertTrue(error.isRetryable)
        assertNotNull(error.presentation)
    }

    @Test fun `parent cancellation while preparing is never converted into platform denial`() = runTest {
        var reported = false
        assertTrue(runCatching {
            checkAfterForeground(prepare = { throw CancellationException("stopped") }, check = { Unit }, denied = { reported = true })
        }.exceptionOrNull() is CancellationException)
        assertFalse(reported)
    }

    @Test fun `internal page timeout remains a timeout instead of unknown failure`() = runTest {
        val timeout = runCatching { withTimeout(1) { delay(2) } }.exceptionOrNull()!!
        val result = ErrorHandler().handleError(timeout)
        assertEquals(CheckFailure.TIMEOUT, CheckFailure.from(result, timeout))
        assertTrue(result.isRetryable)
    }
}
