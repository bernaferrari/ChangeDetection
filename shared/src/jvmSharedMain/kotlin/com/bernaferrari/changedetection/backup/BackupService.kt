package com.bernaferrari.changedetection.backup

import androidx.room3.withReadTransaction
import androidx.room3.withWriteTransaction
import com.bernaferrari.changedetection.extensions.isValidUrl
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.local.ChangeDatabase
import com.bernaferrari.changedetection.repo.source.local.SitesDao
import com.bernaferrari.changedetection.repo.source.local.SnapsDao
import com.bernaferrari.changedetection.storage.SnapshotFileCleanup
import com.bernaferrari.changedetection.util.MAX_SNAPSHOT_BYTES
import com.bernaferrari.changedetection.util.fileIoDispatcher
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream

/** Backups are created on request. Restore adds pages and leaves existing pages and history alone. */
@OptIn(ExperimentalSerializationApi::class)
class BackupService(
    private val database: ChangeDatabase,
    private val sitesDao: SitesDao,
    private val snapsDao: SnapsDao,
    private val files: BackupSnapshotStorage,
    private val cleanup: SnapshotFileCleanup,
    private val readSettings: suspend () -> Map<String, String>,
    private val writeSettings: suspend (Map<String, String>) -> Unit,
    private val now: () -> Long = { kotlin.time.Clock.System.now().toEpochMilliseconds() },
) {
    suspend fun exportPages(output: OutputStream, appVersion: String): BackupSummary = withContext(fileIoDispatcher) {
        val sites = sitesDao.getSitesList()
        writeManifest(output, manifest(appVersion, emptyList(), sites))
        BackupSummary(pages = sites.size, versions = 0)
    }

    /** Keep payload files available while exporting the corresponding point-in-time history. */
    suspend fun exportEverything(sink: BackupSink, appVersion: String): BackupSummary = cleanup.withRestoreLock {
        val (sites, snaps) = database.withReadTransaction { sitesDao.getSitesList() to snapsDao.getAllSnaps() }
        val manifest = manifest(appVersion, snaps, sites)
        sink.writeEntry(MANIFEST_ENTRY) { writeManifest(it, manifest) }
        var written = 0
        val checkCancellation = currentCoroutineContext()::ensureActive
        for (snap in snaps) {
            checkCancellation()
            val snapshot = try {
                files.openSnapshot(snap.snapId)
            } catch (_: IOException) {
                continue
            }
            snapshot.use { input ->
                val intact = try {
                    copySnapshot(input, null, snap, checkCancellation)
                } catch (_: IOException) {
                    false
                } catch (_: InvalidBackupException) {
                    false
                }
                if (!intact) return@use
                // Snapshot files are immutable and cleanup holds the export lock. Rewind the
                // validated handle so the ZIP gets the same bytes even if the path is replaced.
                input.channel.position(0)
                sink.writeEntry(SNAPSHOT_PREFIX + snap.snapId) { output ->
                    check(copySnapshot(input, output, snap, checkCancellation, verifyHash = false)) { "Saved version changed during export" }
                }
                written++
            }
        }
        BackupSummary(pages = sites.size, versions = written)
    }

    private suspend fun manifest(appVersion: String, snaps: List<Snap>, sites: List<Site>) = BackupManifest(exportedAt = now(), appVersion = appVersion, settings = readSettings(), sites = sites, snaps = snaps)

    private fun writeManifest(output: OutputStream, manifest: BackupManifest) {
        var remaining = MAX_MANIFEST_BYTES
        val limited = object : OutputStream() {
            override fun write(value: Int) {
                check(remaining-- > 0) { "Backup manifest is too large" }
                output.write(value)
            }

            override fun write(bytes: ByteArray, offset: Int, length: Int) {
                check(length <= remaining) { "Backup manifest is too large" }
                remaining -= length
                output.write(bytes, offset, length)
            }
        }
        backupJson.encodeToStream(manifest, limited)
    }

    suspend fun preview(source: BackupSource): RestorePreview = withContext(fileIoDispatcher) {
        val manifest = readManifest(source)
        val usable = usablePages(manifest)
        val current = sitesDao.getSitesList().mapTo(HashSet()) { it.id }
        val (existing, added) = usable.partition { it.id in current }
        val addedIds = added.mapTo(HashSet()) { it.id }
        val checkCancellation = currentCoroutineContext()::ensureActive
        var count = 0
        readEntries(source, manifest) { snap, input ->
            if (copySnapshot(input, null, snap, checkCancellation) && snap.siteId in addedIds && snapsDao.getSnapById(snap.snapId) == null && !files.fileExists(snap.snapId)) {
                count++
            }
        }
        RestorePreview(added, existing, count)
    }

    suspend fun import(source: BackupSource, restoreSettings: Boolean): ImportSummary = withContext(fileIoDispatcher) {
        val manifest = readManifest(source)
        val usable = usablePages(manifest)
        cleanup.drain()
        val writtenFiles = mutableListOf<String>()
        val previousSettings = if (restoreSettings) readSettings() else null
        var settingsWritten = false
        var databaseCommitted = false
        val (added, restored) = try {
            cleanup.withRestoreLock {
                database.withWriteTransaction {
                    val existing = sitesDao.getSitesList().mapTo(HashSet()) { it.id }
                    val added = usable.filter { it.id !in existing }.associate { site ->
                        val restoredSite = site.copy(isSuccessful = true, lastFailureCode = null, failureCount = 0, failingSince = null, failureAlerted = false)
                        sitesDao.insertSite(restoredSite)
                        site.id to restoredSite
                    }
                    val checkCancellation = currentCoroutineContext()::ensureActive
                    var restored = 0
                    val pagesWithVersions = HashSet<String>()
                    readEntries(source, manifest) { snap, input ->
                        if (snap.siteId !in added || snapsDao.getSnapById(snap.snapId) != null || files.fileExists(snap.snapId)) {
                            copySnapshot(input, null, snap, checkCancellation)
                        } else {
                            if (files.writeSnapshot(snap.snapId, snap.contentSize) { output -> copySnapshot(input, output, snap, checkCancellation) }) {
                                writtenFiles += snap.snapId
                                snapsDao.insertSnap(snap)
                                pagesWithVersions += snap.siteId
                                restored++
                            }
                        }
                    }
                    for ((id, site) in added) {
                        if (id !in pagesWithVersions) {
                            sitesDao.insertSite(site.copy(timestamp = 0L, lastSuccessfulCheck = null, isRead = true))
                        }
                    }
                    if (restoreSettings) {
                        withContext(NonCancellable) {
                            writeSettings(manifest.settings)
                            settingsWritten = true
                        }
                    }
                    added.size to restored
                }.also { databaseCommitted = true }
            }
        } catch (error: Throwable) {
            withContext(NonCancellable) {
                if (settingsWritten && !databaseCommitted && previousSettings != null) {
                    try {
                        writeSettings(previousSettings)
                    } catch (rollbackError: Exception) {
                        error.addSuppressed(rollbackError)
                    }
                }
                writtenFiles.forEach { cleanup.removeOrphan(it) }
            }
            throw error
        }
        ImportSummary(
            pagesAdded = added,
            pagesAlreadyHere = usable.size - added,
            pagesRejected = manifest.sites.size - usable.size,
            versionsAdded = restored,
            settingsRestored = restoreSettings,
            duplicateVersions = manifest.snaps.size - manifest.snaps.distinctBy(Snap::snapId).size,
        )
    }

    private fun usablePages(manifest: BackupManifest) = manifest.sites.distinctBy(Site::id)
        .filter { it.id.isNotBlank() && it.id.length <= MAX_ID_LENGTH && it.url.isValidUrl() }

    private fun readManifest(source: BackupSource): BackupManifest {
        if (nextBackupEntry(source) != MANIFEST_ENTRY) throw InvalidBackupException("No manifest")
        val manifest = try {
            backupJson.decodeFromStream<BackupManifest>(LimitedBackupInput(source.currentStream(), MAX_MANIFEST_BYTES))
        } catch (_: IllegalArgumentException) {
            throw InvalidBackupException("Manifest unreadable")
        }
        if (manifest.format != BACKUP_FORMAT || manifest.version < 1) throw InvalidBackupException("Not a backup")
        if (manifest.version > BACKUP_VERSION) throw BackupTooNewException()
        return manifest
    }

    private suspend fun readEntries(source: BackupSource, manifest: BackupManifest, read: suspend (Snap, InputStream) -> Unit) {
        val versions = manifest.snaps.distinctBy(Snap::snapId)
            .filter { SNAP_ID.matches(it.snapId) && it.contentSize in 1..MAX_SNAPSHOT_BYTES }.associateBy(Snap::snapId)
        val seen = HashSet<String>()
        while (true) {
            currentCoroutineContext().ensureActive()
            val name = nextBackupEntry(source) ?: return
            val snap = name.takeIf { it.startsWith(SNAPSHOT_PREFIX) }?.let { versions[it.removePrefix(SNAPSHOT_PREFIX)] }
                ?: throw InvalidBackupException("Unexpected entry")
            if (!seen.add(name)) throw InvalidBackupException("Duplicate entry")
            read(snap, source.currentStream())
        }
    }

    private fun nextBackupEntry(source: BackupSource): String? {
        repeat(1_024) {
            val name = source.nextName() ?: return null
            if (name != "snapshots/" && name != ".DS_Store" && !name.startsWith("__MACOSX/")) return name
            LimitedBackupInput(source.currentStream(), 256 * 1024).copyTo(discardedOutput)
        }
        throw InvalidBackupException("Too many metadata entries")
    }

    private fun copySnapshot(input: InputStream, output: OutputStream?, snap: Snap, checkCancellation: () -> Unit, verifyHash: Boolean = true): Boolean {
        val limited = LimitedBackupInput(input, snap.contentSize)
        val digest = if (!verifyHash || snap.payloadHash.isEmpty()) null else MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(16 * 1024)
        while (true) {
            checkCancellation()
            val count = limited.read(buffer)
            if (count < 0) break
            digest?.update(buffer, 0, count)
            output?.write(buffer, 0, count)
        }
        return limited.bytesRead == snap.contentSize && (digest == null || digest.digest().toHexString() == snap.payloadHash)
    }

    private companion object {
        const val MAX_ID_LENGTH = 128
        val discardedOutput = object : OutputStream() {
            override fun write(value: Int) = Unit
            override fun write(bytes: ByteArray, offset: Int, length: Int) = Unit
        }
    }
}

/** Counts decompressed bytes and leaves the underlying ZIP entry stream open. */
internal class LimitedBackupInput(input: InputStream, private val limit: Int) : FilterInputStream(input) {
    var bytesRead = 0
        private set

    override fun read(): Int = super.read().also { if (it >= 0) record(1) }

    override fun read(bytes: ByteArray, offset: Int, length: Int): Int = super.read(bytes, offset, minOf(length, limit - bytesRead + 1)).also { if (it > 0) record(it) }

    private fun record(length: Int) {
        bytesRead += length
        if (bytesRead > limit) throw InvalidBackupException("Entry larger than declared")
    }

    override fun close() = Unit
}
