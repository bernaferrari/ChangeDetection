package com.bernaferrari.changedetection.error

import com.bernaferrari.changedetection.browser.CaptureIssue
import com.bernaferrari.changedetection.browser.CaptureNotReadyException
import com.bernaferrari.changedetection.diagnostics.*
import com.bernaferrari.changedetection.shared.generated.resources.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class ResourceCaptureFailureTest {
    @Test fun `known failed HTTP resource is incomplete capture rather than timeout`() = runTest {
        val error = CaptureNotReadyException(CaptureIssue.RESOURCE_FAILED)
        val result = ErrorHandler().handleError(error)
        assertEquals(ErrorType.NETWORK, result.type)
        assertTrue(result.isRetryable)
        assertEquals(RecoveryAction.RETRY, result.recoveryAction)
        assertEquals(CheckFailure.CAPTURE_INCOMPLETE, CheckFailure.from(result, error))
        assertEquals(Res.string.check_failure_capture_incomplete_detail, result.presentation?.body)
        assertEquals(DiagnosticException.HTTP, exceptionKind(error))
        val span = DiagnosticSpan()
        span.failed(error)
        assertEquals(CheckFailure.CAPTURE_INCOMPLETE, span.snapshot().second?.category)
    }

    @Test fun `other capture readiness issues retain timeout classification`() {
        CaptureIssue.entries.filter { it != CaptureIssue.RESOURCE_FAILED && it != CaptureIssue.DOCUMENT_CHANGED }.forEach { issue ->
            val error = CaptureNotReadyException(issue)
            val result = ErrorHandler().handleError(error)
            assertEquals(issue.name, ErrorType.TIMEOUT, result.type)
            assertEquals(issue.name, CheckFailure.TIMEOUT, CheckFailure.from(result, error))
            assertEquals(issue.name, DiagnosticException.TIMEOUT, exceptionKind(error))
        }
    }
}
