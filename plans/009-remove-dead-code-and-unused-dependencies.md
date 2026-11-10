# Plan 009: Remove code, fonts and dependencies that nothing uses

> **Executor instructions**
> You are implementing one self-contained cleanup. You have not seen the audit that produced this plan; everything you need is in this file.
> 1. Run the drift check first: `git diff --stat a9f8f8ba..HEAD -- shared/build.gradle.kts androidApp/build.gradle.kts shared/src/commonMain/kotlin/com/bernaferrari/changedetection/sync/SyncEngine.kt shared/src/commonMain/kotlin/com/bernaferrari/changedetection/util/ shared/src/commonMain/kotlin/com/bernaferrari/changedetection/repo/source/Result.kt shared/src/androidMain/res/font/`. `SyncEngine.kt` is expected to differ (Plans 005 and 006 edit it), which moves its line numbers; find functions by name, not by line. For every item, the "is it still unused?" command in its step is the real check.
> 2. Follow the steps in order and run each **Verify** command.
> 3. Touch only the files listed under "In scope".
> 4. If a STOP condition applies, stop and report. Do not improvise.
> 5. When finished, set this plan's row in `plans/README.md` to `DONE`.

## Status

| Field | Value |
| --- | --- |
| Priority | P2 |
| Effort | S to M (2 hours; the optional step adds 1 to 2) |
| Risk of the fix | Low. Every deletion is checked by a search first and by the compiler after |
| Depends on | Plans 005 and 006 should land first (same file) |
| Category | Tech debt |
| Planned at | commit `a9f8f8ba`, 2026-11-09 |

## Why this matters

The project carries code from earlier designs that nothing calls any more: a 300-line validation helper, a LiveData-era event wrapper, a `SeekBar` helper from before the Compose UI, three public methods on the central `SyncEngine` class, five font files, and three library dependencies with no imports.

None of it is dangerous. The cost is that it misleads: a reader (human or AI agent) who finds `SyncEngine.syncDueSites` or `ValidationHelper` reasonably assumes it is part of how the app works and may build on it. Unused dependencies also lengthen builds, enlarge the dependency surface that has to be kept up to date, and, for the fonts, ship unused bytes in the APK.

## Current state

Each item below was confirmed to have **no reference outside its own definition** at commit `a9f8f8ba`, searching `shared/src`, `androidApp/src`, `iosApp`, `webDemo` and `sqliteWasmWorker`.

### A. Unused Kotlin

| Item | Location |
| --- | --- |
| `object ValidationHelper` (whole file, about 300 lines) | `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/util/ValidationHelper.kt` |
| `open class Event<out T>` (whole file) | `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/util/Event.kt` |
| `SeekBar.setOnSeekBarChangeListener` (whole file) | `shared/src/androidMain/kotlin/com/bernaferrari/changedetection/extensions/SeekBarExtensions.kt` |
| `sealed class Result` and anything else in the file (it starts with `@file:Suppress("unused")`) | `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/repo/source/Result.kt` |
| `fun syncDueSites(intervalMinutes: Long): Int` | `SyncEngine.kt`, near line 594 at the planned commit |
| `fun parseHtml(content: String): String` | `SyncEngine.kt`, near line 688 |
| `fun generateChangeDescription(newContent: ByteArray, contentType: String): String` | `SyncEngine.kt`, near line 697 |

The three `SyncEngine` functions at the planned commit:

```kotlin
    /** Checks only the active pages whose last check is older than [intervalMinutes]; used when the app comes back. */
    suspend fun syncDueSites(intervalMinutes: Long): Int = withContext(Dispatchers.Default) {
        val now = currentTimeMillis()
        val due = sitesDao.getSitesList().filter { it.isSyncEnabled && (now < it.timestamp || now - it.timestamp >= intervalMinutes * 60_000L) }
        due.forEach { trySyncSite(it.id) }
        due.size
    }
```

```kotlin
    /**
     * Parse HTML content and extract text using Ksoup.
     */
    fun parseHtml(content: String): String = try {
        Ksoup.parse(content).text()
    } catch (e: Exception) {
        content
    }

    /**
     * Generate a meaningful description of what changed.
     */
    fun generateChangeDescription(newContent: ByteArray, contentType: String): String = try {
        // ... about 20 lines that return strings such as "Major content update" ...
    } catch (e: Exception) {
        "Content changed"
    }
```

`parseHtml` and `generateChangeDescription` are the only users of the `Ksoup` import in `SyncEngine.kt` (`import com.fleeksoft.ksoup.Ksoup`, line 16), and the class KDoc near line 62 says "Ksoup for HTML parsing (multiplatform)".

### B. Unused font files

`shared/src/androidMain/res/font/` contains exactly five files, and no source, XML or Gradle file mentions any of their names:

`ibm_mono_medium.otf`, `lato_bold.ttf`, `work_medium.ttf`, `work_regular.ttf`, `work_semibold.ttf`

(The app's real fonts are Compose resources under `shared/src/commonMain/composeResources/font/`. Do not touch that directory.)

### C. Unused dependencies

| Dependency line | File | Evidence |
| --- | --- | --- |
| `implementation(libs.androidx.palette.ktx)` | `shared/build.gradle.kts:133` | no `androidx.palette` import anywhere |
| `implementation(libs.androidx.browser)` | `shared/build.gradle.kts:135` | no `androidx.browser.` import anywhere |
| `implementation(libs.androidx.compose.material3.window.size)` | `shared/build.gradle.kts:128` and `androidApp/build.gradle.kts:133` | no `material3.windowsizeclass` import anywhere |
| `implementation(libs.androidx.glance)` and `implementation(libs.androidx.glance.appwidget)` | `androidApp/build.gradle.kts:136-137` | Glance is used only inside `shared` (which keeps its own declaration at `shared/build.gradle.kts:137-138`); `androidApp/src` has no Glance import |

`libs.androidx.startup` is used. Keep it.

## Commands you will need

| Purpose | Command | Expected |
| --- | --- | --- |
| Full gate | `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest` | `BUILD SUCCESSFUL` |
| Android lint and release build | `./gradlew :androidApp:lintDebug :androidApp:assembleRelease` | `BUILD SUCCESSFUL` |
| Device tests still compile | `./gradlew :shared:assembleAndroidDeviceTest` | `BUILD SUCCESSFUL` |
| iOS compiles (macOS only) | `./gradlew :shared:compileKotlinIosSimulatorArm64` | `BUILD SUCCESSFUL` |
| Web demo compiles | `./gradlew :webDemo:wasmJsBrowserDistribution` | `BUILD SUCCESSFUL` |
| Auto-format | `./gradlew :shared:spotlessApply` | `BUILD SUCCESSFUL` |

The build needs JDK 21 and Android SDK platform 37.2.

## Before you start: the state of the build

Checked on 2026-11-09 at commit `a9f8f8ba`. Read this before the steps; it overrides any line below that expects otherwise.

1. **Plan 000 must be done first.** Before you change anything, run `./gradlew :shared:spotlessCheck`. If it fails, Plan 000 (`plans/000-make-the-gates-green.md`) has not landed: STOP and report that. Do not run `spotlessApply` to get past it; on an unprepared tree it rewrites about 36 unrelated files. Once Plan 000 has landed, the test and format commands in this plan behave as described, and `spotlessApply` only touches what you changed.
2. **Two commands fail for reasons no plan fixes.** `./gradlew :shared:compileKotlinIosSimulatorArm64` stops with five errors, all in `shared/src/iosMain/kotlin/com/bernaferrari/changedetection/browser/IosPageCapture.kt` (the first is `Unresolved reference 'kmpwright'`; a library was built with a newer Kotlin than this project uses). `./gradlew :webDemo:wasmJsBrowserDistribution` compiles the Kotlin and then stops at `:kotlinWasmStoreYarnLock` with `Lock file was changed`. Wherever this plan expects one of them to print `BUILD SUCCESSFUL`, the bar is instead: for iOS, **those same five errors and no others**; for the web demo, run `./gradlew :webDemo:compileKotlinWasmJs` and expect `BUILD SUCCESSFUL`. Never run `kotlinWasmUpgradeYarnLock`. If either command has since been repaired and passes, that is fine.
3. **Line numbers may have moved.** Two changes landed after this plan was written. Plan 000 re-formats about 36 files (layout only). The "failure experience" change (`plans/failure-experience-changes.md`) added code to `SyncEngine.kt`, `SitesDao.kt`, `Site.kt`, `CheckFailure.kt`, `DiagnosticTrace.kt`, `PageDetailScreen.kt`, `MainViewModel.kt`, `LegacyMigration.kt`, `DatabaseFactory.kt`, `AppContent.kt`, `AndroidChangeNotifier.kt`, the four `PlatformDiModule` files and the string files. Find code by the text this plan quotes, not by line number. A quoted block that differs only in line breaks or indentation is the same code. The drift check at the top of this plan will list those files as changed; that alone is not a reason to stop. Stop only if the quoted code is gone or does something different.

## Scope

**In scope:**

- Delete: the four whole files in table A.
- `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/sync/SyncEngine.kt` (remove three functions, one import, one KDoc line)
- Delete: the five files in `shared/src/androidMain/res/font/` (and the then-empty directory)
- `shared/build.gradle.kts`, `androidApp/build.gradle.kts` (remove the lines in table C)
- Optional Step 6 only: `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/repo/source/local/SnapsDao.kt` and `shared/src/androidUnitTest/kotlin/com/bernaferrari/changedetection/ui/logs/LogsViewModelDeleteUndoTest.kt` (find the latter with `rg -l "class LogsViewModelDeleteUndoTest" shared/src`)
- `plans/README.md` (status column only)

**Out of scope:**

- `gradle/libs.versions.toml`. Leave the catalog entries; removing them is harmless but not needed, and other plans may use them.
- `ContentHasher.quickSizeCheck`, `clearCaches`, `hashedWith`, `isChangedFrom`. They are referenced only by `ContentHasherTest`; deleting them means deleting tests, which is the maintainer's call.
- Repository pass-through methods (`getAllMostRecentSnaps`, `getLastContentHash`, `getContentTypeInfo`) and `ErrorHandler.errorEvents`. Several test fakes implement them.
- `androidApp/src/main/assets/licenses/`.
- Anything that the searches in the steps show is still referenced.

## Git workflow

- Branch from `main` (after Plans 005 and 006): `git switch -c advisor/009-remove-dead-code-and-unused-dependencies`
- One commit per step keeps this easy to review and to revert. Commit subjects are plain sentences, for example `Unused helpers from the pre-Compose app are gone`, `Unused fonts no longer ship in the APK`, `Dependencies with no imports are removed`.
- Do not push, merge, or open a pull request.

## Steps

### Step 1: Delete the four unused files

For each file, run the search first. Delete the file only if the search prints **nothing except lines inside the file itself**.

```bash
rg -n "ValidationHelper" shared/src androidApp/src iosApp webDemo
rg -n "\bEvent<|getContentIfNotHandled|peekContent|hasBeenHandled" shared/src androidApp/src iosApp webDemo
rg -n "setOnSeekBarChangeListener|android\.widget\.SeekBar" shared/src androidApp/src
rg -n "WebResult|repo\.source\.Result\b|\bResult\.(Success|Error)\b" shared/src androidApp/src iosApp webDemo
```

Notes for reading the output:

- The second search must match only `util/Event.kt`. (`AndroidDiagnosticStore.kt` has an unrelated nested class `data class Event(val value: AppEvent)`; it does not match this pattern and must not be touched.)
- The fourth search will also print lines containing `SyncEngine.FetchResult.Success` or `FetchResult.Error`. Those are a different class and do not count. To see only real uses, also run `rg -n "import com\.bernaferrari\.changedetection\.repo\.source\.Result" shared/src androidApp/src` (expected: nothing) and open `Result.kt` to read every top-level name it declares, then search for each of those names as a whole word. Classes in the same package (`repo/source/`) can use `Result` without an import, so also run `rg -n "\bResult<" shared/src/commonMain/kotlin/com/bernaferrari/changedetection/repo` (expected: only lines in `Result.kt`).

Then delete:

```bash
git rm shared/src/commonMain/kotlin/com/bernaferrari/changedetection/util/ValidationHelper.kt
git rm shared/src/commonMain/kotlin/com/bernaferrari/changedetection/util/Event.kt
git rm shared/src/androidMain/kotlin/com/bernaferrari/changedetection/extensions/SeekBarExtensions.kt
git rm shared/src/commonMain/kotlin/com/bernaferrari/changedetection/repo/source/Result.kt
```

**Verify:** `./gradlew :shared:testAndroidHostTest :shared:desktopTest`
Expected: `BUILD SUCCESSFUL`. A compile error naming one of the deleted symbols means it was used after all: restore that file with `git restore --staged --worktree <path>` and record it in your report.

### Step 2: Remove the three unused `SyncEngine` functions

Confirm they are unused:

```bash
rg -n "syncDueSites|parseHtml|generateChangeDescription" shared/src androidApp/src iosApp webDemo
```

Expected: exactly three lines, all definitions in `SyncEngine.kt`.

In `SyncEngine.kt`:

1. Delete `syncDueSites` with its one-line KDoc.
2. Delete `parseHtml` with its KDoc.
3. Delete `generateChangeDescription` with its KDoc.
4. Run `rg -n "Ksoup" shared/src/commonMain/kotlin/com/bernaferrari/changedetection/sync/SyncEngine.kt`. If the only matches left are the import and the class KDoc line, delete the import `import com.fleeksoft.ksoup.Ksoup` and the KDoc line ` * - Ksoup for HTML parsing (multiplatform)`. If Ksoup is still used in code, leave both.

Do not delete anything else, even if it looks unused.

**Verify:** `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest`
Expected: `BUILD SUCCESSFUL`. Run `./gradlew :shared:spotlessApply` if the format check complains about blank lines.

**Verify (macOS):** `./gradlew :shared:compileKotlinIosSimulatorArm64` is `BUILD SUCCESSFUL`. The iOS app calls into `SyncEngine` from Swift through helper objects; the search above included `iosApp`, and this compile is the second check.

### Step 3: Delete the unused fonts

```bash
for f in ibm_mono_medium lato_bold work_medium work_regular work_semibold; do rg -n "$f" shared androidApp iosApp webDemo website -g '!*.otf' -g '!*.ttf' -g '!**/build/**'; done
```

Expected: no output.

```bash
git rm -r shared/src/androidMain/res/font
```

**Verify:** `./gradlew :androidApp:lintDebug :androidApp:assembleRelease`
Expected: `BUILD SUCCESSFUL`.

### Step 4: Remove the unused dependencies

Confirm there are no imports:

```bash
rg -n "androidx\.palette" shared/src androidApp/src
rg -n "androidx\.browser\." shared/src androidApp/src
rg -n "windowsizeclass" shared/src androidApp/src
rg -n "androidx\.glance" androidApp/src
```

Expected: no output from any of the four. If one prints something, skip that dependency and record it.

Then remove these lines:

- `shared/build.gradle.kts`: `implementation(libs.androidx.compose.material3.window.size)`, `implementation(libs.androidx.palette.ktx)`, `implementation(libs.androidx.browser)`. Leave the two Glance lines in this file.
- `androidApp/build.gradle.kts`: `implementation(libs.androidx.compose.material3.window.size)`, `implementation(libs.androidx.glance)`, `implementation(libs.androidx.glance.appwidget)`.

Tidy any doubled blank lines the removals leave behind.

**Verify:** `./gradlew :shared:testAndroidHostTest :androidApp:lintDebug :androidApp:assembleRelease :shared:assembleAndroidDeviceTest`
Expected: `BUILD SUCCESSFUL`. The release build runs code shrinking, which is the real proof that nothing needed these libraries. If the build fails with an unresolved class from one of the removed libraries, restore that one line and record it.

### Step 5: Final gates

**Verify:** `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest`, then `./gradlew :webDemo:wasmJsBrowserDistribution`, then on macOS `./gradlew :shared:compileKotlinIosSimulatorArm64`.
Expected: `BUILD SUCCESSFUL` for each.

### Step 6 (optional, separate commit): remove database queries that only a test fake mentions

Skip this step if you are short on time; say so in your report.

`SnapsDao` (`shared/src/commonMain/kotlin/com/bernaferrari/changedetection/repo/source/local/SnapsDao.kt`) declares these twelve query methods that no production code calls. Their only other mentions are `override` stubs in a hand-written fake DAO inside `LogsViewModelDeleteUndoTest.kt`:

`getContentTypesCount`, `getContentTypesParams`, `getLastSnapsSize`, `getAllSnaps`, `countNumberOfChanges`, `countSitesWithChanges`, `countVersionsInHistory`, `getSnapsWithChanges`, `getLatestSnapsFromSitesWithChangesList`, `hasContentWithHash`, `getAverageContentSize`, `getContentDuplicationStats`

For **each** name, one at a time:

1. `rg -n "\b<name>\b" shared/src androidApp/src iosApp webDemo`
2. Continue only if every match is either the declaration in `SnapsDao.kt` or a line starting with `override` in a test file. If any other line matches (a call, a repository method that forwards to it, a non-fake test), skip this name.
3. Delete the method from `SnapsDao.kt` together with its `@Query(...)` annotation and KDoc, and delete the matching `override` stub from the test fake.

Do **not** remove `getSnapsWithChange2` or `getUniqueContentSnaps`: real database tests call them.

After the last name, check whether a result type became unused: `rg -n "ContentDuplicationStats" shared/src`. If the only match is its own declaration, delete the declaration too.

**Verify:** `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest :shared:assembleAndroidDeviceTest`
Expected: `BUILD SUCCESSFUL`.

**Verify:** `git status --short shared/schemas`
Expected: no output. Removing query methods must not change the exported database schema. If a schema file changed, STOP.

## Test plan

No new tests: this plan only deletes. The safety net is the compiler on every platform, the full existing test suites, and the shrunk release build.

## Done criteria

- [ ] The four files in table A no longer exist.
- [ ] `rg -n "syncDueSites|parseHtml|generateChangeDescription" shared/src` prints nothing.
- [ ] `ls shared/src/androidMain/res/font` fails with "No such file or directory".
- [ ] `rg -n "palette|androidx.browser|material3.window.size" shared/build.gradle.kts androidApp/build.gradle.kts` prints nothing, and `rg -n "glance" androidApp/build.gradle.kts` prints nothing.
- [ ] `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest` is `BUILD SUCCESSFUL`.
- [ ] `./gradlew :androidApp:lintDebug :androidApp:assembleRelease :shared:assembleAndroidDeviceTest` is `BUILD SUCCESSFUL`.
- [ ] `./gradlew :webDemo:wasmJsBrowserDistribution` is `BUILD SUCCESSFUL`.
- [ ] The report lists anything that was skipped because a search found a use.
- [ ] `git status --short` lists only in-scope files.

## STOP conditions

- A search in Step 1, 2 or 4 shows a real use of an item. Skip that item and continue with the rest; stop only if more than two items turn out to be used, because then this plan's survey is out of date.
- Removing a dependency breaks the release build in a way that restoring the line does not fix.
- An exported schema file under `shared/schemas` changes.
- You find yourself wanting to delete something not listed here. Record it in your report instead.

## Maintenance notes

- Left in place on purpose, for the maintainer to decide: the `ContentHasher` helpers that only `ContentHasherTest` uses; the repository pass-throughs `getAllMostRecentSnaps`, `getLastContentHash` and `getContentTypeInfo`; `ErrorHandler.errorEvents`; the file `androidApp/src/main/assets/licenses/webwright-LICENSE`, which nothing reads (Plan 013 deals with licence notices).
- `shared/src/jvmSharedMain` is an empty directory that no Gradle file references. Git does not track empty directories, so there is nothing to commit; delete it locally if it bothers you.
- To keep dead code from returning, consider enabling a "unused declaration" inspection in review rather than a build-breaking rule: Kotlin Multiplatform `expect`/`actual` and Swift callers produce false positives.
