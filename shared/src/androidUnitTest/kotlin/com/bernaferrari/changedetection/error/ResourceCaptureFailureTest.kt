package com.bernaferrari.changedetection.error

import com.bernaferrari.changedetection.browser.CaptureIssue
import com.bernaferrari.changedetection.browser.CaptureNotReadyException
import com.bernaferrari.changedetection.browser.CaptureResourceFailure
import com.bernaferrari.changedetection.browser.CaptureResourceKind
import com.bernaferrari.changedetection.browser.FontProbeFailure
import com.bernaferrari.changedetection.browser.FontProbeFailureReason
import com.bernaferrari.changedetection.diagnostics.*
import com.bernaferrari.changedetection.shared.generated.resources.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
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

    @Test fun `a rejected font probe remains a capture failure but is not labeled HTTP`() = runTest {
        val probe = FontProbeFailure(FontProbeFailureReason.INVALID_QUERY, rebuiltShorthand = true)
        val error = CaptureNotReadyException(CaptureIssue.RESOURCE_FAILED, listOf(CaptureResourceFailure(CaptureResourceKind.FONT, fontProbe = probe)))
        val span = DiagnosticSpan()
        span.failed(error)
        val failure = span.snapshot().second!!
        assertEquals(CheckFailure.CAPTURE_INCOMPLETE, failure.category)
        assertEquals(listOf(DiagnosticException.FONT), failure.causes)
        assertEquals(probe, failure.captureResources.single().fontProbe)
        val json = Json.encodeToString(failure)
        assertTrue(json.contains("INVALID_QUERY"))
        assertTrue(json.contains("\"rebuiltShorthand\":true"))
        assertEquals(failure, Json.decodeFromString<DiagnosticFailure>(json))
    }

    @Test fun `a page that never holds still is not reported as a slow site`() {
        CaptureIssue.entries.filter { it != CaptureIssue.RESOURCE_FAILED && it != CaptureIssue.DOCUMENT_CHANGED }.forEach { issue ->
            val error = CaptureNotReadyException(issue)
            val result = ErrorHandler().handleError(error)
            assertEquals(issue.name, ErrorType.TIMEOUT, result.type)
            assertEquals(issue.name, CheckFailure.PAGE_UNSETTLED, CheckFailure.from(result, error))
            assertEquals(issue.name, DiagnosticException.TIMEOUT, exceptionKind(error))
        }
    }
}
