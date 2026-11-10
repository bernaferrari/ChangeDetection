# Plan 013: Make the privacy policy and the in-app licence list match what the app ships

> **Executor instructions**
> You are implementing one small, self-contained change to documentation and a static list. You have not seen the audit that produced this plan; everything you need is in this file.
> 1. Run the drift check first: `git diff --stat a9f8f8ba..HEAD -- PRIVACY.md shared/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/about/AboutLicensesScreen.kt gradle/libs.versions.toml`. `PRIVACY.md` may already contain a section titled "Backups and Moving to a New Phone" (added by Plan 004); that is expected. If `AboutLicensesScreen.kt` no longer matches the excerpt below, STOP and report.
> 2. Follow the steps in order and run each **Verify** command.
> 3. Touch only the files listed under "In scope".
> 4. If a STOP condition applies, stop and report. Do not improvise.
> 5. When finished, set this plan's row in `plans/README.md` to `DONE`.

## Status

| Field | Value |
| --- | --- |
| Priority | P3 |
| Effort | S (1 to 2 hours) |
| Risk of the fix | Low. One Markdown file and one static list |
| Depends on | Nothing. Reads better after Plans 004, 007 and 016, and adapts to whether they have landed |
| Category | Documentation / compliance |
| Planned at | commit `a9f8f8ba`, 2026-11-09 |

## Why this matters

Two public statements about the app have fallen behind the code.

**The privacy policy.** `PRIVACY.md` says the app collects no "log data" and operates entirely on the device. That is true in spirit, but the policy is silent on three things a careful reader would ask about:

- The app keeps a short technical log of recent checks on the device, and Settings has a "diagnostics" entry that lets the user share that report by email, pre-addressed to the developer. Nothing is sent unless the user taps share, and the report is designed to exclude page addresses and content. A policy that says "no log data" while the app offers to email a log should say exactly this, or it reads as a contradiction.
- The in-app browser keeps cookies and sign-ins for sites the user logs in to.
- What happens to all of this on backup and phone-to-phone transfer.

**The licence list.** The About screen lists open-source components. It names 11. The app also ships Coil, Compose WebView Multiplatform, Kermit, Okio, the kotlinx libraries and the Google Sans Flex font, none of which are listed. Google Sans Flex is under the SIL Open Font License, which asks that its copyright notice and licence accompany redistributed copies; the Noto fonts in the same list already get this treatment. The list also sends every non-Apache entry to a generic page (`opensource.org/licenses`) instead of the actual licence.

## Current state

### `PRIVACY.md` (27 lines, whole file at the planned commit)

```text
# Privacy Policy for Change Detection

## Last Updated: July 11, 2026

Thank you for choosing to use Change Detection. We take your privacy seriously. This privacy policy clarifies our data
handling practices, emphasizing our commitment to not collect, store, or share your personal data.

Data We Do Not Collect
Change Detection does not collect, store, or share any personal data, including no personal identifiers, log data, or
tracking cookies. The app is designed to prioritize your privacy and operates entirely locally on your device.

Network Requests
The app connects directly to the web addresses you choose to monitor. Those websites and their infrastructure may
receive standard request information such as your IP address, user agent, and request time, according to their own
privacy policies. Change Detection does not send those requests through a Change Detection server.

How the App Functions
Change Detection functions without the need for a server. All processing tasks are done locally on your device, ensuring
there are no external databases or clouds where data might be stored or processed.

Changes to This Privacy Policy
...
Contact Us
...
```

Section titles are plain lines (not Markdown headings), followed by a paragraph wrapped at about 120 characters.

### Facts the new text may rely on (verified in the code at the planned commit)

- Diagnostics are local. `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/diagnostics/DiagnosticTrace.kt:113` documents the rule: "No URLs, page content or raw messages enter this schema; error text is scrubbed first. Android installs a bounded sink."
- The report leaves the device only through the Android share sheet, started by the user from a dialog that first shows the report. `shared/src/androidMain/kotlin/com/bernaferrari/changedetection/diagnostics/DiagnosticsEntry.android.kt:170-181` builds an `ACTION_SEND` intent with the report as an attachment and the developer's address pre-filled. (Do not copy that address into any file.)
- The app has no analytics, crash-reporting or advertising SDK (`gradle/libs.versions.toml` contains none).
- The in-app browser shares one cookie store across the app (`shared/src/androidMain/kotlin/com/bernaferrari/changedetection/browser/AndroidWebViewTools.kt:38`).
- Cloud backup and device transfer are excluded for the database, files and preferences (`shared/src/androidMain/res/xml/data_extraction_rules.xml`).

### The licence list

`shared/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/about/AboutLicensesScreen.kt:47-66`:

```kotlin
private data class License(
    val name: String,
    val author: String,
    val licenseType: String,
    val category: String,
)

private val licenses = listOf(
    License("Android Open Source Project", "Google", "Apache License 2.0", "Android"),
    License("Jetpack Compose", "Google", "Apache License 2.0", "Android"),
    License("Material 3", "Google", "Apache License 2.0", "Android"),
    License("Koin", "Kotzilla", "Apache License 2.0", "Core"),
    License("Kotlin", "JetBrains", "Apache License 2.0", "Core"),
    License("Ktor", "JetBrains", "Apache License 2.0", "Core"),
    License("Room 3", "Android Open Source Project", "Apache License 2.0", "Data"),
    License("AndroidX SQLite", "Android Open Source Project", "Apache License 2.0", "Data"),
    License("Ksoup", "Fleek Soft", "Apache License 2.0", "Networking"),
    License("MaterialKolor", "Jordond", "Apache License 2.0", "UI"),
    License("Noto Sans (Devanagari, Bengali, Simplified Chinese)", "The Noto Project", "SIL Open Font License 1.1", "UI"),
)
```

Same file, lines 139-147 (inside the list's `onClick`):

```kotlin
                                        onUrlClick(
                                            if (license.licenseType == "Apache License 2.0") {
                                                "https://www.apache.org/licenses/LICENSE-2.0"
                                            } else {
                                                "https://opensource.org/licenses"
                                            },
                                        )
```

Rows use `key = { it.name }`, so every `name` in the list must be unique.

Components the app ships that are missing from the list (from `gradle/libs.versions.toml` and the bundled fonts):

| Component | Author | Licence | Evidence |
| --- | --- | --- | --- |
| Coil | Coil Contributors | Apache License 2.0 | `io.coil-kt.coil3:coil` 3.6.3 |
| Compose WebView Multiplatform | Kevin Zou | Apache License 2.0 | `io.github.kevinnzou:compose-webview-multiplatform` 2.0.3 |
| Kermit | Touchlab | Apache License 2.0 | `co.touchlab:kermit` 2.2.0 |
| Okio | Square | Apache License 2.0 | `com.squareup.okio:okio` 3.18.2 |
| kotlinx.coroutines, serialization and datetime | JetBrains | Apache License 2.0 | `org.jetbrains.kotlinx:*` |
| Google Sans Flex | The Google Sans Flex Authors | SIL Open Font License 1.1 | the name table of `shared/src/commonMain/composeResources/font/gsf_regular.ttf` reads "Copyright 2015 The Google Sans Flex Authors (https://github.com/googlefonts/googlesans-flex)" |
| KMPwright | Bernardo Ferrari | MIT License | `io.github.bernaferrari:kmpwright-core` 0.0.2; licence text at `androidApp/src/main/assets/licenses/webwright-LICENSE` |

## Commands you will need

| Purpose | Command | Expected |
| --- | --- | --- |
| Full gate | `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest` | `BUILD SUCCESSFUL` |
| About screen tests | `./gradlew :shared:desktopTest --tests '*AboutScreenUiTest'` | `BUILD SUCCESSFUL` |
| Auto-format | `./gradlew :shared:spotlessApply` | `BUILD SUCCESSFUL` |
| Other platforms compile | `./gradlew :webDemo:wasmJsBrowserDistribution` and (macOS) `./gradlew :shared:compileKotlinIosSimulatorArm64` | `BUILD SUCCESSFUL` |

The build needs JDK 21 and Android SDK platform 37.2.

## Before you start: the state of the build

Checked on 2026-11-09 at commit `a9f8f8ba`. Read this before the steps; it overrides any line below that expects otherwise.

1. **Plan 000 must be done first.** Before you change anything, run `./gradlew :shared:spotlessCheck`. If it fails, Plan 000 (`plans/000-make-the-gates-green.md`) has not landed: STOP and report that. Do not run `spotlessApply` to get past it; on an unprepared tree it rewrites about 36 unrelated files. Once Plan 000 has landed, the test and format commands in this plan behave as described, and `spotlessApply` only touches what you changed.
2. **Two commands fail for reasons no plan fixes.** `./gradlew :shared:compileKotlinIosSimulatorArm64` stops with five errors, all in `shared/src/iosMain/kotlin/com/bernaferrari/changedetection/browser/IosPageCapture.kt` (the first is `Unresolved reference 'kmpwright'`; a library was built with a newer Kotlin than this project uses). `./gradlew :webDemo:wasmJsBrowserDistribution` compiles the Kotlin and then stops at `:kotlinWasmStoreYarnLock` with `Lock file was changed`. Wherever this plan expects one of them to print `BUILD SUCCESSFUL`, the bar is instead: for iOS, **those same five errors and no others**; for the web demo, run `./gradlew :webDemo:compileKotlinWasmJs` and expect `BUILD SUCCESSFUL`. Never run `kotlinWasmUpgradeYarnLock`. If either command has since been repaired and passes, that is fine.
3. **Line numbers may have moved.** Two changes landed after this plan was written. Plan 000 re-formats about 36 files (layout only). The "failure experience" change (`plans/failure-experience-changes.md`) added code to `SyncEngine.kt`, `SitesDao.kt`, `Site.kt`, `CheckFailure.kt`, `DiagnosticTrace.kt`, `PageDetailScreen.kt`, `MainViewModel.kt`, `LegacyMigration.kt`, `DatabaseFactory.kt`, `AppContent.kt`, `AndroidChangeNotifier.kt`, the four `PlatformDiModule` files and the string files. Find code by the text this plan quotes, not by line number. A quoted block that differs only in line breaks or indentation is the same code. The drift check at the top of this plan will list those files as changed; that alone is not a reason to stop. Stop only if the quoted code is gone or does something different.

## Scope

**In scope:**

- `PRIVACY.md`
- `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/about/AboutLicensesScreen.kt`
- `plans/README.md` (status column only)

**Out of scope:**

- `website/app/privacy/page.tsx` (the website's own privacy page). Read it; if it contradicts the new `PRIVACY.md`, say so in your report, but do not edit it.
- The diagnostics feature itself, including its pre-filled recipient.
- Version numbers. The desktop and iOS development builds carry older version strings than Android, and the HTTP `User-Agent` says `ChangeDetection/2.0`. These were looked at and deliberately left: the first two are unreleased builds, and changing the `User-Agent` can make some servers return different HTML, which would raise false change alerts.
- Adding a licence-report Gradle plugin.

## Git workflow

- Branch from `main`: `git switch -c advisor/013-privacy-policy-and-licence-notices`
- Two commits, one per file. Commit subjects are plain sentences, for example `The privacy policy covers diagnostics reports and saved sign-ins` and `The licence list names every bundled library and font`.
- Do not push, merge, or open a pull request.

## Steps

### Step 1: Update `PRIVACY.md`

1. Change the date line to today's date in the same format, for example `## Last Updated: November 9, 2026`.

2. In the section "Data We Do Not Collect", replace the first sentence so it no longer claims there is no log data at all. New paragraph:

```text
Data We Do Not Collect
Change Detection does not collect, store, or share any personal data on a server. There are no accounts, no analytics,
no advertising identifiers, and no tracking cookies. The app is designed to prioritize your privacy and operates entirely
locally on your device.
```

3. Add these two sections directly after "How the App Functions" and before "Changes to This Privacy Policy" (if a section titled "Backups and Moving to a New Phone" exists, put them before it):

```text
Diagnostic Reports
To help find problems, the app keeps a short technical record of recent checks on your device, such as how long each
step took and what kind of error occurred. This record is designed not to contain the addresses or the content of the
pages you watch. It never leaves your device on its own. If you choose "Send report" in Settings, the app shows you the
report first and then opens your own email or sharing app so that you can send it to the developer. Whether to send it is
always your decision.

Sign-ins in the In-App Browser
If you sign in to a website inside the app, the cookies and site data for that sign-in are kept in the app's private
browser storage on your device, so that later checks can open the same page. They are sent only to the websites they
belong to, as in any browser.
```

4. Conditional sentences. Run each search and add the sentence only if the search prints a match:

- `rg -n "site_sessions_title" shared/src/commonMain/composeResources/values/strings_v7.xml` (Plan 007 has landed): append to the end of the "Sign-ins in the In-App Browser" paragraph: `You can delete them at any time under Settings, Saved sign-ins.`
- `rg -n "backup_title" shared/src/commonMain/composeResources/values/strings_v7.xml` (Plan 016 has landed): do nothing here; Plan 016 adds its own paragraph.
- If `PRIVACY.md` has no section "Backups and Moving to a New Phone" (Plan 004 has not landed), add this section after the two new ones:

```text
Backups and Moving to a New Phone
Your watched pages, their saved versions and your settings are excluded from Android cloud backup and from
device-to-device transfer. They stay on the device where they were created.
```

5. Check the "Send report" wording against the app: `rg -n 'name="diagnostics_(export|share)"' shared/src/androidMain/res/values/strings.xml`. Use the exact English label of the share button from that file in place of "Send report" if it differs.

**Verify:** `rg -n "^Diagnostic Reports$|^Sign-ins in the In-App Browser$" PRIVACY.md` prints two lines, and `rg -n "log data" PRIVACY.md` prints nothing.

**Verify:** `rg -n "@" PRIVACY.md` prints nothing (no email address was added).

### Step 2: Complete the licence list

In `AboutLicensesScreen.kt`:

1. Replace the `licenses` list with:

```kotlin
private val licenses = listOf(
    License("Android Open Source Project", "Google", "Apache License 2.0", "Android"),
    License("Jetpack Compose", "Google", "Apache License 2.0", "Android"),
    License("Material 3", "Google", "Apache License 2.0", "Android"),
    License("Koin", "Kotzilla", "Apache License 2.0", "Core"),
    License("Kotlin", "JetBrains", "Apache License 2.0", "Core"),
    License("kotlinx.coroutines, serialization and datetime", "JetBrains", "Apache License 2.0", "Core"),
    License("Ktor", "JetBrains", "Apache License 2.0", "Core"),
    License("Kermit", "Touchlab", "Apache License 2.0", "Core"),
    License("Okio", "Square", "Apache License 2.0", "Core"),
    License("Room 3", "Android Open Source Project", "Apache License 2.0", "Data"),
    License("AndroidX SQLite", "Android Open Source Project", "Apache License 2.0", "Data"),
    License("Ksoup", "Fleek Soft", "Apache License 2.0", "Networking"),
    License("Coil", "Coil Contributors", "Apache License 2.0", "UI"),
    License("Compose WebView Multiplatform", "Kevin Zou", "Apache License 2.0", "UI"),
    License("KMPwright", "Bernardo Ferrari", "MIT License", "UI"),
    License("MaterialKolor", "Jordond", "Apache License 2.0", "UI"),
    License("Google Sans Flex", "The Google Sans Flex Authors", "SIL Open Font License 1.1", "UI"),
    License("Noto Sans (Devanagari, Bengali, Simplified Chinese)", "The Noto Project", "SIL Open Font License 1.1", "UI"),
)

private fun licenseUrl(licenseType: String): String = when (licenseType) {
    "Apache License 2.0" -> "https://www.apache.org/licenses/LICENSE-2.0"
    "MIT License" -> "https://opensource.org/license/mit"
    "SIL Open Font License 1.1" -> "https://openfontlicense.org/open-font-license-official-text/"
    else -> "https://opensource.org/licenses"
}
```

2. Replace the `if (license.licenseType == "Apache License 2.0") { ... } else { ... }` expression inside `onUrlClick(` with `licenseUrl(license.licenseType)`, so the call reads:

```kotlin
                                    onClick = { onUrlClick(licenseUrl(license.licenseType)) },
```

Before saving, confirm each added library is still a dependency: `rg -n "coil|kermit|okio|webview-multiplatform|kmpwright|kotlinx-datetime" gradle/libs.versions.toml` must show all six. Leave out any that is gone and note it.

**Verify:** `rg -c 'License\("' shared/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/about/AboutLicensesScreen.kt`
Expected: `18`.

**Verify:** `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest`
Expected: `BUILD SUCCESSFUL`. Run `./gradlew :shared:spotlessApply` first if the format check fails. `AboutScreenUiTest` must still pass; if it asserts an exact number of licence rows and fails for that reason alone, update that number and say so in your report.

**Verify:** `./gradlew :webDemo:wasmJsBrowserDistribution`, and on macOS `./gradlew :shared:compileKotlinIosSimulatorArm64`.
Expected: `BUILD SUCCESSFUL`.

## Test plan

- Existing `AboutScreenUiTest` (desktop) must pass.
- No new test: the list is static data, and a test that repeats the list would only duplicate it.
- Read the finished `PRIVACY.md` top to bottom once and check that no sentence promises more than the "Facts the new text may rely on" section supports.

## Done criteria

- [ ] `PRIVACY.md` has the sections "Diagnostic Reports" and "Sign-ins in the In-App Browser", a current date, no "log data" claim, and no email address.
- [ ] `rg -c 'License\("' .../AboutLicensesScreen.kt` prints `18` (or fewer by the number of libraries you left out, with a note).
- [ ] `rg -n "fun licenseUrl" shared/src/commonMain` prints one line.
- [ ] `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest` is `BUILD SUCCESSFUL`.
- [ ] `git status --short` lists only in-scope files.

## STOP conditions

- The diagnostics code no longer matches the facts above: for example the report is uploaded automatically, or `DiagnosticTrace.kt` no longer says URLs and content are excluded. The policy text would then be false; report what you found.
- You find an analytics, crash-reporting or advertising dependency in `gradle/libs.versions.toml`. Report it; do not write around it.
- `AboutScreenUiTest` fails for a reason other than a row count.

## Maintenance notes

- The privacy text describes behaviour. Whoever changes the diagnostics report contents, adds any network destination other than watched pages, or adds an SDK must update `PRIVACY.md` in the same change. The website has its own privacy page (`website/app/privacy/page.tsx`) that should say the same.
- The licence list is maintained by hand. When a dependency is added to `gradle/libs.versions.toml`, add a row. If the list keeps drifting, a generated list (for example from a licence-report Gradle plugin) is the durable fix.
- `androidApp/src/main/assets/licenses/webwright-LICENSE` is bundled in the APK but no screen opens it. That is acceptable (the notice ships with the app); linking it from the About screen would be nicer.
- Deliberately not changed: desktop `packageVersion` ("2.31.0", `shared/build.gradle.kts:218`), iOS `MARKETING_VERSION` ("2.31", `iosApp/project.yml:8`) and the HTTP `User-Agent` (`SyncEngine.kt`, `ChangeDetection/2.0`), all behind Android's 3.2. Align the first two when those builds are released. Change the `User-Agent` only together with a release note, because servers that vary their HTML by user agent would produce one false alert per page.
