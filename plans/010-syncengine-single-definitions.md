# Plan 010: Give `SyncEngine` one definition each for "same page settings" and "is an image", and follow the cancellation convention

> **Executor instructions**
> You are implementing one small, self-contained tidy-up. You have not seen the audit that produced this plan; everything you need is in this file.
> 1. Run the drift check first: `git diff --stat a9f8f8ba..HEAD -- shared/src/commonMain/kotlin/com/bernaferrari/changedetection/sync/SyncEngine.kt shared/src/commonMain/kotlin/com/bernaferrari/changedetection/diagnostics/DiagnosticTrace.kt`. `SyncEngine.kt` is expected to differ (Plans 005, 006 and 009 edit it), which moves its line numbers; find code by the quoted text, not by line. If a quoted excerpt no longer exists in the file, STOP and report.
> 2. Follow the steps in order and run each **Verify** command.
> 3. Touch only the files listed under "In scope".
> 4. If a STOP condition applies, stop and report. Do not improvise.
> 5. When finished, set this plan's row in `plans/README.md` to `DONE`.

## Status

| Field | Value |
| --- | --- |
| Priority | P3 |
| Effort | S (1 to 2 hours) |
| Risk of the fix | Low. No intended behaviour change except the one noted in Step 2 |
| Depends on | Plans 005, 006 and 009 should land first (same file) |
| Category | Tech debt (consistency) |
| Planned at | commit `a9f8f8ba`, 2026-11-09 |

## Why this matters

`SyncEngine` is the class that decides whether a page changed and saves the result. Three small inconsistencies in it are worth removing. None is a known bug today; each is a place where a future edit can easily go wrong.

1. **The rule "is this still the same page, with the same settings, and still active?" is written out twice.** A check reads a page, works for up to 90 seconds, and before saving must confirm the user did not edit or pause the page meanwhile. That six-part comparison exists once at the start of result processing and again inside the save transaction. If someone adds a seventh setting to one copy only, results can be saved under settings they were not captured with.
2. **"Is this an image?" is tested two ways.** Most of the file uses `contentType.startsWith("image/", true)`. Two places use `contentType.contains("image", ignoreCase = true)` to set the page's `isBrowser` flag.
3. **Four places catch and discard coroutine cancellation.** The project's convention is to rethrow `CancellationException` before catching `Exception`. Four spots use `runCatching` or a bare `catch (_: Exception)` around a suspending call. In each case the next suspension point rethrows the cancellation, so the practical effect today is a slightly delayed cancellation; it is still a convention worth keeping uniform, because the same pattern in a loop without a later suspension point would be a real hang.

## Current state

All excerpts are from commit `a9f8f8ba`. File: `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/sync/SyncEngine.kt` unless stated.

### 1. The duplicated predicate

In `processFetchResultUnlocked` (line 203):

```kotlin
        if (currentSite.url != site.url || currentSite.captureMode != site.captureMode || currentSite.threshold != site.threshold || currentSite.visualChangeThresholdBps != site.visualChangeThresholdBps || currentSite.capturePolicy() != site.capturePolicy() || !currentSite.isSyncEnabled) {
            return@withContext SyncResult.Skipped(site.id, "Page changed or paused during check")
        }
```

In `currentCaptureSite` (lines 536-539):

```kotlin
    /** Must be read under the same write transaction as the status or snapshot commit. */
    private suspend fun currentCaptureSite(site: Site): Site? = sitesDao.getSiteById(site.id)?.takeIf {
        it.url == site.url && it.isSyncEnabled && it.threshold == site.threshold && it.visualChangeThresholdBps == site.visualChangeThresholdBps && it.captureMode == site.captureMode && it.capturePolicy() == site.capturePolicy()
    }
```

Both compare the same six things: `url`, `captureMode`, `threshold`, `visualChangeThresholdBps`, `capturePolicy()`, and that the stored page `isSyncEnabled`.

### 2. The two "image" tests

Line 407:

```kotlin
            val updated = updateStatusIfCurrent(site, successful = true, isBrowser = result.contentType.contains("image", ignoreCase = true), expectedSnapshot = lastSnap)
```

Line 466 (inside the `sitesDao.updateSyncMetadata(` call in `saveNewSnap`):

```kotlin
                            isBrowser = result.contentType.contains("image", ignoreCase = true),
```

Everywhere else in the file, for example line 394: `result.contentType.startsWith("image/", true)`.

### 3. Swallowed cancellation

`resolveMissingIcons` (lines 89-90):

```kotlin
            runCatching { com.bernaferrari.changedetection.addedit.htmlIcon(result.data.decodeToString(), site.url) }
                .getOrNull()?.let { runCatching { sitesDao.updateIcon(site.id, it) } }
```

In result processing (lines 329-332):

```kotlin
        if (site.iconUrl.isBlank() && incoming.contentType.contains("html", true)) {
            runCatching { com.bernaferrari.changedetection.addedit.htmlIcon(incoming.data.decodeToString(), site.url) }
                .getOrNull()?.let { runCatching { sitesDao.updateIcon(site.id, it) } }
        }
```

`htmlIcon(html: String, pageUrl: String): String?` is an ordinary (non-suspending) function. `sitesDao.updateIcon` is a suspending database call.

`shared/src/commonMain/kotlin/com/bernaferrari/changedetection/diagnostics/DiagnosticTrace.kt:177-181`:

```kotlin
    suspend fun persistCheckpoint() {
        try {
            checkpoint?.invoke()
        } catch (_: Exception) { }
    }
```

Same file, lines 290-294:

```kotlin
    val token = try {
        withContext(Dispatchers.Default) { withTimeoutOrNull(200) { sink.watchToken(watchId) } ?: "" }
    } catch (_: Exception) {
        ""
    }
```

### The convention to follow

The same file already shows it, for example in `fetchFromServer`:

```kotlin
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (e: Exception) {
```

`CancellationException` is already imported in `SyncEngine.kt`. In `DiagnosticTrace.kt`, check the imports; if it is not imported, write it fully qualified as `kotlinx.coroutines.CancellationException` (that file uses fully qualified names freely).

## Commands you will need

| Purpose | Command | Expected |
| --- | --- | --- |
| Engine tests | `./gradlew :shared:testAndroidHostTest --tests '*SyncEngine*'` | `BUILD SUCCESSFUL` |
| Format check | `./gradlew :shared:spotlessCheck` | `BUILD SUCCESSFUL` |
| Auto-format | `./gradlew :shared:spotlessApply` | `BUILD SUCCESSFUL` |
| Full gate | `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest` | `BUILD SUCCESSFUL` |
| Other platforms compile | `./gradlew :webDemo:wasmJsBrowserDistribution` and (macOS) `./gradlew :shared:compileKotlinIosSimulatorArm64` | `BUILD SUCCESSFUL` |

The build needs JDK 21 and Android SDK platform 37.2.

## Before you start: the state of the build

Checked on 2026-11-09 at commit `a9f8f8ba`. Read this before the steps; it overrides any line below that expects otherwise.

1. **Plan 000 must be done first.** Before you change anything, run `./gradlew :shared:spotlessCheck`. If it fails, Plan 000 (`plans/000-make-the-gates-green.md`) has not landed: STOP and report that. Do not run `spotlessApply` to get past it; on an unprepared tree it rewrites about 36 unrelated files. Once Plan 000 has landed, the test and format commands in this plan behave as described, and `spotlessApply` only touches what you changed.
2. **Two commands fail for reasons no plan fixes.** `./gradlew :shared:compileKotlinIosSimulatorArm64` stops with five errors, all in `shared/src/iosMain/kotlin/com/bernaferrari/changedetection/browser/IosPageCapture.kt` (the first is `Unresolved reference 'kmpwright'`; a library was built with a newer Kotlin than this project uses). `./gradlew :webDemo:wasmJsBrowserDistribution` compiles the Kotlin and then stops at `:kotlinWasmStoreYarnLock` with `Lock file was changed`. Wherever this plan expects one of them to print `BUILD SUCCESSFUL`, the bar is instead: for iOS, **those same five errors and no others**; for the web demo, run `./gradlew :webDemo:compileKotlinWasmJs` and expect `BUILD SUCCESSFUL`. Never run `kotlinWasmUpgradeYarnLock`. If either command has since been repaired and passes, that is fine.
3. **Line numbers may have moved.** Two changes landed after this plan was written. Plan 000 re-formats about 36 files (layout only). The "failure experience" change (`plans/failure-experience-changes.md`) added code to `SyncEngine.kt`, `SitesDao.kt`, `Site.kt`, `CheckFailure.kt`, `DiagnosticTrace.kt`, `PageDetailScreen.kt`, `MainViewModel.kt`, `LegacyMigration.kt`, `DatabaseFactory.kt`, `AppContent.kt`, `AndroidChangeNotifier.kt`, the four `PlatformDiModule` files and the string files. Find code by the text this plan quotes, not by line number. A quoted block that differs only in line breaks or indentation is the same code. The drift check at the top of this plan will list those files as changed; that alone is not a reason to stop. Stop only if the quoted code is gone or does something different.

## Scope

**In scope:**

- `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/sync/SyncEngine.kt`
- `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/diagnostics/DiagnosticTrace.kt`
- `plans/README.md` (status column only)

**Out of scope:**

- Any change to *what* the six-part comparison compares.
- The hash-prefix string literals (`"image-v2:"`, `"text-v1:"` and so on). They identify stored data formats; leave them.
- The `try { ... } catch (cancelled: CancellationException) { throw cancelled } catch (error: Exception) { throw error }` block around the visual comparison. Its second `catch` does nothing, but removing it means re-indenting a long expression for no behaviour gain.
- Splitting `SyncEngine` into smaller classes. That was considered and rejected for now; the existing tests pin the class as a whole.
- The other `catch (_: Exception)` in `DiagnosticTrace.kt` that sits inside `withContext(NonCancellable)` (it cannot be cancelled there, and its comment explains it).

## Git workflow

- Branch from `main` (after Plans 005, 006 and 009): `git switch -c advisor/010-syncengine-single-definitions`
- Commit subjects are plain sentences. Suggested: `The sync engine has one rule for "same page settings" and one for "is an image"`.
- Do not push, merge, or open a pull request.

## Steps

### Step 1: One definition of "same page settings"

In `SyncEngine.kt`, add this private function directly above `currentCaptureSite`:

```kotlin
    /**
     * True while [stored] is still the page this check captured: same address, same capture settings, still active.
     * Every place that decides whether a finished check may be recorded must use this one rule.
     */
    private fun captureStillApplies(stored: Site, captured: Site): Boolean = stored.url == captured.url && stored.isSyncEnabled && stored.threshold == captured.threshold && stored.visualChangeThresholdBps == captured.visualChangeThresholdBps && stored.captureMode == captured.captureMode && stored.capturePolicy() == captured.capturePolicy()
```

Change `currentCaptureSite` to use it:

```kotlin
    /** Must be read under the same write transaction as the status or snapshot commit. */
    private suspend fun currentCaptureSite(site: Site): Site? = sitesDao.getSiteById(site.id)?.takeIf { captureStillApplies(it, site) }
```

Change the condition in `processFetchResultUnlocked` to:

```kotlin
        if (!captureStillApplies(currentSite, site)) {
            return@withContext SyncResult.Skipped(site.id, "Page changed or paused during check")
        }
```

**Verify:** `rg -n "visualChangeThresholdBps == |visualChangeThresholdBps != " shared/src/commonMain/kotlin/com/bernaferrari/changedetection/sync/SyncEngine.kt`
Expected: exactly one line, inside `captureStillApplies`.

**Verify:** `./gradlew :shared:testAndroidHostTest --tests '*SyncEngine*'`
Expected: `BUILD SUCCESSFUL`.

### Step 2: One definition of "is an image"

Add this private function next to `captureStillApplies`:

```kotlin
    private fun FetchResult.Success.isImage(): Boolean = contentType.startsWith("image/", ignoreCase = true)
```

Replace both occurrences of `result.contentType.contains("image", ignoreCase = true)` (the two `isBrowser = ...` arguments) with `result.isImage()`.

Before relying on the receiver type, confirm that `result` is a `FetchResult.Success` at both places (hover in an IDE, or read the enclosing function's parameter list). If at either place `result` is the broader `FetchResult` type, make the helper `private fun String.isImageType(): Boolean = startsWith("image/", ignoreCase = true)` and call `result.contentType.isImageType()` instead.

This is the only intended behaviour change in this plan: a content type that contains the word "image" without starting with `image/` (none is known to occur) is no longer treated as an image for the `isBrowser` flag. Do not replace the other `startsWith("image/", true)` calls in the file; they already agree, and touching them only enlarges the diff.

**Verify:** `rg -n 'contains\("image"' shared/src/commonMain/kotlin/com/bernaferrari/changedetection/sync/SyncEngine.kt`
Expected: no output.

### Step 3: Follow the cancellation convention

In `SyncEngine.kt`, add one private helper next to the two above:

```kotlin
    /** Best effort: a page's icon is cosmetic, so a failure is ignored, but cancellation still propagates. */
    private suspend fun saveIconFromHtml(site: Site, html: ByteArray) {
        val icon = runCatching { com.bernaferrari.changedetection.addedit.htmlIcon(html.decodeToString(), site.url) }.getOrNull() ?: return
        try {
            sitesDao.updateIcon(site.id, icon)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
        }
    }
```

(`runCatching` around `htmlIcon` is fine: that function does not suspend, so it cannot throw a cancellation.)

Replace the two-line `runCatching { ... htmlIcon ... }.getOrNull()?.let { runCatching { sitesDao.updateIcon(site.id, it) } }` in `resolveMissingIcons` with:

```kotlin
            saveIconFromHtml(site, result.data)
```

and the same two lines inside `if (site.iconUrl.isBlank() && incoming.contentType.contains("html", true)) { ... }` with:

```kotlin
            saveIconFromHtml(site, incoming.data)
```

In `DiagnosticTrace.kt`, change `persistCheckpoint` to:

```kotlin
    suspend fun persistCheckpoint() {
        try {
            checkpoint?.invoke()
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
        }
    }
```

and the token lookup to:

```kotlin
    val token = try {
        withContext(Dispatchers.Default) { withTimeoutOrNull(200) { sink.watchToken(watchId) } ?: "" }
    } catch (cancelled: kotlinx.coroutines.CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        ""
    }
```

`withTimeoutOrNull` returns `null` on its own timeout and does not throw, so the 200 ms limit still yields `""` as before.

**Verify:** `rg -n "runCatching \{ sitesDao" shared/src/commonMain` prints nothing.

**Verify:** `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest`
Expected: `BUILD SUCCESSFUL`. Run `./gradlew :shared:spotlessApply` first if the format check fails (ktlint may want the empty `catch` body formatted differently; accept what it produces).

**Verify:** `./gradlew :webDemo:wasmJsBrowserDistribution`, and on macOS `./gradlew :shared:compileKotlinIosSimulatorArm64`.
Expected: `BUILD SUCCESSFUL`.

## Test plan

No new tests. This is a refactor guarded by the existing suites, which are extensive for this class: `SyncEngineTest`, `BaselineInitializationTest`, `FullReviewBaselineIntegrityTest`, `MonitoringBatchTest`, the diagnostics tests, and the desktop database tests. All must pass unchanged. Do not edit a test to make it pass.

## Done criteria

- [ ] `rg -n "fun captureStillApplies|fun saveIconFromHtml" shared/src/commonMain` prints two lines.
- [ ] `rg -n 'contains\("image"' shared/src/commonMain/kotlin/com/bernaferrari/changedetection/sync/SyncEngine.kt` prints nothing.
- [ ] `rg -n "runCatching \{ sitesDao" shared/src/commonMain` prints nothing.
- [ ] `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest` is `BUILD SUCCESSFUL` with no test file changed (`git diff --stat` shows only the two in-scope source files and `plans/README.md`).

## STOP conditions

- The two copies of the six-part comparison no longer compare the same six things (someone already changed one). Report the difference; do not pick one.
- Any existing test fails after a step. Revert that step and report which test.
- A diagnostics test fails after the `DiagnosticTrace.kt` change. That would mean something depends on a cancelled check still writing its first checkpoint; report it and revert only that file.

## Maintenance notes

- When a new per-page capture setting is added (anything that changes what a check captures), add it to `captureStillApplies`. That function is now the single place.
- The convention for new code: in a `suspend` function, never wrap a suspending call in `runCatching`, and never write `catch (e: Exception)` without `catch (c: CancellationException) { throw c }` before it.
- Left as is: the no-op `catch (error: Exception) { throw error }` around the visual comparison, and the hash-prefix literals.
