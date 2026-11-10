# Plan 015: Build the backup engine: export and import of pages, settings and (optionally) saved versions

> **Executor instructions**
> You are building one self-contained feature core, with no user interface. You have not seen the audit or the conversation that produced this plan; everything you need is in this file.
> 1. Run the drift check first: `git diff --stat a9f8f8ba..HEAD -- shared/src/commonMain/kotlin/com/bernaferrari/changedetection/repo/ shared/src/commonMain/kotlin/com/bernaferrari/changedetection/storage/SnapshotFileCleanup.kt shared/src/commonMain/kotlin/com/bernaferrari/changedetection/di/CoreDiModule.kt shared/src/commonMain/kotlin/com/bernaferrari/changedetection/util/FileStorage.kt`. Changes are likely (other work lands in these files). What matters is that the signatures quoted under "Current state" still exist; check each one. `Site` may have gained fields; that is fine and needs no change to this plan.
> 2. Follow the steps in order and run each **Verify** command.
> 3. Touch only the files listed under "In scope".
> 4. If a STOP condition applies, stop and report. Do not improvise.
> 5. When finished, set this plan's row in `plans/README.md` to `DONE`.

## Status

| Field | Value |
| --- | --- |
| Priority | Direction (most-requested missing feature) |
| Effort | M to L (1 to 2 days) |
| Risk | Medium. Writes user data; an imported file is untrusted input |
| Depends on | Nothing. Plan 016 (the Settings screen entry) depends on this |
| Category | Feature |
| Planned at | commit `a9f8f8ba`, 2026-11-09 |

## Why this matters

Everything this app knows lives on one device, on purpose: no account, no cloud, and the system backup is switched off. The cost is that a lost, reset or replaced phone loses every watched page and all history, and there is no way to move a watchlist to another device or hand it to someone else. The app's own backup rules already promise an "explicit in-app export"; it does not exist yet.

This plan builds the engine for it. Two kinds of file, chosen by the user:

1. **Pages and settings** (`.json`, a few kilobytes). The watched pages with all their per-page options, plus the app's settings. No saved versions. Readable in any text editor, easy to keep or share. After import each page simply takes a fresh first version on its next check.
2. **Everything** (`.zip`). The same JSON plus the saved versions (screenshots, text), so history survives a move to a new phone. Can be large.

Import accepts either file and **only adds**: a page that is already on the device is left exactly as it is, so importing can never overwrite or delete anything, and importing the same file twice changes nothing.

Privacy stays as it is: the file is created only when the user asks and goes only where the user saves it. Cookies and sign-ins are never included.

## The file format (version 1)

The JSON document, called the manifest:

```json
{
  "format": "change-detection-backup",
  "version": 1,
  "exportedAt": 1794268800000,
  "appVersion": "3.2",
  "settings": { "sync_interval": "60", "theme_mode": "DARK" },
  "sites": [ { "title": "…", "url": "https://…", "id": "…", "…": "…" } ],
  "snaps": [ { "snapId": "…", "siteId": "…", "contentSize": 1234, "…": "…" } ]
}
```

- A **pages-and-settings** backup is this document alone, with `"snaps": []`.
- An **everything** backup is a zip whose **first** entry is `manifest.json` (the same document, with `snaps` filled in), followed by one entry per saved version named `snapshots/<snapId>` holding the stored bytes unchanged.
- `sites` and `snaps` are the app's existing `Site` and `Snap` classes, which are already `@Serializable`. Unknown fields are ignored when reading, and missing fields take their defaults, so the format tolerates newer and older app versions.
- `settings` maps a preference name to its value written as text. Only the names in the allow-list below are ever written or read.

## Current state

Nothing related to backup exists (`rg -n -i "backup" shared/src/commonMain/kotlin` prints nothing). The pieces to build on:

**The data classes.** `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/repo/Site.kt:14-59` and `repo/Snap.kt:20,42-52`:

```kotlin
@Serializable
@Entity(tableName = "sites")
data class Site(
    val title: String?,
    val url: String,
    val timestamp: Long,
    @PrimaryKey @ColumnInfo(name = "siteId") val id: String,
    val isSuccessful: Boolean,
    // ... more fields, several with defaults; includes lastFailureCode: String? and lastSuccessfulCheck: Long?,
    // and the run of failed checks: failureCount: Int = 0, failingSince: Long? = null, failureAlerted: Boolean = false
)
```

```kotlin
@Serializable
data class Snap(
    @PrimaryKey val snapId: String,
    val siteId: String,
    val timestamp: Long,
    val contentType: String,
    val contentCharset: String,
    val contentSize: Int,
    val contentHash: String = "",
    val processedContentHash: String = "",
    @ColumnInfo(defaultValue = "''") val payloadHash: String = "", // Exact stored-byte integrity; legacy rows remain unmodified.
)
```

`Snap.snapId` is a UUID string and is **also the file name** of the stored version. `Snap.contentSize` is the exact byte length of that file. `Snap.payloadHash`, when not empty, is `ContentHasher.hashContent(bytes)` (SHA-256 hex) of that file.

**Database access.** `repo/source/local/SitesDao.kt`: `suspend fun getSitesList(): List<Site>` (line 24), `suspend fun getSiteById(siteId: String): Site?` (56), `@Upsert suspend fun insertSite(site: Site)` (72-73), `suspend fun deleteSiteById(siteId: String)` (207). `repo/source/local/SnapsDao.kt`: `suspend fun getAllSnapsForSiteId(siteId: String): List<Snap>` (100), `suspend fun getSnapById(id: String): Snap?` (125). Deleting a site deletes its `snaps` rows (foreign key cascade), and a database trigger queues each deleted version's file for removal.

**Files.** `util/FileStorage.kt`:

```kotlin
const val MAX_SNAPSHOT_BYTES: Int = 32 * 1024 * 1024

interface FileStorage {
    fun writeBytes(fileName: String, bytes: ByteArray)
    fun readBytes(fileName: String): ByteArray
    fun deleteFile(fileName: String): Boolean
    fun fileExists(fileName: String): Boolean
}
```

On Android `writeBytes` writes `fileName` directly inside the app's private files directory. **A file name taken from an imported file must therefore be validated before it gets anywhere near this interface.**

**Writing a version safely.** `storage/SnapshotFileCleanup.kt:47-55` already has the right primitive, used today by "undo delete":

```kotlin
    /** Undo owns the same lock as deletion, including retries already past their row-existence check. */
    suspend fun restore(snap: Snap, bytes: ByteArray) = withContext(ioDispatcher) {
        mutex.withLock {
            withContext(NonCancellable) {
                files.writeBytes(snap.snapId, bytes)
                snaps.insertSnap(snap)
                pending.complete(snap.snapId)
            }
        }
    }
```

and `suspend fun drain()` (line 28), which deletes the files of versions whose rows were removed. Its constructor is `SnapshotFileCleanup(pending: PendingFileDeletionDao, snaps: SnapsDao, files: FileStorage, ioDispatcher = fileIoDispatcher)`; the database exposes `pendingFileDeletionDao()`.

**Address validation.** `extensions/StringExtensions.kt:48`: `fun String.isValidUrl(): Boolean` is true only for complete `http`/`https` addresses.

**Settings.** App settings are a `DataStore<Preferences>`. The names that are real user settings (all other names are runtime state and must never be exported):

| Preference name | Type | Declared at |
| --- | --- | --- |
| `background_sync` | Boolean | `background/MonitoringSchedule.kt:23` |
| `sync_interval` | Long (minutes; readers clamp to 15..10080) | `MonitoringSchedule.kt:24` |
| `require_wifi`, `require_charging`, `require_battery_not_low` | Boolean | `MonitoringSchedule.kt:25-27` |
| `require_device_idle` | Boolean | `ui/settings/SettingsViewModel.kt:69` |
| `notification_image_previews` | Boolean | `sync/NotificationPreferences.kt:7` |
| `theme_mode`, `app_palette`, `color_style` | String (enum names; readers fall back to a default for unknown text) | `ui/theme/ThemeViewModel.kt:28-30` |
| `comparison_method` | String (enum name; unknown text falls back) | `SettingsViewModel.kt:73` |
| `browser_delay_ms` | Long (milliseconds; **not** clamped by readers) | `SettingsViewModel.kt:74` |

**Dependency injection.** Services are declared as functions in `di/CoreDiModule.kt`, for example (line 88):

```kotlin
    @Single
    fun snapshotFileCleanup(database: ChangeDatabase, fileStorage: FileStorage): com.bernaferrari.changedetection.storage.SnapshotFileCleanup = ...
```

**Test exemplar (real database on the desktop JVM).** `shared/src/desktopTest/kotlin/com/bernaferrari/changedetection/sync/BaselineInitializationTest.kt:76-92`:

```kotlin
    private class Fixture : AutoCloseable {
        private val directory = Files.createTempDirectory("baseline-initialization").toFile()
        val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = directory.resolve("test.db").path), BundledSQLiteDriver())
        val files = DesktopFileStorage(directory.resolve("files").also { it.mkdirs() })
        private val client = HttpClient()
        val engine = SyncEngine(client, db, db.siteDao(), db.snapsDao(), files, ErrorHandler(), ChangeNotifier { _, id -> notices.add(id) })
        val site = Site("Page", "https://example.test", 0L, "", "page", 0 to 0).copy(captureMode = CaptureMode.SOURCE)
        fun capture(text: String) = SyncEngine.FetchResult.Success("text/plain", text.encodeToByteArray())
        override fun close() { client.close(); db.close(); directory.deleteRecursively() }
    }
```

`engine.processFetchResult(site, capture("text"))` saves a real version (row, file and `payloadHash`). Tests are written as `@Test fun camelCaseSentence() = runBlocking { Fixture().use { f -> ... } }` with `org.junit.Assert.*`.

**Repository conventions.** In `suspend` code, `catch (e: CancellationException) { throw e }` comes before `catch (e: Exception)`. Functions that protect a rule carry a one-line KDoc saying which. Formatting is enforced by Spotless.

## Commands you will need

| Purpose | Command | Expected |
| --- | --- | --- |
| Engine tests | `./gradlew :shared:desktopTest --tests '*BackupServiceTest' --tests '*BackupSettingsTest'` | `BUILD SUCCESSFUL` |
| Zip tests | `./gradlew :shared:testAndroidHostTest --tests '*ZipBackupTest'` | `BUILD SUCCESSFUL` |
| Format check / fix | `./gradlew :shared:spotlessCheck` / `./gradlew :shared:spotlessApply` | `BUILD SUCCESSFUL` |
| Full gate | `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest` | `BUILD SUCCESSFUL` |
| Other platforms compile | `./gradlew :webDemo:wasmJsBrowserDistribution` and (macOS) `./gradlew :shared:compileKotlinIosSimulatorArm64` | `BUILD SUCCESSFUL` |
| Android release build | `./gradlew :androidApp:lintDebug :androidApp:assembleRelease` | `BUILD SUCCESSFUL` |

The build needs JDK 21 and Android SDK platform 37.2.

## Before you start: the state of the build

Checked on 2026-11-09 at commit `a9f8f8ba`. Read this before the steps; it overrides any line below that expects otherwise.

1. **Plan 000 must be done first.** Before you change anything, run `./gradlew :shared:spotlessCheck`. If it fails, Plan 000 (`plans/000-make-the-gates-green.md`) has not landed: STOP and report that. Do not run `spotlessApply` to get past it; on an unprepared tree it rewrites about 36 unrelated files. Once Plan 000 has landed, the test and format commands in this plan behave as described, and `spotlessApply` only touches what you changed.
2. **Two commands fail for reasons no plan fixes.** `./gradlew :shared:compileKotlinIosSimulatorArm64` stops with five errors, all in `shared/src/iosMain/kotlin/com/bernaferrari/changedetection/browser/IosPageCapture.kt` (the first is `Unresolved reference 'kmpwright'`; a library was built with a newer Kotlin than this project uses). `./gradlew :webDemo:wasmJsBrowserDistribution` compiles the Kotlin and then stops at `:kotlinWasmStoreYarnLock` with `Lock file was changed`. Wherever this plan expects one of them to print `BUILD SUCCESSFUL`, the bar is instead: for iOS, **those same five errors and no others**; for the web demo, run `./gradlew :webDemo:compileKotlinWasmJs` and expect `BUILD SUCCESSFUL`. Never run `kotlinWasmUpgradeYarnLock`. If either command has since been repaired and passes, that is fine.
3. **Line numbers may have moved.** Two changes landed after this plan was written. Plan 000 re-formats about 36 files (layout only). The "failure experience" change (`plans/failure-experience-changes.md`) added code to `SyncEngine.kt`, `SitesDao.kt`, `Site.kt`, `CheckFailure.kt`, `DiagnosticTrace.kt`, `PageDetailScreen.kt`, `MainViewModel.kt`, `LegacyMigration.kt`, `DatabaseFactory.kt`, `AppContent.kt`, `AndroidChangeNotifier.kt`, the four `PlatformDiModule` files and the string files. Find code by the text this plan quotes, not by line number. A quoted block that differs only in line breaks or indentation is the same code. The drift check at the top of this plan will list those files as changed; that alone is not a reason to stop. Stop only if the quoted code is gone or does something different.

## Scope

**In scope (all new unless marked):**

- `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/backup/BackupFormat.kt`
- `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/backup/BackupSettings.kt`
- `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/backup/BackupService.kt`
- `shared/src/androidMain/kotlin/com/bernaferrari/changedetection/backup/ZipBackup.kt`
- `shared/src/desktopTest/kotlin/com/bernaferrari/changedetection/backup/BackupServiceTest.kt`
- `shared/src/desktopTest/kotlin/com/bernaferrari/changedetection/backup/BackupSettingsTest.kt`
- `shared/src/androidUnitTest/kotlin/com/bernaferrari/changedetection/backup/ZipBackupTest.kt`
- `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/di/CoreDiModule.kt` (existing; add one function)
- `plans/README.md` (status column only)

**Out of scope:**

- Any user interface, strings, file pickers. That is Plan 016.
- Changing `Site`, `Snap`, any DAO, `FileStorage` or `SnapshotFileCleanup`.
- Encryption, cloud destinations, scheduled backups.
- Cookies, sign-ins, diagnostics, runtime preference names (`monitoring_batch_*`, `quick_monitor_*`).
- Merging history into a page that already exists on the device.
- Desktop and iOS zip support (their builds are for development; the engine itself is common code and works there once someone adds a zip reader).

## Git workflow

- Branch from `main`: `git switch -c advisor/015-backup-core`
- One commit per step is ideal. Commit subjects are plain sentences, for example `Pages, settings and saved versions can be written to and read from a backup`.
- Do not push, merge, or open a pull request.

## Steps

### Step 1: The format and the two small interfaces

Create `backup/BackupFormat.kt`:

```kotlin
package com.bernaferrari.changedetection.backup

import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
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
    val format: String = BACKUP_FORMAT,
    val version: Int = BACKUP_VERSION,
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

data class ImportSummary(val pagesAdded: Int, val pagesAlreadyHere: Int, val pagesRejected: Int, val versionsAdded: Int, val settingsRestored: Boolean)
```

**Verify:** `./gradlew :shared:testAndroidHostTest` is `BUILD SUCCESSFUL` (it compiles).

### Step 2: Settings allow-list, test first

Create `shared/src/desktopTest/kotlin/com/bernaferrari/changedetection/backup/BackupSettingsTest.kt`:

```kotlin
package com.bernaferrari.changedetection.backup

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class BackupSettingsTest {
    @Test fun onlyUserSettingsAreExported() {
        val exported = BackupSettings.read(
            preferencesOf(
                booleanPreferencesKey("background_sync") to false,
                longPreferencesKey("sync_interval") to 120L,
                stringPreferencesKey("theme_mode") to "DARK",
                booleanPreferencesKey("quick_monitor_active") to true,
                stringPreferencesKey("monitoring_batch_cursor") to "abc",
            ),
        )
        assertEquals(mapOf("background_sync" to "false", "sync_interval" to "120", "theme_mode" to "DARK"), exported)
    }

    @Test fun importedSettingsAreWrittenWithTheirRealTypes() {
        val target = mutablePreferencesOf()
        BackupSettings.apply(mapOf("background_sync" to "false", "sync_interval" to "120", "theme_mode" to "DARK", "browser_delay_ms" to "8000"), target)
        assertEquals(false, target[booleanPreferencesKey("background_sync")])
        assertEquals(120L, target[longPreferencesKey("sync_interval")])
        assertEquals("DARK", target[stringPreferencesKey("theme_mode")])
        assertEquals(8000L, target[longPreferencesKey("browser_delay_ms")])
    }

    @Test fun unknownNamesAndBadValuesAreIgnored() {
        val target = mutablePreferencesOf(longPreferencesKey("sync_interval") to 60L)
        BackupSettings.apply(
            mapOf(
                "quick_monitor_active" to "true",
                "made_up" to "1",
                "background_sync" to "yes",
                "sync_interval" to "soon",
                "browser_delay_ms" to "99999999",
                "theme_mode" to "x".repeat(200),
            ),
            target,
        )
        assertNull(target[booleanPreferencesKey("quick_monitor_active")])
        assertNull(target[booleanPreferencesKey("background_sync")])
        assertEquals("a bad value must leave the current one alone", 60L, target[longPreferencesKey("sync_interval")])
        assertNull(target[longPreferencesKey("browser_delay_ms")])
        assertNull(target[stringPreferencesKey("theme_mode")])
        assertFalse(target.asMap().keys.any { it.name == "made_up" })
    }
}
```

**Verify:** `./gradlew :shared:desktopTest --tests '*BackupSettingsTest'` **fails to compile** (unresolved `BackupSettings`). Expected.

Create `backup/BackupSettings.kt`:

```kotlin
package com.bernaferrari.changedetection.backup

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * The app settings a backup carries. Anything not listed here (running Turbo sessions, batch cursors) is runtime
 * state and must never travel between devices; an imported file can only set these names, with checked values.
 */
internal object BackupSettings {
    private val booleans = listOf("background_sync", "require_wifi", "require_charging", "require_battery_not_low", "require_device_idle", "notification_image_previews")

    /** Name to the range an imported value must fall in. */
    private val longs = mapOf("sync_interval" to 15L..10_080L, "browser_delay_ms" to 0L..60_000L)
    private val strings = listOf("theme_mode", "app_palette", "color_style", "comparison_method")
    private const val MAX_TEXT = 64

    fun read(preferences: Preferences): Map<String, String> = buildMap {
        booleans.forEach { name -> preferences[booleanPreferencesKey(name)]?.let { put(name, it.toString()) } }
        longs.keys.forEach { name -> preferences[longPreferencesKey(name)]?.let { put(name, it.toString()) } }
        strings.forEach { name -> preferences[stringPreferencesKey(name)]?.let { put(name, it) } }
    }

    fun apply(values: Map<String, String>, target: MutablePreferences) {
        booleans.forEach { name -> values[name]?.toBooleanStrictOrNull()?.let { target[booleanPreferencesKey(name)] = it } }
        longs.forEach { (name, range) -> values[name]?.toLongOrNull()?.takeIf { it in range }?.let { target[longPreferencesKey(name)] = it } }
        strings.forEach { name -> values[name]?.takeIf { it.isNotBlank() && it.length <= MAX_TEXT }?.let { target[stringPreferencesKey(name)] = it } }
    }
}
```

Before moving on, confirm the allow-list against the code: `rg -n "PreferencesKey\(\"" shared/src/commonMain shared/src/androidMain`. Every name in the three lists above must appear in that output with the matching type. If a listed name is missing or has another type, fix the list and say so in your report. Do not add names that start with `monitoring_batch` or `quick_monitor`.

**Verify:** `./gradlew :shared:desktopTest --tests '*BackupSettingsTest'` passes with three tests.

### Step 3: Export, test first

Create `shared/src/desktopTest/kotlin/com/bernaferrari/changedetection/backup/BackupServiceTest.kt` with the fixture and the first tests. Later steps add more tests to this file.

```kotlin
package com.bernaferrari.changedetection.backup

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.bernaferrari.changedetection.error.ErrorHandler
import com.bernaferrari.changedetection.repo.CaptureMode
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.local.ChangeDatabase
import com.bernaferrari.changedetection.repo.source.local.getRoomDatabase
import com.bernaferrari.changedetection.storage.SnapshotFileCleanup
import com.bernaferrari.changedetection.sync.SyncEngine
import com.bernaferrari.changedetection.util.DesktopFileStorage
import io.ktor.client.HttpClient
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test

class BackupServiceTest {
    /** A backup held in memory, so the engine is tested without zip files. */
    private class MemoryArchive(val entries: MutableList<Pair<String, ByteArray>> = mutableListOf()) : BackupSink {
        override fun write(name: String, bytes: ByteArray) {
            entries += name to bytes
        }

        fun source() = object : BackupSource {
            private var index = -1
            override fun nextName(): String? = entries.getOrNull(++index)?.first
            override fun readCurrent(maxBytes: Int): ByteArray? = entries[index].second.takeIf { it.size <= maxBytes }
        }

        fun manifest(): BackupManifest = backupJson.decodeFromString(entries.first().second.decodeToString())
    }

    private class Device(name: String) : AutoCloseable {
        private val directory = Files.createTempDirectory("backup-$name").toFile()
        val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = directory.resolve("test.db").path), BundledSQLiteDriver())
        val files = DesktopFileStorage(directory.resolve("files").also { it.mkdirs() })
        private val client = HttpClient()
        val engine = SyncEngine(client, db, db.siteDao(), db.snapsDao(), files, ErrorHandler())
        var settings = mapOf("sync_interval" to "120", "theme_mode" to "DARK")
        val service = BackupService(
            db,
            db.siteDao(),
            db.snapsDao(),
            files,
            SnapshotFileCleanup(db.pendingFileDeletionDao(), db.snapsDao(), files),
            readSettings = { settings },
            writeSettings = { settings = it },
            now = { 1_794_268_800_000 },
        )

        suspend fun watch(id: String, url: String, vararg versions: String): Site {
            val site = Site("Page $id", url, 0L, "", id, 0 to 0).copy(captureMode = CaptureMode.SOURCE)
            db.siteDao().insertSite(site)
            versions.forEach { text -> assertTrue(engine.processFetchResult(requireNotNull(db.siteDao().getSiteById(id)), SyncEngine.FetchResult.Success("text/plain", text.encodeToByteArray())) is com.bernaferrari.changedetection.sync.SyncResult.Success) }
            return requireNotNull(db.siteDao().getSiteById(id))
        }

        suspend fun versions(id: String): List<Snap> = db.snapsDao().getAllSnapsForSiteId(id)

        override fun close() {
            client.close()
            db.close()
            directory.deleteRecursively()
        }
    }

    @Test fun aPagesBackupHoldsPagesAndSettingsButNoVersions() = runBlocking {
        Device("a").use { phone ->
            phone.watch("one", "https://example.test/one", "First", "Second")

            val manifest: BackupManifest = backupJson.decodeFromString(phone.service.exportPages("3.2").decodeToString())

            assertEquals(BACKUP_FORMAT, manifest.format)
            assertEquals(1, manifest.version)
            assertEquals("3.2", manifest.appVersion)
            assertEquals(1_794_268_800_000, manifest.exportedAt)
            assertEquals(listOf("https://example.test/one"), manifest.sites.map { it.url })
            assertTrue(manifest.snaps.isEmpty())
            assertEquals(phone.settings, manifest.settings)
        }
    }

    @Test fun aFullBackupStartsWithTheManifestAndCarriesEveryVersionUnchanged() = runBlocking {
        Device("a").use { phone ->
            phone.watch("one", "https://example.test/one", "First", "Second")
            val archive = MemoryArchive()

            val summary = phone.service.exportEverything(archive, "3.2")

            assertEquals(BackupSummary(pages = 1, versions = 2), summary)
            assertEquals(MANIFEST_ENTRY, archive.entries.first().first)
            val stored = phone.versions("one")
            assertEquals(stored.map { it.snapId }.toSet(), archive.manifest().snaps.map { it.snapId }.toSet())
            stored.forEach { snap ->
                val entry = archive.entries.single { it.first == SNAPSHOT_PREFIX + snap.snapId }
                assertArrayEquals(phone.files.readBytes(snap.snapId), entry.second)
            }
        }
    }

    @Test fun aVersionWhoseFileIsMissingIsLeftOutOfAFullBackupWithoutFailingIt() = runBlocking {
        Device("a").use { phone ->
            phone.watch("one", "https://example.test/one", "First", "Second")
            val lost = phone.versions("one").first()
            phone.files.deleteFile(lost.snapId)
            val archive = MemoryArchive()

            val summary = phone.service.exportEverything(archive, "3.2")

            assertEquals(1, summary.versions)
            assertFalse(archive.entries.any { it.first == SNAPSHOT_PREFIX + lost.snapId })
        }
    }
}
```

`SyncResult` and the `SyncEngine` constructor are used exactly as in `BaselineInitializationTest`. If `SyncEngine`'s constructor has gained required parameters since this plan was written, copy the call from that test.

**Verify:** `./gradlew :shared:desktopTest --tests '*BackupServiceTest'` **fails to compile** (unresolved `BackupService`). Expected.

Create `backup/BackupService.kt` with the export half:

```kotlin
package com.bernaferrari.changedetection.backup

import androidx.room3.withWriteTransaction
import com.bernaferrari.changedetection.extensions.isValidUrl
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.local.ChangeDatabase
import com.bernaferrari.changedetection.repo.source.local.SitesDao
import com.bernaferrari.changedetection.repo.source.local.SnapsDao
import com.bernaferrari.changedetection.storage.SnapshotFileCleanup
import com.bernaferrari.changedetection.util.ContentHasher
import com.bernaferrari.changedetection.util.FileStorage
import com.bernaferrari.changedetection.util.MAX_SNAPSHOT_BYTES
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
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
    suspend fun exportPages(appVersion: String): ByteArray = withContext(Dispatchers.Default) {
        encode(manifest(appVersion, snaps = emptyList()))
    }

    /** The complete backup: the manifest first, then every saved version whose file is intact. */
    suspend fun exportEverything(sink: BackupSink, appVersion: String): BackupSummary = withContext(Dispatchers.Default) {
        val snaps = sitesDao.getSitesList().flatMap { snapsDao.getAllSnapsForSiteId(it.id) }
        val manifest = manifest(appVersion, snaps)
        sink.write(MANIFEST_ENTRY, encode(manifest))
        var written = 0
        for (snap in snaps) {
            currentCoroutineContext().ensureActive()
            // A version that cannot be read is left out; import skips versions without content.
            val bytes = runCatching { files.readBytes(snap.snapId) }.getOrNull()?.takeIf { it.size == snap.contentSize } ?: continue
            sink.write(SNAPSHOT_PREFIX + snap.snapId, bytes)
            written++
        }
        BackupSummary(pages = manifest.sites.size, versions = written)
    }

    private suspend fun manifest(appVersion: String, snaps: List<Snap>) = BackupManifest(exportedAt = now(), appVersion = appVersion, settings = readSettings(), sites = sitesDao.getSitesList(), snaps = snaps)

    private fun encode(manifest: BackupManifest): ByteArray = backupJson.encodeToString(manifest).encodeToByteArray()
}
```

(`runCatching` is acceptable around `files.readBytes` because that function does not suspend. Some imports above are used only after Step 4; if Spotless or the compiler objects to unused imports now, add them in Step 4 instead.)

**Verify:** `./gradlew :shared:desktopTest --tests '*BackupServiceTest'` passes with three tests.

### Step 4: Import, test first

Add these tests to `BackupServiceTest`:

```kotlin
    @Test fun aFullBackupRestoresPagesVersionsAndTheirContentOnAnotherDevice() = runBlocking {
        Device("old").use { old ->
            Device("new").use { new ->
                val page = old.watch("one", "https://example.test/one", "First", "Second")
                val archive = MemoryArchive().also { old.service.exportEverything(it, "3.2") }

                val summary = new.service.import(archive.source(), restoreSettings = false)

                assertEquals(ImportSummary(pagesAdded = 1, pagesAlreadyHere = 0, pagesRejected = 0, versionsAdded = 2, settingsRestored = false), summary)
                val restored = requireNotNull(new.db.siteDao().getSiteById("one"))
                assertEquals(page.url, restored.url)
                assertEquals(page.title, restored.title)
                assertEquals(page.captureMode, restored.captureMode)
                assertEquals(old.versions("one").toSet(), new.versions("one").toSet())
                old.versions("one").forEach { assertArrayEquals(old.files.readBytes(it.snapId), new.files.readBytes(it.snapId)) }
            }
        }
    }

    @Test fun aPagesBackupAddsPagesThatStartFresh() = runBlocking {
        Device("old").use { old ->
            Device("new").use { new ->
                old.watch("one", "https://example.test/one", "First")
                // A page that was failing when it was exported must not arrive marked as failing.
                old.db.siteDao().insertSite(requireNotNull(old.db.siteDao().getSiteById("one")).copy(isSuccessful = false, lastFailureCode = "SERVER", failureCount = 4, failingSince = 1L, failureAlerted = true))

                val summary = new.service.import(ManifestOnlySource(old.service.exportPages("3.2")), restoreSettings = false)

                assertEquals(1, summary.pagesAdded)
                assertEquals(0, summary.versionsAdded)
                val restored = requireNotNull(new.db.siteDao().getSiteById("one"))
                assertTrue(restored.isSuccessful)
                assertNull(restored.lastFailureCode)
                assertEquals(0, restored.failureCount)
                assertNull(restored.failingSince)
                assertFalse(restored.failureAlerted)
                assertTrue(new.versions("one").isEmpty())
            }
        }
    }

    @Test fun importingTheSameBackupTwiceChangesNothing() = runBlocking {
        Device("old").use { old ->
            Device("new").use { new ->
                old.watch("one", "https://example.test/one", "First", "Second")
                val archive = MemoryArchive().also { old.service.exportEverything(it, "3.2") }
                new.service.import(archive.source(), restoreSettings = false)

                val again = new.service.import(archive.source(), restoreSettings = false)

                assertEquals(ImportSummary(pagesAdded = 0, pagesAlreadyHere = 1, pagesRejected = 0, versionsAdded = 0, settingsRestored = false), again)
                assertEquals(2, new.versions("one").size)
            }
        }
    }

    @Test fun aPageAlreadyOnTheDeviceKeepsItsOwnOptionsAndHistory() = runBlocking {
        Device("old").use { old ->
            old.watch("one", "https://example.test/one", "First")
            val archive = MemoryArchive().also { old.service.exportEverything(it, "3.2") }
            old.watch("one", "https://example.test/one", "Second", "Third")
            val before = old.versions("one")

            val summary = old.service.import(archive.source(), restoreSettings = false)

            assertEquals(1, summary.pagesAlreadyHere)
            assertEquals(before, old.versions("one"))
        }
    }

    @Test fun settingsAreRestoredOnlyWhenAsked() = runBlocking {
        Device("old").use { old ->
            Device("new").use { new ->
                new.settings = mapOf("theme_mode" to "LIGHT")
                val file = old.service.exportPages("3.2")

                assertFalse(new.service.import(ManifestOnlySource(file), restoreSettings = false).settingsRestored)
                assertEquals(mapOf("theme_mode" to "LIGHT"), new.settings)

                assertTrue(new.service.import(ManifestOnlySource(file), restoreSettings = true).settingsRestored)
                assertEquals(old.settings, new.settings)
            }
        }
    }

    private fun page(id: String, url: String) = Site("Page", url, 0L, "", id, 0 to 0)

    private fun file(manifest: BackupManifest, vararg entries: Pair<String, ByteArray>) = MemoryArchive(mutableListOf(MANIFEST_ENTRY to backupJson.encodeToString(manifest).encodeToByteArray(), *entries))

    @Test fun onlyWebAddressesAreImported() = runBlocking {
        Device("new").use { new ->
            val hostile = file(BackupManifest(sites = listOf(page("a", "file:///data/data/private"), page("b", "javascript:alert(1)"), page("c", "content://settings/secure"), page("d", "https://example.test/ok"))))

            val summary = new.service.import(hostile.source(), restoreSettings = false)

            assertEquals(1, summary.pagesAdded)
            assertEquals(3, summary.pagesRejected)
            assertEquals(listOf("d"), new.db.siteDao().getSitesList().map { it.id })
        }
    }

    @Test fun aVersionNameThatIsNotAnIdRefusesTheFileAndLeavesNothingBehind() = runBlocking {
        Device("new").use { new ->
            val body = "x".encodeToByteArray()
            val hostile = file(
                BackupManifest(sites = listOf(page("a", "https://example.test/a")), snaps = listOf(Snap("../escape", "a", 1L, "text/plain", "UTF-8", body.size))),
                SNAPSHOT_PREFIX + "../escape" to body,
            )

            try {
                new.service.import(hostile.source(), restoreSettings = false)
                fail("a file with an undeclared or unsafe entry must be refused")
            } catch (expected: InvalidBackupException) {
            }
            assertTrue("the page added before the bad entry must be removed again", new.db.siteDao().getSitesList().isEmpty())
            assertFalse(new.files.fileExists("escape"))
        }
    }

    @Test fun anEntryLargerThanItDeclaresRefusesTheFile() = runBlocking {
        Device("new").use { new ->
            val id = "11111111-2222-3333-4444-555555555555"
            val hostile = file(
                BackupManifest(sites = listOf(page("a", "https://example.test/a")), snaps = listOf(Snap(id, "a", 1L, "text/plain", "UTF-8", 4))),
                SNAPSHOT_PREFIX + id to ByteArray(4096),
            )

            try {
                new.service.import(hostile.source(), restoreSettings = false)
                fail("an entry that inflates past its declared size must be refused")
            } catch (expected: InvalidBackupException) {
            }
            assertTrue(new.db.siteDao().getSitesList().isEmpty())
            assertFalse(new.files.fileExists(id))
        }
    }

    @Test fun aDamagedVersionIsSkippedAndTheRestIsKept() = runBlocking {
        Device("old").use { old ->
            Device("new").use { new ->
                old.watch("one", "https://example.test/one", "First", "Second")
                val archive = MemoryArchive().also { old.service.exportEverything(it, "3.2") }
                val damaged = archive.entries.indexOfFirst { it.first.startsWith(SNAPSHOT_PREFIX) }
                val (name, bytes) = archive.entries[damaged]
                archive.entries[damaged] = name to ByteArray(bytes.size) { '?'.code.toByte() }

                val summary = new.service.import(archive.source(), restoreSettings = false)

                assertEquals(1, summary.pagesAdded)
                assertEquals(1, summary.versionsAdded)
                assertFalse(new.files.fileExists(name.removePrefix(SNAPSHOT_PREFIX)))
            }
        }
    }

    @Test fun aFileFromANewerAppOrOfAnotherKindIsRefusedUntouched() = runBlocking {
        Device("new").use { new ->
            val pages = listOf(page("a", "https://example.test/a"))
            for ((hostile, expected) in listOf(
                file(BackupManifest(version = BACKUP_VERSION + 1, sites = pages)) to BackupTooNewException::class,
                file(BackupManifest(format = "something-else", sites = pages)) to InvalidBackupException::class,
                MemoryArchive(mutableListOf(MANIFEST_ENTRY to "not json".encodeToByteArray())) to InvalidBackupException::class,
                MemoryArchive(mutableListOf("other.txt" to "hello".encodeToByteArray())) to InvalidBackupException::class,
                MemoryArchive() to InvalidBackupException::class,
            )) {
                try {
                    new.service.import(hostile.source(), restoreSettings = true)
                    fail("must be refused")
                } catch (error: Exception) {
                    assertEquals(expected, error::class)
                }
            }
            assertTrue(new.db.siteDao().getSitesList().isEmpty())
            assertEquals("settings must not change when a file is refused", mapOf("sync_interval" to "120", "theme_mode" to "DARK"), new.settings)
        }
    }

    @Test fun aVersion1FileWrittenByHandStillReads() = runBlocking {
        Device("new").use { new ->
            val handwritten = """
                {"format":"change-detection-backup","version":1,"exportedAt":1,"appVersion":"3.2","settings":{},
                 "sites":[{"title":"Docs","url":"https://example.test/docs","timestamp":0,"id":"hand","isSuccessful":true,"isBrowser":false,
                           "threshold":0,"isRead":true,"isSyncEnabled":true,"isNotificationEnabled":true,"notes":"",
                           "colors":{"first":1,"second":2},"constraints":{"wifi":false,"charging":false,"batteryNotLow":false,"deviceIdle":false},
                           "aFieldFromTheFuture":123}],
                 "snaps":[]}
            """.trimIndent()

            val summary = new.service.import(ManifestOnlySource(handwritten.encodeToByteArray()), restoreSettings = false)

            assertEquals(1, summary.pagesAdded)
            assertEquals("https://example.test/docs", requireNotNull(new.db.siteDao().getSiteById("hand")).url)
        }
    }
```

**Verify:** `./gradlew :shared:desktopTest --tests '*BackupServiceTest'` **fails to compile** (unresolved `import`). Expected.

Add the import half to `BackupService` (inside the class, after `exportEverything`):

```kotlin
    /**
     * Adds the pages of a backup that are not on this device yet, with their versions when the backup has them.
     * Nothing already here is changed. A file that is not a well-formed backup is refused and leaves no trace.
     */
    suspend fun import(source: BackupSource, restoreSettings: Boolean): ImportSummary = withContext(Dispatchers.Default) {
        val manifest = readManifest(source)
        val declared = manifest.sites.distinctBy { it.id }
        // Only complete web addresses: an imported page is later opened in the in-app browser.
        val usable = declared.filter { it.id.isNotBlank() && it.id.length <= MAX_ID_LENGTH && it.url.isValidUrl() }
        val added = usable.filter { sitesDao.getSiteById(it.id) == null }
        val addedIds = added.mapTo(HashSet()) { it.id }
        // A version's id becomes a file name, so only ids shaped like this app's own are ever considered.
        val versions = manifest.snaps.filter { SNAP_ID.matches(it.snapId) && it.contentSize in 1..MAX_SNAPSHOT_BYTES }.associateBy { it.snapId }

        // Files queued for deletion must be gone before the same ids can be written again.
        cleanup.drain()
        database.withWriteTransaction {
            // A page arrives ready for its next check, not carrying the old device's last error.
            added.forEach { sitesDao.insertSite(it.copy(isSuccessful = true, lastFailureCode = null, failureCount = 0, failingSince = null, failureAlerted = false)) }
        }
        var restored = 0
        try {
            while (true) {
                currentCoroutineContext().ensureActive()
                val name = source.nextName() ?: break
                // Every entry must be a version the manifest declared; anything else is not our file.
                val snap = name.takeIf { it.startsWith(SNAPSHOT_PREFIX) }?.let { versions[it.removePrefix(SNAPSHOT_PREFIX)] } ?: throw InvalidBackupException("Unexpected entry")
                // Reading is capped at the declared size, so a small file cannot expand without limit.
                val bytes = source.readCurrent(snap.contentSize) ?: throw InvalidBackupException("Entry larger than declared")
                val intact = bytes.size == snap.contentSize && (snap.payloadHash.isEmpty() || ContentHasher.hashContent(bytes) == snap.payloadHash)
                if (snap.siteId !in addedIds || !intact) continue
                if (snapsDao.getSnapById(snap.snapId) != null || files.fileExists(snap.snapId)) continue
                cleanup.restore(snap, bytes)
                restored++
            }
        } catch (error: Throwable) {
            // All or nothing: the pages added by this import go again, and their files with them.
            withContext(NonCancellable) {
                added.forEach { sitesDao.deleteSiteById(it.id) }
                cleanup.drain()
            }
            throw error
        }
        if (restoreSettings) writeSettings(manifest.settings)
        ImportSummary(pagesAdded = added.size, pagesAlreadyHere = usable.size - added.size, pagesRejected = declared.size - usable.size, versionsAdded = restored, settingsRestored = restoreSettings)
    }

    private fun readManifest(source: BackupSource): BackupManifest {
        if (source.nextName() != MANIFEST_ENTRY) throw InvalidBackupException("No manifest")
        val bytes = source.readCurrent(MAX_MANIFEST_BYTES) ?: throw InvalidBackupException("Manifest too large")
        val manifest = try {
            backupJson.decodeFromString<BackupManifest>(bytes.decodeToString())
        } catch (error: SerializationException) {
            throw InvalidBackupException("Manifest unreadable")
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
```

Notes on this code:

- `catch (error: Throwable)` followed by an unconditional `throw error` is the one place a broad catch is right: it also runs when the import is cancelled, and it always rethrows.
- `SerializationException` is a subclass of `IllegalArgumentException` in kotlinx.serialization; if the compiler warns that the second `catch` is unreachable or the first is redundant, keep only `catch (error: IllegalArgumentException)`.
- Settings are written last, only after every entry was accepted.

**Verify:** `./gradlew :shared:desktopTest --tests '*BackupServiceTest'`
Expected: `BUILD SUCCESSFUL`, fourteen tests pass.

If `aVersion1FileWrittenByHandStillReads` fails because a field name in the handwritten JSON does not match how `Site` is really written, print one real manifest (`println(phone.service.exportPages("3.2").decodeToString())` in a scratch test), correct the handwritten document to use the real names, and remove the scratch code. That handwritten document is the frozen definition of format version 1: once this plan is done, it must never be edited again.

### Step 5: Zip reading and writing for Android, test first

Create `shared/src/androidUnitTest/kotlin/com/bernaferrari/changedetection/backup/ZipBackupTest.kt`:

```kotlin
package com.bernaferrari.changedetection.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ZipBackupTest {
    private fun zipOf(vararg entries: Pair<String, ByteArray>): ByteArray = ByteArrayOutputStream().also { out ->
        ZipBackupSink(out).use { sink -> entries.forEach { (name, bytes) -> sink.write(name, bytes) } }
    }.toByteArray()

    @Test fun `entries come back in order with their exact bytes`() {
        val picture = ByteArray(70_000) { (it % 251).toByte() }
        val source = ZipBackupSource(ByteArrayInputStream(zipOf("manifest.json" to "{}".toByteArray(), "snapshots/a" to picture, "snapshots/b" to ByteArray(0))))

        assertEquals("manifest.json", source.nextName())
        assertArrayEquals("{}".toByteArray(), source.readCurrent(10))
        assertEquals("snapshots/a", source.nextName())
        assertArrayEquals(picture, source.readCurrent(picture.size))
        assertEquals("snapshots/b", source.nextName())
        assertArrayEquals(ByteArray(0), source.readCurrent(0))
        assertNull(source.nextName())
    }

    @Test fun `an entry larger than the limit is refused instead of read`() {
        // 4 MB of zeros compresses to a few kilobytes: the classic way to exhaust memory.
        val source = ZipBackupSource(ByteArrayInputStream(zipOf("manifest.json" to ByteArray(4 * 1024 * 1024))))

        assertEquals("manifest.json", source.nextName())
        assertNull(source.readCurrent(1024))
    }

    @Test fun `an entry can be skipped without being read`() {
        val source = ZipBackupSource(ByteArrayInputStream(zipOf("a" to "one".toByteArray(), "b" to "two".toByteArray())))

        assertEquals("a", source.nextName())
        assertEquals("b", source.nextName())
        assertArrayEquals("two".toByteArray(), source.readCurrent(3))
    }

    @Test fun `a file that is not a zip has no entries`() {
        assertNull(ZipBackupSource(ByteArrayInputStream("{\"format\":\"x\"}".toByteArray())).nextName())
    }

    @Test fun `zip files are recognised by their first bytes`() {
        assertTrue(looksLikeZip(zipOf("a" to "one".toByteArray())))
        assertFalse(looksLikeZip("{\"format\":\"change-detection-backup\"}".toByteArray()))
        assertFalse(looksLikeZip(ByteArray(0)))
    }
}
```

**Verify:** `./gradlew :shared:testAndroidHostTest --tests '*ZipBackupTest'` **fails to compile**. Expected.

Create `shared/src/androidMain/kotlin/com/bernaferrari/changedetection/backup/ZipBackup.kt`:

```kotlin
package com.bernaferrari.changedetection.backup

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** A zip file starts with "PK"; a pages-and-settings backup starts with "{". */
internal fun looksLikeZip(firstBytes: ByteArray): Boolean = firstBytes.size >= 2 && firstBytes[0] == 'P'.code.toByte() && firstBytes[1] == 'K'.code.toByte()

/** Screenshots are already compressed, so speed matters more than ratio. */
internal class ZipBackupSink(output: OutputStream) : BackupSink, Closeable {
    private val zip = ZipOutputStream(BufferedOutputStream(output)).apply { setLevel(Deflater.BEST_SPEED) }

    override fun write(name: String, bytes: ByteArray) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(bytes)
        zip.closeEntry()
    }

    override fun close() = zip.close()
}

/** Entry names are only ever compared, never used as paths, and no entry is read past the caller's limit. */
internal class ZipBackupSource(input: InputStream) : BackupSource, Closeable {
    private val zip = ZipInputStream(BufferedInputStream(input))

    override fun nextName(): String? = zip.nextEntry?.name

    override fun readCurrent(maxBytes: Int): ByteArray? {
        val output = ByteArrayOutputStream(minOf(maxBytes, 64 * 1024))
        val buffer = ByteArray(16 * 1024)
        while (true) {
            val count = zip.read(buffer)
            if (count < 0) return output.toByteArray()
            if (output.size() + count > maxBytes) return null
            output.write(buffer, 0, count)
        }
    }

    override fun close() = zip.close()
}
```

**Verify:** `./gradlew :shared:testAndroidHostTest --tests '*ZipBackupTest'` passes with five tests.

### Step 6: Make the service available to the app

In `di/CoreDiModule.kt`, add this function next to `snapshotFileCleanup` (inside the class), and add the three imports it needs at the top of the file (`androidx.datastore.core.DataStore`, `androidx.datastore.preferences.core.Preferences`, `androidx.datastore.preferences.core.edit`, `kotlinx.coroutines.flow.first`) unless they are already imported:

```kotlin
    @Single
    fun backupService(
        database: ChangeDatabase,
        sitesDao: SitesDao,
        snapsDao: SnapsDao,
        fileStorage: FileStorage,
        cleanup: com.bernaferrari.changedetection.storage.SnapshotFileCleanup,
        preferences: DataStore<Preferences>,
    ): com.bernaferrari.changedetection.backup.BackupService = com.bernaferrari.changedetection.backup.BackupService(
        database,
        sitesDao,
        snapsDao,
        fileStorage,
        cleanup,
        readSettings = { com.bernaferrari.changedetection.backup.BackupSettings.read(preferences.data.first()) },
        writeSettings = { values -> preferences.edit { com.bernaferrari.changedetection.backup.BackupSettings.apply(values, it) } },
    )
```

`DataStore<Preferences>` is already provided on every platform (other services in the app take it as a constructor parameter; check with `rg -n "DataStore<Preferences>" shared/src/*/kotlin/com/bernaferrari/changedetection/di`). If one platform's module does not provide it, STOP.

**Verify:** `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest`
Expected: `BUILD SUCCESSFUL`. Run `./gradlew :shared:spotlessApply` first if needed. The desktop suite starts the real dependency container in its journey tests, which proves the new service can be constructed.

**Verify:** `./gradlew :webDemo:wasmJsBrowserDistribution`, on macOS `./gradlew :shared:compileKotlinIosSimulatorArm64`, and `./gradlew :androidApp:lintDebug :androidApp:assembleRelease`.
Expected: `BUILD SUCCESSFUL` for each.

## Test plan

| Test class | Where | What it proves |
| --- | --- | --- |
| `BackupSettingsTest` (3) | desktop | Only allow-listed settings travel; bad or unknown values are ignored |
| `BackupServiceTest` (14) | desktop, real database and files | Both export kinds; restore on another device; pages start fresh; idempotent import; existing pages untouched; settings only when asked; non-web addresses rejected; unsafe names, oversized entries, wrong or newer files refused with nothing left behind; damaged versions skipped; the frozen version 1 document |
| `ZipBackupTest` (5) | Android host JVM | Zip round trip, size limit, skipping, non-zip input, zip detection |

## Done criteria

- [ ] `./gradlew :shared:desktopTest --tests '*BackupServiceTest' --tests '*BackupSettingsTest'` passes (17 tests).
- [ ] `./gradlew :shared:testAndroidHostTest --tests '*ZipBackupTest'` passes (5 tests).
- [ ] `rg -n "fun backupService" shared/src/commonMain` prints one line.
- [ ] `rg -n "writeBytes" shared/src/commonMain/kotlin/com/bernaferrari/changedetection/backup` prints nothing (the engine never writes files itself; it goes through `SnapshotFileCleanup.restore`).
- [ ] The full gate, the web demo build, and the Android lint and release build are `BUILD SUCCESSFUL`.
- [ ] `git status --short` lists only in-scope files.

## STOP conditions

- `SnapshotFileCleanup.restore` or `drain` no longer exists with the shape quoted above.
- `Site` or `Snap` is no longer `@Serializable`.
- A test in Step 4 that expects a refusal (`aVersionNameThatIsNotAnIdRefusesTheFileAndLeavesNothingBehind`, `anEntryLargerThanItDeclaresRefusesTheFile`, `onlyWebAddressesAreImported`) passes only after you weaken the validation. Do not weaken it; report.
- A platform's dependency module does not provide `DataStore<Preferences>`.
- You are tempted to let import update or replace a page that already exists. That is a deliberate non-goal.

## Maintenance notes

- **The format is a promise.** `aVersion1FileWrittenByHandStillReads` must keep passing forever. Adding an optional field to `Site`, `Snap` or the manifest is compatible. Renaming or removing a field, or changing what a value means, needs `BACKUP_VERSION = 2` and a reader that still accepts version 1.
- **New settings.** A new user setting does not travel in backups until its name is added to `BackupSettings`. Runtime state must never be added.
- **Import is additive by design.** If "replace this device with the backup" is ever wanted, build it as a separate, clearly labelled action, not as a change to `import`.
- **Size.** A full export reads one version at a time, so memory stays bounded, but the manifest holds every `Snap` row in memory. That is fine for tens of thousands of versions; revisit if histories grow far beyond that.
- **Not included, deliberately:** cookies and sign-ins (they are credentials), diagnostics, pending alerts.
- Reviewers of future changes here should look hardest at anything that turns text from a file into a file name, an address the app will open, or a preference name.
