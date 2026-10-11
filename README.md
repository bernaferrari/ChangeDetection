# Change Detection

Know when a web page changes. Change Detection monitors pages from your device, saves their history, and notifies you of updates.

Free and open source. No account, cloud service, or first-party tracking.

## Features

- Compare readable text, HTML, images, or screenshots.
- Watch a specific part of a page and ignore the rest.
- Schedule background checks or check on demand.
- Review saved versions and see what changed.
- Use Turbo for short sessions with more frequent checks.

Android is the main app. Screenshot capture and background monitoring are Android features. Desktop monitoring runs while the app is open; iOS is still in development. Android screenshots can reuse a browser login; text and source monitoring cannot.

The app supports English, Spanish, Italian, Portuguese, German, Hindi, Bengali, and Simplified Chinese. It follows your device language; Android 13 and newer also let you choose an app language in system settings.

## Run locally

For Android, use JDK 21 and Android SDK platform `android-37.2`. The Gradle wrapper handles Gradle installation.

```bash
# Build the Android debug APK
./gradlew :androidApp:assembleDebug

# Run the desktop app with hot reload
./gradlew :shared:hotRunDesktop

# Package the desktop app for your current OS
./gradlew :shared:packageDistributionForCurrentOS

# Run the browser demo
./gradlew :webDemo:wasmJsBrowserDevelopmentRun
```

For the marketing website, use Node 26.11.1 and pnpm 12.11.2:

```bash
cd website
pnpm install --frozen-lockfile
pnpm dev
```

For iOS, open `iosApp/iosApp.xcodeproj` in Xcode; simulator builds require Apple silicon. Edit its layered app icon in Apple's Icon Composer.

Desktop packages use the same icon on macOS, Windows, and Linux. macOS packaging requires Xcode for the layered icon.

## Project structure

| Directory | Purpose |
| --- | --- |
| `shared/` | Kotlin Multiplatform app logic, Compose UI, and platform integrations |
| `androidApp/` | Android application and packaging |
| `iosApp/` | iOS application and Xcode project |
| `webDemo/` | Browser demo of the shared app, using sample data |
| `website/` | Next.js marketing website |
| `sqliteWasmWorker/` | SQLite support for the browser demo |

## Checks

```bash
./gradlew spotlessCheck :shared:testAndroidHostTest :shared:desktopTest :androidApp:lint
```

For the website, run `pnpm lint` and `pnpm build` from `website/`.

Database upgrades preserve history from the original public schema (1) to schema 4. Retired development databases may reset their data.

## Android publishing

Publishing uses Gradle Play Publisher. Set `CHANGEDETECTION_SIGNING_PROPERTIES` to an external signing file containing `storeFile` (absolute path), `storePassword`, `keyAlias`, and `keyPassword`. Set `CHANGEDETECTION_PLAY_CREDENTIALS` to a Play service-account JSON file, and increase `versionCode` before uploading.

```bash
# Upload a draft to the internal track
./gradlew -PplayPublishing=true :androidApp:publishReleaseBundle
```

See [Privacy](PRIVACY.md) and the [Apache 2.0 license](LICENSE).
