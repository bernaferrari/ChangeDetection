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

/**
 * [classes] are exception class names along the cause chain (no messages). [detail] is the first
 * cause's message after [scrubErrorText]: no addresses, paths, quoted text or long numbers survive.
 */
@Serializable data class DiagnosticFailure(
    val stage: DiagnosticStage,
    val category: CheckFailure? = null,
    val causes: List<DiagnosticException> = emptyList(),
    val classes: List<String> = emptyList(),
    val detail: String? = null,
)

/** Things that happen outside a page check but explain why checks fail. */
@Serializable enum class AppEventKind { DATABASE_CREATED, DATABASE_UPGRADED, DATABASE_RESET, CRASH, BACKGROUND_FAILURE }

@Serializable data class AppEvent(
    val kind: AppEventKind,
    val at: Long,
    val fromVersion: Int? = null,
    val toVersion: Int? = null,
    val classes: List<String> = emptyList(),
    val detail: String? = null,
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
    fun appEvent(kind: AppEventKind, error: Throwable? = null, fromVersion: Int? = null, toVersion: Int? = null) {
        try {
            sink?.appEvent(AppEvent(kind, kotlin.time.Clock.System.now().toEpochMilliseconds(), fromVersion, toVersion, errorClasses(error), errorDetail(error)))
        } catch (_: Throwable) { }
    }

    fun databaseOpened(version: Int) {
        try { sink?.databaseOpened(version) } catch (_: Throwable) { }
    }
}

/** Exception class names along the cause chain, outermost first. Names only, never messages. */
fun errorClasses(error: Throwable?): List<String> =
    generateSequence(error) { it.cause }.take(4).map { it::class.simpleName ?: "Anonymous" }.filter(::isSafeClassName).toList()

internal fun isSafeClassName(name: String) = name.length in 1..64 && name.all { it.isLetterOrDigit() || it == '_' || it == '$' }

/**
 * The message of the deepest SQLite or file error in the cause chain, scrubbed. Only those layers
 * are trusted: they say things like "database is locked" or "disk I/O error" and never carry page
 * content. Every other exception (parsers, network, the app's own wrappers) contributes its class
 * name only, because its message may contain what the page said.
 */
fun errorDetail(error: Throwable?): String? = generateSequence(error) { it.cause }.take(6)
    .filter(::isSystemLevelError)
    .mapNotNull { it.message?.let(::scrubErrorText) }
    .lastOrNull { it.isNotBlank() }

private fun isSystemLevelError(error: Throwable): Boolean =
    error::class.simpleName.orEmpty().contains("SQLite") || error is kotlinx.io.IOException

/** Keeps the shape of an error ("no such column: lastFailureCode") and drops anything that could identify a page or person. */
fun scrubErrorText(text: String): String = text
    .replace(Regex("""[a-zA-Z][a-zA-Z0-9+.-]*://\S+"""), "<url>")
    .replace(Regex("""\S+@\S+\.\S+"""), "<email>")
    .replace(Regex("""(/[^\s/]+){2,}/?"""), "<path>")
    .replace(Regex(""""[^"]*"|'[^']*'|`[^`]*`"""), "<text>")
    .replace(Regex("""\b\d{5,}\b"""), "<n>")
    .replace(Regex("""(\w+)=\S+"""), "$1=<value>")
    .replace(Regex("""\b(?:[a-z0-9-]+\.)+(?:com|net|org|io|dev|app|br|co|uk|de|info)\b""", RegexOption.IGNORE_CASE), "<host>")
    .filter { it.isLetterOrDigit() || it in " _:.,;()=<>#+-[]/" }
    .replace(Regex("""\s+"""), " ")
    .trim()
    .take(200)

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
        failure = failure?.copy(category = category ?: failure?.category ?: resolvedCategory)
            ?: DiagnosticFailure(stage ?: active, resolvedCategory, causes, errorClasses(error), errorDetail(error))
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
