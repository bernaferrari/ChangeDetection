package com.bernaferrari.changedetection.diagnostics

import android.content.Context
import android.os.Build
import android.os.StatFs
import android.webkit.WebView
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.File
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Serializable enum class DiagnosticVariant { DEBUG, RELEASE, UNKNOWN }

@Serializable enum class DiagnosticProvider { GOOGLE, SYSTEM, CHROME, OTHER, UNAVAILABLE }

@Serializable enum class DiagnosticSpace { BELOW_32_MIB, BELOW_256_MIB, AT_LEAST_256_MIB, UNKNOWN }

@Serializable data class DiagnosticRuntime(val versionCode: Long, val versionName: String, val variant: DiagnosticVariant, val revision: String, val androidApi: Int, val provider: DiagnosticProvider, val providerVersion: String, val space: DiagnosticSpace, val heapLimitMiB: Int)

@Serializable internal data class StoredDiagnostic(val runtime: DiagnosticRuntime, val attempt: DiagnosticAttempt)
class DiagnosticReport internal constructor(val json: String, val attemptCount: Int) {
    val lines: List<String> = json.lines()
}

/** Fixed schema limits apply on write AND read, including files left by an older process. */
class AndroidDiagnosticStore internal constructor(
    private val context: Context,
    directory: File? = null,
    private val now: () -> Long = System::currentTimeMillis,
    private val runtime: () -> DiagnosticRuntime = { diagnosticRuntime(context) },
) : DiagnosticSink,
    AutoCloseable {
    private val explicitDirectory = directory
    private val directory: File get() = explicitDirectory ?: File(context.filesDir, "check-diagnostics")
    private val mutex = Mutex()
    private sealed interface Write {
        data class Attempt(val value: DiagnosticAttempt) : Write
        data class Flush(val ready: CompletableDeferred<Unit>) : Write
    }

    // Application-owned bounded writer. Checks enqueue only; disk failure/latency cannot fail a check.
    private val writerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val queue = kotlinx.coroutines.channels.Channel<Write>(32)
    private val dropped = java.util.concurrent.atomic.AtomicInteger()
    init {
        writerScope.launch {
            for (command in queue) {
                when (command) {
                    is Write.Attempt -> try {
                        persist(command.value)
                    } catch (_: Exception) {
                        dropped.incrementAndGet()
                    }

                    is Write.Flush -> command.ready.complete(Unit)
                }
            }
        }
    }
    override fun close() {
        queue.close()
        writerScope.cancel()
    }
    private suspend fun flush() {
        withTimeout(2_000) {
            val ready = CompletableDeferred<Unit>()
            queue.send(Write.Flush(ready))
            ready.await()
        }
    }

    private val json = Json { prettyPrint = true }
    private val compact = Json
    private val versionPattern = Regex("[0-9][0-9A-Za-z.+_-]{0,63}")
    private val tokenPattern = Regex("[0-9a-f]{32}")

    override suspend fun watchToken(watchId: String?): String = withContext(Dispatchers.IO) {
        if (watchId == null) return@withContext ""
        mutex.withLock {
            check(directory.exists() || directory.mkdirs())
            val keyFile = File(directory, "identity-key")
            val key = if (keyFile.isFile && keyFile.length() == 32L) {
                keyFile.readBytes()
            } else {
                ByteArray(32).also {
                    SecureRandom().nextBytes(it)
                    keyFile.writeBytes(it)
                }
            }
            val mac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(key, "HmacSHA256")) }
            mac.doFinal(watchId.toByteArray()).take(16).joinToString("") { "%02x".format(it) }
        }
    }

    override suspend fun append(attempt: DiagnosticAttempt) {
        if (!queue.trySend(Write.Attempt(attempt)).isSuccess) dropped.incrementAndGet()
    }
    private suspend fun persist(attempt: DiagnosticAttempt) = withContext(Dispatchers.IO) {
        mutex.withLock {
            check(directory.exists() || directory.mkdirs())
            val stored = sanitize(StoredDiagnostic(runtime(), attempt)) ?: return@withLock
            val bytes = compact.encodeToString(stored).toByteArray()
            if (bytes.size > MAX_RECORD_BYTES) return@withLock
            val file = File(directory, "trace-${stored.attempt.attemptId}.json")
            val temporary = File(directory, file.name + ".partial")
            try {
                temporary.writeBytes(bytes)
                check(temporary.renameTo(file))
            } finally {
                temporary.delete()
            }
            retained()
        }
    }

    private fun sanitize(value: StoredDiagnostic): StoredDiagnostic? {
        val a = value.attempt
        if (!tokenPattern.matches(a.attemptId) || a.schema != 1 || a.startedAt < now() - RETENTION_MS || a.startedAt > now() + 60_000 || (a.watchToken.isNotEmpty() && !tokenPattern.matches(a.watchToken))) return null
        val safeRuntime = value.runtime.copy(
            versionName = value.runtime.versionName.takeIf(versionPattern::matches) ?: "unavailable",
            revision = value.runtime.revision.takeIf { Regex("[0-9a-f]{7,40}").matches(it) } ?: "unavailable",
            providerVersion = value.runtime.providerVersion.takeIf(versionPattern::matches) ?: "unavailable",
            versionCode = value.runtime.versionCode.coerceIn(0, Int.MAX_VALUE.toLong()),
            androidApi = value.runtime.androidApi.coerceIn(1, 100),
            heapLimitMiB = value.runtime.heapLimitMiB.coerceIn(0, 65_536),
        )
        return StoredDiagnostic(safeRuntime, a.copy(events = a.events.take(64).map { it.copy(elapsedMs = it.elapsedMs.coerceIn(0, 600_000), durationMs = it.durationMs.coerceIn(0, 600_000), code = it.code?.coerceIn(-100, 599)) }, failure = a.failure?.copy(causes = a.failure.causes.take(4))))
    }

    private fun retained(): List<File> {
        val candidates = directory.listFiles().orEmpty().filter { it.name.startsWith("trace-") }.sortedByDescending { it.lastModified() }
        var bytes = 0L
        val keep = mutableListOf<File>()
        for (f in candidates) {
            if (!f.name.endsWith(".json") || f.length() > MAX_RECORD_BYTES || keep.size >= MAX_RECORDS || bytes + f.length() > MAX_DISK_BYTES) {
                f.delete()
                continue
            }
            val valid = runCatching { sanitize(compact.decodeFromString<StoredDiagnostic>(f.readText())) }.getOrNull()
            if (valid == null) {
                f.delete()
                continue
            }
            keep += f
            bytes += f.length()
        }
        return keep
    }

    suspend fun report(watchId: String? = null): DiagnosticReport = withContext(Dispatchers.IO) {
        flush()
        val selected = watchId?.let { watchToken(it) }
        mutex.withLock {
            val header = sanitize(StoredDiagnostic(runtime(), DiagnosticAttempt(startedAt = now(), mode = com.bernaferrari.changedetection.repo.CaptureMode.DEFAULT, selected = false, excluded = false, trigger = DiagnosticTrigger.CHECK, outcome = DiagnosticOutcome.INCOMPLETE, events = emptyList())))!!.runtime
            fun envelope(rows: List<JsonElement>) = buildJsonObject {
                put("schema", 1)
                put("runtime", compact.encodeToJsonElement(header))
                put("droppedRecords", dropped.get().coerceIn(0, 100_000))
                put("attempts", JsonArray(rows))
            }
            val rows = mutableListOf<JsonElement>()
            val aliases = mutableMapOf<String, String>()
            for (file in retained()) {
                val value = runCatching { sanitize(compact.decodeFromString<StoredDiagnostic>(file.readText())) }.getOrNull() ?: continue
                if (selected != null && value.attempt.watchToken != selected) continue
                val alias = if (value.attempt.watchToken.isEmpty()) "preview" else aliases.getOrPut(value.attempt.watchToken) { "watch-${aliases.size + 1}" }
                val encoded = compact.encodeToJsonElement(value).jsonObject
                val attempt = encoded.getValue("attempt").jsonObject.toMutableMap().apply {
                    remove("watchToken")
                    remove("startedAt")
                    remove("attemptId")
                    put("watch", JsonPrimitive(alias))
                    put("ageMinutes", JsonPrimitive(((now() - value.attempt.startedAt) / 60_000).coerceIn(0, 10_080)))
                }
                val row = JsonObject(encoded.toMutableMap().apply { put("attempt", JsonObject(attempt)) })
                val candidate = envelope(rows + row)
                if (json.encodeToString(candidate).toByteArray().size > MAX_REPORT_BYTES) break
                rows += row
                if (rows.size == MAX_REPORT_RECORDS) break
            }
            val report = envelope(rows)
            DiagnosticReport(json.encodeToString(report), rows.size)
        }
    }

    suspend fun prepareShare(report: DiagnosticReport): File = withContext(Dispatchers.IO) {
        require(report.json.toByteArray().size <= MAX_REPORT_BYTES)
        // Only schema-produced report data should be passed here; never share stored raw files.
        mutex.withLock {
            val dir = File(context.cacheDir, "shares").apply { check(exists() || mkdirs()) }
            val old = dir.listFiles().orEmpty().filter { it.name.startsWith("diagnostics-") }.sortedByDescending { it.lastModified() }
            old.forEachIndexed { index, file -> if (index >= 7 || file.lastModified() < now() - 24 * 3_600_000L) file.delete() }
            File(dir, "diagnostics-${java.util.UUID.randomUUID()}.json").apply { writeText(report.json) }
        }
    }

    companion object {
        const val MAX_RECORDS = 100
        const val MAX_RECORD_BYTES = 16 * 1024
        const val MAX_DISK_BYTES = 1024 * 1024
        const val MAX_REPORT_BYTES = 64 * 1024
        const val MAX_REPORT_RECORDS = 10
        const val RETENTION_MS = 7 * 24 * 3_600_000L
    }
}

object AndroidDiagnostics {
    @Volatile private var store: AndroidDiagnosticStore? = null

    @Volatile internal var variant = DiagnosticVariant.UNKNOWN

    @Volatile internal var revision = "unavailable"

    @Synchronized fun install(context: Context, buildType: String? = null, buildRevision: String? = null): AndroidDiagnosticStore {
        buildType?.let {
            variant = when (it) {
                "release" -> DiagnosticVariant.RELEASE
                "debug" -> DiagnosticVariant.DEBUG
                else -> DiagnosticVariant.UNKNOWN
            }
        }
        buildRevision?.let { revision = it }
        return (store ?: AndroidDiagnosticStore(context.applicationContext).also { store = it }).also { DiagnosticLog.sink = it }
    }
}

@Suppress("DEPRECATION")
private fun diagnosticRuntime(context: Context): DiagnosticRuntime {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    val web = WebView.getCurrentWebViewPackage()
    val provider = when (web?.packageName) {
        null -> DiagnosticProvider.UNAVAILABLE
        "com.google.android.webview" -> DiagnosticProvider.GOOGLE
        "com.android.webview" -> DiagnosticProvider.SYSTEM
        "com.android.chrome" -> DiagnosticProvider.CHROME
        else -> DiagnosticProvider.OTHER
    }
    val available = runCatching { StatFs(context.filesDir.path).availableBytes }.getOrNull()
    val space = when {
        available == null -> DiagnosticSpace.UNKNOWN
        available < 32L * 1024 * 1024 -> DiagnosticSpace.BELOW_32_MIB
        available < 256L * 1024 * 1024 -> DiagnosticSpace.BELOW_256_MIB
        else -> DiagnosticSpace.AT_LEAST_256_MIB
    }
    return DiagnosticRuntime(if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong(), info.versionName.orEmpty(), AndroidDiagnostics.variant, AndroidDiagnostics.revision, Build.VERSION.SDK_INT, provider, web?.versionName.orEmpty(), space, (Runtime.getRuntime().maxMemory() / (1024 * 1024)).toInt())
}
