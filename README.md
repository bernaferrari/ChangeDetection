# Change Detection

Change Detection monitors public web pages from your device and saves a local snapshot when their content changes. It is free, open source, requires no account, and does not use a Change Detection cloud service.

## What the app does

- Monitor public HTTP and HTTPS pages.
- Check all enabled sites on demand or on a battery-friendly background schedule.
- Notify when a check detects updated content.
- Keep local version history and compare text, rendered HTML, images, or screenshots.
- Share a URL from another Android app directly into the add-site flow.
- Run short, high-frequency Turbo sessions for time-sensitive pages.

Authenticated pages and saved login cookies are not supported in the current release.

## Choosing what counts as a change

New monitors start with **Readable text**: compare text from the fetched response while ignoring markup, scripts, styles and explicitly hidden elements. The app does not execute JavaScript or infer visibility from external CSS in this mode. Preview the result before saving; choose **Screenshot** for content that only appears after JavaScript runs.

Screenshot monitors offer an optional **Visual change filter**: off, at least 0.1%, 1%, or 5% of compared pixels. It uses a reduced-size comparison, ignores tiny color shifts, and compares against the last saved version so small changes can accumulate. Image-size changes are always saved. It is off for existing and new monitors unless explicitly enabled; enabling it replaces a legacy byte filter. This setting applies to screenshot captures, which currently run on Android. Preview statistics are not a guarantee of full-resolution pixel identity.


In Text mode, **Part of the page** offers Whole page, Main content, and Advanced CSS. Main content selects semantic `main`, `[role=main]`, or `article` elements. Advanced CSS accepts an include selector and an optional selector for ignored elements. **Choose text on the page** opens the visual selection tools. Preview before saving: only the selected HTML is stored, and a missing region records a failure while keeping history. Blank include rules monitor the whole page except ignored elements. Region edits establish a new baseline without a change alert.

**Any source change** detects changes in the response content, including scripts and whitespace. **Page HTML** retains the existing normalized-HTML behavior, and **App default** follows Settings. Existing monitors keep their saved choice. Switching detection rules preserves history and establishes a new baseline without sending a change alert for the rule switch itself.

Older monitors with a positive byte threshold show a **Legacy byte filter** notice in the editor. This filter compares stored data rather than visible pixels. **Remove filter on save** removes it only when the edit is saved; renaming or cancelling an edit preserves it. Selecting Readable text or Any source change removes this extra filter on save.

Normal monitoring supports intervals from **15 minutes to 7 days**. Android can delay checks because of power management, connectivity and selected conditions; the requested interval is not an exact appointment.

## Turbo, precisely

Start Turbo from the lightning action in **Home**, the page-selection toolbar, or a page’s details. Setup applies only to the selected active pages; paused pages are excluded. When no previous selection exists, the app selects every enabled page by default. Review the selection before starting.

Turbo checks at 15-second, 30-second, 1-minute, or 2-minute intervals. Choose a duration from 15 minutes to three hours. It runs as an Android foreground service with a persistent notification and stops when its timer expires or you stop it. Normal background sync is configured separately in **Settings**. On Desktop, Turbo runs while the app is open.

## Repository map

| Path | Purpose |
| --- | --- |
| `androidApp/` | Android application shell, manifest, activity, and packaging |
| `shared/src/commonMain/` | Shared product models, data layer, view models, and Compose UI |
| `shared/src/androidMain/` | Android workers, foreground services, notifications, browser integration, and widgets |
| `shared/src/iosMain/` | iOS platform implementations; the iOS product remains in development |
| `iosApp/` | Swift application shell |
| `webDemo/` | Compose Multiplatform Wasm demo with synthetic local monitoring data |
| `web/` | Next.js marketing and documentation site |
| `website/` | New Next.js marketing site; CI currently verifies `web/` |

The active app architecture is Kotlin Multiplatform with Compose Multiplatform, Navigation 3, Koin, Room, DataStore, WorkManager, Ktor, and coroutines. Website content and snapshot metadata stay on the device; network requests naturally reach the pages the user chooses to monitor.

## Build and test

Android builds use the platform WebView for page capture and a bounded pixel comparison implementation. A fresh checkout needs no sibling repositories or unpublished artifacts. The Android verification CI job runs formatting, host tests, lint, debug/release assembly, and the instrumentation suite on an API 36 emulator. Test and lint reports are retained as workflow artifacts. The workflow is independent of the web-demo and marketing-site jobs. Configure `Android verification` as a required repository check before relying on it as a merge gate.

| Check | Command | Coverage |
| --- | --- | --- |
| Compose Desktop UI | `./gradlew :shared:desktopTest` | Shared monitoring tests and real Compose UI journeys, including enlarged text. No emulator. |
| Desktop native browser | `./gradlew :shared:desktopBrowserSmoke` | Bundled JCEF startup, real saved-HTML rendering/text readback, native capture and reopening/disposal. Uses a temporary cache and synthetic content. Requires JBR 25 with JCEF and a graphical session. |
| Android host tests | `./gradlew :shared:testAndroidHostTest` | Domain behavior and pure comparison UI logic using synthetic local data. |
| Android instrumentation | `./gradlew :shared:connectedAndroidDeviceTest` | Executes Room, browser, storage, notification and Compose tests on a connected device. Use `ANDROID_SERIAL=<device-serial>` to select an explicitly authorized device. UI iteration for this redesign uses Desktop, without starting an emulator. |
| Android device-test compile | `./gradlew :shared:compileAndroidDeviceTest` | Current Room, browser, and Compose semantic test sources. No device is required for compilation. |
| Android debug app | `./gradlew :androidApp:assembleDebug` | Android integration, browser adapter, and packaging. |
| iOS shared code | `./gradlew :shared:compileKotlinIosSimulatorArm64` | iOS implementation of the shared KMP surface. |
| Kotlin formatting | `./gradlew :shared:spotlessCheck` | Kotlin formatting and copyright headers for the Compose module. |
| Web demo | `./gradlew :webDemo:wasmJsBrowserDistribution` | Builds the Compose Multiplatform Wasm demo; CI also verifies its output files. |
| Website lint | `cd web && pnpm lint` | TypeScript and React static analysis. |
| Website production build | `cd web && pnpm build` | Type checking and static rendering of `/` and `/docs`. |

Tests and preview data must stay local and synthetic: do not make a verification command depend on a live monitored URL, saved browsing content, or credentials.

## Android publishing

[Gradle Play Publisher](https://github.com/Triple-T/gradle-play-publisher) handles optional Google Play uploads; no Ruby, Bundler, or Fastlane installation is needed. Regular builds and CI do not enable publishing.

Set `CHANGEDETECTION_SIGNING_PROPERTIES` to an external properties file containing `storeFile`, `storePassword`, `keyAlias`, and `keyPassword`. Use an absolute keystore path. Set `CHANGEDETECTION_PLAY_CREDENTIALS` to an external Google Play service-account JSON file. Complete the documented checks and set an unused `versionCode` in `androidApp/build.gradle.kts` before uploading.

```bash
# Upload an internal draft (the default).
./gradlew -PplayPublishing=true :androidApp:publishReleaseBundle

# Make the uploaded bundle available to internal testers.
./gradlew -PplayPublishing=true :androidApp:promoteReleaseArtifact --from-track internal --promote-track internal --release-status completed

# Promote that tested bundle to production.
./gradlew -PplayPublishing=true :androidApp:promoteReleaseArtifact --from-track internal --promote-track production --release-status completed
```

Publishing is disabled unless `-PplayPublishing=true` is supplied, and enabling it requires both credential files. Store listings remain managed in Play Console; these commands only publish or promote the app bundle. Build output is under `androidApp/build/outputs/bundle/release/`.

## Desktop UI development

Run `./gradlew :shared:hotRunDesktop` to open the shared app with Compose Hot Reload. Apply edits with `./gradlew :shared:hotReloadDesktopMain` while it runs. The Gradle toolchain resolver provisions JetBrains Runtime for Hot Reload. Run compilation/test commands sequentially so generated resource accessors and application classes stay consistent.

Desktop uses real Room storage, DataStore preferences, HTTP text checks, saved versions, and the shared Turbo engine. Its separate data directory is `~/.change-detection-desktop/`. Regular checks run while the desktop process remains open. Restarting retains history and configuration, marks a formerly running Turbo session as interrupted, and requires an explicit Start to begin another session. This development host does not currently implement desktop OS notifications or screenshot capture; it does not validate Android background execution.

Snapshot row deletion records pending file cleanup in the same SQLite transaction, including page cascades, history reset and retention. Cleanup reads at most 500 IDs per batch; failed files remain queued and retry while the process is running, including after restart with monitoring disabled. The queue handles committed deletions immediately; the age-gated orphan sweep remains a fallback for files left by interrupted writes. This is restart recovery, not a guarantee against filesystem corruption or permanent deletion failure.


The official Compose Hot Reload MCP server is available through `./gradlew --no-daemon --quiet --console=plain :shared:hotMcpServerDesktop` for semantic UI inspection and interaction. Desktop UI tests save rendered review captures in `build/desktop-review/`; these captures come from Compose rendering and remain valid when another application obscures the desktop window.

The native browser smoke check saves `build/desktop-review/native-browser-smoke.png` through Chromium itself. This verifies browser pixels independently of desktop screen capture, but does not prove the whole app window is visually correct. Restart the Desktop process after changing native-runtime initialization; Hot Reload cannot recreate the process-wide Chromium runtime. The smoke task uses a macOS accessory process to avoid the shared Java restore-windows prompt after a development crash; it does not change the app’s normal launch configuration.

Text capture offers Page and Text list selection views on Android, iOS and Desktop. The Page view renders sanitized fetched HTML and runs only the app’s hash-authorized selection script; external resources and original page scripts are disabled, so it may differ from the live website. Selection is a draft until confirmed, and both views resolve to the same original-source CSS rule. Reopening restores the existing watched/ignored areas; unchanged confirmation preserves the original rule. Wasm currently uses the text list. The native smoke check also exercises the selection/navigation bridge and saves `build/desktop-review/native-region-selection.png`. The picker supports multiple watched areas, persistent Watch/Ignore tools, restoration and Undo. Its Preview tab shows the extracted text with ignored content omitted before Use selection confirms the rule. Smaller/Larger adjust the active area in either Watch or Ignore mode. Confirmation validates the complete rule against the original fetched HTML and rejects empty results. The picker preserves page scroll position across scope/ignore changes and view switching. Some actions still recreate the browser view, so a brief redraw is possible. Full live-page styling remains refinement work.

## Privacy

See [PRIVACY.md](PRIVACY.md). The app does not require an account and has no first-party analytics or tracking service.

## License

Apache License 2.0. See [LICENSE](LICENSE).
