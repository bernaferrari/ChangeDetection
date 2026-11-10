# Plan 004: Keep in-app browser cookies and sign-ins out of device transfer and backup

> **Executor instructions**
> You are implementing one small, self-contained change. You have not seen the audit that produced this plan; everything you need is in this file.
> 1. Run the drift check first: `git diff --stat a9f8f8ba..HEAD -- shared/src/androidMain/res/xml/ androidApp/src/main/AndroidManifest.xml PRIVACY.md`. If it prints anything, compare the "Current state" excerpts with the real files. If they no longer match, STOP and report.
> 2. Follow the steps in order and run each **Verify** command.
> 3. Touch only the files listed under "In scope".
> 4. If a STOP condition applies, stop and report. Do not improvise.
> 5. When finished, set this plan's row in `plans/README.md` to `DONE`.

## Status

| Field | Value |
| --- | --- |
| Priority | P1 |
| Effort | S (under 1 hour, plus an optional device check) |
| Risk of the fix | Low. Two XML rules |
| Depends on | Nothing |
| Category | Security / privacy |
| Planned at | commit `a9f8f8ba`, 2026-11-09 |

## Why this matters

The app's promise is that everything stays on the device. Its backup rules say so explicitly: watched addresses and history are excluded from cloud backup and from device-to-device transfer.

The app also has an in-app browser, and users sign in to websites there so that checks can see pages behind a login. Android stores WebView cookies, local storage and other site data in a directory named `app_webview` directly under the app's data root. That directory is **not** covered by the current rules, which exclude only the `database`, `file` and `sharedpref` domains.

`android:allowBackup="false"` already stops cloud backup. It does not reliably stop device-to-device transfer: for apps that target Android 12 or later, some manufacturers' phone-to-phone migration tools ignore it and follow the `dataExtractionRules` file instead. So today a phone migration can copy the user's live session cookies for every site they signed in to, which is exactly the data the rules were written to keep on the device.

## Current state

`shared/src/androidMain/res/xml/data_extraction_rules.xml` (whole file):

```xml
<?xml version="1.0" encoding="utf-8"?>
<!--
  Local-only privacy posture: monitored URLs and snapshot history never leave the
  device via cloud backup. Device transfer is also excluded so the full browsing
  corpus is not exported over a cable without explicit in-app export.
-->
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="database" />
        <exclude domain="file" />
        <exclude domain="sharedpref" />
    </cloud-backup>
    <device-transfer>
        <exclude domain="database" />
        <exclude domain="file" />
        <exclude domain="sharedpref" />
    </device-transfer>
</data-extraction-rules>
```

`shared/src/androidMain/res/xml/backup_rules.xml` (whole file, used only below Android 12):

```xml
<?xml version="1.0" encoding="utf-8"?><!--
  Legacy (API < 31) backup rules. Local monitoring history stays on-device.
-->
<full-backup-content>
    <exclude domain="database" path="changes.db" />
</full-backup-content>
```

`androidApp/src/main/AndroidManifest.xml:18-23`:

```xml
    <application
        android:name=".App"
        android:allowBackup="false"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:enableOnBackInvokedCallback="true"
        android:fullBackupContent="@xml/backup_rules"
```

Facts you need:

- In Android's backup XML, `domain="root"` means the app's private data directory itself. `app_webview` is a sub-directory of it. A rule with a directory `path` applies to everything inside that directory.
- The app never changes the WebView data directory name (`rg -n "setDataDirectorySuffix" shared/src androidApp/src` prints nothing), so the name is the default `app_webview`.
- `PRIVACY.md` (27 lines) does not mention backup, device transfer or the in-app browser's saved sign-ins.

## Commands you will need

| Purpose | Command | Expected |
| --- | --- | --- |
| XML is well formed | `xmllint --noout shared/src/androidMain/res/xml/data_extraction_rules.xml shared/src/androidMain/res/xml/backup_rules.xml` | no output, exit code 0 |
| Android lint and release build | `./gradlew :androidApp:lintDebug :androidApp:assembleRelease` | `BUILD SUCCESSFUL` |
| Host tests | `./gradlew :shared:testAndroidHostTest` | `BUILD SUCCESSFUL` |

If `xmllint` is not installed, use `python3 -c "import sys, xml.dom.minidom as m; [m.parse(f) for f in sys.argv[1:]]" <file1> <file2>` instead.

The build needs JDK 21 and Android SDK platform 37.2.

## Before you start: the state of the build

Checked on 2026-11-09 at commit `a9f8f8ba`. Read this before the steps; it overrides any line below that expects otherwise.

1. **Plan 000 must be done first.** Before you change anything, run `./gradlew :shared:spotlessCheck`. If it fails, Plan 000 (`plans/000-make-the-gates-green.md`) has not landed: STOP and report that. Do not run `spotlessApply` to get past it; on an unprepared tree it rewrites about 36 unrelated files. Once Plan 000 has landed, the test and format commands in this plan behave as described, and `spotlessApply` only touches what you changed.
2. **Two commands fail for reasons no plan fixes.** `./gradlew :shared:compileKotlinIosSimulatorArm64` stops with five errors, all in `shared/src/iosMain/kotlin/com/bernaferrari/changedetection/browser/IosPageCapture.kt` (the first is `Unresolved reference 'kmpwright'`; a library was built with a newer Kotlin than this project uses). `./gradlew :webDemo:wasmJsBrowserDistribution` compiles the Kotlin and then stops at `:kotlinWasmStoreYarnLock` with `Lock file was changed`. Wherever this plan expects one of them to print `BUILD SUCCESSFUL`, the bar is instead: for iOS, **those same five errors and no others**; for the web demo, run `./gradlew :webDemo:compileKotlinWasmJs` and expect `BUILD SUCCESSFUL`. Never run `kotlinWasmUpgradeYarnLock`. If either command has since been repaired and passes, that is fine.
3. **Line numbers may have moved.** Two changes landed after this plan was written. Plan 000 re-formats about 36 files (layout only). The "failure experience" change (`plans/failure-experience-changes.md`) added code to `SyncEngine.kt`, `SitesDao.kt`, `Site.kt`, `CheckFailure.kt`, `DiagnosticTrace.kt`, `PageDetailScreen.kt`, `MainViewModel.kt`, `LegacyMigration.kt`, `DatabaseFactory.kt`, `AppContent.kt`, `AndroidChangeNotifier.kt`, the four `PlatformDiModule` files and the string files. Find code by the text this plan quotes, not by line number. A quoted block that differs only in line breaks or indentation is the same code. The drift check at the top of this plan will list those files as changed; that alone is not a reason to stop. Stop only if the quoted code is gone or does something different.

## Scope

**In scope:**

- `shared/src/androidMain/res/xml/data_extraction_rules.xml`
- `shared/src/androidMain/res/xml/backup_rules.xml`
- `PRIVACY.md`
- `plans/README.md` (status column only)

**Out of scope:**

- `androidApp/src/main/AndroidManifest.xml`. Do not change `allowBackup`, `dataExtractionRules` or `fullBackupContent`.
- Any Kotlin file. Do not add code that clears cookies; that is Plan 007.
- Do not switch the rules to `<include>` elements. An `<include>` changes the default from "everything" to "nothing else", which is a larger change than this plan intends.

## Git workflow

- Branch from `main`: `git switch -c advisor/004-exclude-browser-data-from-device-transfer`
- Commit subjects are plain sentences. Suggested: `Browser sign-ins stay on the device during phone transfer`.
- Do not push, merge, or open a pull request.

## Steps

### Step 1: Exclude WebView data in the Android 12+ rules

Edit `data_extraction_rules.xml` so that both sections gain one line, and the comment explains it:

```xml
<?xml version="1.0" encoding="utf-8"?>
<!--
  Local-only privacy posture: monitored URLs and snapshot history never leave the
  device via cloud backup. Device transfer is also excluded so the full browsing
  corpus is not exported over a cable without explicit in-app export.
  app_webview holds the in-app browser's cookies and site storage (sign-ins), which
  live under the data root and are not covered by the database/file/sharedpref domains.
-->
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="database" />
        <exclude domain="file" />
        <exclude domain="sharedpref" />
        <exclude domain="root" path="app_webview" />
    </cloud-backup>
    <device-transfer>
        <exclude domain="database" />
        <exclude domain="file" />
        <exclude domain="sharedpref" />
        <exclude domain="root" path="app_webview" />
    </device-transfer>
</data-extraction-rules>
```

**Verify:** `xmllint --noout shared/src/androidMain/res/xml/data_extraction_rules.xml`
Expected: no output.

### Step 2: Mirror it in the legacy rules

Edit `backup_rules.xml` to:

```xml
<?xml version="1.0" encoding="utf-8"?><!--
  Legacy (API < 31) backup rules. Local monitoring history and browser sign-ins stay on-device.
-->
<full-backup-content>
    <exclude domain="database" path="changes.db" />
    <exclude domain="root" path="app_webview" />
</full-backup-content>
```

**Verify:** `xmllint --noout shared/src/androidMain/res/xml/backup_rules.xml`
Expected: no output.

### Step 3: Say it in the privacy policy

In `PRIVACY.md`, add this section directly before the line `Changes to This Privacy Policy`, keeping the file's existing style (a plain title line followed by a paragraph, lines wrapped at about 120 characters):

```text
Backups and Moving to a New Phone
Your watched pages, their saved versions, your settings, and any cookies or sign-ins saved by the in-app browser are
excluded from Android cloud backup and from device-to-device transfer. They stay on the device where they were created.
If you sign in to a website inside the app, that sign-in is stored only in the app's private browser storage on your
device.
```

Change the date on line 3 to the date you make this change, in the same format (for example `## Last Updated: November 9, 2026`).

**Verify:** `rg -n "Backups and Moving to a New Phone" PRIVACY.md` prints one line.

### Step 4: Build gates

**Verify:** `./gradlew :androidApp:lintDebug :androidApp:assembleRelease`
Expected: `BUILD SUCCESSFUL`, and lint reports no new issue for either XML file. Check with: `rg -n "data_extraction_rules|backup_rules" androidApp/build/reports/lint-results-debug.txt` (no output means no finding; if the report file has a different name, list `androidApp/build/reports/` and use the `.txt` or `.xml` lint report there).

**Verify:** `./gradlew :shared:testAndroidHostTest`
Expected: `BUILD SUCCESSFUL`.

### Step 5 (optional, only with a device or emulator on Android 12 or later): observe the rule

This step is evidence for the maintainer; skip it and say so if no device is connected.

1. `./gradlew :androidApp:installDebug`
2. Open the app, add any page through the in-app browser so that WebView storage exists.
3. `adb shell run-as com.bernaferrari.changedetection ls` and confirm `app_webview` is listed. (If the debug build uses a different application id, read it from `androidApp/build.gradle.kts`, `applicationId` plus any `applicationIdSuffix`.)
4. Report the directory listing. Do not attempt to script a phone-to-phone transfer.

## Test plan

There is no automated test for backup rules in this repo, and adding one would need a device farm. Verification is: well-formed XML, a clean lint run (Android lint validates backup rule domains and paths), and the optional directory check in Step 5.

## Done criteria

- [ ] `rg -c 'domain="root" path="app_webview"' shared/src/androidMain/res/xml/data_extraction_rules.xml` prints `2`.
- [ ] `rg -c 'domain="root" path="app_webview"' shared/src/androidMain/res/xml/backup_rules.xml` prints `1`.
- [ ] Both XML files parse.
- [ ] `./gradlew :androidApp:lintDebug :androidApp:assembleRelease` is `BUILD SUCCESSFUL` with no lint finding on these files.
- [ ] `PRIVACY.md` contains the new section and an updated date.
- [ ] `git status --short` lists only in-scope files.

## STOP conditions

- Lint reports an error or warning on the new `<exclude>` lines (for example "path is not in an included path" or an unknown domain). Report the exact message; do not suppress it.
- Step 5 shows that WebView data is stored under a different directory name than `app_webview`. Report the listing.
- You find the manifest no longer references these two XML files.

## Maintenance notes

- If the app ever calls `WebView.setDataDirectorySuffix`, the directory name changes to `app_webview_<suffix>` and these rules need the new name.
- Plans 015 and 016 add an explicit in-app export. That is the intended way for a user to move their data; these rules should stay strict.
- Plan 007 adds a way to clear the saved sign-ins.
- Reviewers: any new storage location for user data (a new `getDir(...)`, external files, a second database) needs a matching exclude here.
