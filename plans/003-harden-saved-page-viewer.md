# Plan 003: Make the saved-page viewer refuse navigation and stop re-emitting unchecked style text

> **Executor instructions**
> You are implementing one self-contained change. You have not seen the audit that produced this plan; everything you need is in this file.
> 1. Run the drift check first: `git diff --stat a9f8f8ba..HEAD -- shared/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/comparison/SecureSnapshotWebView.kt shared/src/androidUnitTest/kotlin/com/bernaferrari/changedetection/ui/comparison/SecureSnapshotDocumentTest.kt`. If it prints anything, compare the "Current state" excerpts with the real files. If they no longer match, STOP and report.
> 2. Follow the steps in order and run each **Verify** command.
> 3. Touch only the files listed under "In scope".
> 4. If a STOP condition applies, stop and report. Do not improvise.
> 5. When finished, set this plan's row in `plans/README.md` to `DONE`.

## Status

| Field | Value |
| --- | --- |
| Priority | P1 |
| Effort | S (1 to 2 hours) |
| Risk of the fix | Low. Two small guards plus tests |
| Depends on | Nothing |
| Category | Security (defence in depth) |
| Planned at | commit `a9f8f8ba`, 2026-11-09 |

## Why this matters

The app saves copies of web pages and later shows them in a "saved page" viewer. Saved pages are untrusted input: whoever controls a watched website controls that HTML. The viewer is meant to show the copy as an inert historical document that never talks to the network and never becomes a live page.

It does this well already (scripts, forms, links and remote resources are stripped; a strict Content-Security-Policy is added; JavaScript is turned off). Two gaps remain:

1. **Nothing blocks navigation.** The viewer does not pass a navigation callback, and in this codebase "no callback" means "allow every navigation". The only backstop is the CSP directive `navigate-to 'none'`, which browsers do not implement. If any navigation trigger survives sanitising (for example a `<meta http-equiv="refresh">` that slips through), the "inert" viewer loads a live remote page.
2. **Style text is copied back in raw.** The sanitiser reads the text of each `<style>` element, checks it against a small deny-list, and writes it back using a raw data node, which is not HTML-escaped. If the HTML parser ever hands back style text containing `</style>` followed by markup (this can happen with `<style>` inside `<svg>`, where the content is parsed as markup or CDATA rather than raw text), that markup would be written into the "safe" document as live HTML.

Whether gap 2 is reachable with the current parser version was **not** confirmed by execution during the audit. That is why this plan starts with tests that find out. Both fixes are worth making either way, because they remove the dependency on parser details.

## Current state

File: `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/comparison/SecureSnapshotWebView.kt`.

Lines 19-31 (the composable):

```kotlin
@Composable
internal fun SecureSnapshotWebView(
    html: String,
    modifier: Modifier = Modifier,
) {
    val document = remember(html) { secureSnapshotDocument(html) }
    val state = rememberWebViewStateWithHTMLData(data = document)
    state.applySecureSnapshotSettings()

    BrowserRuntimeHost(modifier) {
        PlatformBrowserView(state = state, modifier = modifier)
    }
}
```

`PlatformBrowserView` has an optional parameter `onNavigationRequest: ((String) -> Boolean)? = null`. Returning `true` allows a navigation, `false` rejects it. `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/browser/NavigationInterceptor.kt:11-12` returns a null interceptor when the callback is null, which allows everything.

The area picker already blocks navigation for its own inert document. `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/addedit/RenderedRegionPreview.kt:36` ends its callback with:

```kotlin
            url == "about:blank" || url.startsWith("data:text/html")
```

Those two forms are what the platforms report for the initial load of in-memory HTML (Android and iOS report `about:blank`; desktop loads a `data:text/html` URL). Reuse the same rule.

Lines 60-73 (style collection inside `secureSnapshotDocument`):

```kotlin
internal fun secureSnapshotDocument(html: String): String = runCatching {
    val document = Ksoup.parse(html)
    // Keep self-contained typography/layout. Network-dependent or escaped CSS is
    // omitted conservatively; the deny-by-default CSP remains the security boundary.
    var styleBudget = MAX_SNAPSHOT_STYLE_CHARS
    val styles = document.select("style").take(MAX_SNAPSHOT_STYLES).mapNotNull { style ->
        val css = style.data()
        if (style.attr("type").let { it.isNotBlank() && !it.equals("text/css", ignoreCase = true) } || css.length > styleBudget || UNSAFE_CSS.containsMatchIn(css) || '\\' in css) {
            null
        } else {
            styleBudget -= css.length
            css to style.attr("media")
        }
    }
```

Lines 103-108 (style re-emission):

```kotlin
    styles.forEach { (css, media) ->
        rendered.head().appendElement("style").apply {
            if (media.isNotBlank()) attr("media", media)
            appendChild(com.fleeksoft.ksoup.nodes.DataNode(css))
        }
    }
```

Lines 171-174:

```kotlin
private val UNSAFE_CSS = Regex(
    pattern = "url\\s*\\(|@import|expression\\s*\\(|-moz-binding",
    option = RegexOption.IGNORE_CASE,
)
```

Existing tests: `shared/src/androidUnitTest/kotlin/com/bernaferrari/changedetection/ui/comparison/SecureSnapshotDocumentTest.kt` (four tests, JUnit 4, backtick test names, `org.junit.Assert.assertTrue/assertFalse`). One existing test, `stored snapshot preserves self contained layout and body identity`, asserts that the selector `.schedule > h1` survives. Your change must keep that test green, so you may not reject the `>` character.

`secureSnapshotDocument` is also called by `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/detection/RenderedSelectionDocument.kt:94`, so the hardening protects the area picker too.

## Commands you will need

| Purpose | Command | Expected |
| --- | --- | --- |
| Run the one test class | `./gradlew :shared:testAndroidHostTest --tests '*SecureSnapshotDocumentTest'` | `BUILD SUCCESSFUL` |
| Format check | `./gradlew :shared:spotlessCheck` | `BUILD SUCCESSFUL` |
| Auto-format | `./gradlew :shared:spotlessApply` | `BUILD SUCCESSFUL` |
| Full gate | `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest` | `BUILD SUCCESSFUL` |
| iOS still compiles (macOS only) | `./gradlew :shared:compileKotlinIosSimulatorArm64` | `BUILD SUCCESSFUL` |
| Web demo still compiles | `./gradlew :webDemo:wasmJsBrowserDistribution` | `BUILD SUCCESSFUL` |

The build needs JDK 21 and Android SDK platform 37.2.

## Before you start: the state of the build

Checked on 2026-11-09 at commit `a9f8f8ba`. Read this before the steps; it overrides any line below that expects otherwise.

1. **Plan 000 must be done first.** Before you change anything, run `./gradlew :shared:spotlessCheck`. If it fails, Plan 000 (`plans/000-make-the-gates-green.md`) has not landed: STOP and report that. Do not run `spotlessApply` to get past it; on an unprepared tree it rewrites about 36 unrelated files. Once Plan 000 has landed, the test and format commands in this plan behave as described, and `spotlessApply` only touches what you changed.
2. **Two commands fail for reasons no plan fixes.** `./gradlew :shared:compileKotlinIosSimulatorArm64` stops with five errors, all in `shared/src/iosMain/kotlin/com/bernaferrari/changedetection/browser/IosPageCapture.kt` (the first is `Unresolved reference 'kmpwright'`; a library was built with a newer Kotlin than this project uses). `./gradlew :webDemo:wasmJsBrowserDistribution` compiles the Kotlin and then stops at `:kotlinWasmStoreYarnLock` with `Lock file was changed`. Wherever this plan expects one of them to print `BUILD SUCCESSFUL`, the bar is instead: for iOS, **those same five errors and no others**; for the web demo, run `./gradlew :webDemo:compileKotlinWasmJs` and expect `BUILD SUCCESSFUL`. Never run `kotlinWasmUpgradeYarnLock`. If either command has since been repaired and passes, that is fine.
3. **Line numbers may have moved.** Two changes landed after this plan was written. Plan 000 re-formats about 36 files (layout only). The "failure experience" change (`plans/failure-experience-changes.md`) added code to `SyncEngine.kt`, `SitesDao.kt`, `Site.kt`, `CheckFailure.kt`, `DiagnosticTrace.kt`, `PageDetailScreen.kt`, `MainViewModel.kt`, `LegacyMigration.kt`, `DatabaseFactory.kt`, `AppContent.kt`, `AndroidChangeNotifier.kt`, the four `PlatformDiModule` files and the string files. Find code by the text this plan quotes, not by line number. A quoted block that differs only in line breaks or indentation is the same code. The drift check at the top of this plan will list those files as changed; that alone is not a reason to stop. Stop only if the quoted code is gone or does something different.

## Scope

**In scope:**

- `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/comparison/SecureSnapshotWebView.kt`
- `shared/src/androidUnitTest/kotlin/com/bernaferrari/changedetection/ui/comparison/SecureSnapshotDocumentTest.kt`
- `plans/README.md` (status column only)

**Out of scope:**

- `NavigationInterceptor.kt`, every `BrowserRuntimeHost.kt`, `RenderedRegionPreview.kt`, `RenderedSelectionDocument.kt`.
- The CSP string, `UNSAFE_ELEMENTS`, `NAVIGABLE_ATTRIBUTES`. Do not loosen or reorder them.
- Desktop JavaScript settings (`DesktopBrowserSession.kt`). See maintenance notes.

## Git workflow

- Branch from `main`: `git switch -c advisor/003-harden-saved-page-viewer`
- Commit subjects are plain sentences. Suggested: `Saved pages cannot navigate or smuggle markup through style text`.
- Do not push, merge, or open a pull request.

## Steps

### Step 1: Add characterization tests

Add these tests to `SecureSnapshotDocumentTest.kt`, after the last existing test. They record what the sanitiser does today with markup hidden in style text.

```kotlin
    @Test
    fun `style text can never carry markup into the safe document`() {
        val payloads = listOf(
            // Style inside SVG is parsed as markup or CDATA, not raw text.
            """<svg><style><![CDATA[</style><meta http-equiv="refresh" content="0;url=//example.test/a"><a id="smuggled" href="//example.test/b">go</a>]]></style></svg><p>Readable</p>""",
            """<svg><style>.a{color:red}</style><img id="smuggled" src="x"></style></svg><p>Readable</p>""",
            """<math><style><![CDATA[</style><a id="smuggled" href="//example.test/c">go</a>]]></style></math><p>Readable</p>""",
            """<style>.a{color:red}/*<![CDATA[*/</style><a id="smuggled" href="//example.test/d">go</a>/*]]>*/</style><p>Readable</p>""",
        )
        for (payload in payloads) {
            val result = secureSnapshotDocument(payload)
            val reparsed = com.fleeksoft.ksoup.Ksoup.parse(result)
            assertTrue(payload, result.contains("Readable"))
            assertTrue(payload, reparsed.select("#smuggled[href], #smuggled[src]").isEmpty())
            assertTrue(payload, reparsed.select("meta[http-equiv=refresh]").isEmpty())
            assertFalse(payload, result.contains("example.test"))
            // Whatever is kept as style text must not contain a tag opener.
            assertTrue(payload, reparsed.select("style").none { '<' in it.data() })
        }
    }

    @Test
    fun `only the inert document itself may load in the viewer`() {
        assertTrue(isInertSnapshotNavigation("about:blank"))
        assertTrue(isInertSnapshotNavigation("data:text/html;charset=utf-8;base64,PGh0bWw+"))
        assertFalse(isInertSnapshotNavigation("https://example.test/"))
        assertFalse(isInertSnapshotNavigation("http://example.test/"))
        assertFalse(isInertSnapshotNavigation("file:///data/data/private"))
        assertFalse(isInertSnapshotNavigation("intent://example.test#Intent;end"))
        assertFalse(isInertSnapshotNavigation("javascript:alert(1)"))
        assertFalse(isInertSnapshotNavigation("data:image/png;base64,AAAA"))
    }
```

**Verify:** `./gradlew :shared:testAndroidHostTest --tests '*SecureSnapshotDocumentTest'`
Expected: the build **fails to compile**, because `isInertSnapshotNavigation` does not exist yet. That is expected.

Temporarily comment out the second test, run again, and **write down in your final report** whether `style text can never carry markup into the safe document` passes or fails before the fix, and for which payload. Either outcome is fine; the report tells the maintainer whether the gap was reachable. Then un-comment the second test.

### Step 2: Reject style text that contains a tag opener

In `SecureSnapshotWebView.kt`, extend the condition at line 67 with one more clause so that it reads:

```kotlin
        if (style.attr("type").let { it.isNotBlank() && !it.equals("text/css", ignoreCase = true) } || css.length > styleBudget || UNSAFE_CSS.containsMatchIn(css) || '\\' in css || '<' in css) {
```

and update the comment on lines 62-63 to:

```kotlin
    // Keep self-contained typography/layout. Network-dependent or escaped CSS is omitted conservatively,
    // and so is any style text containing "<": it is written back unescaped, so it must never hold markup.
    // The deny-by-default CSP remains the security boundary.
```

Real CSS almost never contains `<`. A stylesheet that does is dropped as a whole; the page text stays readable, which matches how the existing deny-list behaves.

**Verify:** `./gradlew :shared:testAndroidHostTest --tests '*SecureSnapshotDocumentTest'` still fails to compile only because of `isInertSnapshotNavigation`.

### Step 3: Block navigation in the viewer

In `SecureSnapshotWebView.kt`:

1. Add this function directly above `private fun WebViewState.applySecureSnapshotSettings()` (line 33):

```kotlin
/**
 * The viewer may only load its own in-memory document: Android and iOS report that load as "about:blank",
 * desktop as a data URL. Everything else (links, redirects, meta refresh, intents) is refused.
 */
internal fun isInertSnapshotNavigation(url: String): Boolean = url == "about:blank" || url.startsWith("data:text/html")
```

2. Change line 29 from

```kotlin
        PlatformBrowserView(state = state, modifier = modifier)
```

to

```kotlin
        PlatformBrowserView(state = state, modifier = modifier, onNavigationRequest = ::isInertSnapshotNavigation)
```

**Verify:** `./gradlew :shared:testAndroidHostTest --tests '*SecureSnapshotDocumentTest'`
Expected: `BUILD SUCCESSFUL`, six tests pass.

### Step 4: Check the viewer still renders on desktop

The desktop test suite contains a real-browser smoke test that renders sanitised documents (`shared/src/desktopTest/kotlin/com/bernaferrari/changedetection/browser/DesktopBrowserSmoke.kt`) and UI tests that open saved pages.

**Verify:** `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest`
Expected: `BUILD SUCCESSFUL`. Run `./gradlew :shared:spotlessApply` first if the format check fails.

**Verify:** `./gradlew :webDemo:wasmJsBrowserDistribution` and, on macOS, `./gradlew :shared:compileKotlinIosSimulatorArm64`.
Expected: `BUILD SUCCESSFUL`.

## Test plan

- `style text can never carry markup into the safe document`: four payloads, asserts no smuggled element, no meta refresh, no remote host, no `<` in kept style text.
- `only the inert document itself may load in the viewer`: pins the navigation rule.
- All four existing tests in `SecureSnapshotDocumentTest` must stay green, in particular `stored snapshot preserves self contained layout and body identity` (keeps `.schedule > h1`).

## Done criteria

- [ ] `rg -n "isInertSnapshotNavigation" shared/src` shows the definition, the use in `SecureSnapshotWebView`, and the test.
- [ ] `rg -n "'<' in css" shared/src/commonMain` prints one line.
- [ ] `./gradlew :shared:testAndroidHostTest --tests '*SecureSnapshotDocumentTest'` passes with six tests.
- [ ] `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest` is `BUILD SUCCESSFUL`.
- [ ] The final report states whether the markup test passed or failed before Step 2.
- [ ] `git status --short` lists only in-scope files.

## STOP conditions

- After Step 2 the markup test still fails for any payload. Report the payload and the sanitised output (it contains no secrets). Do not add special cases per payload; the maintainer needs to see it.
- After Step 3 a desktop UI test that opens a saved page fails or shows a blank viewer. This would mean a platform reports the initial load with a different URL form. Report the URL form; do not widen the rule to "allow everything".
- An existing `SecureSnapshotDocumentTest` test fails.

## Maintenance notes

- Any new composable that shows stored or sanitised HTML must pass `onNavigationRequest`. In this codebase a missing callback means "allow".
- If the picker's rule in `RenderedRegionPreview.kt:36` changes (for example a new platform reports a different initial URL), `isInertSnapshotNavigation` needs the same change.
- Not addressed here: on desktop the viewer's `isJavaScriptEnabled = false` setting is not applied to the embedded Chromium (`DesktopBrowserSession.kt` only reads `disablePopupWindows`). Scripts are stripped and blocked by the CSP, and desktop is a development build, so this is recorded rather than planned.
- Not addressed here: on Android WebView, `shouldOverrideUrlLoading` is not called for every kind of navigation (for example some server redirects of sub-frames). Frames are already stripped; keep them stripped.
