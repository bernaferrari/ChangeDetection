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

## Turbo, precisely

Turbo applies only to the sites selected on the **Turbo** tab. When no previous selection exists, the app selects every saved site by default; you can deselect any of them before starting.

Turbo checks at 15-second, 30-second, 1-minute, or 2-minute intervals. It runs as an Android foreground service with a persistent notification and stops automatically after three hours to limit battery use. Normal background sync is configured separately in **Settings**.

## Repository map

| Path | Purpose |
| --- | --- |
| `app/` | Android application shell, manifest, activity, and packaging |
| `composeApp/src/commonMain/` | Shared product models, data layer, view models, and Compose UI |
| `composeApp/src/androidMain/` | Android workers, foreground services, notifications, browser integration, and widgets |
| `composeApp/src/iosMain/` | iOS platform implementations; the iOS product remains in development |
| `iosApp/` | Swift application shell |
| `web/` | Next.js marketing and documentation site |

The active app architecture is Kotlin Multiplatform with Compose Multiplatform, Navigation 3, Koin, Room, DataStore, WorkManager, Ktor, and coroutines. Website content and snapshot metadata stay on the device; network requests naturally reach the pages the user chooses to monitor.

## Build and test

This checkout uses the Webwright browser libraries from the sibling `../../kotlin/webwright` checkout, as declared in `settings.gradle.kts`. Ensure that checkout is present before building.

| Check | Command | Coverage |
| --- | --- | --- |
| Android host tests | `./gradlew :composeApp:testAndroidHostTest` | Domain behavior and pure comparison UI logic using synthetic local data. |
| Android device-test compile | `./gradlew :composeApp:compileAndroidDeviceTest` | Current Room, browser, and Compose semantic test sources. No device is required for compilation. |
| Android debug app | `./gradlew :app:assembleDebug` | Android integration, browser adapter, and packaging. |
| iOS shared code | `./gradlew :composeApp:compileKotlinIosSimulatorArm64` | iOS implementation of the shared KMP surface. |
| Kotlin formatting | `./gradlew :composeApp:spotlessCheck` | Kotlin formatting and copyright headers for the Compose module. |
| Website lint | `cd web && pnpm lint` | TypeScript and React static analysis. |
| Website production build | `cd web && pnpm build` | Type checking and static rendering of `/` and `/docs`. |

Tests and preview data must stay local and synthetic: do not make a verification command depend on a live monitored URL, saved browsing content, or credentials.

## Privacy

See [PRIVACY.md](PRIVACY.md). The app does not require an account and has no first-party analytics or tracking service.

## License

Apache License 2.0. See [LICENSE](LICENSE).
