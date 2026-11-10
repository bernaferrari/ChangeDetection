# Plan 005: A check that already saved its result must not be recorded as "timed out"

> **Executor instructions**
> You are implementing one self-contained change. You have not seen the audit that produced this plan; everything you need is in this file.
> 1. Run the drift check first: `git diff --stat a9f8f8ba..HEAD -- shared/src/commonMain/kotlin/com/bernaferrari/changedetection/sync/SyncEngine.kt shared/src/androidMain/kotlin/com/bernaferrari/changedetection/sync/AndroidCheckRunner.kt shared/src/androidMain/kotlin/com/bernaferrari/changedetection/SyncWorker.kt`. If it prints anything, compare the "Current state" excerpts with the real files. If they no longer match, STOP and report.
> 2. Follow the steps in order and run each **Verify** command.
> 3. Touch only the files listed under "In scope".
> 4. If a STOP condition applies, stop and report. Do not improvise.
> 5. When finished, set this plan's row in `plans/README.md` to `DONE`.

## Status

| Field | Value |
| --- | --- |
| Priority | P1 |
| Effort | S (1 to 2 hours) |
| Risk of the fix | Low. One new engine method, two call sites, one new test file |
| Depends on | Nothing. Plans 006, 009 and 010 also edit `SyncEngine.kt`; run this one first |
| Category | Bug (wrong status, needless retry) |
| Planned at | commit `a9f8f8ba`, 2026-11-09 |

## Why this matters

Each page check on Android runs inside a 90 second timeout. When a check finds a change, it saves the new version in a short database commit that deliberately cannot be cancelled, and then does follow-up work that can be cancelled (delivering the alert, pruning old versions).

If the 90 seconds run out during or just after the commit, the timeout handler runs next. It does not know a result was already saved, so it records "Page check timed out" on the page. The user then sees a page that shows both a new change and a failed status. In the background worker the failed result also turns into `Result.retry()`, so the whole check runs again for nothing.

The rule this plan adds: a timeout may only be recorded if the page's status row has not moved since the check began. If it has moved, a newer result exists and the timeout is stale.

## Current state

### The engine

`shared/src/commonMain/kotlin/com/bernaferrari/changedetection/sync/SyncEngine.kt`.

Lines 174-182:

```kotlin
    suspend fun processFetchResult(
        site: Site,
        result: FetchResult,
        threshold: Int = 0,
    ): SyncResult = withSiteLock(site.id) {
        withContext(Dispatchers.Default) {
            processFetchResultUnlocked(site, result, threshold)
        }
    }
```

Lines 195-205 (start of `processFetchResultUnlocked`): it re-reads the page and skips the result if the page's settings changed during the check. It does **not** compare the status timestamp.

```kotlin
    private suspend fun processFetchResultUnlocked(
        site: Site,
        result: FetchResult,
        threshold: Int = 0,
        firstVersionOnly: Boolean = false,
    ): SyncResult = withContext(Dispatchers.Default) {
        val currentSite = sitesDao.getSiteById(site.id)
            ?: return@withContext SyncResult.Skipped(site.id, "Page deleted during check")
        if (currentSite.url != site.url || currentSite.captureMode != site.captureMode || currentSite.threshold != site.threshold || currentSite.visualChangeThresholdBps != site.visualChangeThresholdBps || currentSite.capturePolicy() != site.capturePolicy() || !currentSite.isSyncEnabled) {
            return@withContext SyncResult.Skipped(site.id, "Page changed or paused during check")
        }
```

Lines 451-481 (inside `saveNewSnap`): the commit runs in `withContext(NonCancellable)` and calls `sitesDao.updateSyncMetadata(siteId = site.id, timestamp = checkedAt, successful = true, ...)`. After it (lines 514-533) the alert and maintenance steps rethrow `CancellationException`.

Every status write, success or failure, goes through `sitesDao.updateSyncMetadata`, which always sets `sites.timestamp` to the current time (`SitesDao.kt:187-209`). So `Site.timestamp` changes whenever any check result is recorded for that page.

`SyncResult` (lines 38-56) has three cases: `Success`, `Failed`, `Skipped(siteId, reason)`.

### The two timeout handlers

`shared/src/androidMain/kotlin/com/bernaferrari/changedetection/sync/AndroidCheckRunner.kt:30-55`:

```kotlin
    override suspend fun check(siteId: String): SyncResult = locks.withLock(siteId) {
        capacity.withPermit {
            val site = sites.getSiteById(siteId) ?: return@withPermit SyncResult.Skipped(siteId, "Page deleted")
            if (!site.isSyncEnabled) return@withPermit SyncResult.Skipped(siteId, "Page paused")
            recordDiagnostic(site.captureMode, site.textIncludeSelector.isNotBlank(), site.textExcludeSelector.isNotBlank(), DiagnosticTrigger.CHECK, site.id) {
                try {
                    withTimeout(90_000) {
                        // ... runs the check, which may commit a result ...
                    }
                } catch (timeout: TimeoutCancellationException) {
                    currentCoroutineContext().ensureActive()
                    engine.processFetchResult(site, SyncEngine.FetchResult.Error("Page check timed out", timeout), site.threshold)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    engine.processFetchResult(site, SyncEngine.FetchResult.Error("Page check failed", error), site.threshold)
                }
            }
        }
    }
```

Here `site` was read at the start of this check, which is what the new rule needs.

`shared/src/androidMain/kotlin/com/bernaferrari/changedetection/SyncWorker.kt:170-183`:

```kotlin
    /** Includes time queued behind manual/Turbo checks, not only time inside a browser. */
    private suspend fun boundedCheck(site: com.bernaferrari.changedetection.repo.Site, foreground: Boolean, completed: Int, total: Int): SyncResult = try {
        withTimeout(90_000) {
            checkAfterForeground(prepare = {
                if (foreground) setForeground(createForegroundInfo(pageName = site.title?.takeIf { it.isNotBlank() } ?: site.url, completed = completed, total = total))
            }, check = { checkRunner.check(site.id) }, denied = { error ->
                com.bernaferrari.changedetection.diagnostics.DiagnosticLog.appEvent(com.bernaferrari.changedetection.diagnostics.AppEventKind.BACKGROUND_FAILURE, error, stage = com.bernaferrari.changedetection.diagnostics.AppEventStage.WORKER_FOREGROUND)
                syncEngine.processFetchResult(site, SyncEngine.FetchResult.Error("Background check could not start", error), site.threshold)
            })
        }
    } catch (timeout: TimeoutCancellationException) {
        currentCoroutineContext().ensureActive()
        syncEngine.processFetchResult(site, SyncEngine.FetchResult.Error("Page check timed out", timeout), site.threshold)
    }
```

Here `site` comes from a list read at the start of the whole batch (`syncAllSites`, line 149), possibly minutes earlier. For the new rule it must be re-read just before the timeout window starts.

`SyncWorker.toWorkResult` (lines 185-189) maps `Success` and `Skipped` to `Result.success()` and a retryable `Failed` to `Result.retry()`.

### Test exemplar to copy

`shared/src/desktopTest/kotlin/com/bernaferrari/changedetection/sync/BaselineInitializationTest.kt` runs the real engine against a real Room database on the desktop JVM. Its fixture (lines 76-92):

```kotlin
    private class Fixture : AutoCloseable {
        private val directory = Files.createTempDirectory("baseline-initialization").toFile()
        val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = directory.resolve("test.db").path), BundledSQLiteDriver())
        val files = DesktopFileStorage(directory.resolve("files").also { it.mkdirs() })
        private val client = HttpClient()
        val notices = mutableListOf<String>()
        val engine = SyncEngine(client, db, db.siteDao(), db.snapsDao(), files, ErrorHandler(), ChangeNotifier { _, id -> notices.add(id) })
        val site = Site("Page", "https://example.test", 0L, "", "page", 0 to 0).copy(captureMode = CaptureMode.SOURCE)
        fun capture(text: String) = SyncEngine.FetchResult.Success("text/plain", text.encodeToByteArray())
        fun preview(text: String) = CapturePreview.Html(text, text.length, firstVersion = capture(text))
        override fun close() {
            client.close()
            db.close()
            directory.deleteRecursively()
        }
    }
```

Tests in that file are written as `@Test fun camelCaseSentence() = runBlocking { Fixture().use { fixture -> ... } }` with `org.junit.Assert.*`.

## Commands you will need

| Purpose | Command | Expected |
| --- | --- | --- |
| Run the new test class | `./gradlew :shared:desktopTest --tests '*TimeoutAfterSavedResultTest'` | `BUILD SUCCESSFUL` |
| Host tests (compiles Android code) | `./gradlew :shared:testAndroidHostTest` | `BUILD SUCCESSFUL` |
| Format check | `./gradlew :shared:spotlessCheck` | `BUILD SUCCESSFUL` |
| Auto-format | `./gradlew :shared:spotlessApply` | `BUILD SUCCESSFUL` |
| Full gate | `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest` | `BUILD SUCCESSFUL` |
| iOS still compiles (macOS only) | `./gradlew :shared:compileKotlinIosSimulatorArm64` | `BUILD SUCCESSFUL` |

The build needs JDK 21 and Android SDK platform 37.2.

## Before you start: the state of the build

Checked on 2026-11-09 at commit `a9f8f8ba`. Read this before the steps; it overrides any line below that expects otherwise.

1. **Plan 000 must be done first.** Before you change anything, run `./gradlew :shared:spotlessCheck`. If it fails, Plan 000 (`plans/000-make-the-gates-green.md`) has not landed: STOP and report that. Do not run `spotlessApply` to get past it; on an unprepared tree it rewrites about 36 unrelated files. Once Plan 000 has landed, the test and format commands in this plan behave as described, and `spotlessApply` only touches what you changed.
2. **Two commands fail for reasons no plan fixes.** `./gradlew :shared:compileKotlinIosSimulatorArm64` stops with five errors, all in `shared/src/iosMain/kotlin/com/bernaferrari/changedetection/browser/IosPageCapture.kt` (the first is `Unresolved reference 'kmpwright'`; a library was built with a newer Kotlin than this project uses). `./gradlew :webDemo:wasmJsBrowserDistribution` compiles the Kotlin and then stops at `:kotlinWasmStoreYarnLock` with `Lock file was changed`. Wherever this plan expects one of them to print `BUILD SUCCESSFUL`, the bar is instead: for iOS, **those same five errors and no others**; for the web demo, run `./gradlew :webDemo:compileKotlinWasmJs` and expect `BUILD SUCCESSFUL`. Never run `kotlinWasmUpgradeYarnLock`. If either command has since been repaired and passes, that is fine.
3. **Line numbers may have moved.** Two changes landed after this plan was written. Plan 000 re-formats about 36 files (layout only). The "failure experience" change (`plans/failure-experience-changes.md`) added code to `SyncEngine.kt`, `SitesDao.kt`, `Site.kt`, `CheckFailure.kt`, `DiagnosticTrace.kt`, `PageDetailScreen.kt`, `MainViewModel.kt`, `LegacyMigration.kt`, `DatabaseFactory.kt`, `AppContent.kt`, `AndroidChangeNotifier.kt`, the four `PlatformDiModule` files and the string files. Find code by the text this plan quotes, not by line number. A quoted block that differs only in line breaks or indentation is the same code. The drift check at the top of this plan will list those files as changed; that alone is not a reason to stop. Stop only if the quoted code is gone or does something different.

## Scope

**In scope:**

- `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/sync/SyncEngine.kt` (add one method)
- `shared/src/androidMain/kotlin/com/bernaferrari/changedetection/sync/AndroidCheckRunner.kt`
- `shared/src/androidMain/kotlin/com/bernaferrari/changedetection/SyncWorker.kt`
- New file: `shared/src/desktopTest/kotlin/com/bernaferrari/changedetection/sync/TimeoutAfterSavedResultTest.kt`
- `plans/README.md` (status column only)

**Out of scope:**

- `processFetchResult` and `processFetchResultUnlocked` themselves. Do not add the timestamp comparison there: many existing tests deliberately pass an older `Site` object to `processFetchResult` and expect it to be processed.
- The 90 second limit, `saveNewSnap`, the `NonCancellable` commit.
- The generic `catch (error: Exception)` branch in `AndroidCheckRunner` and the `denied` callback in `SyncWorker`.
- iOS and desktop check runners.

## Git workflow

- Branch from `main`: `git switch -c advisor/005-timeout-after-saved-result`
- Commit subjects are plain sentences. Suggested: `A timeout no longer overwrites a result the same check just saved`.
- Do not push, merge, or open a pull request.

## Steps

### Step 1: Write the failing test

Create `shared/src/desktopTest/kotlin/com/bernaferrari/changedetection/sync/TimeoutAfterSavedResultTest.kt`:

```kotlin
package com.bernaferrari.changedetection.sync

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.bernaferrari.changedetection.error.ErrorHandler
import com.bernaferrari.changedetection.repo.CaptureMode
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.source.local.ChangeDatabase
import com.bernaferrari.changedetection.repo.source.local.getRoomDatabase
import com.bernaferrari.changedetection.util.DesktopFileStorage
import io.ktor.client.HttpClient
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** The 90 second limit can fire after a check has already committed its result; that result must stand. */
class TimeoutAfterSavedResultTest {
    @Test fun aTimeoutAfterTheCheckSavedItsResultDoesNotMarkThePageFailed() = runBlocking {
        Fixture().use { fixture ->
            fixture.db.siteDao().insertSite(fixture.site)
            // The check commits a version; fixture.site is still the row as it was when the check began.
            assertTrue(fixture.engine.processFetchResult(fixture.site, fixture.capture("Saved")) is SyncResult.Success)

            val result = fixture.engine.recordTimeout(fixture.site, RuntimeException("timeout"))

            assertTrue("a stale timeout is skipped, got $result", result is SyncResult.Skipped)
            val stored = requireNotNull(fixture.db.siteDao().getSiteById(fixture.site.id))
            assertTrue("the saved result must stay successful", stored.isSuccessful)
            assertNull(stored.lastFailureCode)
            assertEquals(1, fixture.db.snapsDao().getAllSnapsForSiteId(fixture.site.id).size)
        }
    }

    @Test fun aTimeoutWithNoResultMarksThePageFailed() = runBlocking {
        Fixture().use { fixture ->
            fixture.db.siteDao().insertSite(fixture.site)

            val result = fixture.engine.recordTimeout(fixture.site, RuntimeException("timeout"))

            assertTrue("a real timeout is a failure, got $result", result is SyncResult.Failed)
            val stored = requireNotNull(fixture.db.siteDao().getSiteById(fixture.site.id))
            assertFalse(stored.isSuccessful)
            assertNotNull(stored.lastFailureCode)
        }
    }

    @Test fun aTimeoutForADeletedPageIsSkipped() = runBlocking {
        Fixture().use { fixture ->
            assertTrue(fixture.engine.recordTimeout(fixture.site, RuntimeException("timeout")) is SyncResult.Skipped)
        }
    }

    private class Fixture : AutoCloseable {
        private val directory = Files.createTempDirectory("timeout-after-saved-result").toFile()
        val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = directory.resolve("test.db").path), BundledSQLiteDriver())
        private val files = DesktopFileStorage(directory.resolve("files").also { it.mkdirs() })
        private val client = HttpClient()
        val engine = SyncEngine(client, db, db.siteDao(), db.snapsDao(), files, ErrorHandler())
        val site = Site("Page", "https://example.test", 0L, "", "page", 0 to 0).copy(captureMode = CaptureMode.SOURCE)
        fun capture(text: String) = SyncEngine.FetchResult.Success("text/plain", text.encodeToByteArray())
        override fun close() {
            client.close()
            db.close()
            directory.deleteRecursively()
        }
    }
}
```

**Verify:** `./gradlew :shared:desktopTest --tests '*TimeoutAfterSavedResultTest'`
Expected: the build **fails to compile** with "Unresolved reference: recordTimeout" (or similar wording). That is the expected red state.

### Step 2: Add `recordTimeout` to the engine

In `SyncEngine.kt`, add this method directly after `processFetchResult` (after line 182) and before the KDoc of `establishFirstVersion`:

```kotlin

    /**
     * Records a check that ran out of time. The limit can fire after the same check already committed its
     * result (the commit cannot be cancelled; the alert and maintenance steps after it can), so a status row
     * that moved since [site] was read means a newer result exists and this failure is stale.
     *
     * @param site the page exactly as it was read when the timed-out check began.
     */
    suspend fun recordTimeout(site: Site, timeout: Throwable): SyncResult = withSiteLock(site.id) {
        withContext(Dispatchers.Default) {
            val current = sitesDao.getSiteById(site.id)
                ?: return@withContext SyncResult.Skipped(site.id, "Page deleted during check")
            if (current.timestamp != site.timestamp) {
                return@withContext SyncResult.Skipped(site.id, "A newer result was recorded before the timeout")
            }
            processFetchResultUnlocked(site, FetchResult.Error("Page check timed out", timeout), site.threshold)
        }
    }
```

**Verify:** `./gradlew :shared:desktopTest --tests '*TimeoutAfterSavedResultTest'`
Expected: `BUILD SUCCESSFUL`, three tests pass.

### Step 3: Use it in `AndroidCheckRunner`

In `AndroidCheckRunner.kt`, change the timeout branch (lines 45-47) to:

```kotlin
                } catch (timeout: TimeoutCancellationException) {
                    currentCoroutineContext().ensureActive()
                    engine.recordTimeout(site, timeout)
```

Leave the other two `catch` branches unchanged.

### Step 4: Use it in `SyncWorker`, with a fresh read of the page

In `SyncWorker.kt`, replace `boundedCheck` (lines 170-183) with:

```kotlin
    /** Includes time queued behind manual/Turbo checks, not only time inside a browser. */
    private suspend fun boundedCheck(site: com.bernaferrari.changedetection.repo.Site, foreground: Boolean, completed: Int, total: Int): SyncResult {
        // The batch list can be minutes old. A timeout is only real if the status has not moved since this attempt began.
        val startedFrom = sitesRepository.getSiteById(site.id) ?: return SyncResult.Skipped(site.id, "Page deleted")
        return try {
            withTimeout(90_000) {
                checkAfterForeground(prepare = {
                    if (foreground) setForeground(createForegroundInfo(pageName = site.title?.takeIf { it.isNotBlank() } ?: site.url, completed = completed, total = total))
                }, check = { checkRunner.check(site.id) }, denied = { error ->
                    com.bernaferrari.changedetection.diagnostics.DiagnosticLog.appEvent(com.bernaferrari.changedetection.diagnostics.AppEventKind.BACKGROUND_FAILURE, error, stage = com.bernaferrari.changedetection.diagnostics.AppEventStage.WORKER_FOREGROUND)
                    syncEngine.processFetchResult(site, SyncEngine.FetchResult.Error("Background check could not start", error), site.threshold)
                })
            }
        } catch (timeout: TimeoutCancellationException) {
            currentCoroutineContext().ensureActive()
            syncEngine.recordTimeout(startedFrom, timeout)
        }
    }
```

Copy the body of the `withTimeout` block from the file as it is; only the function wrapper, the `startedFrom` read and the `catch` body change. `sitesRepository` is already a constructor property of `SyncWorker` and `getSiteById` is already used at line 143.

**Verify:** `./gradlew :shared:testAndroidHostTest`
Expected: `BUILD SUCCESSFUL` (this compiles `AndroidCheckRunner` and `SyncWorker`).

### Step 5: Final gates

**Verify:** `rg -n '"Page check timed out"' shared/src`
Expected: exactly one match, inside `SyncEngine.recordTimeout`.

**Verify:** `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest`
Expected: `BUILD SUCCESSFUL`. Run `./gradlew :shared:spotlessApply` first if the format check fails.

On macOS also run `./gradlew :shared:compileKotlinIosSimulatorArm64`; expected `BUILD SUCCESSFUL`.

## Test plan

New file `TimeoutAfterSavedResultTest.kt` with three tests (Step 1):

- a timeout after a saved result is skipped and leaves the page successful;
- a timeout with no result marks the page failed (the existing behaviour, now pinned);
- a timeout for a page that no longer exists is skipped.

Existing suites that must stay green: `SyncEngineTest`, `BaselineInitializationTest`, `MonitoringBatchTest`, `BackgroundExecutionTest`.

## Done criteria

- [ ] `rg -n "fun recordTimeout" shared/src/commonMain` prints one line.
- [ ] `rg -n "recordTimeout\(" shared/src/androidMain` prints two lines (`AndroidCheckRunner.kt`, `SyncWorker.kt`).
- [ ] `rg -n '"Page check timed out"' shared/src` prints one line.
- [ ] `./gradlew :shared:desktopTest --tests '*TimeoutAfterSavedResultTest'` passes with three tests.
- [ ] `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest` is `BUILD SUCCESSFUL`.
- [ ] `git status --short` lists only in-scope files.

## STOP conditions

- `aTimeoutWithNoResultMarksThePageFailed` fails after Step 2. That would mean a freshly inserted page's status already moves before any check, which breaks the rule this plan relies on.
- `sitesDao.updateSyncMetadata` no longer sets `timestamp` on every call (check `SitesDao.kt:187-209`).
- Any existing test starts failing after Step 2. `recordTimeout` is a new method and should not affect them.
- You are tempted to change `processFetchResult` or `processFetchResultUnlocked`. Do not; report instead.

## Maintenance notes

- A stale timeout returns `Skipped`, which the worker treats as success and does not retry. That is intended: the result is already saved.
- The rule depends on every status write changing `sites.timestamp`. If a future change adds a status write that keeps the timestamp, revisit `recordTimeout`.
- Any new caller that wraps a check in `withTimeout` should use `recordTimeout` with the page as read at the start of that attempt, not `processFetchResult(..., FetchResult.Error(...))`.
- The generic `catch (error: Exception)` branch in `AndroidCheckRunner` has the same theoretical gap. It was left alone because the steps after the commit already catch and log their own exceptions, so an exception after a commit is not expected.
