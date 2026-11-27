# Improvement plans for Change Detection

Written 2026-11-09 against commit `a9f8f8ba` on `main`.

Each numbered file is a complete work order for one change. A plan is written so that someone who has never seen this repository, or the audit behind it, can carry it out: it quotes the code as it was, gives the commands to run, and says when to stop.

**How to use a plan:** open it, read "Before you start: the state of the build", run the drift check (it tells you whether the code moved since the plan was written), then follow the steps in order. Update the Status column below when you start and finish.

**Two things to do before any plan:**

1. Commit the failure-experience change that is sitting in the working tree (see "Code changed after planning"). The plans' drift checks compare commits and cannot see uncommitted work.
2. Do Plan 000. The repository's own checks fail at `a9f8f8ba`, so until it lands no plan can tell its own breakage from what was already broken.

## Order of work and status

Status is one of `TODO`, `IN PROGRESS`, `DONE`, `BLOCKED`, `REJECTED`.

| # | Plan | Priority | Effort | Depends on | Status |
| --- | --- | --- | --- | --- | --- |
| 000 | [Make the format check and the desktop tests pass](000-make-the-gates-green.md) | P0, unblocks the rest | S | the failure-experience change is committed | DONE |
| 001 | [Stop the database from silently erasing itself when a migration is missing](001-narrow-destructive-database-fallback.md) | P1, data loss | S | 000 | DONE |
| 002 | [Keep the app alive when a web page crashes the browser renderer](002-survive-webview-renderer-crash.md) | P1, crash | S to M | | DONE |
| 003 | [Make the saved-page viewer refuse navigation and stop re-emitting unchecked style text](003-harden-saved-page-viewer.md) | P1, security | S | | DONE |
| 004 | [Keep in-app browser cookies and sign-ins out of device transfer and backup](004-exclude-browser-data-from-device-transfer.md) | P1, privacy | S | | DONE |
| 005 | [A check that already saved its result must not be recorded as "timed out"](005-timeout-after-saved-result.md) | P1, bug | S | first of the `SyncEngine.kt` chain | DONE |
| 006 | [Decode fetched pages with the charset they declare](006-decode-fetched-pages-correctly.md) | P2, bug | S to M | 005 | DONE |
| 007 | [Let people delete the in-app browser's saved sign-ins from Settings](007-sign-out-of-sites-in-settings.md) | P2, privacy | M | | DONE |
| 008 | [Stop re-reading full screenshots from disk for cached thumbnails](008-load-thumbnails-through-coil.md) | P2, performance | S to M | | DONE |
| 009 | [Remove code, fonts and dependencies that nothing uses](009-remove-dead-code-and-unused-dependencies.md) | P2, tech debt | S to M | 005, 006 | DONE |
| 011 | [Make the website describe Turbo the way the app works](011-website-turbo-copy.md) | P2, accuracy | S | | DONE |
| 010 | [Give `SyncEngine` one definition each for "same page settings" and "is an image"](010-syncengine-single-definitions.md) | P3, tech debt | S | 005, 006, 009 | DONE |
| 012 | [Fix the visibly broken French, Russian, Indonesian and Arabic strings](012-fix-broken-translations.md) | P3, localisation | S | | DONE |
| 013 | [Make the privacy policy and the licence list match what the app ships](013-privacy-policy-and-licence-notices.md) | P3, compliance | S | reads better after 004, 007, 016 | DONE |
| 014 | [Fix two git-ignore traps and add a root guide for contributors and coding agents](014-repo-hygiene-and-agent-guide.md) | P3, hygiene | S | | DONE |
| 015 | [Build the backup engine: pages, settings and (optionally) saved versions](015-backup-core.md) | Direction | M to L | | DONE |
| 016 | [Add "Backup" to Settings on Android](016-backup-ui.md) | Direction | M | 015 | DONE |
| 017 | [Spike: can the app read "the number" on a page well enough to alert on a condition?](017-numeric-conditions-spike.md) | Direction | S, time-boxed | | DONE |

### Ordering rules

- **000 comes first, for every plan.** It reformats about 35 files, so doing it later means conflicts with whatever is in flight.
- **`SyncEngine.kt` is edited by four plans. Do them in this order: 005 → 006 → 009 → 010.** Each one's line numbers assume the earlier ones have landed; each drift check says what to re-read.
- **016 needs 015.** 015 can ship alone (it has no screen), 016 cannot.
- After 000, everything else is independent and can be done in any order or in parallel.
- If time is short: 001 to 005 are the ones that protect people's data and the app's stability. 015 and 016 are the feature people will notice.

Plan 017 found that explicit number selection is viable for a narrow watched region. See [the numeric conditions spike](../docs/research/numeric-conditions.md).

Plan 012's counts and sample matches are in [Translation checks](../docs/research/translation-checks.md).

The optional unused DAO query cleanup in Plan 009 was skipped. Plan 004 verified the backup rules and observed the WebView data directory on an emulator. Actual device-to-device transfer was outside the verification run.

Plan 016 was exercised through the Android 16 system file picker with two fixture pages and three saved versions. Both exports were inspected. The walkthrough covered repeat ZIP restore, restoring a deleted page and its two versions, rejecting an unrelated JSON file, restoring pages and settings after clearing app data, and rotation before and after an action.

## Backup, in one paragraph

Two files, the person chooses. **Pages and settings** is a small readable `.json` with the watched pages, their options and the app's settings, and no saved versions; it is the first choice in the dialog. **Everything** is a `.zip` with the same content plus every saved version. Restoring accepts either, only adds pages that are not on the device, and never changes what is there. Cookies and sign-ins are never in a backup.

## Code changed after planning

While the plans were being written, the maintainer asked for the failure experience (what the app says and offers when a check fails) to be improved directly in the code instead of through a plan. That work is described in [`failure-experience-changes.md`](failure-experience-changes.md). It landed as the prerequisite commit `896bc650`. It raised the database to version 6 and touched files that several plans quote, so those plans’ drift checks report changes. What each plan has to know is listed in that document.

## Checks that fail at `a9f8f8ba`

Each of these was reproduced on an untouched copy of the commit.

| Check | What fails | Fixed by |
| --- | --- | --- |
| `./gradlew :shared:spotlessCheck` | About 35 files are not formatted, and one lint error at `MonitoringBatch.kt:37` that the formatter cannot fix by itself | Plan 000 |
| `./gradlew :shared:desktopTest` | One test, `DesignReviewTest.largeTextTour`: at large text the Settings list has not yet built the row the test looks for | Plan 000 |
| `./gradlew :shared:compileKotlinIosSimulatorArm64` | Five unresolved references to the screenshot library in `IosPageCapture.kt` | Direct WebKit snapshots on iOS |
| `./gradlew :webDemo:wasmJsBrowserDistribution` | The generated Yarn lockfile omitted the SQLite worker and the patched WebSocket dependency | A Wasm-specific worker dependency and restored WebSocket resolution |

Plan 000 was tried on a copy first: with its changes, `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest` passes.

## Open questions for the maintainer

The Chinese, Hindi and Bengali fonts are subsets. `LocalizationTest` checks each translation against its bundled font. See [the font subset guide](../third_party/notosanssc-subsets.md) for changes to the Chinese font.

## Looked at and deliberately not planned

| Finding | Why there is no plan |
| --- | --- |
| Transparent pixels in `BoxDownscaler` | Not reachable: desktop and iOS decoders already force opaque alpha before rows are added, and Android browser screenshots are opaque |
| Shell tests for `SyncWorker` and `QuickMonitorService` | Would need Robolectric or device tests for little logic; the logic worth testing is covered through `AndroidCheckRunner` and the engine |
| An `.editorconfig` | Spotless defines the format; once Plan 000 lands its check passes and enforces it |
| Turning JavaScript off in the desktop saved-page viewer | Desktop is a development build; Plan 003 covers the shipped Android path |
| One version number for desktop, iOS and the User-Agent | Cosmetic for development builds; recorded as a maintenance note in Plan 013 |
| Legacy import losing write-ahead-log contents | Only affects a one-time import from a very old version |
| A charset value in quotes being mis-read | Fixed as part of Plan 006 |
| `evaluateJavascript` result size limit | No failing case found; the capture path already bounds what it asks for |
| Wi-Fi and charging switches on iOS | iOS is a development build without background constraints to honour |
| iOS background task expiry | Same reason |
| The baseline being re-read on unchanged checks | Deliberate; pinned by `FullReviewBaselineIntegrityTest` |
| Screenshot decoded twice, checks run one after another, watchlist re-queried, icon sweep at start | Measured as small next to network and render time; revisit only with a profile showing otherwise |
| Compose beta in production | A maintainer decision already taken with eyes open |
| Collapsing the repository layer; splitting `SyncEngine` | Large refactors with no user-visible gain today; Plan 010 does the small, safe part |
| Gradle worker cap; Navigation 3 declared twice | Harmless |
| Continuous integration | Excluded at the maintainer's request |

## What was not audited

- `store-listing/` (store text and images).
- The internals of the KMPwright dependency.
- Known vulnerabilities in dependencies (no audit tool was run against a vulnerability database).
- Desktop and web-demo internals beyond what shared code exercises.
- Screen layout and visual design.
- Translation quality beyond the strings named in Plan 012.
- Whether the repository's CI passes.

## Things stated without proof

Be aware of these when reading the plans:

- **No plan's code was compiled, except Plan 000's**, which was applied to a copy and passed. Every plan has a red-then-green test sequence so that mistakes surface early, and STOP conditions for when they do.
- `failure-experience-changes.md` records the verification performed when the original failure-experience change landed.
- Plans 001 to 017 were written before the failure-experience change and Plan 000. Their line numbers were corrected where the change was known to matter (001, 005, 015), and every plan carries a note on what moved; the rest were not re-checked line by line.
- Plan 003: whether the SVG/MathML/CDATA style payloads actually reach the viewer was not demonstrated; the plan's tests characterise it and the executor must report what they show.
- Plan 004: on-device behaviour of device-to-device transfer was reasoned from the platform documentation, not observed.
- Plan 002: `chrome://crash` may behave differently on some emulator images.
- Plans 007, 012 and 016, and the failure-experience strings: all translations are drafts by the author and need review by native speakers.
- Plan 016: file pickers cannot be unit-tested here; the plan ends with a manual device check.
