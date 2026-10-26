package com.bernaferrari.changedetection.preview

import com.bernaferrari.changedetection.util.FileStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Previews that outlive the process. Saved versions never change, so the preview between two of them is
 * worked out once: screenshot comparisons in particular are too slow to repeat every time Activity, Home
 * or the version list opens after the app was closed.
 *
 * One small file, newest [capacity] pairs kept, written shortly after previews stop arriving. A preview is
 * a few text segments or area coordinates, never pixels.
 */
class PreviewStore(
    private val files: FileStorage,
    private val capacity: Int = 512,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val lock = Mutex()
    private var entries: LinkedHashMap<String, ChangePreview>? = null
    private var pendingWrite: Job? = null

    suspend fun get(key: String): ChangePreview? = lock.withLock { loaded()[key] }

    suspend fun put(key: String, preview: ChangePreview) {
        lock.withLock {
            val map = loaded()
            map.remove(key)
            map[key] = preview
            while (map.size > capacity) map.remove(map.keys.first())
            pendingWrite?.cancel()
            pendingWrite = scope.launch {
                delay(WRITE_DELAY_MS)
                write()
            }
        }
    }

    private suspend fun loaded(): LinkedHashMap<String, ChangePreview> = entries ?: withContext(com.bernaferrari.changedetection.util.fileIoDispatcher) {
        val stored = runCatching {
            if (!files.fileExists(FILE_NAME)) return@runCatching null
            json.decodeFromString(StoredPreviews.serializer(), files.readBytes(FILE_NAME).decodeToString())
        }.getOrNull()
        LinkedHashMap<String, ChangePreview>().apply {
            // A preview format from another app version is dropped rather than misread.
            if (stored?.version == FORMAT_VERSION) stored.entries.forEach { put(it.key, it.preview) }
        }
    }.also { entries = it }

    private suspend fun write() {
        val snapshot = lock.withLock { entries?.map { StoredPreview(it.key, it.value) } } ?: return
        val bytes = json.encodeToString(StoredPreviews.serializer(), StoredPreviews(FORMAT_VERSION, snapshot)).encodeToByteArray()
        withContext(com.bernaferrari.changedetection.util.fileIoDispatcher) {
            // A cache: a failed write only means the next launch works the previews out again.
            runCatching { files.writeBytes(FILE_NAME, bytes) }
        }
    }

    @Serializable
    private data class StoredPreviews(val version: Int, val entries: List<StoredPreview>)

    @Serializable
    private data class StoredPreview(val key: String, val preview: ChangePreview)

    private companion object {
        /** Not a snapshot-shaped name, so the orphan sweep never mistakes it for a lost capture. */
        const val FILE_NAME = "change-previews.json"
        const val FORMAT_VERSION = 1
        const val WRITE_DELAY_MS = 2_000L
        val json = Json { ignoreUnknownKeys = true }
    }
}
