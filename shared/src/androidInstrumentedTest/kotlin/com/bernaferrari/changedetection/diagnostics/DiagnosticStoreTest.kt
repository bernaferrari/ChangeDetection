package com.bernaferrari.changedetection.diagnostics

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bernaferrari.changedetection.repo.CaptureMode
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class DiagnosticStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun appEventsKeepTheirEventTimeBuildAndLegacyEventsStayUnattributed() = runBlocking {
        val dir = File(context.cacheDir, "event-build-${java.util.UUID.randomUUID()}")
        val at = 1_790_000_000_000L
        var build = AppEventBuild(41, "abcdef123456", AppBuildVariant.RELEASE)
        val store = AndroidDiagnosticStore(context, dir, { at }, ::runtime, { build })
        try {
            dir.mkdirs()
            File(dir, "app-events.json").writeText(Json.encodeToString(listOf(AppEvent(AppEventKind.CRASH, at, classes = listOf("IOException")))))
            store.appEvent(AppEvent(AppEventKind.BACKGROUND_FAILURE, at, stage = AppEventStage.WORKER_FOREGROUND))
            build = AppEventBuild(42, "123456abcdef", AppBuildVariant.DEBUG)
            val report = Json.parseToJsonElement(store.report().json).jsonObject
            val events = report.getValue("appEvents").jsonArray
            assertFalse(events[0].jsonObject.containsKey("build"))
            val saved = events[1].jsonObject.getValue("build").jsonObject
            assertEquals(41, saved.getValue("versionCode").jsonPrimitive.int)
            assertEquals("abcdef123456", saved.getValue("revision").jsonPrimitive.content)
            assertEquals("RELEASE", saved.getValue("variant").jsonPrimitive.content)
            assertEquals("WORKER_FOREGROUND", events[1].jsonObject.getValue("stage").jsonPrimitive.content)
            assertTrue(report.getValue("summary").toString().contains("Build not recorded (older event)"))
        } finally {
            store.close()
            dir.deleteRecursively()
        }
    }

    @Test fun storedEventBuildMetadataCannotExportRawInput() = runBlocking {
        fixture { store, dir, _ ->
            dir.mkdirs()
            val event = AppEvent(AppEventKind.CRASH, 1_790_000_000_000L, build = AppEventBuild(Long.MAX_VALUE, "https://private.invalid/person?token=SECRET", AppBuildVariant.UNKNOWN))
            File(dir, "app-events.json").writeText(Json.encodeToString(listOf(event)))
            val json = store.report().json
            assertFalse(json.contains("private.invalid"))
            assertFalse(json.contains("SECRET"))
            assertFalse(json.contains(Long.MAX_VALUE.toString()))
            assertTrue(json.contains("unavailable"))
        }
    }

    @Test fun captureGeometryIsBoundedAndContainsNoDocumentData() = runBlocking {
        fixture { store, _, _ ->
            val context = com.bernaferrari.changedetection.diagnostics.CaptureDiagnostic(Int.MAX_VALUE, -1, Int.MAX_VALUE, -1, Int.MAX_VALUE, -1, Int.MAX_VALUE, -1, Int.MAX_VALUE, true, Int.MAX_VALUE)
            store.append(com.bernaferrari.changedetection.diagnostics.DiagnosticAttempt(startedAt = 1_790_000_000_000L, mode = com.bernaferrari.changedetection.repo.CaptureMode.SCREENSHOT, selected = true, excluded = false, trigger = com.bernaferrari.changedetection.diagnostics.DiagnosticTrigger.CHECK, outcome = com.bernaferrari.changedetection.diagnostics.DiagnosticOutcome.FAILURE, events = listOf(com.bernaferrari.changedetection.diagnostics.DiagnosticEvent(com.bernaferrari.changedetection.diagnostics.DiagnosticStage.CAPTURE_PIXELS, 0, capture = context))))
            val json = store.report().json
            assertFalse(json.contains("2147483647"))
            assertTrue(json.contains("nativeWidth"))
            assertFalse(json.contains("selector"))
            assertFalse(json.contains("documentText"))
        }
    }

    @Test fun summaryShowsBuildAndTlsContextRemainsBoundedAndTyped() = runBlocking {
        fixture { store, _, _ ->
            val tls = DiagnosticTlsContext(3, List(100) { it }, TlsRequestKind.UNKNOWN, TlsUrlRelation.DIFFERENT_ORIGIN, true, false, false)
            store.append(DiagnosticAttempt(startedAt = 1_790_000_000_000L, mode = CaptureMode.TEXT, selected = false, excluded = false, trigger = DiagnosticTrigger.PREVIEW, outcome = DiagnosticOutcome.FAILURE, events = listOf(DiagnosticEvent(DiagnosticStage.NAVIGATE, 0, tls = tls))))
            val report = Json.parseToJsonElement(store.report().json).jsonObject
            val header = report.getValue("summary").jsonArray.first().jsonPrimitive.content
            assertTrue(header, header.contains("version code 38"))
            assertTrue(header, header.contains("revision abcdef123456"))
            assertFalse(report.toString().contains("private.invalid"))
            val context = report.getValue("attempts").jsonArray.single().jsonObject.getValue("attempt").jsonObject.getValue("events").jsonArray.single().jsonObject.getValue("tls").jsonObject
            assertEquals((0..5).toList(), context.getValue("errors").jsonArray.map { it.jsonPrimitive.int })
        }
    }

    @Test fun summaryNamesTheTlsProblemAndSplitsChecksFromPreviews() = runBlocking {
        fixture { store, _, _ ->
            // The shape of a shared report: two failed checks, previews that failed or were abandoned, one first version.
            fun attempt(trigger: DiagnosticTrigger, outcome: DiagnosticOutcome, events: List<DiagnosticEvent> = emptyList()) = DiagnosticAttempt(
                startedAt = 1_790_000_000_000L,
                mode = CaptureMode.SCREENSHOT,
                selected = false,
                excluded = false,
                trigger = trigger,
                outcome = outcome,
                events = events,
                failure = if (outcome == DiagnosticOutcome.FAILURE) DiagnosticFailure(DiagnosticStage.CAPTURE_PIXELS, causes = listOf(DiagnosticException.TLS), classes = listOf("SSLException")) else null,
            )
            val page = DiagnosticTlsContext(3, listOf(3), TlsRequestKind.UNKNOWN, TlsUrlRelation.DIFFERENT_ORIGIN, true, false, false, blocking = true)
            val image = DiagnosticTlsContext(1, listOf(1), TlsRequestKind.SUBRESOURCE, TlsUrlRelation.DIFFERENT_ORIGIN, true, true, true, blocking = false)
            store.append(attempt(DiagnosticTrigger.CHECK, DiagnosticOutcome.SUCCESS, listOf(DiagnosticEvent(DiagnosticStage.SETTLE, 0, metric = DiagnosticMetric.TLS_ERROR, code = 1, tls = image))))
            store.append(attempt(DiagnosticTrigger.CHECK, DiagnosticOutcome.FAILURE, listOf(DiagnosticEvent(DiagnosticStage.NAVIGATE, 0, metric = DiagnosticMetric.TLS_ERROR, code = 3, tls = page))))
            store.append(attempt(DiagnosticTrigger.PREVIEW, DiagnosticOutcome.FAILURE))
            store.append(attempt(DiagnosticTrigger.PREVIEW, DiagnosticOutcome.CANCELLED))
            store.append(attempt(DiagnosticTrigger.BASELINE, DiagnosticOutcome.SUCCESS))
            val summary = Json.parseToJsonElement(store.report().json).jsonObject.getValue("summary").jsonArray.map { it.jsonPrimitive.content }
            val all = summary.joinToString("\n")
            assertTrue(all, summary.contains("Checks: 1 of 2 failed · Previews: 1 of 2 failed, 1 cancelled · First versions: 0 of 1 failed"))
            assertTrue(all, all.contains("Latest TLS failure: Untrusted certificate on an unobserved request (different origin, before the page committed)"))
            assertTrue(all, all.contains("1 sub-resource certificate errors were refused"))
        }
    }

    @Test fun reportsRemovePrivateDetailsFromPreviouslyStoredAttemptsAndEvents() = runBlocking {
        fixture { store, dir, _ ->
            dir.mkdirs()
            val secret = "Patient Alice Smith at private.customer.xyz/203.0.113.7:443"
            val attempt = DiagnosticAttempt(startedAt = 1_790_000_000_000L, mode = CaptureMode.TEXT, selected = false, excluded = false, trigger = DiagnosticTrigger.CHECK, outcome = DiagnosticOutcome.FAILURE, events = emptyList(), failure = DiagnosticFailure(DiagnosticStage.NAVIGATE, classes = listOf("IOException"), detail = secret))
            val safeRuntime = withContext(Dispatchers.IO) { runtime() }
            File(dir, "trace-${attempt.attemptId}.json").writeText(Json.encodeToString(StoredDiagnostic(safeRuntime, attempt)))
            File(dir, "app-events.json").writeText(Json.encodeToString(listOf(AppEvent(AppEventKind.BACKGROUND_FAILURE, attempt.startedAt, classes = listOf("IOException"), detail = "disk I/O error $secret"))))
            val report = store.report()
            assertEquals(1, report.attemptCount)
            assertEquals(1, report.eventCount)
            for (part in listOf("Alice Smith", "private.customer.xyz", "203.0.113.7")) assertFalse(part, report.json.contains(part))
            assertTrue(report.json.contains("disk I/O error"))
        }
    }

    @Test fun recentSuccessesCannotCrowdOutOlderFailureAndOmissionsAreCounted() = runBlocking {
        fixture { store, dir, _ ->
            val at = 1_790_000_000_000L
            val token = store.watchToken("private-watch")
            store.append(DiagnosticAttempt(startedAt = at, mode = CaptureMode.SCREENSHOT, selected = false, excluded = false, trigger = DiagnosticTrigger.CHECK, watchToken = token, outcome = DiagnosticOutcome.FAILURE, events = emptyList(), failure = DiagnosticFailure(DiagnosticStage.NAVIGATE, causes = listOf(DiagnosticException.TLS))))
            repeat(14) { i -> store.append(DiagnosticAttempt(startedAt = at + 1000 + i, mode = CaptureMode.SCREENSHOT, selected = false, excluded = false, trigger = DiagnosticTrigger.CHECK, watchToken = token, outcome = DiagnosticOutcome.SUCCESS, events = emptyList())) }
            store.report() // Flush, then force deterministic disk recency, independent of filesystem clock resolution.
            dir.listFiles().orEmpty().filter { it.name.startsWith("trace-") && it.extension == "json" }.forEach { file -> file.setLastModified(Json.decodeFromString<StoredDiagnostic>(file.readText()).attempt.startedAt) }
            val report = Json.parseToJsonElement(store.report().json).jsonObject
            val rows = report.getValue("attempts").jsonArray
            assertEquals("FAILURE", rows.first().jsonObject.getValue("attempt").jsonObject.getValue("outcome").jsonPrimitive.content)
            val selection = report.getValue("export").jsonObject
            assertEquals(15, selection.getValue("eligibleAttempts").jsonPrimitive.int)
            assertEquals(10, selection.getValue("exportedAttempts").jsonPrimitive.int)
            assertEquals(5, selection.getValue("omittedAttempts").jsonPrimitive.int)
            assertEquals(0, selection.getValue("omittedFailures").jsonPrimitive.int)
            assertEquals(0, report.getValue("droppedRecords").jsonPrimitive.int)
            assertTrue(report.getValue("summary").toString().contains("omitted"))
            assertFalse(report.toString().contains(token))
        }
    }

    @Test fun failuresRepresentDifferentWatchesAndPageScopeHasHonestCounts() = runBlocking {
        fixture { store, _, _ ->
            val at = 1_790_000_000_000L
            val tokens = listOf("private-a", "private-b", "private-c").map { store.watchToken(it) }
            repeat(3) { i -> store.append(DiagnosticAttempt(startedAt = at + i, mode = CaptureMode.SCREENSHOT, selected = false, excluded = false, trigger = DiagnosticTrigger.CHECK, watchToken = tokens[i], outcome = DiagnosticOutcome.FAILURE, events = emptyList())) }
            repeat(12) { i -> store.append(DiagnosticAttempt(startedAt = at + 1000 + i, mode = CaptureMode.SCREENSHOT, selected = false, excluded = false, trigger = DiagnosticTrigger.CHECK, watchToken = tokens[0], outcome = DiagnosticOutcome.SUCCESS, events = emptyList())) }
            val global = Json.parseToJsonElement(store.report().json).jsonObject
            val failures = global.getValue("attempts").jsonArray.filter { it.jsonObject.getValue("attempt").jsonObject.getValue("outcome").jsonPrimitive.content == "FAILURE" }
            assertEquals(3, failures.map { it.jsonObject.getValue("attempt").jsonObject.getValue("watch").jsonPrimitive.content }.distinct().size)
            val page = Json.parseToJsonElement(store.report("private-b").json).jsonObject
            val selection = page.getValue("export").jsonObject
            assertEquals("WATCH", selection.getValue("scope").jsonPrimitive.content)
            assertEquals(15, selection.getValue("retainedAttempts").jsonPrimitive.int)
            assertEquals(1, selection.getValue("eligibleAttempts").jsonPrimitive.int)
            assertEquals(14, selection.getValue("excludedOtherWatchAttempts").jsonPrimitive.int)
            assertEquals(0, selection.getValue("omittedAttempts").jsonPrimitive.int)
            assertEquals(1, page.getValue("attempts").jsonArray.size)
            for (secret in tokens + listOf("private-a", "private-b", "private-c")) assertFalse(page.toString().contains(secret))
        }
    }

    @Test fun byteLimitedExportSkipsLargeRowsAndStillIncludesSmallOlderEvidence() = runBlocking {
        fixture { store, _, _ ->
            val at = 1_790_000_000_000L
            val tokens = (0..2).map { store.watchToken("heavy-$it") }
            val tls = DiagnosticTlsContext(3, (0..5).toList(), TlsRequestKind.SUBRESOURCE, TlsUrlRelation.DIFFERENT_ORIGIN, true, true, true)
            repeat(3) { i -> store.append(DiagnosticAttempt(startedAt = at + 1000 + i, mode = CaptureMode.SCREENSHOT, selected = false, excluded = false, trigger = DiagnosticTrigger.CHECK, watchToken = tokens[i], outcome = DiagnosticOutcome.FAILURE, events = List(64) { DiagnosticEvent(DiagnosticStage.NAVIGATE, it.toLong(), metric = DiagnosticMetric.TLS_ERROR, code = 3, tls = tls) })) }
            store.append(DiagnosticAttempt(startedAt = at, mode = CaptureMode.SCREENSHOT, selected = true, excluded = false, trigger = DiagnosticTrigger.CHECK, watchToken = tokens[0], outcome = DiagnosticOutcome.FAILURE, events = emptyList()))
            val report = store.report()
            val root = Json.parseToJsonElement(report.json).jsonObject
            assertEquals(4, root.getValue("export").jsonObject.getValue("eligibleAttempts").jsonPrimitive.int)
            assertTrue(root.getValue("attempts").jsonArray.any { it.jsonObject.getValue("attempt").jsonObject.getValue("selected").jsonPrimitive.boolean })
            assertTrue(root.getValue("export").jsonObject.getValue("omittedByByteLimit").jsonPrimitive.int > 0)
            assertTrue(report.json.toByteArray().size <= AndroidDiagnosticStore.MAX_REPORT_BYTES)
        }
    }

    private fun runtime(): DiagnosticRuntime {
        assertNotEquals(android.os.Looper.getMainLooper(), android.os.Looper.myLooper())
        return DiagnosticRuntime(38, "3.0", DiagnosticVariant.DEBUG, "abcdef123456", 36, DiagnosticProvider.GOOGLE, "133.0.6943.137", DiagnosticSpace.AT_LEAST_256_MIB, 256)
    }
    private suspend fun fixture(block: suspend (AndroidDiagnosticStore, File, (Long) -> Unit) -> Unit) {
        val dir = File(context.cacheDir, "diagnostic-test-${java.util.UUID.randomUUID()}")
        var clock = 1_790_000_000_000L
        val store = AndroidDiagnosticStore(context, dir, { clock }, ::runtime)
        try {
            block(store, dir) { clock = it }
        } finally {
            store.close()
            dir.deleteRecursively()
        }
    }

    @Test fun secretsNeverEnterDiskOrExportAndRestartPreservesSanitizedData() = runBlocking {
        fixture { store, dir, _ ->
            val secret = "https://user:password@secret.invalid/path?token=SECRET#private Cookie: session=VALUE <html>PERSONAL</html>"
            val token = store.watchToken(secret)
            store.append(DiagnosticAttempt(startedAt = 1_790_000_000_000L, mode = CaptureMode.TEXT, selected = true, excluded = true, trigger = DiagnosticTrigger.CHECK, watchToken = token, outcome = DiagnosticOutcome.FAILURE, events = listOf(DiagnosticEvent(DiagnosticStage.EXTRACT_TEXT, 10)), failure = DiagnosticFailure(DiagnosticStage.EXTRACT_TEXT, causes = listOf(exceptionKind(IllegalArgumentException(secret))))))
            val report = store.report(secret)
            assertEquals(1, report.attemptCount)
            for (part in listOf("secret.invalid", "password", "SECRET", "Cookie", "PERSONAL", token, "startedAt", "attemptId")) assertFalse(part, report.json.contains(part))
            assertTrue(report.json.contains("watch-1"))
            assertTrue(report.json.contains("INVALID_CONTENT"))
            assertFalse(dir.listFiles().orEmpty().filter { it.extension == "json" }.joinToString { it.readText() }.contains("SECRET"))
            store.close()
            val reopened = AndroidDiagnosticStore(context, dir, { 1_790_000_000_000L }, ::runtime)
            try {
                assertEquals(1, reopened.report(secret).attemptCount)
            } finally {
                reopened.close()
            }
        }
    }

    @Test fun retentionCountDiskAndReportLimitsRejectOversizedAndUnknownRawFields() = runBlocking {
        fixture { store, dir, advance ->
            repeat(120) { i ->
                store.append(DiagnosticAttempt(startedAt = 1_790_000_000_000L, mode = CaptureMode.TEXT, selected = false, excluded = false, trigger = DiagnosticTrigger.CHECK, outcome = DiagnosticOutcome.SUCCESS, events = List(1000) { DiagnosticEvent(DiagnosticStage.COMPARE, it.toLong()) }))
                if (i % 10 == 0) store.report()
            }
            var report = store.report()
            val files = dir.listFiles().orEmpty().filter { it.name.startsWith("trace-") }
            assertTrue(files.size <= AndroidDiagnosticStore.MAX_RECORDS)
            assertTrue(files.sumOf { it.length() } <= AndroidDiagnosticStore.MAX_DISK_BYTES)
            assertTrue(report.json.toByteArray().size <= AndroidDiagnosticStore.MAX_REPORT_BYTES)
            assertTrue(report.attemptCount <= 10)
            File(dir, "trace-oversized.json").writeBytes(ByteArray(AndroidDiagnosticStore.MAX_RECORD_BYTES + 1))
            File(dir, "trace-unknown.json").writeText("{\"url\":\"https://private.invalid?token=SECRET\"}")
            report = store.report()
            assertFalse(report.json.contains("SECRET"))
            assertFalse(File(dir, "trace-oversized.json").exists())
            assertFalse(File(dir, "trace-unknown.json").exists())
            advance(1_790_000_000_000L + AndroidDiagnosticStore.RETENTION_MS + 1)
            assertEquals(0, store.report().attemptCount)
        }
    }

    @Test fun incompleteCheckpointSurvivesRestartAndFinalRecordReplacesIt() = runBlocking {
        fixture { store, dir, _ ->
            val attempt = DiagnosticAttempt(startedAt = 1_790_000_000_000L, mode = CaptureMode.SCREENSHOT, selected = false, excluded = false, trigger = DiagnosticTrigger.CHECK, outcome = DiagnosticOutcome.INCOMPLETE, events = listOf(DiagnosticEvent(DiagnosticStage.CAPTURE_PIXELS, 100)))
            store.append(attempt)
            assertTrue(store.report().json.contains("INCOMPLETE"))
            store.close()
            val reopened = AndroidDiagnosticStore(context, dir, { 1_790_000_000_000L }, ::runtime)
            try {
                assertTrue(reopened.report().json.contains("CAPTURE_PIXELS"))
                reopened.append(attempt.copy(outcome = DiagnosticOutcome.FAILURE, failure = DiagnosticFailure(DiagnosticStage.CAPTURE_PIXELS)))
                val report = reopened.report()
                assertEquals(1, report.attemptCount)
                assertFalse(report.json.contains("INCOMPLETE"))
            } finally {
                reopened.close()
            }
        }
    }

    @Test fun runtimeStringsAreValidatedAndShareCacheIsBoundedWithReadOnlyIntent() = runBlocking {
        val dir = File(context.cacheDir, "diagnostic-runtime-${java.util.UUID.randomUUID()}")
        val store = AndroidDiagnosticStore(context, dir, { 1_790_000_000_000L }, { runtime().copy(versionName = "https://token=SECRET", providerVersion = "Cookie: SECRET", revision = "password=SECRET") })
        try {
            store.append(DiagnosticAttempt(startedAt = 1_790_000_000_000L, mode = CaptureMode.TEXT, selected = false, excluded = false, trigger = DiagnosticTrigger.PREVIEW, outcome = DiagnosticOutcome.SUCCESS, events = emptyList()))
            val report = store.report()
            assertFalse(report.json.contains("SECRET"))
            assertTrue(report.json.contains("unavailable"))
            repeat(12) { store.prepareShare(report) }
            assertTrue(File(context.cacheDir, "shares").listFiles().orEmpty().count { it.name.startsWith("diagnostics-") } <= 8)
            val file = store.prepareShare(report)
            val chooser = diagnosticShareIntent(context, file)

            @Suppress("DEPRECATION")
            val send = chooser.getParcelableExtra<android.content.Intent>(android.content.Intent.EXTRA_INTENT)!!
            assertEquals(android.content.Intent.ACTION_SEND, send.action)
            assertTrue(send.flags and android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
            assertEquals(0, send.flags and android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        } finally {
            store.close()
            dir.deleteRecursively()
        }
    }

    @Test fun concurrentWritersAndBrokenStorageCannotFailMonitoring() = runBlocking {
        fixture { store, _, advance ->
            advance(System.currentTimeMillis())
            val prior = DiagnosticLog.sink
            try {
                DiagnosticLog.sink = store
                coroutineScope {
                    repeat(8) { worker ->
                        launch(Dispatchers.Default) {
                            repeat(4) {
                                assertEquals(
                                    worker,
                                    recordDiagnostic(CaptureMode.TEXT, false, false, DiagnosticTrigger.CHECK, "watch-$worker") {
                                        diagnosticStage(DiagnosticStage.COMPARE) { worker }
                                    },
                                )
                            }
                        }
                    }
                }
                val report = store.report()
                assertTrue(report.attemptCount > 0)
                assertTrue(report.json.toByteArray().size <= AndroidDiagnosticStore.MAX_REPORT_BYTES)
            } finally {
                DiagnosticLog.sink = prior
            }
        }
        val blocked = File(context.cacheDir, "diagnostic-blocked-${java.util.UUID.randomUUID()}").apply { writeText("file instead of directory") }
        val broken = AndroidDiagnosticStore(context, blocked, runtime = ::runtime)
        val prior = DiagnosticLog.sink
        try {
            DiagnosticLog.sink = broken
            assertEquals(42, recordDiagnostic(CaptureMode.TEXT, false, false, DiagnosticTrigger.CHECK, "watch") { 42 })
            assertTrue(Json.parseToJsonElement(broken.report().json).jsonObject.getValue("droppedRecords").jsonPrimitive.int > 0)
        } finally {
            DiagnosticLog.sink = prior
            broken.close()
            blocked.delete()
        }
    }
}
