package com.bernaferrari.changedetection.backup

import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal const val BACKUP_FORMAT = "change-detection-backup"
internal const val BACKUP_VERSION = 1
internal const val MANIFEST_ENTRY = "manifest.json"
internal const val SNAPSHOT_PREFIX = "snapshots/"

/** A manifest lists pages and versions, never their content; anything larger is not one of ours. */
internal const val MAX_MANIFEST_BYTES = 8 * 1024 * 1024

/** Saved versions are stored under their id, so an id from a file must look exactly like the ids this app creates. */
internal val SNAP_ID = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")

/** The whole of a pages-and-settings backup, and the first entry of a full one. */
@Serializable
data class BackupManifest(
    @Required val format: String = BACKUP_FORMAT,
    @Required val version: Int = BACKUP_VERSION,
    val exportedAt: Long = 0,
    val appVersion: String = "",
    val settings: Map<String, String> = emptyMap(),
    val sites: List<Site> = emptyList(),
    val snaps: List<Snap> = emptyList(),
)

/** Unknown fields are ignored so a newer app's file still reads; defaults are written so the file is self-describing. */
internal val backupJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = true
}

/** Snapshot files can be copied without retaining the encoded payload in memory. */
interface BackupSnapshotStorage : com.bernaferrari.changedetection.util.FileStorage {
    /** A read-only handle to an immutable snapshot, which export can rewind after validation. */
    fun openSnapshot(fileName: String): FileInputStream

    /** Publishes the temporary file only when [write] validates its contents. */
    fun writeSnapshot(fileName: String, size: Int, write: (OutputStream) -> Boolean): Boolean
}

/** Where a full backup is written, one entry after another. */
interface BackupSink {
    fun writeEntry(name: String, write: (OutputStream) -> Unit)

    fun write(name: String, bytes: ByteArray) = writeEntry(name) { it.write(bytes) }
}

/** A backup being read, one entry after another, in the order it was written. */
interface BackupSource {
    fun nextName(): String?

    /** Reads the current entry. The source owns this stream and advances it at [nextName]. */
    fun currentStream(): InputStream

    fun readCurrent(maxBytes: Int): ByteArray? = try {
        LimitedBackupInput(currentStream(), maxBytes).readBytes()
    } catch (_: InvalidBackupException) {
        null
    }
}

class ManifestOnlySource(private val input: InputStream) : BackupSource {
    constructor(manifest: ByteArray) : this(ByteArrayInputStream(manifest))

    private var served = false
    override fun nextName(): String? = if (served) null else MANIFEST_ENTRY.also { served = true }
    override fun currentStream(): InputStream = input
}

/** The file is not a backup this app can read: wrong shape, damaged, or built to mislead. */
class InvalidBackupException(message: String = "Not a usable backup") : Exception(message)

/** The file was written by a newer app whose format this one does not know. */
class BackupTooNewException : Exception("Backup made by a newer app version")

data class BackupSummary(val pages: Int, val versions: Int)

data class RestorePreview(val pagesToAdd: List<Site>, val pagesAlreadyHere: List<Site>, val versionsToAdd: Int)

data class ImportSummary(val pagesAdded: Int, val pagesAlreadyHere: Int, val pagesRejected: Int, val versionsAdded: Int, val settingsRestored: Boolean, val duplicateVersions: Int = 0)
