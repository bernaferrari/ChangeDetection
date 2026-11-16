package com.bernaferrari.changedetection.backup

import androidx.room3.withWriteTransaction
import com.bernaferrari.changedetection.extensions.isValidUrl
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.local.ChangeDatabase
import com.bernaferrari.changedetection.repo.source.local.SitesDao
import com.bernaferrari.changedetection.repo.source.local.SnapsDao
import com.bernaferrari.changedetection.storage.SnapshotFileCleanup
import com.bernaferrari.changedetection.util.ContentHasher
import com.bernaferrari.changedetection.util.FileStorage
import com.bernaferrari.changedetection.util.MAX_SNAPSHOT_BYTES
import com.bernaferrari.changedetection.util.fileIoDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString

/**
 * Writes and reads backups. A backup is made only when the user asks and goes only where they put it.
 * Import only ever adds: a page already on this device is never changed, and a file is treated as untrusted.
 */
class BackupService(
    private val database: ChangeDatabase,
    private val sitesDao: SitesDao,
    private val snapsDao: SnapsDao,
    private val files: FileStorage,
    private val cleanup: SnapshotFileCleanup,
    private val readSettings: suspend () -> Map<String, String>,
    private val writeSettings: suspend (Map<String, String>) -> Unit,
    private val now: () -> Long = { kotlin.time.Clock.System.now().toEpochMilliseconds() },
) {
    /** The small, readable backup: every page with its options, and the app's settings. No saved versions. */
    suspend fun exportPages(appVersion: String): ByteArray = withContext(fileIoDispatcher) {
        encode(manifest(appVersion, snaps = emptyList(), sites = sitesDao.getSitesList()))
    }

    /** The complete backup: the manifest first, then every saved version whose file is intact. */
    suspend fun exportEverything(sink: BackupSink, appVersion: String): BackupSummary = withContext(fileIoDispatcher) {
        val sites = sitesDao.getSitesList()
        val snaps = sites.flatMap { snapsDao.getAllSnapsForSiteId(it.id) }
        val manifest = manifest(appVersion, snaps, sites)
        sink.write(MANIFEST_ENTRY, encode(manifest))
        var written = 0
        for (snap in snaps) {
            currentCoroutineContext().ensureActive()
            // A version that cannot be read is left out; import skips versions without content.
            val bytes = runCatching { files.readBytes(snap.snapId) }.getOrNull()?.takeIf { it.size == snap.contentSize && (snap.payloadHash.isEmpty() || ContentHasher.hashContent(it) == snap.payloadHash) } ?: continue
            sink.write(SNAPSHOT_PREFIX + snap.snapId, bytes)
            written++
        }
        BackupSummary(pages = manifest.sites.size, versions = written)
    }

    private suspend fun manifest(appVersion: String, snaps: List<Snap>, sites: List<Site>) = BackupManifest(exportedAt = now(), appVersion = appVersion, settings = readSettings(), sites = sites, snaps = snaps)

    private fun encode(manifest: BackupManifest): ByteArray = backupJson.encodeToString(manifest).encodeToByteArray().also {
        check(it.size <= MAX_MANIFEST_BYTES) { "Backup manifest is too large" }
    }

    /** Inspect the file and current database without changing either; import checks everything again. */
    suspend fun preview(source: BackupSource): RestorePreview = withContext(fileIoDispatcher) {
        val manifest = readManifest(source)
        val usable = manifest.sites.distinctBy { it.id }.filter { it.id.isNotBlank() && it.id.length <= MAX_ID_LENGTH && it.url.isValidUrl() }
        val added = mutableListOf<Site>()
        val existing = mutableListOf<Site>()
        for (site in usable) {
            if (sitesDao.getSiteById(site.id) == null) added += site else existing += site
        }
        val addedIds = added.map { it.id }.toSet()
        val versions = manifest.snaps.filter { SNAP_ID.matches(it.snapId) && it.contentSize in 1..MAX_SNAPSHOT_BYTES }.associateBy { it.snapId }
        val seen = HashSet<String>()
        var count = 0
        while (true) {
            currentCoroutineContext().ensureActive()
            val name = source.nextName() ?: break
            val snap = name.takeIf { it.startsWith(SNAPSHOT_PREFIX) }?.let { versions[it.removePrefix(SNAPSHOT_PREFIX)] }
                ?: throw InvalidBackupException("Unexpected entry")
            if (!seen.add(name)) throw InvalidBackupException("Duplicate entry")
            val bytes = source.readCurrent(snap.contentSize) ?: throw InvalidBackupException("Entry larger than declared")
            if (snap.siteId !in addedIds || bytes.size != snap.contentSize) continue
            if (snap.payloadHash.isNotEmpty() && ContentHasher.hashContent(bytes) != snap.payloadHash) continue
            if (snapsDao.getSnapById(snap.snapId) != null) continue
            count++
        }
        RestorePreview(added, existing, count)
    }

    /**
     * Adds the pages of a backup that are not on this device yet, with their versions when the backup has them.
     * Nothing already here is changed. A file that is not a well-formed backup is refused and leaves no trace.
     */
    suspend fun import(source: BackupSource, restoreSettings: Boolean): ImportSummary = withContext(fileIoDispatcher) {
        val manifest = readManifest(source)
        val declared = manifest.sites.distinctBy { it.id }
        // Only complete web addresses: an imported page is later opened in the in-app browser.
        val usable = declared.filter { it.id.isNotBlank() && it.id.length <= MAX_ID_LENGTH && it.url.isValidUrl() }
        val versions = manifest.snaps.filter { SNAP_ID.matches(it.snapId) && it.contentSize in 1..MAX_SNAPSHOT_BYTES }.associateBy { it.snapId }
        // A deleted page may still have snapshot files queued for cleanup.
        cleanup.drain()
        val writtenFiles = mutableListOf<String>()
        val (added, restored) = try {
            cleanup.withRestoreLock<Pair<Int, Int>> {
                database.withWriteTransaction {
                    val added = mutableMapOf<String, Site>()
                    for (site in usable) {
                        if (sitesDao.getSiteById(site.id) != null) continue
                        val restoredSite = site.copy(isSuccessful = true, lastFailureCode = null, failureCount = 0, failingSince = null, failureAlerted = false)
                        sitesDao.insertSite(restoredSite)
                        added[site.id] = restoredSite
                    }
                    var restored = 0
                    val entries = HashSet<String>()
                    val pagesWithVersions = HashSet<String>()
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val name = source.nextName() ?: break
                        val snap = name.takeIf { it.startsWith(SNAPSHOT_PREFIX) }?.let { versions[it.removePrefix(SNAPSHOT_PREFIX)] }
                            ?: throw InvalidBackupException("Unexpected entry")
                        if (!entries.add(name)) throw InvalidBackupException("Duplicate entry")
                        val bytes = source.readCurrent(snap.contentSize) ?: throw InvalidBackupException("Entry larger than declared")
                        if (snap.siteId !in added || bytes.size != snap.contentSize) continue
                        if (snap.payloadHash.isNotEmpty() && ContentHasher.hashContent(bytes) != snap.payloadHash) continue
                        if (snapsDao.getSnapById(snap.snapId) != null || files.fileExists(snap.snapId)) continue
                        writtenFiles += snap.snapId
                        files.writeBytes(snap.snapId, bytes)
                        snapsDao.insertSnap(snap)
                        pagesWithVersions += snap.siteId
                        restored++
                    }
                    for ((id, site) in added) {
                        if (id !in pagesWithVersions) {
                            sitesDao.insertSite(site.copy(timestamp = 0L, lastSuccessfulCheck = null, isRead = true))
                        }
                    }
                    added.size to restored
                }
            }
        } catch (error: Throwable) {
            // Room rolls back the rows. Remove only files that still have no owning row.
            withContext(NonCancellable) { writtenFiles.forEach { cleanup.removeOrphan(it) } }
            throw error
        }
        if (restoreSettings) writeSettings(manifest.settings)
        ImportSummary(pagesAdded = added, pagesAlreadyHere = usable.size - added, pagesRejected = declared.size - usable.size, versionsAdded = restored, settingsRestored = restoreSettings)
    }

    private fun readManifest(source: BackupSource): BackupManifest {
        if (source.nextName() != MANIFEST_ENTRY) throw InvalidBackupException("No manifest")
        val bytes = source.readCurrent(MAX_MANIFEST_BYTES) ?: throw InvalidBackupException("Manifest too large")
        val manifest = try {
            backupJson.decodeFromString<BackupManifest>(bytes.decodeToString())
        } catch (error: IllegalArgumentException) {
            throw InvalidBackupException("Manifest unreadable")
        }
        if (manifest.format != BACKUP_FORMAT || manifest.version < 1) throw InvalidBackupException("Not a backup")
        if (manifest.version > BACKUP_VERSION) throw BackupTooNewException()
        return manifest
    }

    private companion object {
        const val MAX_ID_LENGTH = 128
    }
}
