package com.bernaferrari.changedetection.diagnostics

import com.bernaferrari.changedetection.error.*
import com.bernaferrari.changedetection.repo.CaptureMode
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class DiagnosticTraceTest {
    private val previous = DiagnosticLog.sink

    @After fun restore() {
        DiagnosticLog.sink = previous
    }
    private class Sink : DiagnosticSink {
        val records = mutableListOf<DiagnosticAttempt>()
        override suspend fun watchToken(watchId: String?) = "a".repeat(32)
        override suspend fun append(attempt: DiagnosticAttempt) {
            records += attempt
        }
    }

    @Test fun firstActualStageAndAllowlistedCauseChainSurviveOuterWrapping() = runTest {
        val sink = Sink()
        DiagnosticLog.sink = sink
        val secret = "https://private.invalid/path?token=SECRET cookie=PASSWORD <html>PERSONAL</html>"
        try {
            recordDiagnostic(CaptureMode.TEXT, true, false, DiagnosticTrigger.CHECK, secret) {
                try {
                    diagnosticStage(DiagnosticStage.PREPARE_CONTENT) { throw IllegalArgumentException(secret) }
                } catch (error: Exception) {
                    diagnosticFailure(DatabaseError(DatabaseError.Operation.READ, secret, error), CheckFailure.UNKNOWN)
                    throw error
                }
            }
            fail("Expected original failure")
        } catch (_: IllegalArgumentException) { }
        val final = sink.records.last()
        assertEquals(DiagnosticStage.PREPARE_CONTENT, final.failure!!.stage)
        assertEquals(listOf(DiagnosticException.INVALID_CONTENT), final.failure.causes)
        assertFalse(Json.encodeToString(final).contains("SECRET"))
        assertFalse(Json.encodeToString(final).contains("private.invalid"))
        assertTrue(sink.records.any { it.outcome == DiagnosticOutcome.INCOMPLETE })
    }

    @Test fun recorderFailureCannotFailOrReplaceTheMonitoringResult() = runTest {
        DiagnosticLog.sink = object : DiagnosticSink {
            override suspend fun watchToken(watchId: String?): String = error("storage failed")
            override suspend fun append(attempt: DiagnosticAttempt) {
                error("storage failed")
            }
        }
        assertEquals(42, recordDiagnostic(CaptureMode.TEXT, false, false, DiagnosticTrigger.CHECK) { diagnosticStage(DiagnosticStage.COMPARE) { 42 } })
        val original = IllegalStateException("original")
        try {
            recordDiagnostic(CaptureMode.TEXT, false, false, DiagnosticTrigger.CHECK) { throw original }
            fail()
        } catch (error: IllegalStateException) {
            assertEquals(original.message, error.message)
        }
    }

    @Test fun cancellationIsRecordedAndPropagatesWithoutMixingConcurrentAttempts() = runTest {
        val sink = Sink()
        DiagnosticLog.sink = sink
        val waiting = CompletableDeferred<Unit>()
        val first = launch {
            recordDiagnostic(CaptureMode.TEXT, true, false, DiagnosticTrigger.CHECK) {
                diagnosticStage(DiagnosticStage.NAVIGATE) {
                    waiting.complete(Unit)
                    awaitCancellation()
                }
            }
        }
        waiting.await()
        recordDiagnostic(CaptureMode.SOURCE, false, false, DiagnosticTrigger.PREVIEW) { diagnosticStage(DiagnosticStage.PREPARE_CONTENT) { 1 } }
        first.cancelAndJoin()
        val final = sink.records.filter { it.outcome != DiagnosticOutcome.INCOMPLETE }
        assertEquals(2, final.size)
        assertEquals(DiagnosticOutcome.CANCELLED, final.single { it.mode == CaptureMode.TEXT }.outcome)
        assertEquals(DiagnosticOutcome.SUCCESS, final.single { it.mode == CaptureMode.SOURCE }.outcome)
        assertEquals(2, final.map { it.attemptId }.toSet().size)
    }

    @Test fun stageEventsAndCauseDepthStayBoundedAndOptionalNotificationsDoNotFailChecks() = runTest {
        val sink = Sink()
        DiagnosticLog.sink = sink
        recordDiagnostic(CaptureMode.TEXT, false, false, DiagnosticTrigger.CHECK) {
            repeat(500) { diagnosticMetric(DiagnosticStage.NAVIGATE, DiagnosticMetric.HTTP_STATUS, 200) }
            try {
                diagnosticStage(DiagnosticStage.NOTIFY) { error("notifier failed") }
            } catch (_: Exception) { }
            1
        }
        assertTrue(sink.records.last().events.size <= 64)
        assertEquals(DiagnosticOutcome.SUCCESS, sink.records.last().outcome)
        assertNull(sink.records.last().failure)
    }

    @Test fun browserCallbackMetricsSurviveEveryCheckpoint() = runTest {
        val sink = Sink()
        DiagnosticLog.sink = sink
        recordDiagnostic(CaptureMode.TEXT, false, false, DiagnosticTrigger.CHECK) {
            currentCoroutineContext()[DiagnosticSpan]!!.callback(DiagnosticStage.NAVIGATE, DiagnosticMetric.HTTP_STATUS, 200)
            diagnosticStage(DiagnosticStage.SETTLE) { 1 }
            diagnosticStage(DiagnosticStage.EXTRACT_TEXT) { 1 }
        }
        assertEquals(1, sink.records.last().events.count { it.metric == DiagnosticMetric.HTTP_STATUS && it.code == 200 })
    }
}
