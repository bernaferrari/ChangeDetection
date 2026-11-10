# Failure experience: what was changed in the code, and what it means for the plans

Written 2026-11-09. Base commit `a9f8f8ba`. **The change is in the working tree and not committed.**

This is a record, not a work order. Plans 000 to 017 are work orders; this file describes a change that was made directly, at the maintainer's request, after most of those plans were written. Read it if a plan's drift check lists a file you did not expect to have changed.

## What the problem was

When a check failed, the app often could not say why, never said that a page had stopped being watched, and never tried another way.

- A screenshot that could not be taken because the page kept moving was reported as "Timed out", with the advice "usually temporary". It is not temporary: the same page fails the same way every time.
- A site answering 400, 405, 451 and similar was reported as a connection problem.
- "Access denied" offered one button, "Sign in", although most refusals are bot walls.
- A page could fail for weeks and the only sign was a red dot in the list.
- The one alternative that often works (checking the page's text instead of a screenshot or the raw source) was three taps deep, inside a diagnostics dialog.

## What changed, for the person using the app

1. **The real cause is named.** A new failure, "Page didn't finish loading", for pages that never hold still. Refusals other than 401/403/404/410/429 are "Access denied" instead of "Connection problem". "Access denied" now leads with Retry and offers "Sign in" second. The page shows "Failed checks in a row: N" from the second failure.
2. **One notification when a page stops being watched** (Android). Sent once per run of failures, withdrawn when the page works again, and tapping it opens the page. When it fires:

   | Kind of failure | Examples | Notified after |
   | --- | --- | --- |
   | Only the person can fix it | sign-in needed, page gone, chosen part missing, capture not possible | 2 failed checks in a row |
   | May pass by itself | server error, timeout, connection, refused, rate limit | 3 failed checks spread over 6 hours, or 2 over 24 hours |
   | The device is the cause | storage full, background restricted | never (every page fails together) |

   Never for a paused page or a page with notifications turned off.
3. **A working alternative is found and offered** (Android and iOS). When a failing page is opened and Text is a genuinely different route for that failure, the app quietly loads the page's text. If that works, a card says "Text works for this page" with a "Switch to Text" button. The app never switches by itself. Nothing is offered for failures where another route cannot help (server down, page gone, no connection).

   | Failure | Text is tried when the page is checked by |
   | --- | --- |
   | Page didn't finish loading, capture incomplete, capture unavailable, page too large | anything except Text |
   | Timeout, only from the second failure in a row | anything except Text |
   | Sign-in needed, access denied, unusable content | Source or HTML (these are fetched without the in-app browser; Text goes through it and reuses its sign-ins) |

## What changed in the code

| Area | Files | Change |
| --- | --- | --- |
| Classification | `error/CheckFailure.kt`, `diagnostics/DiagnosticTrace.kt` | `FailureUrgency` (`NEEDS_YOU`, `MAY_PASS`, `DEVICE`) on every `CheckFailure`; new `PAGE_UNSETTLED`; other 4xx map to `ACCESS_DENIED` |
| Storage | `repo/Site.kt`, `repo/source/local/SitesDao.kt`, `ChangeDatabase.kt`, `LegacyMigration.kt`, `DatabaseFactory.kt`, `shared/schemas/.../6.json` | Three columns on `sites` (`failureCount`, `failingSince`, `failureAlerted`); **database version 6** with `MIGRATION_5_6`; the streak is kept inside the SQL of `updateSyncMetadata`; `claimFailureAlert`; `switchToTextIfCurrent` now accepts any mode except Text |
| Alert rule | `sync/FailureAlerts.kt` (new) | `FailureNotifier`, `shouldAlertFailure` |
| Engine | `sync/SyncEngine.kt` | New last constructor parameter `failureNotifier`; alert after a recorded failure; withdrawal after a success |
| Android | `sync/AndroidFailureNotifier.kt` (new), `sync/AndroidChangeNotifier.kt`, `di/PlatformDiModule.android.kt` | The notification (channel `page_problems_v1`) |
| Other platforms | `PlatformDiModule.{desktop,ios,wasmJs}.kt`, `di/CoreDiModule.kt` | No-op notifier wired in |
| Recovery rule | `error/FailureRecovery.kt` (new), `background/MonitoringCapabilities*.kt` | `textMayWork`; `browserText` capability (true on Android and iOS) |
| Screen | `ui/detail/PageDetailScreen.kt`, `ui/main/MainViewModel.kt`, `ui/components/CheckFailureNotice.kt`, `error/ErrorPresentation.kt` | Failure card, `TextRecovery`, the offer |
| Navigation | `AppContent.kt` | A notification without a version opens the page instead of a comparison |
| Text | 12 compose locales, 12 Android resource locales | 8 new compose strings, `advice_login` rewritten, 6 notification strings |

Tests added: `FailureAlertPolicyTest`, `FailureRecoveryTest` (both JVM targets), `FailureStreakTest` (desktop, real database), six cases in `PageDetailUiTest`, `versionFiveUpgradeKeepsPagesAndStartsFailureStreaksEmpty` in `SchemaUpgradeTest`, `AndroidFailureNotifierTest` (device). Tests updated: `ResourceCaptureFailureTest`, `CheckFailureTest`, `SchemaUpgradeTest`, `LogsViewModelDeleteUndoTest` (fake DAO), `SnapsDaoTest` (device).

## What was checked, and what was not

Checked on 2026-11-09:

- `./gradlew :shared:testAndroidHostTest`: 516 tests, all pass.
- `./gradlew :shared:desktopTest`: 415 tests, one failure, `DesignReviewTest.largeTextTour`. The same test fails on an untouched copy of `a9f8f8ba`; Plan 000 fixes it.
- `./gradlew :androidApp:lintDebug :androidApp:assembleRelease`: pass (0 lint errors).
- `./gradlew :shared:compileAndroidDeviceTest`: passes.
- Web demo: `:shared:compileKotlinWasmJs` and `:webDemo:compileKotlinWasmJs` pass. Packaging fails at the lock-file step exactly as it does at `a9f8f8ba`.
- Formatting: the changed files were run through the formatter in a scratch copy; nothing in the new code needed re-formatting.

Not checked:

- **Nothing was run on a device.** `AndroidFailureNotifierTest` compiles and was not run. The notification, the tap that opens the page, and the Text trial through the real in-app browser are untested on Android. Run `./gradlew :shared:connectedAndroidDeviceTest` on an emulator before releasing.
- **iOS did not compile, before or after** (see Plan 000, "Known failures this plan leaves alone"). The two iOS edits are one named argument and one provider function.
- All translations are drafts by the author and need review by native speakers.

## Decisions worth knowing about

- **Opening a failing page costs one page load** when Text is a different route for that failure, because the trial runs without being asked. It does not run while a check is in progress, and runs once each time the page is opened, not on every redraw.
- **A page checked by "Default" is treated as going through the browser.** If the app-wide default is set to Source, a "Default" page that hits a sign-in wall is not offered Text. Conservative on purpose: the screen does not know the app-wide default.
- **A notification is claimed before it is posted.** If posting fails, that run of failures gets no notification. A repeated alert was judged worse than a missed one, since the page shows the failure either way.
- **Version 6 was once a development-only schema number.** `SchemaUpgradeTest.retiredDevelopmentSchemasStartFresh` no longer lists 6. A developer device still holding the retired version 6 must clear app data.
- **On iOS a notification link without a version now opens the page**, where it used to open the latest comparison. On Android change alerts always carry a version, so nothing changes there.
- Notifications for deleted or paused pages are not withdrawn. Tapping one for a deleted page opens the screen the app already shows for a page that no longer exists.

## What each plan has to know

Each plan now has a section "Before you start: the state of the build" that covers the general points. Plan-specific consequences, already written into the plans:

| Plan | Consequence |
| --- | --- |
| 001 | Rewritten for version 6: `PUBLIC_MIGRATIONS` must contain `MIGRATION_5_6`; the retired-schema test lists `2, 3, 10`; six tests are expected in `SchemaUpgradeTest` |
| 005, 006, 009, 010 | `SyncEngine.kt` gained 31 lines: one each at lines 78, 410, 517 and 566, and 27 from line 571. `recordTimeout` in Plan 005 goes through `processFetchResultUnlocked`, so timeouts recorded that way count towards the streak and the alert with no extra work |
| 005 | The status query is now at `SitesDao.kt:187-209` and still sets `timestamp` on every call |
| 007, 016 | Chinese drafts reworded to characters the bundled font contains (see below) |
| 012 | None of its seventeen strings were touched. `advice_login`, which is not in that plan, was rewritten in all twelve languages |
| 015 | The import resets `failureCount`, `failingSince` and `failureAlerted` along with the old error, and the test asserts it |

## Found on the way

- **The bundled Chinese font is a subset** (562 characters above ASCII). `LocalizationTest` fails on any Chinese string that uses a character outside it. Four characters in the new strings and five in the drafts of Plans 007 and 016 were outside; all were reworded. The usual word for "backup" cannot be written with this font, so Plan 016's Chinese title says "Save and restore". Extending the font is described in `third_party/notosanssc-subsets.md` and is a maintainer decision.
- **The format check, one desktop test, the iOS compile and the web demo packaging all fail at `a9f8f8ba`.** Plan 000 fixes the first two and describes the other two.
- An older device test, `SnapsDaoTest.retiredDevelopmentSchemasStartFresh`, listed version 5 (the then-current version) as retired. It now lists `2, 3, 10`. Not run.
