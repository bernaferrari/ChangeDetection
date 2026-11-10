# Plan 011: Make the website describe Turbo the way the app actually works

> **Executor instructions**
> You are implementing one small copy change. You have not seen the audit that produced this plan; everything you need is in this file.
> 1. Run the drift check first: `git diff --stat a9f8f8ba..HEAD -- website/app/page.tsx website/lib/use-cases.ts shared/src/commonMain/kotlin/com/bernaferrari/changedetection/background/TurboSession.kt`. If `TurboSession.kt` changed, re-read the three constants quoted under "Current state"; if their values differ, STOP and report. If the website files changed, find the sentences by their text instead of by line number.
> 2. Follow the steps in order and run each **Verify** command.
> 3. Touch only the files listed under "In scope".
> 4. If a STOP condition applies, stop and report. Do not improvise.
> 5. When finished, set this plan's row in `plans/README.md` to `DONE`.

## Status

| Field | Value |
| --- | --- |
| Priority | P2 |
| Effort | S (30 minutes) |
| Risk of the fix | Very low. Text only |
| Depends on | Nothing |
| Category | Documentation / marketing accuracy |
| Planned at | commit `a9f8f8ba`, 2026-11-09 |

## Why this matters

"Turbo" is the app's fast-checking mode for moments like a ticket release or a restock. The marketing website describes it in six places as checking "every 30 seconds while the app is open".

Both halves are inaccurate, and both undersell the feature:

- The pace is a choice of 5, 15, 30 or 60 seconds (30 is only the default), and a session lasts 15 minutes to three hours.
- On Android, Turbo runs as a foreground service with a notification. It keeps going after the user leaves the app. "While the app is open" tells people they must keep the screen on the app, which is the opposite of the reason to use it.

The repository's own `README.md` already says it correctly: "Turbo checks the pages you pick every 5 to 60 seconds for up to three hours." This plan brings the website in line with that sentence.

## Current state

The source of truth, `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/background/TurboSession.kt:26-28`:

```kotlin
const val TURBO_DURATION_MS = 3 * 60 * 60 * 1000L
val TURBO_INTERVAL_SECONDS = setOf(5, 15, 30, 60)
val TURBO_DURATION_MINUTES = setOf(15, 30, 60, 180)
```

The default pace is 30 seconds (`TurboSession.kt:37`, `val intervalSeconds: Int = 30`).

The sentences to change (text exactly as in the files):

`website/app/page.tsx:16`

```text
Turbo checks every 30 seconds while the app is open.
```

`website/lib/use-cases.ts:31`

```text
For drops and flash deals, Turbo checks every 30 seconds while the app is open.
```

`website/lib/use-cases.ts:49`

```text
Works on any store, checks as often as every 30 seconds with Turbo.
```

`website/lib/use-cases.ts:56`

```text
For limited releases, Turbo checks every 30 seconds while the app is open and shows a countdown to the next check.
```

`website/lib/use-cases.ts:63`

```text
With Turbo, pages are checked every 30 seconds while the app is open.
```

`website/lib/use-cases.ts:103`

```text
Turbo keeps checking every 30 seconds while the app is open, with a countdown to the next check.
```

Places that mention 30 seconds and are **correct enough to leave alone**, because 30 seconds is the default and they do not claim the app must stay open:

- `website/app/page.tsx:136`, the feature title `Turbo: every 30 seconds`
- `website/lib/use-cases.ts:98`, "(or every 30 seconds with Turbo)"
- `website/lib/site.ts:38`, the alt text of a screenshot that really shows a 30 second countdown
- `README.md:27` and the files under `store-listing/`, which describe store screenshots

The website is a Next.js app managed with pnpm. Its scripts (`website/package.json`): `lint` (`eslint`), `typecheck` (`tsc --noEmit`), `build` (`next build`).

## Commands you will need

| Purpose | Command | Expected |
| --- | --- | --- |
| Install website dependencies (only if `website/node_modules` is missing) | `(cd website && pnpm install --frozen-lockfile)` | exits 0 |
| Website checks | `(cd website && pnpm lint && pnpm typecheck && pnpm build)` | all three exit 0 |

The repo expects Node 26 and pnpm 12.

## Before you start: the state of the build

Checked on 2026-11-09 at commit `a9f8f8ba`. Read this before the steps; it overrides any line below that expects otherwise.

1. **Plan 000 must be done first.** Before you change anything, run `./gradlew :shared:spotlessCheck`. If it fails, Plan 000 (`plans/000-make-the-gates-green.md`) has not landed: STOP and report that. Do not run `spotlessApply` to get past it; on an unprepared tree it rewrites about 36 unrelated files. Once Plan 000 has landed, the test and format commands in this plan behave as described, and `spotlessApply` only touches what you changed.
2. **Two commands fail for reasons no plan fixes.** `./gradlew :shared:compileKotlinIosSimulatorArm64` stops with five errors, all in `shared/src/iosMain/kotlin/com/bernaferrari/changedetection/browser/IosPageCapture.kt` (the first is `Unresolved reference 'kmpwright'`; a library was built with a newer Kotlin than this project uses). `./gradlew :webDemo:wasmJsBrowserDistribution` compiles the Kotlin and then stops at `:kotlinWasmStoreYarnLock` with `Lock file was changed`. Wherever this plan expects one of them to print `BUILD SUCCESSFUL`, the bar is instead: for iOS, **those same five errors and no others**; for the web demo, run `./gradlew :webDemo:compileKotlinWasmJs` and expect `BUILD SUCCESSFUL`. Never run `kotlinWasmUpgradeYarnLock`. If either command has since been repaired and passes, that is fine.
3. **Line numbers may have moved.** Two changes landed after this plan was written. Plan 000 re-formats about 36 files (layout only). The "failure experience" change (`plans/failure-experience-changes.md`) added code to `SyncEngine.kt`, `SitesDao.kt`, `Site.kt`, `CheckFailure.kt`, `DiagnosticTrace.kt`, `PageDetailScreen.kt`, `MainViewModel.kt`, `LegacyMigration.kt`, `DatabaseFactory.kt`, `AppContent.kt`, `AndroidChangeNotifier.kt`, the four `PlatformDiModule` files and the string files. Find code by the text this plan quotes, not by line number. A quoted block that differs only in line breaks or indentation is the same code. The drift check at the top of this plan will list those files as changed; that alone is not a reason to stop. Stop only if the quoted code is gone or does something different.

## Scope

**In scope:**

- `website/app/page.tsx` (one sentence)
- `website/lib/use-cases.ts` (five sentences)
- `plans/README.md` (status column only)

**Out of scope:**

- `website/lib/site.ts`, `README.md`, everything under `store-listing/` (generated store assets and their captions).
- Any layout, component or styling change.
- App strings under `shared/`.
- Claims about iPhone. The website already says that on iPhone the system decides when background checks run; leave that sentence as it is.

## Git workflow

- Branch from `main`: `git switch -c advisor/011-website-turbo-copy`
- Commit subjects are plain sentences. Suggested: `The website describes Turbo's real pace and that it keeps running in the background`.
- Do not push, merge, or open a pull request.

## Steps

### Step 1: Replace the six sentences

Make exactly these replacements. Keep the surrounding text, quotes and punctuation of each line as they are.

| File and line | Replace | With |
| --- | --- | --- |
| `website/app/page.tsx:16` | `Turbo checks every 30 seconds while the app is open.` | `Turbo checks the pages you pick every 5 to 60 seconds for up to three hours.` |
| `website/lib/use-cases.ts:31` | `For drops and flash deals, Turbo checks every 30 seconds while the app is open.` | `For drops and flash deals, Turbo checks the pages you pick every 5 to 60 seconds for up to three hours.` |
| `website/lib/use-cases.ts:49` | `Works on any store, checks as often as every 30 seconds with Turbo.` | `Works on any store, and checks as often as every 5 seconds with Turbo.` |
| `website/lib/use-cases.ts:56` | `For limited releases, Turbo checks every 30 seconds while the app is open and shows a countdown to the next check.` | `For limited releases, Turbo checks every 5 to 60 seconds for up to three hours and shows a countdown to the next check. On Android it keeps going after you leave the app.` |
| `website/lib/use-cases.ts:63` | `With Turbo, pages are checked every 30 seconds while the app is open.` | `With Turbo, the pages you pick are checked every 5 to 60 seconds for up to three hours.` |
| `website/lib/use-cases.ts:103` | `Turbo keeps checking every 30 seconds while the app is open, with a countdown to the next check.` | `Turbo keeps checking every 5 to 60 seconds for up to three hours, with a countdown to the next check.` |

**Verify:** `rg -n "while the app is open" website/app website/lib`
Expected: no output.

**Verify:** `rg -c "5 to 60 seconds" website/app/page.tsx website/lib/use-cases.ts`
Expected: `website/app/page.tsx:1` and `website/lib/use-cases.ts:4`.

### Step 2: Website checks

**Verify:** `(cd website && pnpm lint && pnpm typecheck && pnpm build)`
Expected: all three commands exit 0. If `node_modules` is missing, run the install command from the table first.

The build may write to `website/.next/`, which is ignored by git. `git status --short` must show only the two website source files and `plans/README.md`.

## Test plan

No automated test covers marketing copy. Verification is the two searches in Step 1 plus the website's lint, type check and build.

## Done criteria

- [ ] `rg -n "while the app is open" website/app website/lib` prints nothing.
- [ ] `rg -n "as often as every 30 seconds" website` prints nothing.
- [ ] `(cd website && pnpm lint && pnpm typecheck && pnpm build)` exits 0.
- [ ] `git status --short` lists only `website/app/page.tsx`, `website/lib/use-cases.ts` and `plans/README.md`.

## STOP conditions

- `TURBO_INTERVAL_SECONDS` no longer contains 5 and 60, or `TURBO_DURATION_MS` is no longer three hours. The replacement text would then be wrong; report the new values.
- A sentence listed above is not found. Report which one; do not rewrite nearby text to compensate.

## Maintenance notes

- The numbers in marketing copy come from three constants in `TurboSession.kt`. Whoever changes those constants should search the website and `README.md` for "seconds" and "three hours".
- The sentence "On Android it keeps going after you leave the app" rests on Turbo being a foreground service (`QuickMonitorService`). If Turbo ever becomes foreground-only, remove it.
- The store listing headline "Check every 30 seconds." (`store-listing/en-US/copy.json`) was left alone: it is a headline about the default, and the store assets are generated from that file.
