# Plan 008: Stop re-reading full screenshots from disk for thumbnails that are already cached

> **Executor instructions**
> You are implementing one self-contained change. You have not seen the audit that produced this plan; everything you need is in this file.
> 1. Run the drift check first: `git diff --stat a9f8f8ba..HEAD -- shared/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/kit/ChangePreviewViews.kt shared/build.gradle.kts`. If it prints anything, compare the "Current state" excerpts with the real files. If `CaptureTop` no longer matches, STOP and report.
> 2. Follow the steps in order and run each **Verify** command.
> 3. Touch only the files listed under "In scope".
> 4. If a STOP condition applies, stop and report. Do not improvise.
> 5. When finished, set this plan's row in `plans/README.md` to `DONE`.

## Status

| Field | Value |
| --- | --- |
| Priority | P2 |
| Effort | S to M (2 to 3 hours) |
| Risk of the fix | Low to medium. Touches the thumbnail used on the home list, page detail and version list |
| Depends on | Nothing |
| Category | Performance (memory and disk reads while scrolling) |
| Planned at | commit `a9f8f8ba`, 2026-11-09 |

## Why this matters

The home list, the page detail screen and the version list show a small thumbnail of each saved screenshot. Saved screenshots are full-page captures and are often several megabytes each.

The thumbnail composable, `CaptureTop`, works like this today:

1. Read the **entire** screenshot file into a `ByteArray`.
2. Keep that array in composition state for as long as the row is on screen.
3. Hand the array to the image library (Coil), which decodes a small version and caches the small bitmap in memory under a key.

Step 3's cache is never used to avoid steps 1 and 2. Every time a row appears (first display, scrolling back, returning to the screen), the whole file is read from disk again even though the small bitmap is already in Coil's memory cache. While rows are visible, each one also holds its multi-megabyte array. On a list of twenty pages that is tens of megabytes of short-lived allocations and repeated disk reads during scrolling, on the app's most-used screen.

The fix is to let Coil ask for the bytes itself, only when it does not already have the bitmap. Coil supports this through a small adapter called a `Fetcher`.

## Current state

`shared/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/kit/ChangePreviewViews.kt:474-502`:

```kotlin
/** Resolves a dependency when the app's container is running; previews and UI tests may not have one. */
internal inline fun <reified T : Any> optional(): T? = KoinPlatformTools.defaultContext().getOrNull()?.getOrNull<T>()

/**
 * The top of a saved capture, cropped to fill [modifier]. Decoded small and cached by snapshot, so long
 * full-page screenshots stay cheap in lists.
 */
@Composable
fun CaptureTop(snapId: String, modifier: Modifier, alpha: Float = 1f, decodeWidth: Int = 720) {
    val bytes by produceState<ByteArray?>(null, snapId) {
        value = runCatching { optional<com.bernaferrari.changedetection.repo.source.SnapsRepository>()?.getSnapContent(snapId) }.getOrNull()
    }
    val context = coil3.compose.LocalPlatformContext.current
    Box(modifier.background(androidx.compose.ui.graphics.Color.White)) {
        bytes?.let { data ->
            coil3.compose.AsyncImage(
                model = remember(snapId, decodeWidth) {
                    coil3.request.ImageRequest.Builder(context).data(data).memoryCacheKey("top-$snapId-$decodeWidth").size(coil3.size.Size(coil3.size.Dimension(decodeWidth), coil3.size.Dimension.Undefined)).build()
                },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                alpha = alpha,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}
```

`CaptureTop` is called from three places (`rg -n "CaptureTop\(" shared/src`): `ui/home/HomeScreen.kt:874`, `ui/detail/PageDetailScreen.kt:552`, `ui/comparison/VersionSheet.kt:257`. Its signature does not change in this plan, so the callers are untouched.

`SnapsRepository.getSnapContent(snapId: String): ByteArray` is a `suspend` function that reads the file on an IO dispatcher and throws if the file is missing.

Facts about the image library, checked against the Coil 3.6.3 artifacts this project uses:

- `coil3.request.ImageRequest.Builder` has `fetcherFactory(factory: Fetcher.Factory<T>)`, which attaches a fetcher to one request without changing the app-wide image loader.
- When a request has an explicit `memoryCacheKey(String)`, Coil uses exactly that key and looks in its memory cache **before** it runs any fetcher.
- Coil's own fetcher for byte arrays (`coil3.fetch.ByteArrayFetcher`) is the model to copy:

```kotlin
internal class ByteArrayFetcher(
    private val data: ByteArray,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        return SourceFetchResult(
            source = ImageSource(
                source = Buffer().apply { write(data) },
                fileSystem = options.fileSystem,
            ),
            mimeType = null,
            dataSource = DataSource.MEMORY,
        )
    }

    class Factory : Fetcher.Factory<ByteArray> {
        override fun create(data: ByteArray, options: Options, imageLoader: ImageLoader): Fetcher = ByteArrayFetcher(data, options)
    }
}
```

`Buffer` is `okio.Buffer`. Coil exposes Okio to its users, so `import okio.Buffer` is expected to resolve in `commonMain` without a build change.

A name to avoid: `com.bernaferrari.changedetection.ui.comparison.SnapshotImage` already exists (it is imported at `ChangePreviewViews.kt:81`). Do not create anything called `SnapshotImage`.

Desktop tests live in `shared/src/desktopTest/kotlin/com/bernaferrari/changedetection/...` and run with the working directory `shared/`. They use JUnit 4 and camelCase test names. The directory `shared/src/desktopTest/kotlin/com/bernaferrari/changedetection/ui/kit/` already exists.

## Commands you will need

| Purpose | Command | Expected |
| --- | --- | --- |
| New test | `./gradlew :shared:desktopTest --tests '*SnapshotThumbnailFetcherTest'` | `BUILD SUCCESSFUL` |
| Format check | `./gradlew :shared:spotlessCheck` | `BUILD SUCCESSFUL` |
| Auto-format | `./gradlew :shared:spotlessApply` | `BUILD SUCCESSFUL` |
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

**In scope:**

- New: `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/kit/SnapshotThumbnailFetcher.kt`
- New: `shared/src/desktopTest/kotlin/com/bernaferrari/changedetection/ui/kit/SnapshotThumbnailFetcherTest.kt`
- `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/kit/ChangePreviewViews.kt` (only `CaptureTop`, plus imports that become unused)
- `shared/build.gradle.kts`, **only** if `okio.Buffer` does not resolve (see Step 2)
- `plans/README.md` (status column only)

**Out of scope:**

- `di/Koin.kt` and the app-wide image loader. The fetcher is attached per request on purpose, so previews and tests that never call `initKoin` behave the same as the app.
- `SnapCrop` in `VersionSheet.kt` and the other `getSnapContent` callers in `ChangePreviewViews.kt` (line 348). They crop a region and have their own cache.
- The three callers of `CaptureTop`.
- Disk caching of thumbnails.

## Git workflow

- Branch from `main`: `git switch -c advisor/008-load-thumbnails-through-coil`
- Commit subjects are plain sentences. Suggested: `Cached thumbnails no longer re-read the full screenshot`.
- Do not push, merge, or open a pull request.

## Steps

### Step 1: Write the failing test

Create `shared/src/desktopTest/kotlin/com/bernaferrari/changedetection/ui/kit/SnapshotThumbnailFetcherTest.kt`:

```kotlin
package com.bernaferrari.changedetection.ui.kit

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.size.Dimension
import coil3.size.Size
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapshotThumbnailFetcherTest {
    private val png: ByteArray = ByteArrayOutputStream().also { ImageIO.write(BufferedImage(64, 256, BufferedImage.TYPE_INT_RGB), "png", it) }.toByteArray()

    private fun request(snapId: String, read: suspend (String) -> ByteArray?) = ImageRequest.Builder(PlatformContext.INSTANCE)
        .data(SnapshotThumbnail(snapId))
        .fetcherFactory(SnapshotThumbnailFetcher.Factory(read))
        .memoryCacheKey("top-$snapId-32")
        .size(Size(Dimension(32), Dimension.Undefined))
        .build()

    @Test fun aCachedThumbnailIsNotReadFromStorageAgain() = runBlocking {
        val loader = ImageLoader(PlatformContext.INSTANCE)
        val reads = mutableListOf<String>()
        val read: suspend (String) -> ByteArray? = { id ->
            reads += id
            png
        }
        try {
            assertTrue(loader.execute(request("snap-1", read)) is SuccessResult)
            assertTrue(loader.execute(request("snap-1", read)) is SuccessResult)
            assertEquals("the second request must come from the memory cache", listOf("snap-1"), reads)

            assertTrue(loader.execute(request("snap-2", read)) is SuccessResult)
            assertEquals(listOf("snap-1", "snap-2"), reads)
        } finally {
            loader.shutdown()
        }
    }

    @Test fun aSnapshotThatCannotBeReadIsAnErrorResultNotACrash() = runBlocking {
        val loader = ImageLoader(PlatformContext.INSTANCE)
        try {
            assertTrue(loader.execute(request("missing") { null }) is ErrorResult)
            assertTrue(loader.execute(request("broken") { error("file is gone") }) is ErrorResult)
        } finally {
            loader.shutdown()
        }
    }
}
```

**Verify:** `./gradlew :shared:desktopTest --tests '*SnapshotThumbnailFetcherTest'`
Expected: the build **fails to compile** (unresolved `SnapshotThumbnail`, `SnapshotThumbnailFetcher`). That is the expected red state.

### Step 2: Add the fetcher

Create `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/kit/SnapshotThumbnailFetcher.kt`:

```kotlin
package com.bernaferrari.changedetection.ui.kit

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import okio.Buffer

/** Names a saved screenshot to the image loader without holding its bytes. */
internal data class SnapshotThumbnail(val snapId: String)

/**
 * Reads a saved screenshot only when the image loader actually has to decode it. A thumbnail already in the
 * loader's memory cache is served without touching storage, and no composable keeps the full file in memory.
 */
internal class SnapshotThumbnailFetcher(
    private val thumbnail: SnapshotThumbnail,
    private val options: Options,
    private val read: suspend (snapId: String) -> ByteArray?,
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        val bytes = read(thumbnail.snapId) ?: return null
        return SourceFetchResult(
            source = ImageSource(source = Buffer().apply { write(bytes) }, fileSystem = options.fileSystem),
            mimeType = null,
            dataSource = DataSource.DISK,
        )
    }

    class Factory(private val read: suspend (snapId: String) -> ByteArray?) : Fetcher.Factory<SnapshotThumbnail> {
        override fun create(data: SnapshotThumbnail, options: Options, imageLoader: ImageLoader): Fetcher = SnapshotThumbnailFetcher(data, options, read)
    }
}
```

**Verify:** `./gradlew :shared:desktopTest --tests '*SnapshotThumbnailFetcherTest'`
Expected: `BUILD SUCCESSFUL`, two tests pass.

If compilation fails with "Unresolved reference: okio" (and only then), add `implementation(libs.okio)` to the `commonMain.dependencies { ... }` block in `shared/build.gradle.kts` (the `okio` entry already exists in `gradle/libs.versions.toml`, and the `wasmJsMain` block in the same file shows the exact line), then run the verify command again.

### Step 3: Use it in `CaptureTop`

In `ChangePreviewViews.kt`, replace the whole `CaptureTop` function (keep its KDoc) with:

```kotlin
@Composable
fun CaptureTop(snapId: String, modifier: Modifier, alpha: Float = 1f, decodeWidth: Int = 720) {
    val context = coil3.compose.LocalPlatformContext.current
    Box(modifier.background(androidx.compose.ui.graphics.Color.White)) {
        coil3.compose.AsyncImage(
            model = remember(snapId, decodeWidth) {
                coil3.request.ImageRequest.Builder(context)
                    .data(SnapshotThumbnail(snapId))
                    // The loader asks for the file only on a memory-cache miss; a cached thumbnail costs no read.
                    .fetcherFactory(SnapshotThumbnailFetcher.Factory { id -> optional<SnapsRepository>()?.getSnapContent(id) })
                    .memoryCacheKey("top-$snapId-$decodeWidth")
                    .size(coil3.size.Size(coil3.size.Dimension(decodeWidth), coil3.size.Dimension.Undefined))
                    .build()
            },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            alpha = alpha,
            modifier = Modifier.matchParentSize(),
        )
    }
}
```

`SnapsRepository` is already imported in this file (line 51). `produceState` is still used elsewhere in the file, so keep its import; if the compiler or Spotless reports an unused import, remove exactly the ones it names.

**Verify:** `rg -n "produceState<ByteArray" shared/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/kit/ChangePreviewViews.kt`
Expected: no line inside `CaptureTop` (other functions in the file may still match).

**Verify:** `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest`
Expected: `BUILD SUCCESSFUL`. Run `./gradlew :shared:spotlessApply` first if the format check fails. The desktop suite includes UI tests of the home and detail screens; they must stay green.

### Step 4: Other platforms and the release build

**Verify:** `./gradlew :webDemo:wasmJsBrowserDistribution`, and on macOS `./gradlew :shared:compileKotlinIosSimulatorArm64`.
Expected: `BUILD SUCCESSFUL`.

**Verify:** `./gradlew :androidApp:lintDebug :androidApp:assembleRelease`
Expected: `BUILD SUCCESSFUL` (this also proves code shrinking still accepts the new class).

### Step 5 (optional, only with a device or emulator): look at it

`./gradlew :androidApp:installDebug`, open the app with at least one screenshot-mode page that has a saved version, and confirm that the home list, the page detail header and the version list still show thumbnails. Report what you saw. If no device is available, say so.

## Test plan

- `SnapshotThumbnailFetcherTest.aCachedThumbnailIsNotReadFromStorageAgain`: two requests for the same snapshot cause one read; a different snapshot causes a second read.
- `SnapshotThumbnailFetcherTest.aSnapshotThatCannotBeReadIsAnErrorResultNotACrash`: a missing or throwing read gives an error result.
- Existing desktop UI tests that render the home list and page detail must stay green.

## Done criteria

- [ ] `rg -n "class SnapshotThumbnailFetcher|data class SnapshotThumbnail" shared/src/commonMain` prints two lines.
- [ ] `rg -n "fetcherFactory" shared/src/commonMain` prints one line, in `CaptureTop`.
- [ ] `./gradlew :shared:desktopTest --tests '*SnapshotThumbnailFetcherTest'` passes with two tests.
- [ ] `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest` is `BUILD SUCCESSFUL`.
- [ ] `./gradlew :androidApp:lintDebug :androidApp:assembleRelease` is `BUILD SUCCESSFUL`.
- [ ] `git status --short` lists only in-scope files.

## STOP conditions

- `aCachedThumbnailIsNotReadFromStorageAgain` shows two reads for the same snapshot after Step 2. That would mean Coil does not consult its memory cache before fetching for this kind of request, and the plan's premise is wrong. Report the test output.
- `fetcherFactory`, `SourceFetchResult`, `ImageSource` or `PlatformContext.INSTANCE` does not exist with the shape used here. Report the compiler error; do not switch to registering the fetcher in `Koin.kt` without asking.
- A desktop UI test that checks a thumbnail fails after Step 3.
- Thumbnails are blank on a device in Step 5.

## Maintenance notes

- The cache key `top-<snapId>-<width>` is unchanged. Snapshot files are never rewritten under the same id, so the key cannot go stale.
- `SnapCrop` (`VersionSheet.kt:267`) and the diff previews still read full files, because they crop a region before decoding. If they show up in profiling, they need a different approach (a region decoder), not this fetcher.
- If thumbnails are ever needed outside Compose (widgets, notifications), reuse `SnapshotThumbnail` with the same fetcher factory.
- Reviewers: check that no new code passes a screenshot `ByteArray` as an image model in a list. That pattern is what this plan removes.
