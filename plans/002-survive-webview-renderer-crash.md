# Plan 002: Keep the app alive when a web page crashes the browser renderer

> **Executor instructions**
> You are implementing one self-contained change. You have not seen the audit that produced this plan; everything you need is in this file.
> 1. Run the drift check first: `git diff --stat a9f8f8ba..HEAD -- shared/src/androidMain/kotlin/com/bernaferrari/changedetection/browser/BrowserRuntimeHost.kt shared/src/commonMain/kotlin/com/bernaferrari/changedetection/browser/BrowserRuntimeHost.kt shared/src/commonMain/kotlin/com/bernaferrari/changedetection/addedit/WatchBrowser.kt`. If it prints anything, compare the "Current state" excerpts with the real files. If they no longer match, STOP and report.
> 2. Follow the steps in order and run each **Verify** command.
> 3. Touch only the files listed under "In scope".
> 4. If a STOP condition applies, stop and report. Do not improvise.
> 5. When finished, set this plan's row in `plans/README.md` to `DONE`.

## Status

| Field | Value |
| --- | --- |
| Priority | P1 |
| Effort | S to M (2 to 3 hours) |
| Risk of the fix | Low to medium. Adds one callback and a retry state in two composables |
| Depends on | Nothing |
| Category | Bug (crash) |
| Planned at | commit `a9f8f8ba`, 2026-11-09 |

## Why this matters

On Android, web pages are drawn by a separate "renderer" process. A heavy or broken page can make that process crash or be killed for memory. When that happens Android calls `WebViewClient.onRenderProcessGone`. If the app does not override it and return `true`, **Android kills the whole app**.

This app shows arbitrary web pages in three places: the in-app browser used to add a page, the saved-page viewer, and the area picker. None of them handle a renderer crash. A user who opens a heavy page to watch it can lose the app mid-flow. A Turbo session (a foreground service in the same process) dies with it.

The background capture path already handles this correctly, so the app has an example to follow.

## Current state

### The handled exemplar (do not change it)

`shared/src/androidMain/kotlin/com/bernaferrari/changedetection/browser/AndroidPageCapture.kt:494-500`:

```kotlin
            override fun onRenderProcessGone(webView: WebView, detail: RenderProcessGoneDetail): Boolean {
                trace?.callback(DiagnosticStage.NAVIGATE, DiagnosticMetric.RENDERER_CRASH, if (detail.didCrash()) 1 else 0)
                val error = IOException("The browser renderer stopped. Try the page again.")
                rendererFailure = error
                loaded.completeExceptionally(error)
                return true
            }
```

### The unhandled client

`shared/src/androidMain/kotlin/com/bernaferrari/changedetection/browser/BrowserRuntimeHost.kt:23-83` defines `rememberBrowserPlatformParams()`. It builds one `AccompanistWebViewClient` subclass that overrides `shouldInterceptRequest`, `onPageCommitVisible`, `onPageStarted`, `onPageFinished`, `onReceivedError`, `onReceivedHttpError` and `onReceivedSslError`. It has **no** `onRenderProcessGone`. The library base class (`com.multiplatform.webview.web.AccompanistWebViewClient`, version 2.0.3) does not override it either.

The file uses fully-qualified names inline instead of imports. The existing pattern for reporting a failure is:

```kotlin
            private fun fail(url: String, error: com.multiplatform.webview.web.WebViewError) {
                pendingErrors.add(url to error)
                state.errorsForCurrentRequest.add(error)
            }
```

`state.errorsForCurrentRequest` is a Compose `SnapshotStateList<WebViewError>`. `WebViewError` is `data class WebViewError(val code: Int, val description: String, val isFromMainFrame: Boolean)`.

### Where the client is used

1. `PlatformBrowserView` in the same Android file, lines 9-19:

```kotlin
@Composable
internal actual fun PlatformBrowserView(state: com.multiplatform.webview.web.WebViewState, modifier: Modifier, onNavigationRequest: ((String) -> Boolean)?, highlightUpdate: com.bernaferrari.changedetection.detection.RegionHighlightUpdate?) {
    val interceptor = rememberNavigationInterceptor(onNavigationRequest)
    val navigator = com.multiplatform.webview.web.rememberWebViewNavigator(requestInterceptor = interceptor)
    com.multiplatform.webview.web.WebView(state = state, modifier = modifier, navigator = navigator, platformWebViewParams = rememberBrowserPlatformParams())
    androidx.compose.runtime.LaunchedEffect(state.loadingState, highlightUpdate) {
        if (state.loadingState is com.multiplatform.webview.web.LoadingState.Finished) {
            highlightUpdate?.let { navigator.evaluateJavaScript(it.script()) }
        }
    }
}
```

   `PlatformBrowserView` is called from `SecureSnapshotWebView.kt:29` (saved-page viewer), `RenderedRegionPreview.kt:27` (area picker) and `LocalBrowserScreen.kt:248` (`LiveBrowserPage`).

2. `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/addedit/WatchBrowser.kt:252` uses the library `WebView` directly:

```kotlin
                BrowserRuntimeHost(Modifier.fillMaxSize()) {
                    WebView(state = state, modifier = Modifier.fillMaxSize().testTag("watch-browser-content").then(com.bernaferrari.changedetection.browser.watchCaptureViewportModifier()), navigator = navigator, webViewJsBridge = bridge, onCreated = { _ -> }, factory = ::createWatchWebView, platformWebViewParams = com.bernaferrari.changedetection.browser.rememberBrowserPlatformParams())
                }
                if (loadFailed) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                        com.bernaferrari.changedetection.ui.components.SharedEmptyState(
                            icon = Icons.Rounded.Refresh,
                            title = stringResource(Res.string.local_browser_error_title),
                            description = stringResource(Res.string.browser_load_error_body),
                            primaryActionLabel = stringResource(Res.string.retry),
                            onPrimaryAction = { navigator.reload() },
                        )
                    }
                }
```

   Earlier in the same function (`WatchBrowser.kt:175`): `val loadFailed = state.errorsForCurrentRequest.any { it.isFromMainFrame }`.

3. `LiveBrowserPage` in `LocalBrowserScreen.kt:241-262` already shows a Retry surface when `browser.errorsForCurrentRequest.any { it.isFromMainFrame }`, and its Retry re-creates everything with `key(url, attempt)`. It needs no change: once the crash is reported as a main-frame error, its existing surface appears.

### Platform rules you must respect

- After `onRenderProcessGone` returns `true`, that `WebView` instance is dead. It must leave the view hierarchy and must not be used again. A new `WebView` must be created to show the page again.
- The library re-loads `state.content` into any newly created native WebView (`WebView.kt:90-91` in the library collects `snapshotFlow { state.content }`), so re-creating the composable under a new `key` is enough to reload.
- Strings `local_browser_error_title` ("Couldn't open this page"), `browser_load_error_body` and `retry` already exist in every language. Do not add strings.

## Commands you will need

| Purpose | Command | Expected |
| --- | --- | --- |
| Compile Android + run host tests | `./gradlew :shared:testAndroidHostTest` | `BUILD SUCCESSFUL` |
| Format check | `./gradlew :shared:spotlessCheck` | `BUILD SUCCESSFUL` |
| Auto-format | `./gradlew :shared:spotlessApply` | `BUILD SUCCESSFUL` |
| Desktop tests (common code changed) | `./gradlew :shared:desktopTest` | `BUILD SUCCESSFUL` |
| Compile the device tests (no emulator needed) | `./gradlew :shared:assembleAndroidDeviceTest` | `BUILD SUCCESSFUL` |
| Run one device test (emulator required) | `./gradlew :shared:connectedAndroidDeviceTest -Pandroid.testInstrumentationRunnerArguments.class=com.bernaferrari.changedetection.browser.RendererCrashTest` | `BUILD SUCCESSFUL` |
| Android lint and release build | `./gradlew :androidApp:lintDebug :androidApp:assembleRelease` | `BUILD SUCCESSFUL` |
| iOS still compiles (macOS only) | `./gradlew :shared:compileKotlinIosSimulatorArm64` | `BUILD SUCCESSFUL` |

The build needs JDK 21 and Android SDK platform 37.2.

## Before you start: the state of the build

Checked on 2026-11-09 at commit `a9f8f8ba`. Read this before the steps; it overrides any line below that expects otherwise.

1. **Plan 000 must be done first.** Before you change anything, run `./gradlew :shared:spotlessCheck`. If it fails, Plan 000 (`plans/000-make-the-gates-green.md`) has not landed: STOP and report that. Do not run `spotlessApply` to get past it; on an unprepared tree it rewrites about 36 unrelated files. Once Plan 000 has landed, the test and format commands in this plan behave as described, and `spotlessApply` only touches what you changed.
2. **Two commands fail for reasons no plan fixes.** `./gradlew :shared:compileKotlinIosSimulatorArm64` stops with five errors, all in `shared/src/iosMain/kotlin/com/bernaferrari/changedetection/browser/IosPageCapture.kt` (the first is `Unresolved reference 'kmpwright'`; a library was built with a newer Kotlin than this project uses). `./gradlew :webDemo:wasmJsBrowserDistribution` compiles the Kotlin and then stops at `:kotlinWasmStoreYarnLock` with `Lock file was changed`. Wherever this plan expects one of them to print `BUILD SUCCESSFUL`, the bar is instead: for iOS, **those same five errors and no others**; for the web demo, run `./gradlew :webDemo:compileKotlinWasmJs` and expect `BUILD SUCCESSFUL`. Never run `kotlinWasmUpgradeYarnLock`. If either command has since been repaired and passes, that is fine.
3. **Line numbers may have moved.** Two changes landed after this plan was written. Plan 000 re-formats about 36 files (layout only). The "failure experience" change (`plans/failure-experience-changes.md`) added code to `SyncEngine.kt`, `SitesDao.kt`, `Site.kt`, `CheckFailure.kt`, `DiagnosticTrace.kt`, `PageDetailScreen.kt`, `MainViewModel.kt`, `LegacyMigration.kt`, `DatabaseFactory.kt`, `AppContent.kt`, `AndroidChangeNotifier.kt`, the four `PlatformDiModule` files and the string files. Find code by the text this plan quotes, not by line number. A quoted block that differs only in line breaks or indentation is the same code. The drift check at the top of this plan will list those files as changed; that alone is not a reason to stop. Stop only if the quoted code is gone or does something different.

## Scope

**In scope:**

- `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/browser/BrowserRuntimeHost.kt` (add one constant)
- `shared/src/androidMain/kotlin/com/bernaferrari/changedetection/browser/BrowserRuntimeHost.kt`
- `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/addedit/WatchBrowser.kt`
- New file: `shared/src/androidInstrumentedTest/kotlin/com/bernaferrari/changedetection/browser/RendererCrashTest.kt`
- `plans/README.md` (status column only)

**Out of scope:**

- `AndroidPageCapture.kt` (already correct).
- The desktop, iOS and wasm `BrowserRuntimeHost.kt` files. Do not change the `expect` signatures of `PlatformBrowserView` or `rememberBrowserPlatformParams`.
- `LocalBrowserScreen.kt`, `SecureSnapshotWebView.kt`, `RenderedRegionPreview.kt`.
- Any string resource file.

## Git workflow

- Branch from `main`: `git switch -c advisor/002-survive-webview-renderer-crash`
- Commit subjects are plain sentences. Suggested: `A crashed page renderer shows Retry instead of closing the app`.
- Do not push, merge, or open a pull request.

## Steps

### Step 1: Add a shared error code

In `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/browser/BrowserRuntimeHost.kt`, add at the end of the file:

```kotlin
/**
 * Error code reported in `WebViewState.errorsForCurrentRequest` when the platform renderer process died.
 * The browser view that reported it is unusable and must be replaced, not reloaded.
 */
internal const val BROWSER_RENDERER_GONE = -1000
```

**Verify:** `rg -n "BROWSER_RENDERER_GONE" shared/src/commonMain` prints one line.

### Step 2: Handle the crash in the Android client

In `shared/src/androidMain/kotlin/com/bernaferrari/changedetection/browser/BrowserRuntimeHost.kt`, inside the anonymous `AccompanistWebViewClient` object, add this override directly after `onReceivedSslError` (which ends at line 81). Match the file's style: fully-qualified names, no new imports.

```kotlin

            override fun onRenderProcessGone(view: android.webkit.WebView, detail: android.webkit.RenderProcessGoneDetail): Boolean {
                // Returning true keeps the app alive. This view is now unusable; the composable replaces it on Retry.
                pendingErrors.clear()
                state.errorsForCurrentRequest.add(com.multiplatform.webview.web.WebViewError(BROWSER_RENDERER_GONE, "The browser renderer stopped", true))
                return true
            }
```

**Verify:** `./gradlew :shared:testAndroidHostTest` (this compiles the Android source set).
Expected: `BUILD SUCCESSFUL`.

### Step 3: Replace the dead view in `PlatformBrowserView` (Android)

In the same Android file, replace the body of `PlatformBrowserView` (lines 10-19) with:

```kotlin
internal actual fun PlatformBrowserView(state: com.multiplatform.webview.web.WebViewState, modifier: Modifier, onNavigationRequest: ((String) -> Boolean)?, highlightUpdate: com.bernaferrari.changedetection.detection.RegionHighlightUpdate?) {
    val interceptor = rememberNavigationInterceptor(onNavigationRequest)
    val navigator = com.multiplatform.webview.web.rememberWebViewNavigator(requestInterceptor = interceptor)
    var generation by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    if (state.errorsForCurrentRequest.any { it.code == BROWSER_RENDERER_GONE }) {
        // The renderer process died. Its view must leave the hierarchy; Retry builds a new one, which reloads the content.
        com.bernaferrari.changedetection.ui.components.SharedEmptyState(
            icon = androidx.compose.material.icons.Icons.Rounded.Refresh,
            title = com.bernaferrari.changedetection.localization.stringResource(com.bernaferrari.changedetection.shared.generated.resources.Res.string.local_browser_error_title),
            description = com.bernaferrari.changedetection.localization.stringResource(com.bernaferrari.changedetection.shared.generated.resources.Res.string.browser_load_error_body),
            modifier = modifier,
            primaryActionLabel = com.bernaferrari.changedetection.localization.stringResource(com.bernaferrari.changedetection.shared.generated.resources.Res.string.retry),
            onPrimaryAction = {
                state.errorsForCurrentRequest.removeAll { it.code == BROWSER_RENDERER_GONE }
                generation++
            },
        )
        return
    }
    androidx.compose.runtime.key(generation) {
        com.multiplatform.webview.web.WebView(state = state, modifier = modifier, navigator = navigator, platformWebViewParams = rememberBrowserPlatformParams())
    }
    androidx.compose.runtime.LaunchedEffect(state.loadingState, highlightUpdate) {
        if (state.loadingState is com.multiplatform.webview.web.LoadingState.Finished) {
            highlightUpdate?.let { navigator.evaluateJavaScript(it.script()) }
        }
    }
}
```

Add these two imports at the top of the Android file (they are needed for the `by` delegate): `import androidx.compose.runtime.getValue` and `import androidx.compose.runtime.setValue`.

Check that the generated resource names resolve: `rg -n "import com.bernaferrari.changedetection.shared.generated.resources" shared/src/commonMain/kotlin/com/bernaferrari/changedetection/addedit/WatchBrowser.kt` shows how other files import `Res` and the string accessors. If the fully-qualified form above does not compile, use the same import style as `WatchBrowser.kt` instead.

**Verify:** `./gradlew :shared:testAndroidHostTest`
Expected: `BUILD SUCCESSFUL`.

### Step 4: Replace the dead view in `WatchBrowser`

In `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/addedit/WatchBrowser.kt`:

1. Next to `val loadFailed = ...` (line 175), add:

```kotlin
    val rendererGone = state.errorsForCurrentRequest.any { it.code == com.bernaferrari.changedetection.browser.BROWSER_RENDERER_GONE }
    var browserGeneration by remember { mutableIntStateOf(0) }
```

2. Wrap the `WebView(...)` call at line 252 so it is not composed while the renderer is gone, and is re-created on retry. Keep the `WebView(...)` arguments exactly as they are:

```kotlin
                BrowserRuntimeHost(Modifier.fillMaxSize()) {
                    if (!rendererGone) {
                        key(browserGeneration) {
                            WebView(/* unchanged arguments */)
                        }
                    }
                }
```

3. Change the Retry action of the `loadFailed` surface from `onPrimaryAction = { navigator.reload() }` to:

```kotlin
                            onPrimaryAction = {
                                if (rendererGone) {
                                    // A dead renderer cannot reload; drop its error and build a new browser view.
                                    state.errorsForCurrentRequest.removeAll { it.code == com.bernaferrari.changedetection.browser.BROWSER_RENDERER_GONE }
                                    browserGeneration++
                                } else {
                                    navigator.reload()
                                }
                            },
```

`key`, `remember` and `mutableIntStateOf` come from `androidx.compose.runtime`. Check the import block at the top of `WatchBrowser.kt`; add `import androidx.compose.runtime.key` and `import androidx.compose.runtime.mutableIntStateOf` if they are missing (the file imports individual names, not a wildcard, for that package; confirm with `rg -n "^import androidx.compose.runtime" shared/src/commonMain/kotlin/com/bernaferrari/changedetection/addedit/WatchBrowser.kt`).

`BROWSER_RENDERER_GONE` is only ever reported on Android, so on desktop, iOS and web `rendererGone` is always false and behaviour is unchanged.

**Verify:** `./gradlew :shared:testAndroidHostTest :shared:desktopTest`
Expected: `BUILD SUCCESSFUL`.

### Step 5: Add a device test

Create `shared/src/androidInstrumentedTest/kotlin/com/bernaferrari/changedetection/browser/RendererCrashTest.kt`. Model the class setup on `shared/src/androidInstrumentedTest/kotlin/com/bernaferrari/changedetection/sync/RefinementUiTest.kt` (it uses `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule` with `androidx.activity.ComponentActivity` and wraps content in `ChangeDetectionTheme(false, false) { ... }`). Android documents that loading `chrome://crash` crashes the renderer for testing.

```kotlin
package com.bernaferrari.changedetection.browser

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import com.multiplatform.webview.web.rememberWebViewState
import org.junit.Rule
import org.junit.Test

/** A page that kills its renderer must leave the app running and offer Retry. */
class RendererCrashTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun awaitRetry() = compose.waitUntil(20_000) { compose.onAllNodesWithText("Retry").fetchSemanticsNodes().isNotEmpty() }

    @Test fun aCrashedRendererOffersRetryInsteadOfClosingTheApp() {
        compose.setContent {
            ChangeDetectionTheme(false, false) {
                PlatformBrowserView(state = rememberWebViewState("chrome://crash"), modifier = Modifier.fillMaxSize())
            }
        }
        awaitRetry()
        // Retry builds a new browser view, which loads the same crashing address again.
        compose.onNodeWithText("Retry").performClick()
        // Seeing Retry a second time proves the process survived both crashes and the view was really replaced.
        awaitRetry()
    }
}
```

The test runs in English because the emulator's default locale is English and the label is the existing `retry` string ("Retry").

**Verify (always):** `./gradlew :shared:assembleAndroidDeviceTest`
Expected: `BUILD SUCCESSFUL`.

**Verify (only if `adb devices` lists an emulator):** run the single device test from the commands table.
Expected: `BUILD SUCCESSFUL`. If no emulator is available, say so in your report; do not skip the compile check above.

### Step 6: Final gates

**Verify:** `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest` then `./gradlew :androidApp:lintDebug :androidApp:assembleRelease`
Expected: both `BUILD SUCCESSFUL`. Run `./gradlew :shared:spotlessApply` first if the format check fails.

## Test plan

- New device test `RendererCrashTest.aCrashedRendererOffersRetryInsteadOfClosingTheApp` (Step 5).
- No host-JVM test is possible: the callback is only invoked by the Android WebView runtime.
- Existing device tests that exercise the browser (`LiveBrowserWatchJourneyTest`, `WebViewLifecycleTest`) must still compile.

## Done criteria

- [ ] `rg -n "onRenderProcessGone" shared/src/androidMain` prints two matches: `AndroidPageCapture.kt` and `BrowserRuntimeHost.kt`.
- [ ] `rg -n "BROWSER_RENDERER_GONE" shared/src` shows the constant in commonMain and uses in Android `BrowserRuntimeHost.kt` and `WatchBrowser.kt`.
- [ ] `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest` is `BUILD SUCCESSFUL`.
- [ ] `./gradlew :shared:assembleAndroidDeviceTest` is `BUILD SUCCESSFUL`.
- [ ] `./gradlew :androidApp:lintDebug :androidApp:assembleRelease` is `BUILD SUCCESSFUL`.
- [ ] If an emulator was available: `RendererCrashTest` passed. If not: stated in the report.
- [ ] `git status --short` lists only in-scope files.

## STOP conditions

- The Android `PlatformBrowserView` or the anonymous client no longer looks like the excerpts (for example the client was already extracted into a named class).
- `RendererCrashTest` fails on an emulator because the process still dies. Report the logcat line containing `Render process` or `onRenderProcessGone`; do not weaken the test.
- `RendererCrashTest` fails because `chrome://crash` never triggers the callback on the emulator image (the wait times out with the page blank). Report it; the production change may still be correct, and the maintainer will decide how to test it.
- You find yourself needing to change an `expect` declaration or any non-Android `actual`.

## Maintenance notes

- Any new `WebViewClient` the app creates for a user-visible WebView must override `onRenderProcessGone` and return `true`. Reviewers should search for `WebViewClient(` in new Android code.
- The library never calls `destroy()` on a released WebView; this plan does not change that. If WebView memory after crashes becomes a concern, `destroySafely` in `AndroidWebViewTools.kt` is the existing helper.
- `BROWSER_RENDERER_GONE` is a private convention between the Android client and the composables. If the WebView library is replaced or upgraded so that `errorsForCurrentRequest` is cleared differently, revisit Steps 3 and 4.
