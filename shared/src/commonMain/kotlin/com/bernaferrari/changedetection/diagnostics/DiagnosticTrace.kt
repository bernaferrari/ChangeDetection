package com.bernaferrari.changedetection.diagnostics

import com.bernaferrari.changedetection.error.CheckFailure
import com.bernaferrari.changedetection.repo.CaptureMode
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.time.TimeSource

@Serializable enum class DiagnosticStage { QUEUED, CREATE_BROWSER, NAVIGATE, DOM_READY, SETTLE, EXTRACT_TEXT, CAPTURE_PIXELS, PREPARE_CONTENT, READ_BASELINE, COMPARE, WRITE_FILE, COMMIT_DATABASE, UPDATE_STATUS, NOTIFY, CLOSE_BROWSER }

@Serializable enum class DiagnosticTrigger { CHECK, PREVIEW, INTERACTIVE_PREVIEW, BASELINE }

@Serializable enum class DiagnosticOutcome { SUCCESS, FAILURE, CANCELLED, SKIPPED, INCOMPLETE }

@Serializable enum class DiagnosticException { HTTP, DATABASE, REGION, TIMEOUT, CANCELLED, STORAGE, IO, TLS, INVALID_CONTENT, JSON, SQLITE, OTHER, DOCUMENT_CHANGED }

@Serializable enum class DiagnosticMetric { HTTP_STATUS, WEBVIEW_ERROR, TLS_ERROR, RENDERER_CRASH, ATTACHED, DOM_READY, CALLBACK_NULL, BYTES_BUCKET, BASELINE_PRESENT, CAPTURE_CONTEXT, CAPTURE_ISSUE, REGION_RESULT, DOCUMENT_CHANGED }

/** Fixed values only: request URLs are compared in memory and never retained in a report. */
@Serializable enum class TlsRequestKind { MAIN_DOCUMENT, SUBRESOURCE, UNKNOWN }

@Serializable enum class TlsUrlRelation { REQUESTED, STARTED, COMMITTED, SAME_ORIGIN, DIFFERENT_ORIGIN, UNKNOWN }

@Serializable data class DiagnosticTlsContext(
    val primary: Int,
    val errors: List<Int>,
    val requestKind: TlsRequestKind,
    val relation: TlsUrlRelation,
    val navigationStarted: Boolean,
    val committed: Boolean,
    val finished: Boolean,
    /** Whether the refusal immediately blocks navigation. Nonblocking resources can still fail captured-content validation. */
    val blocking: Boolean = true,
)

/** Current document state when an older capture is rejected; no URLs or document identifiers. */
@Serializable data class DiagnosticDocumentContext(val navigationStarted: Boolean, val committed: Boolean, val finished: Boolean, val mainPending: Boolean)

/** Geometry/resource state only. Numeric bounds also apply when importing older reports. */
@Serializable data class CaptureDiagnostic(
    val nativeWidth: Int,
    val nativeHeight: Int,
    val layoutWidthTenths: Int,
    val visualWidthTenths: Int,
    val nativeScaleMilli: Int,
    val densityMilli: Int,
    val selectedMatches: Int,
    val selectedWidthTenths: Int,
    val selectedHeightTenths: Int,
    val fontsLoading: Boolean,
    val pendingImages: Int,
) {
    fun bounded() = copy(nativeWidth = nativeWidth.coerceIn(0, 16_384), nativeHeight = nativeHeight.coerceIn(0, 32_768), layoutWidthTenths = layoutWidthTenths.coerceIn(0, 200_000), visualWidthTenths = visualWidthTenths.coerceIn(0, 200_000), nativeScaleMilli = nativeScaleMilli.coerceIn(0, 50_000), densityMilli = densityMilli.coerceIn(0, 16_000), selectedMatches = selectedMatches.coerceIn(0, 1_000), selectedWidthTenths = selectedWidthTenths.coerceIn(0, 1_000_000), selectedHeightTenths = selectedHeightTenths.coerceIn(0, 1_000_000), pendingImages = pendingImages.coerceIn(0, 1_000))
}

@Serializable data class DiagnosticEvent(val stage: DiagnosticStage, val elapsedMs: Long, val durationMs: Long = 0, val failed: Boolean = false, val metric: DiagnosticMetric? = null, val code: Int? = null, val tls: DiagnosticTlsContext? = null, val capture: CaptureDiagnostic? = null, val document: DiagnosticDocumentContext? = null)

/**
 * [classes] are exception class names along the cause chain (no messages). [detail] is the first
 * cause's recognized system-error reason after [scrubErrorText]. Free-form messages are never retained.
 */
@Serializable data class DiagnosticFailure(
    val stage: DiagnosticStage,
    val category: CheckFailure? = null,
    val causes: List<DiagnosticException> = emptyList(),
    val classes: List<String> = emptyList(),
    val detail: String? = null,
    val regionIssue: com.bernaferrari.changedetection.detection.TextRegionIssue? = null,
)

/** Things that happen outside a page check but explain why checks fail. */
@Serializable enum class AppEventKind { DATABASE_CREATED, DATABASE_UPGRADED, DATABASE_RESET, CRASH, BACKGROUND_FAILURE }

@Serializable enum class AppBuildVariant { DEBUG, RELEASE, UNKNOWN }

@Serializable data class AppEventBuild(val versionCode: Long, val revision: String, val variant: AppBuildVariant)

@Serializable enum class AppEventStage { WORKER_FOREGROUND, WORKER_CHECK, TURBO_LAUNCH, TURBO_FOREGROUND, TURBO_SERVICE_START }

@Serializable data class AppEvent(
    val kind: AppEventKind,
    val at: Long,
    val fromVersion: Int? = null,
    val toVersion: Int? = null,
    val classes: List<String> = emptyList(),
    val detail: String? = null,
    val build: AppEventBuild? = null,
    val stage: AppEventStage? = null,
)

@Serializable data class DiagnosticAttempt(val attemptId: String = diagnosticId(), val schema: Int = 1, val startedAt: Long, val mode: CaptureMode, val selected: Boolean, val excluded: Boolean, val trigger: DiagnosticTrigger, val watchToken: String = "", val outcome: DiagnosticOutcome, val events: List<DiagnosticEvent>, val failure: DiagnosticFailure? = null)

interface DiagnosticSink {
    suspend fun watchToken(watchId: String?): String
    suspend fun append(attempt: DiagnosticAttempt)

    /** Must not block or throw; a crash handler may call it. */
    fun appEvent(event: AppEvent) {}

    /** The database opened at [version]; the sink decides whether that's new (created or upgraded). */
    fun databaseOpened(version: Int) {}
}

/** No URLs, page content or raw messages enter this schema; error text is scrubbed first. Android installs a bounded sink. */
object DiagnosticLog {
    @kotlin.concurrent.Volatile var sink: DiagnosticSink? = null

    /** Records an app-level event. Never throws: diagnostics must not make things worse. */
    fun appEvent(kind: AppEventKind, error: Throwable? = null, fromVersion: Int? = null, toVersion: Int? = null, stage: AppEventStage? = null) {
        try {
            sink?.appEvent(AppEvent(kind, kotlin.time.Clock.System.now().toEpochMilliseconds(), fromVersion, toVersion, errorClasses(error), errorDetail(error), stage = stage))
        } catch (_: Throwable) { }
    }

    fun databaseOpened(version: Int) {
        try {
            sink?.databaseOpened(version)
        } catch (_: Throwable) { }
    }
}

/** Exception class names along the cause chain, outermost first. Names only, never messages. */
fun errorClasses(error: Throwable?): List<String> = generateSequence(error) { it.cause }.take(4).map { it::class.simpleName ?: "Anonymous" }.filter(::isSafeClassName).toList()

internal fun isSafeClassName(name: String) = name.length in 1..64 && name.all { it.isLetterOrDigit() || it == '_' || it == '$' }

/**
 * The deepest recognized SQLite or IO reason, using fixed phrases rather than free-form text.
 * IO exceptions also include network failures and WebView descriptions, which can contain private data.
 * Unknown reasons contribute class names only.
 */
fun errorDetail(error: Throwable?): String? = generateSequence(error) { it.cause }.take(6)
    .filter(::isSystemLevelError)
    .mapNotNull { it.message?.let(::scrubErrorText) }
    .lastOrNull { it.isNotBlank() }

private fun isSystemLevelError(error: Throwable): Boolean = error::class.simpleName.orEmpty().contains("SQLite") || error is kotlinx.io.IOException

/** Also applied when reading older records: no free-form error text enters disk or an exported report. */
fun scrubErrorText(text: String): String {
    val reason = safeSystemReasons.firstOrNull { text.contains(it, ignoreCase = true) } ?: return ""
    val code = systemErrorCode.find(text)?.groupValues?.get(1)?.toIntOrNull()
    return reason + (code?.let { " (code $it)" } ?: "")
}

private val systemErrorCode = Regex("""\b(?:error code|code)\s*[:=]?\s*(\d{1,4})\b""", RegexOption.IGNORE_CASE)
private val safeSystemReasons = listOf(
    "database table is locked", "database is locked", "database or disk is full", "disk is full",
    "no space left", "ENOSPC", "SQLITE_FULL", "disk I/O error", "SQLITE_IOERR",
    "unable to open database file", "attempt to write a readonly database", "database disk image is malformed",
    "file is not a database", "no such column:", "no such table:", "no such index:",
    "UNIQUE constraint failed", "NOT NULL constraint failed", "FOREIGN KEY constraint failed",
    "CHECK constraint failed", "permission denied", "read-only file system", "too many open files",
)

internal class DiagnosticSpan : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<DiagnosticSpan>
    private val began = TimeSource.Monotonic.markNow()
    private val lock = Mutex()
    private val events = mutableListOf<DiagnosticEvent>()
    private val callbacks = kotlinx.coroutines.channels.Channel<DiagnosticEvent>(16)

    // Read by WebView callbacks on other threads, to file late events under the step that was running.
    @kotlin.concurrent.Volatile private var active = DiagnosticStage.QUEUED
    private var failure: DiagnosticFailure? = null
    private var cancelled = false
    var checkpoint: (suspend () -> Unit)? = null
    suspend fun persistCheckpoint() {
        try {
            checkpoint?.invoke()
        } catch (_: Exception) { }
    }
    suspend fun event(stage: DiagnosticStage, duration: Long = 0, failed: Boolean = false, metric: DiagnosticMetric? = null, code: Int? = null) = lock.withLock {
        active = stage
        if (events.size < 64) events += DiagnosticEvent(stage, began.elapsedNow().inWholeMilliseconds.coerceIn(0, 600_000), duration.coerceIn(0, 600_000), failed, metric, code?.coerceIn(-100, 599))
    }
    suspend fun capture(context: CaptureDiagnostic, stage: DiagnosticStage) = lock.withLock {
        if (events.size < 64) events += DiagnosticEvent(stage, began.elapsedNow().inWholeMilliseconds.coerceIn(0, 600_000), metric = DiagnosticMetric.CAPTURE_CONTEXT, capture = context.bounded())
    }
    suspend fun failed(error: Throwable, category: CheckFailure? = null, stage: DiagnosticStage? = null) = lock.withLock {
        if (error is CancellationException && error !is TimeoutCancellationException) cancelled = true
        val resolvedCategory = category ?: diagnosticCategory(error)
        val causes = generateSequence(error) { it.cause }.take(4).map(::exceptionKind).toList()
        failure = failure?.copy(category = category ?: failure?.category ?: resolvedCategory)
            ?: DiagnosticFailure(stage ?: active, resolvedCategory, causes, errorClasses(error), errorDetail(error), (error as? com.bernaferrari.changedetection.detection.TextRegionException)?.issue)
    }
    fun callback(stage: DiagnosticStage, metric: DiagnosticMetric, code: Int) {
        callbacks.trySend(DiagnosticEvent(stage, began.elapsedNow().inWholeMilliseconds.coerceIn(0, 600_000), metric = metric, code = code.coerceIn(-100, 599)))
    }
    fun tls(context: DiagnosticTlsContext) {
        // A refused certificate can arrive long after navigation (a late image or frame): file it under the step it interrupted.
        callbacks.trySend(DiagnosticEvent(active, began.elapsedNow().inWholeMilliseconds.coerceIn(0, 600_000), metric = DiagnosticMetric.TLS_ERROR, code = context.primary, tls = context))
    }
    fun documentChanged(context: DiagnosticDocumentContext) {
        callbacks.trySend(DiagnosticEvent(active, began.elapsedNow().inWholeMilliseconds.coerceIn(0, 600_000), metric = DiagnosticMetric.DOCUMENT_CHANGED, document = context))
    }
    suspend fun snapshot() = lock.withLock {
        // Draining a callback for a checkpoint must retain it for later checkpoints and the final record.
        while (true) {
            val callback = callbacks.tryReceive().getOrNull() ?: break
            if (events.size < 64) events += callback
        }
        Triple(events.sortedBy { it.elapsedMs }, failure, cancelled)
    }
}

internal fun exceptionKind(error: Throwable): DiagnosticException = when {
    error is com.bernaferrari.changedetection.browser.CaptureNotReadyException -> when (error.issue) {
        com.bernaferrari.changedetection.browser.CaptureIssue.RESOURCE_FAILED -> DiagnosticException.HTTP
        com.bernaferrari.changedetection.browser.CaptureIssue.DOCUMENT_CHANGED -> DiagnosticException.DOCUMENT_CHANGED
        else -> DiagnosticException.TIMEOUT
    }

    error is TimeoutCancellationException -> DiagnosticException.TIMEOUT

    error is CancellationException -> DiagnosticException.CANCELLED

    error is com.bernaferrari.changedetection.error.NetworkError -> DiagnosticException.HTTP

    error is com.bernaferrari.changedetection.error.SnapshotStorageError -> DiagnosticException.STORAGE

    error is com.bernaferrari.changedetection.error.DatabaseError -> DiagnosticException.DATABASE

    error is com.bernaferrari.changedetection.detection.TextRegionException -> DiagnosticException.REGION

    com.bernaferrari.changedetection.error.isDirectTlsFailure(error) -> DiagnosticException.TLS

    error::class.simpleName in setOf("SQLiteException", "SQLiteFullException", "SQLiteDiskIOException") -> DiagnosticException.SQLITE

    error is kotlinx.serialization.SerializationException -> DiagnosticException.JSON

    error is IllegalArgumentException -> DiagnosticException.INVALID_CONTENT

    error is kotlinx.io.IOException -> DiagnosticException.IO

    else -> DiagnosticException.OTHER
}

suspend fun diagnosticFailure(error: Throwable, category: CheckFailure? = null) {
    currentCoroutineContext()[DiagnosticSpan]?.failed(error, category)
}
suspend fun diagnosticMetric(stage: DiagnosticStage, metric: DiagnosticMetric, code: Int) {
    currentCoroutineContext()[DiagnosticSpan]?.event(stage, metric = metric, code = code)
}
suspend fun <T> diagnosticStage(stage: DiagnosticStage, block: suspend () -> T): T {
    val span = currentCoroutineContext()[DiagnosticSpan] ?: return block()
    span.event(stage)
    span.persistCheckpoint()
    val start = TimeSource.Monotonic.markNow()
    return try {
        block().also { span.event(stage, start.elapsedNow().inWholeMilliseconds) }
    } catch (error: Throwable) {
        // Keep the first actual stage before an outer layer wraps/classifies the failure.
        withContext(NonCancellable) {
            if (stage !in setOf(DiagnosticStage.NOTIFY, DiagnosticStage.CLOSE_BROWSER) || error is CancellationException) span.failed(error, stage = stage)
            span.event(stage, start.elapsedNow().inWholeMilliseconds, failed = true)
        }
        throw error
    }
}

suspend fun <T> recordDiagnostic(mode: CaptureMode, selected: Boolean, excluded: Boolean, trigger: DiagnosticTrigger, watchId: String? = null, block: suspend () -> T): T {
    if (currentCoroutineContext()[DiagnosticSpan] != null) return block()
    val sink = DiagnosticLog.sink ?: return block()
    val token = try {
        withContext(Dispatchers.Default) { withTimeoutOrNull(200) { sink.watchToken(watchId) } ?: "" }
    } catch (_: Exception) {
        ""
    }
    val span = DiagnosticSpan()
    val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
    val id = diagnosticId()
    span.checkpoint = {
        val (events, failure, _) = span.snapshot()
        sink.append(DiagnosticAttempt(attemptId = id, startedAt = now, mode = mode, selected = selected, excluded = excluded, trigger = trigger, watchToken = token, outcome = DiagnosticOutcome.INCOMPLETE, events = events, failure = failure))
    }
    span.persistCheckpoint()
    var skipped = false
    try {
        return withContext(span) { block().also { skipped = it == null || it is com.bernaferrari.changedetection.sync.SyncResult.Skipped } }
    } catch (error: Throwable) {
        withContext(NonCancellable) { span.failed(error) }
        throw error
    } finally {
        // Owned, bounded persistence; recorder failures cannot change the check's result.
        withContext(NonCancellable) {
            try {
                val (events, failure, cancelled) = span.snapshot()
                val outcome = when {
                    cancelled -> DiagnosticOutcome.CANCELLED
                    failure != null -> DiagnosticOutcome.FAILURE
                    skipped -> DiagnosticOutcome.SKIPPED
                    else -> DiagnosticOutcome.SUCCESS
                }
                withTimeoutOrNull(1_000) { sink.append(DiagnosticAttempt(attemptId = id, startedAt = now, mode = mode, selected = selected, excluded = excluded, trigger = trigger, watchToken = token, outcome = outcome, events = events, failure = failure)) }
            } catch (_: Exception) { /* Local diagnostics must never fail monitoring. */ }
        }
    }
}

private fun diagnosticId(): String = kotlin.random.Random.nextBytes(16).joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }

private fun diagnosticCategory(error: Throwable): CheckFailure = when {
    error is com.bernaferrari.changedetection.browser.CaptureNotReadyException -> when (error.issue) {
        com.bernaferrari.changedetection.browser.CaptureIssue.RESOURCE_FAILED -> CheckFailure.CAPTURE_INCOMPLETE
        com.bernaferrari.changedetection.browser.CaptureIssue.DOCUMENT_CHANGED -> CheckFailure.DOCUMENT_CHANGED
        else -> CheckFailure.TIMEOUT
    }

    com.bernaferrari.changedetection.error.isTlsFailure(error) -> CheckFailure.TLS

    error is TimeoutCancellationException -> CheckFailure.TIMEOUT

    error is com.bernaferrari.changedetection.error.NetworkError -> when (error.code) {
        401 -> CheckFailure.LOGIN
        403 -> CheckFailure.ACCESS_DENIED
        404, 410 -> CheckFailure.NOT_FOUND
        429 -> CheckFailure.RATE_LIMITED
        in 500..599 -> CheckFailure.SERVER
        else -> CheckFailure.CONNECTION
    }

    error is com.bernaferrari.changedetection.detection.TextRegionException -> CheckFailure.REGION

    error is com.bernaferrari.changedetection.error.DatabaseError -> CheckFailure.SAVE_FAILED

    error is com.bernaferrari.changedetection.error.SnapshotStorageError -> if (error.lowSpace) CheckFailure.STORAGE_FULL else CheckFailure.SAVE_FAILED

    error is IllegalArgumentException -> CheckFailure.CONTENT

    error is kotlinx.io.IOException -> CheckFailure.CONNECTION

    else -> CheckFailure.UNKNOWN
}

suspend fun diagnosticCapture(context: CaptureDiagnostic, stage: DiagnosticStage = DiagnosticStage.CAPTURE_PIXELS) {
    currentCoroutineContext()[DiagnosticSpan]?.capture(context, stage)
}
