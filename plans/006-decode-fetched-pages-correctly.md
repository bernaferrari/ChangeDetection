# Plan 006: Decode fetched pages with the charset they declare, and recognise files sent without a Content-Type

> **Executor instructions**
> You are implementing one self-contained change. You have not seen the audit that produced this plan; everything you need is in this file.
> 1. Run the drift check first: `git diff --stat a9f8f8ba..HEAD -- shared/src/commonMain/kotlin/com/bernaferrari/changedetection/sync/SyncEngine.kt shared/src/commonMain/kotlin/com/bernaferrari/changedetection/extensions/StringExtensions.kt shared/src/androidUnitTest/kotlin/com/bernaferrari/changedetection/sync/SyncEngineTest.kt`. `SyncEngine.kt` is expected to show the change from Plan 005 (a new `recordTimeout` method near line 183), which shifts later line numbers by about 18. Anything else: compare the "Current state" excerpts with the real files, and if they no longer match, STOP and report.
> 2. Follow the steps in order and run each **Verify** command.
> 3. Touch only the files listed under "In scope".
> 4. If a STOP condition applies, stop and report. Do not improvise.
> 5. When finished, set this plan's row in `plans/README.md` to `DONE`.

## Status

| Field | Value |
| --- | --- |
| Priority | P2 |
| Effort | S to M (2 to 3 hours) |
| Risk of the fix | Medium. Changes the stored bytes for pages that were decoded wrongly before (see "Known side effect") |
| Depends on | Plan 005 should land first (same file) |
| Category | Bug |
| Planned at | commit `a9f8f8ba`, 2026-11-09 |

## Why this matters

When the app fetches a page over plain HTTP (the "source" capture mode on Android, and every check on desktop and iOS), it converts text to UTF-8 before saving it. To do that it must know the page's original character set. Today it looks in one place only, the `Content-Type` response header, and only for a lower-case `charset=` with an unquoted value.

Three things go wrong:

1. **The page declares its charset in the HTML, not in the header.** This is common for older and non-English sites: the header says `text/html` and the document says `<meta charset="windows-1252">` or `<meta http-equiv="Content-Type" content="text/html; charset=Shift_JIS">`. The app decodes the bytes as UTF-8 and every accented or non-Latin character becomes a replacement symbol. The saved copy and every diff of it are unreadable.
2. **The header spells it differently.** `Charset=ISO-8859-1` (capital C) or `charset="iso-8859-1"` (quoted) is not matched, with the same result.
3. **The server sends no Content-Type at all.** The app assumes `text/html`, so an image or PDF is run through a text decoder, which corrupts it permanently.

## Current state

`shared/src/commonMain/kotlin/com/bernaferrari/changedetection/extensions/StringExtensions.kt:61-66`:

```kotlin
fun String.findCharset(): String = "charset=([^()<>@,;:\"/\\[\\]?.=\\s]*)"
    .toRegex()
    .find(this)
    ?.value
    ?.replace("charset=", "")
    ?: ""
```

Its only caller is `SyncEngine.kt` (verify with `rg -n "findCharset" shared/src`: the definition, one import, one call).

`shared/src/commonMain/kotlin/com/bernaferrari/changedetection/sync/SyncEngine.kt`, inside `fetchFromServer` (line numbers at the planned commit; add about 18 after Plan 005).

Line 632:

```kotlin
                    val contentType = response.contentType()?.toString() ?: "text/html"
```

Lines 650-666:

```kotlin
                    // Stream the body so the size cap is enforced even without a
                    // Content-Length header, and keep binary payloads intact
                    // (decoding everything as text corrupted images and other binaries).
                    // Extract the charset label from the Content-Type header text
                    // (multiplatform-safe; response.charset() is JVM-only).
                    val charsetName = contentType.findCharset()?.takeIf { it.isNotBlank() } ?: "UTF-8"
                    val isText = contentType.isTextContentType()

                    val bodyBytes = readCappedBody(response.bodyAsChannel())
                    val data = if (isText) {
                        // Normalize text payloads to UTF-8 bytes (decodeByteArray falls
                        // back to UTF-8 when the label is unknown).
                        decodeByteArray(bodyBytes, charsetName).encodeToByteArray()
                    } else {
                        bodyBytes
                    }
                    FetchResult.Success(contentType, data)
```

Between line 632 and line 650 there is a size check that uses only the `Content-Length` header; it does not use `contentType`.

`shared/src/commonMain/kotlin/com/bernaferrari/changedetection/util/TextContentType.kt` (whole file):

```kotlin
package com.bernaferrari.changedetection.util

/** Text formats must use the same decoding policy during fetching, comparison and saved-page display. */
internal fun String.isTextContentType(): Boolean {
    val type = substringBefore(';').trim().lowercase()
    if (type.startsWith("image/")) return false
    return type.startsWith("text/") ||
        "html" in type || "xml" in type || "json" in type ||
        "javascript" in type || "ecmascript" in type
}
```

`decodeByteArray(bytes: ByteArray, charsetName: String): String` is an `expect` function in `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/util/SharedStringUtils.kt:8`. On Android and desktop it uses `java.nio.charset.Charset.forName` and falls back to UTF-8 for unknown names. On iOS it knows only ASCII, Latin-1 and Windows-1252 and otherwise uses UTF-8. In the web demo it always uses UTF-8. You do not change it.

### Test exemplar

`shared/src/androidUnitTest/kotlin/com/bernaferrari/changedetection/sync/SyncEngineTest.kt:91-108`:

```kotlin
    @Test fun `source scripts are normalized using their declared charset`() = runTest {
        val source = "const label = 'Olá, café';"
        val bytes = source.toByteArray(java.nio.charset.Charset.forName("ISO-8859-1"))
        mockHttpClient = HttpClient(
            MockEngine {
                respond(ByteReadChannel(bytes), headers = headersOf(HttpHeaders.ContentType, "application/javascript; charset=ISO-8859-1"))
            },
        )
        syncEngine = makeSyncEngine()
        try {
            val fetched = syncEngine.fetchFromServer(createTestSite()) as SyncEngine.FetchResult.Success
            assertEquals(source, fetched.data.decodeToString())
        } finally {
            mockHttpClient.close()
        }
    }
```

`mockHttpClient`, `syncEngine`, `makeSyncEngine()` and `createTestSite()` already exist in that class. `respond(ByteReadChannel(bytes))` with no `headers` argument produces a response with no Content-Type.

Pure-function tests that should run on both the Android host and the desktop JVM live in `shared/src/jvmSharedTest/kotlin/...` (JUnit 4, `org.junit.Assert.*`, test names in camelCase without backticks, for example `shared/src/jvmSharedTest/kotlin/com/bernaferrari/changedetection/image/VisualRegionsTest.kt`). The directory `shared/src/jvmSharedTest/kotlin/com/bernaferrari/changedetection/sync/` already exists.

## Commands you will need

| Purpose | Command | Expected |
| --- | --- | --- |
| New pure tests | `./gradlew :shared:testAndroidHostTest --tests '*ResponseDecodingTest'` | `BUILD SUCCESSFUL` |
| Engine tests | `./gradlew :shared:testAndroidHostTest --tests '*SyncEngineTest'` | `BUILD SUCCESSFUL` |
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

- New file: `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/sync/ResponseDecoding.kt`
- New file: `shared/src/jvmSharedTest/kotlin/com/bernaferrari/changedetection/sync/ResponseDecodingTest.kt`
- `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/sync/SyncEngine.kt` (only the lines shown above, and the `findCharset` import)
- `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/extensions/StringExtensions.kt` (remove `findCharset`)
- `shared/src/androidUnitTest/kotlin/com/bernaferrari/changedetection/sync/SyncEngineTest.kt` (add tests)
- `plans/README.md` (status column only)

**Out of scope:**

- Every `decodeByteArray` implementation. Do not add charsets to iOS or the web demo.
- `TextContentType.kt`.
- The browser-rendered capture path (`AndroidScreenshotSyncer`, `PageCapture`). The browser does its own decoding there.
- `LocalBrowserViewModel.kt` (it has its own charset parsing for display; leave it).
- Byte-order marks. Do not strip or interpret them in this plan.

## Git workflow

- Branch from `main` (after Plan 005 has landed): `git switch -c advisor/006-decode-fetched-pages-correctly`
- Commit subjects are plain sentences. Suggested: `Fetched pages use the charset they declare, and untyped files are recognised`.
- Do not push, merge, or open a pull request.

## Steps

### Step 1: Write the failing pure tests

Create `shared/src/jvmSharedTest/kotlin/com/bernaferrari/changedetection/sync/ResponseDecodingTest.kt`:

```kotlin
package com.bernaferrari.changedetection.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class ResponseDecodingTest {
    private fun ascii(text: String) = text.toByteArray(Charsets.US_ASCII)

    @Test fun headerCharsetIsFoundWhateverItsSpelling() {
        for (header in listOf(
            "text/html; charset=ISO-8859-1",
            "text/html; Charset=ISO-8859-1",
            "text/html;CHARSET=ISO-8859-1",
            "text/html; charset=\"ISO-8859-1\"",
            "text/html; charset='ISO-8859-1'",
            "text/html; charset = ISO-8859-1 ; boundary=x",
        )) {
            assertEquals(header, "ISO-8859-1", responseCharset(header, ascii("<p>x</p>")))
        }
    }

    @Test fun headerCharsetWinsOverTheDocument() {
        assertEquals("ISO-8859-1", responseCharset("text/html; charset=ISO-8859-1", ascii("<meta charset=\"Shift_JIS\">")))
    }

    @Test fun htmlWithoutAHeaderCharsetUsesItsMetaDeclaration() {
        for (html in listOf(
            "<!doctype html><html><head><meta charset=\"windows-1252\"><title>x</title>",
            "<html><head><META CHARSET=windows-1252>",
            "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=windows-1252\">",
            "<html><head><meta content='text/html;charset=windows-1252' http-equiv='content-type'>",
        )) {
            assertEquals(html, "windows-1252", responseCharset("text/html", ascii(html)))
        }
    }

    @Test fun aDeclarationOutsideAMetaTagOrBeyondTheScanLimitIsIgnored() {
        assertEquals("UTF-8", responseCharset("text/html", ascii("<p>charset=windows-1252</p>")))
        assertEquals("UTF-8", responseCharset("text/html", ascii("<!--" + "x".repeat(2000) + "--><meta charset=\"windows-1252\">")))
    }

    @Test fun onlyHtmlIsScannedForMetaDeclarations() {
        assertEquals("UTF-8", responseCharset("application/json", ascii("{\"a\":\"<meta charset=windows-1252>\"}")))
        assertEquals("UTF-8", responseCharset("text/plain", ascii("<meta charset=windows-1252>")))
    }

    @Test fun textWithNoDeclarationIsUtf8() {
        assertEquals("UTF-8", responseCharset("text/html", ascii("<p>plain</p>")))
        assertEquals("UTF-8", responseCharset("text/html; charset=", ascii("<p>plain</p>")))
        assertEquals("UTF-8", responseCharset("text/html", ByteArray(0)))
    }

    @Test fun untypedResponsesAreRecognisedByTheirFirstBytes() {
        fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }
        assertEquals("image/png", sniffContentType(bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0)))
        assertEquals("image/jpeg", sniffContentType(bytes(0xFF, 0xD8, 0xFF, 0xE0, 0, 0)))
        assertEquals("image/gif", sniffContentType(ascii("GIF89a") + bytes(1, 0)))
        assertEquals("image/gif", sniffContentType(ascii("GIF87a") + bytes(1, 0)))
        assertEquals("image/webp", sniffContentType(ascii("RIFF") + bytes(1, 2, 3, 4) + ascii("WEBPVP8 ")))
        assertEquals("application/pdf", sniffContentType(ascii("%PDF-1.7\n")))
        assertEquals("text/html", sniffContentType(ascii("<!doctype html><p>x</p>")))
        assertEquals("text/html", sniffContentType(ascii("RIFF") + bytes(1, 2, 3, 4) + ascii("WAVEfmt ")))
        assertEquals("text/html", sniffContentType(ByteArray(0)))
        assertEquals("text/html", sniffContentType(bytes(0x89, 0x50)))
    }
}
```

**Verify:** `./gradlew :shared:testAndroidHostTest --tests '*ResponseDecodingTest'`
Expected: the build **fails to compile** (unresolved `responseCharset` and `sniffContentType`). That is the expected red state.

### Step 2: Add the two pure functions

Create `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/sync/ResponseDecoding.kt`:

```kotlin
package com.bernaferrari.changedetection.sync

/** How far into a document a charset declaration is looked for; the HTML standard uses the same limit. */
private const val META_SCAN_BYTES = 1024

private const val CHARSET_LABEL = "([A-Za-z0-9._:+-]+)"
private val HEADER_CHARSET = Regex("charset\\s*=\\s*[\"']?$CHARSET_LABEL", RegexOption.IGNORE_CASE)
private val META_CHARSET = Regex("<meta\\b[^>]*?charset\\s*=\\s*[\"']?$CHARSET_LABEL", RegexOption.IGNORE_CASE)

/**
 * The charset a text response should be decoded with: the Content-Type header's label when it has one,
 * otherwise (HTML only) the document's own meta declaration, otherwise UTF-8.
 */
internal fun responseCharset(contentType: String, body: ByteArray): String {
    HEADER_CHARSET.find(contentType.substringAfter(';', ""))?.let { return it.groupValues[1] }
    if ("html" !in contentType.substringBefore(';').lowercase()) return "UTF-8"
    // Charset declarations are ASCII in every ASCII-compatible encoding, so the prefix can be read byte for byte.
    val prefix = buildString(minOf(body.size, META_SCAN_BYTES)) {
        for (index in 0 until minOf(body.size, META_SCAN_BYTES)) append((body[index].toInt() and 0xFF).toChar())
    }
    return META_CHARSET.find(prefix)?.groupValues?.get(1) ?: "UTF-8"
}

/** The type of a response that arrived without a Content-Type header. Unrecognised content is treated as a web page. */
internal fun sniffContentType(body: ByteArray): String = when {
    body.startsWith(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) -> "image/png"
    body.startsWith(0xFF, 0xD8, 0xFF) -> "image/jpeg"
    body.startsWith(0x47, 0x49, 0x46, 0x38, 0x37, 0x61) || body.startsWith(0x47, 0x49, 0x46, 0x38, 0x39, 0x61) -> "image/gif"
    body.startsWith(0x52, 0x49, 0x46, 0x46) && body.startsWith(0x57, 0x45, 0x42, 0x50, offset = 8) -> "image/webp"
    body.startsWith(0x25, 0x50, 0x44, 0x46, 0x2D) -> "application/pdf"
    else -> "text/html"
}

private fun ByteArray.startsWith(vararg signature: Int, offset: Int = 0): Boolean = size >= offset + signature.size && signature.indices.all { this[offset + it] == signature[it].toByte() }
```

**Verify:** `./gradlew :shared:testAndroidHostTest --tests '*ResponseDecodingTest'`
Expected: `BUILD SUCCESSFUL`, seven tests pass.

If `aDeclarationOutsideAMetaTagOrBeyondTheScanLimitIsIgnored` or any other test fails, fix the function, not the test.

### Step 3: Write the failing engine tests

Add these three tests to `SyncEngineTest.kt`, directly after `source scripts are normalized using their declared charset`:

```kotlin
    @Test fun `a page that declares its charset only in a meta tag is decoded with it`() = runTest {
        val page = "<!doctype html><html><head><meta charset=\"windows-1252\"><title>Café</title></head><body>Preço: 10 €</body></html>"
        mockHttpClient = HttpClient(
            MockEngine {
                respond(ByteReadChannel(page.toByteArray(java.nio.charset.Charset.forName("windows-1252"))), headers = headersOf(HttpHeaders.ContentType, "text/html"))
            },
        )
        syncEngine = makeSyncEngine()
        try {
            val fetched = syncEngine.fetchFromServer(createTestSite()) as SyncEngine.FetchResult.Success
            assertEquals(page, fetched.data.decodeToString())
        } finally {
            mockHttpClient.close()
        }
    }

    @Test fun `a charset header is honoured whatever its capitalisation`() = runTest {
        val page = "<p>Olá, café</p>"
        mockHttpClient = HttpClient(
            MockEngine {
                respond(ByteReadChannel(page.toByteArray(java.nio.charset.Charset.forName("ISO-8859-1"))), headers = headersOf(HttpHeaders.ContentType, "text/html; Charset=ISO-8859-1"))
            },
        )
        syncEngine = makeSyncEngine()
        try {
            val fetched = syncEngine.fetchFromServer(createTestSite()) as SyncEngine.FetchResult.Success
            assertEquals(page, fetched.data.decodeToString())
        } finally {
            mockHttpClient.close()
        }
    }

    @Test fun `an image sent without a content type is kept byte for byte`() = runTest {
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 1, 127, 128.toByte(), 195.toByte(), 255.toByte())
        mockHttpClient = HttpClient(MockEngine { respond(ByteReadChannel(png)) })
        syncEngine = makeSyncEngine()
        try {
            val fetched = syncEngine.fetchFromServer(createTestSite()) as SyncEngine.FetchResult.Success
            assertEquals("image/png", fetched.contentType)
            assertArrayEquals(png, fetched.data)
        } finally {
            mockHttpClient.close()
        }
    }
```

Before relying on `fetched.contentType`, confirm the property name: `rg -n "class Success" shared/src/commonMain/kotlin/com/bernaferrari/changedetection/sync/SyncEngine.kt` and read the two lines after it. Use the real name of the first constructor property.

**Verify:** `./gradlew :shared:testAndroidHostTest --tests '*SyncEngineTest'`
Expected: the build **fails**. `a page that declares its charset only in a meta tag is decoded with it` and `an image sent without a content type is kept byte for byte` fail. Write down whether `a charset header is honoured whatever its capitalisation` fails or passes (the HTTP library may normalise the header before the engine sees it; either result is fine and belongs in your report).

### Step 4: Use the functions in the engine

In `SyncEngine.kt`, inside `fetchFromServer`:

1. Replace

```kotlin
                    val contentType = response.contentType()?.toString() ?: "text/html"
```

with

```kotlin
                    val declaredType = response.contentType()?.toString()
```

2. Replace the block shown under "Current state" (from the comment `// Stream the body so the size cap is enforced` down to `FetchResult.Success(contentType, data)`) with:

```kotlin
                    // Stream the body so the size cap is enforced even without a
                    // Content-Length header, and keep binary payloads intact
                    // (decoding everything as text corrupted images and other binaries).
                    val bodyBytes = readCappedBody(response.bodyAsChannel())
                    // A response without a Content-Type is identified by its first bytes before it is assumed to be a page.
                    val contentType = declaredType ?: sniffContentType(bodyBytes)
                    val data = if (contentType.isTextContentType()) {
                        // Normalize text payloads to UTF-8 bytes (decodeByteArray falls
                        // back to UTF-8 when the label is unknown).
                        decodeByteArray(bodyBytes, responseCharset(contentType, bodyBytes)).encodeToByteArray()
                    } else {
                        bodyBytes
                    }
                    FetchResult.Success(contentType, data)
```

3. Check that nothing between those two edits still refers to `contentType`: `rg -n "contentType" shared/src/commonMain/kotlin/com/bernaferrari/changedetection/sync/SyncEngine.kt` and read the matches inside `fetchFromServer`. If the size check or anything else in between uses it, STOP (see STOP conditions).

4. Remove the now-unused import `import com.bernaferrari.changedetection.extensions.findCharset` from `SyncEngine.kt`.

5. In `StringExtensions.kt`, delete the `findCharset` function (lines 61-66 and the blank line before it). Confirm first that nothing else uses it: `rg -n "findCharset" shared/src androidApp/src` must show only the definition after edit 4.

**Verify:** `./gradlew :shared:testAndroidHostTest --tests '*SyncEngineTest' --tests '*ResponseDecodingTest'`
Expected: `BUILD SUCCESSFUL`. The three new engine tests pass, and so do the two existing ones (`binary responses remain unchanged by text decoding`, `source scripts are normalized using their declared charset`).

### Step 5: Final gates

**Verify:** `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest`
Expected: `BUILD SUCCESSFUL`. Run `./gradlew :shared:spotlessApply` first if the format check fails.

**Verify:** `./gradlew :webDemo:wasmJsBrowserDistribution`, and on macOS `./gradlew :shared:compileKotlinIosSimulatorArm64`.
Expected: `BUILD SUCCESSFUL`.

## Test plan

- `ResponseDecodingTest` (new, seven tests): header spellings, header beats document, both meta forms, scan limit, HTML-only scanning, UTF-8 default, magic bytes including two negative cases.
- `SyncEngineTest` (three new tests): end-to-end through `fetchFromServer` with a mock HTTP engine.
- Existing tests in `SyncEngineTest` must stay green.

## Known side effect (put this in your final report and in the commit body)

A page that was being decoded wrongly until now will be decoded correctly on its next check. Its stored bytes therefore differ from the previous version, and the user gets **one** change alert for that page whose diff shows garbled characters turning into correct ones. This happens once per affected page and is the fix working. It affects only pages fetched over plain HTTP whose charset was previously missed.

## Done criteria

- [ ] `rg -n "findCharset" shared/src` prints nothing.
- [ ] `rg -n "responseCharset\(|sniffContentType\(" shared/src/commonMain` shows two definitions and two uses in `SyncEngine.kt`.
- [ ] `./gradlew :shared:testAndroidHostTest --tests '*ResponseDecodingTest'` passes with seven tests.
- [ ] `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest` is `BUILD SUCCESSFUL`.
- [ ] `./gradlew :webDemo:wasmJsBrowserDistribution` is `BUILD SUCCESSFUL`.
- [ ] `git status --short` lists only in-scope files.

## STOP conditions

- `contentType` is used between the header read and the body read in `fetchFromServer` in a way that needs the type before the body is available.
- `findCharset` has gained another caller.
- The Kotlin regex in `ResponseDecoding.kt` fails to compile for iOS or the web demo (regex support differs slightly per platform). Report the exact error; do not move the file to a JVM-only source set.
- More than the three new tests change result in `SyncEngineTest`.

## Maintenance notes

- iOS decodes only ASCII, Latin-1 and Windows-1252; the web demo only UTF-8. A page declaring another charset is still decoded as UTF-8 there. That limit is in the platform `decodeByteArray` implementations, not in this change.
- Byte-order marks are neither stripped nor used. A UTF-16 page without a charset header is still decoded as UTF-8. This is rare enough that it was left out; if it is ever handled, do it in `responseCharset`.
- `LocalBrowserViewModel.kt` parses the charset of a stored content type separately for display. It reads the stored type, which this change does not alter.
- If a new binary type should be recognised without a header, add its signature to `sniffContentType` with a test.
