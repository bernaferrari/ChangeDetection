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

@Serializable enum class DiagnosticException { HTTP, DATABASE, REGION, TIMEOUT, CANCELLED, STORAGE, IO, TLS, INVALID_CONTENT, JSON, SQLITE, OTHER }

@Serializable enum class DiagnosticMetric { HTTP_STATUS, WEBVIEW_ERROR, TLS_ERROR, RENDERER_CRASH, ATTACHED, DOM_READY, CALLBACK_NULL, BYTES_BUCKET, BASELINE_PRESENT }

@Serializable data class DiagnosticEvent(val stage: DiagnosticStage, val elapsedMs: Long, val durationMs: Long = 0, val failed: Boolean = false, val metric: DiagnosticMetric? = null, val code: Int? = null)

@Serializable data class DiagnosticFailure(val stage: DiagnosticStage, val category: CheckFailure? = null, val causes: List<DiagnosticException> = emptyList())

@Serializable data class DiagnosticAttempt(val attemptId: String = diagnosticId(), val schema: Int = 1, val startedAt: Long, val mode: CaptureMode, val selected: Boolean, val excluded: Boolean, val trigger: DiagnosticTrigger, val watchToken: String = "", val outcome: DiagnosticOutcome, val events: List<DiagnosticEvent>, val failure: DiagnosticFailure? = null)

interface DiagnosticSink {
    suspend fun watchToken(watchId: String?): String
    suspend fun append(attempt: DiagnosticAttempt)
}

/** No arbitrary strings, URLs, messages or content enter this schema. Android installs a bounded sink. */
object DiagnosticLog {
    @kotlin.concurrent.Volatile var sink: DiagnosticSink? = null
}

internal class DiagnosticSpan : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<DiagnosticSpan>
    private val began = TimeSource.Monotonic.markNow()
    private val lock = Mutex()
    private val events = mutableListOf<DiagnosticEvent>()
    private val callbacks = kotlinx.coroutines.channels.Channel<DiagnosticEvent>(16)
    private var active = DiagnosticStage.QUEUED
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
    suspend fun failed(error: Throwable, category: CheckFailure? = null, stage: DiagnosticStage? = null) = lock.withLock {
        if (error is CancellationException) cancelled = true
        val resolvedCategory = category ?: diagnosticCategory(error)
        val causes = generateSequence(error) { it.cause }.take(4).map(::exceptionKind).toList()
        failure = failure?.copy(category = category ?: failure?.category ?: resolvedCategory) ?: DiagnosticFailure(stage ?: active, resolvedCategory, causes)
    }
    fun callback(stage: DiagnosticStage, metric: DiagnosticMetric, code: Int) {
        callbacks.trySend(DiagnosticEvent(stage, began.elapsedNow().inWholeMilliseconds.coerceIn(0, 600_000), metric = metric, code = code.coerceIn(-100, 599)))
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
    error is TimeoutCancellationException -> DiagnosticException.TIMEOUT
    error is CancellationException -> DiagnosticException.CANCELLED
    error is com.bernaferrari.changedetection.error.NetworkError -> DiagnosticException.HTTP
    error is com.bernaferrari.changedetection.error.SnapshotStorageError -> DiagnosticException.STORAGE
    error is com.bernaferrari.changedetection.error.DatabaseError -> DiagnosticException.DATABASE
    error is com.bernaferrari.changedetection.detection.TextRegionException -> DiagnosticException.REGION
    error::class.simpleName in setOf("SSLException", "SSLHandshakeException", "CertificateException") -> DiagnosticException.TLS
    error::class.simpleName in setOf("SQLiteException", "SQLiteFullException", "SQLiteDiskIOException") -> DiagnosticException.SQLITE
    error::class.simpleName in setOf("JsonDecodingException", "SerializationException") -> DiagnosticException.JSON
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
