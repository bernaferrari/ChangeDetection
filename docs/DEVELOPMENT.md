# Developing Change Detection

A Kotlin Multiplatform app: one Compose UI in `:shared` for Android, desktop, iOS and a browser demo. `:androidApp` is the thin Android app that ships on Google Play.

[README](../README.md) · [README screenshots](screenshots/README.md) · [Store listing](../store-listing/README.md) · [Privacy](../PRIVACY.md)

## Quick start

```bash
git clone https://github.com/bernaferrari/ChangeDetection.git
cd ChangeDetection
./gradlew :androidApp:installDebug
```

## What you need

| Tool | Version | Notes |
| --- | --- | --- |
| JDK | 21 | What CI starts Gradle with. Gradle provisions the rest through Foojay. |
| JetBrains JDK | 25 | Desktop run and packaging; provisioned automatically. |
| Android SDK | platform 37.2 | Needed at configuration time even for desktop tasks. Set `ANDROID_HOME` or `sdk.dir` in `local.properties`. |
| Xcode | recent | iOS only. |
| Node.js and pnpm | 26.11.1, pnpm 12.11.2 | Website and Screenshot Studio only. |

## Run it

| Target | Command |
| --- | --- |
| Android | `./gradlew :androidApp:installDebug` (APK in `androidApp/build/outputs/apk/debug/`) |
| Desktop | `./gradlew :shared:run` |
| Browser demo | `./gradlew :webDemo:wasmJsBrowserDevelopmentRun` |
| iOS | Open `iosApp/iosApp.xcodeproj` in Xcode and run. |
| Website | `cd website && pnpm install && pnpm dev` |

## Where things live

| Path | What's there |
| --- | --- |
| `shared/` | The app: UI, detection, Room database, sync, Android widgets and workers. `src/commonMain` plus one source set per platform. |
| `androidApp/` | Android entry point (`MainActivity`), manifest, release signing and Play publishing. |
| `iosApp/` | Xcode project (`project.yml` is its XcodeGen spec) and the `AppIcon.icon`. |
| `webDemo/` | Wasm browser demo with sample data; it does not watch live pages. |
| `sqliteWasmWorker/` | SQLite worker used by the Wasm build. |
| `website/` | Next.js marketing site (pnpm). |
| `shared/schemas/` | Room schema history, kept for migration review. |
| `store-listing/` | Play Store artwork, icon layers and Screenshot Studio. |

## Before you push

```bash
./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest
./gradlew :shared:compileKotlinIosSimulatorArm64 :androidApp:lintDebug :androidApp:assembleRelease
./gradlew :webDemo:wasmJsBrowserDistribution
(cd website && pnpm lint && pnpm build)

# With an emulator connected
./gradlew :shared:connectedAndroidDeviceTest
```

[CI](../.github/workflows/ci.yml) runs all of these on every push and pull request, including the device tests on an API 36 emulator. Desktop test reports and renders land in `build/desktop-review/`.

The README screenshots come from the real shared UI on the desktop test host. Regenerate them with `./gradlew :shared:desktopTest --tests '*DesignReviewTest.readmeTour'`, then follow [screenshots/README.md](screenshots/README.md).

## Store screenshots and icon

Screenshot Studio lives in `store-listing/studio/`. It is synced from `~/Documents/screenshot-studio`, so never edit it here.

- Captures come from the instrumented `StoreCaptureTest` (`./gradlew :shared:assembleAndroidDeviceTest`). It seeds fictional sample sites only and turns the emulator's network off. Runs use the Blue palette: light for every language, plus dark phone and light tablets for English. `store-listing/capture-emulator.py` is the scene tour.
- Build, install, capture and export every language and size:

  ```bash
  node store-listing/studio/recapture.cjs
  node store-listing/studio/recapture.cjs en-US --profile phone --no-build
  ```

  It captures only on the dedicated emulator `CAPTURE_SERIAL` (default `emulator-5578`), never a personal phone. `--no-capture` skips straight to export.
- Export only: `PLAYWRIGHT_MODULE=… node store-listing/studio/export.cjs --locale en-US`. Live editor: `node store-listing/studio/server.cjs 8790`.
- Icon: layers in `store-listing/brand/*.svg`, targets in `store-listing/brand/icon.json` (Android, iOS `AppIcon.icon`, desktop `.icns`/`.ico`/`.png`, website and web demo). Regenerate them all with `PLAYWRIGHT_MODULE=… node store-listing/studio/icon.cjs`.

## Good to know

- Release signing reads an external properties file named by `CHANGEDETECTION_SIGNING_PROPERTIES`; without it, release builds are unsigned. Never commit keystores or signing properties.
- Play publishing is off unless `-PplayPublishing=true` and `CHANGEDETECTION_PLAY_CREDENTIALS` points at a service-account JSON outside the repo. It uploads to the internal track as a draft.
- Device tests and captures change emulator settings and install a test app. Use a disposable emulator.
- `shared/src/androidDeviceTest/assets/tls-fixture.p12` is a throwaway TLS fixture for tests, not a signing key.
