package com.bernaferrari.changedetection.browser

import com.bernaferrari.changedetection.diagnostics.*
import com.bernaferrari.changedetection.error.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import javax.net.ssl.SSLException

class DocumentGenerationFailureTest {
    @Test fun staleDocumentIsNotACertificateFailureAndCannotBePromoted() = runTest {
        val health = PageCaptureHealth()
        health.begin("https://private.invalid/path?token=SECRET")
        val old = health.epoch()
        health.begin("https://private.invalid/next")
        val error = runCatching { health.validate(old) }.exceptionOrNull()!!
        assertFalse("A different document is not a TLS failure", error is SSLException)
        val result = ErrorHandler().handleError(error)
        assertEquals("DOCUMENT_CHANGED", CheckFailure.from(result, error).name)
        assertEquals("DOCUMENT_CHANGED", exceptionKind(error).name)
        assertTrue(result.isRetryable)
        val span = DiagnosticSpan()
        span.failed(error)
        assertEquals("DOCUMENT_CHANGED", span.snapshot().second?.category?.name)
        health.validate()
    }

    @Test fun realRefusedCertificateStillHasTlsClassification() = runTest {
        val health = PageCaptureHealth()
        health.begin("https://private.invalid")
        health.request("https://other.invalid", true)
        health.ssl("https://other.invalid", 3, listOf(3))
        val error = runCatching { health.validate() }.exceptionOrNull()!!
        assertTrue(error is SSLException)
        assertEquals(CheckFailure.TLS, CheckFailure.from(ErrorHandler().handleError(error), error))
    }

    @Test fun staleCaptureHasPrivateNavigationContextWithoutBorrowingNewDocumentTls() = runTest {
        val health = PageCaptureHealth()
        health.begin("https://private.invalid/old?token=SECRET")
        val old = health.epoch()
        health.begin("https://private.invalid/new")
        health.started("https://private.invalid/new")
        health.committed("https://private.invalid/new")
        health.request("https://other.invalid/redirect", true)
        health.ssl("https://other.invalid/redirect", 3, listOf(3))
        val span = DiagnosticSpan()
        val stale = kotlinx.coroutines.withContext(span) { runCatching { health.validate(old) }.exceptionOrNull()!! }
        assertEquals("DOCUMENT_CHANGED", exceptionKind(stale).name)
        val event = span.snapshot().first.single()
        assertEquals(DiagnosticMetric.DOCUMENT_CHANGED, event.metric)
        assertTrue(event.document!!.navigationStarted && event.document!!.committed && event.document!!.mainPending)
        assertNull(event.tls)
        val encoded = kotlinx.serialization.json.Json.encodeToString(DiagnosticEvent.serializer(), event)
        assertFalse(encoded.contains("private.invalid") || encoded.contains("SECRET") || encoded.contains("https://"))
        assertTrue(runCatching { health.validate() }.exceptionOrNull() is SSLException)
    }
}
