package com.bernaferrari.changedetection.backup

import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
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

/** Where a full backup is written, one entry after another. */
interface BackupSink {
    fun write(name: String, bytes: ByteArray)
}

/** A backup being read, one entry after another, in the order it was written. */
interface BackupSource {
    /** Moves to the next entry and returns its name, or null at the end. */
    fun nextName(): String?

    /** The current entry's bytes, or null when it holds more than [maxBytes] (the caller must then give up). */
    fun readCurrent(maxBytes: Int): ByteArray?
}

/** A pages-and-settings file is a backup with exactly one entry. */
class ManifestOnlySource(private val manifest: ByteArray) : BackupSource {
    private var served = false
    override fun nextName(): String? = if (served) null else MANIFEST_ENTRY.also { served = true }
    override fun readCurrent(maxBytes: Int): ByteArray? = manifest.takeIf { it.size <= maxBytes }
}

/** The file is not a backup this app can read: wrong shape, damaged, or built to mislead. */
class InvalidBackupException(message: String = "Not a usable backup") : Exception(message)

/** The file was written by a newer app whose format this one does not know. */
class BackupTooNewException : Exception("Backup made by a newer app version")

data class BackupSummary(val pages: Int, val versions: Int)

data class RestorePreview(val pagesToAdd: List<Site>, val pagesAlreadyHere: List<Site>, val versionsToAdd: Int)

data class ImportSummary(val pagesAdded: Int, val pagesAlreadyHere: Int, val pagesRejected: Int, val versionsAdded: Int, val settingsRestored: Boolean)
