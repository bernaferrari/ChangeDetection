# Plan 000: Make the format check and the desktop test suite pass again

> **Executor instructions**
> You are implementing one self-contained repair. You have not seen the audit that produced this plan; everything you need is in this file.
> 1. Run the drift check first: `git diff --stat a9f8f8ba..HEAD -- shared/src/androidMain/kotlin/com/bernaferrari/changedetection/background/MonitoringBatch.kt shared/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/settings/SettingsScreen.kt shared/src/desktopTest/kotlin/com/bernaferrari/changedetection/review/DesignReviewTest.kt`. If it prints anything, compare the three files with "Current state" below before you continue; if the quoted lines are gone, STOP.
> 2. Follow the steps in order and run each **Verify** command.
> 3. Touch only what "In scope" lists. Step 2 runs a formatter over the whole module on purpose; that is the one place where many files change.
> 4. If a STOP condition applies, stop and report. Do not improvise.
> 5. When finished, set this plan's row in `plans/README.md` to `DONE`.

## Status

| Field | Value |
| --- | --- |
| Priority | P1. Every other plan uses these commands to judge its own work |
| Effort | S (under 1 hour, mostly waiting for the test suite) |
| Risk of the fix | Low. One pair of parentheses, a formatter run, one test tag and three lines in a test |
| Depends on | Nothing. **Do this plan before any other** |
| Category | Build health |
| Planned at | commit `a9f8f8ba`, 2026-11-09 |

## Why this matters

The other plans in this folder tell you to prove your work with `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest`. At commit `a9f8f8ba` that command fails **before anyone changes anything**, for two reasons that have nothing to do with those plans:

1. The format check fails. About 35 Kotlin files are not formatted the way the checker wants, and one line has a lint error the formatter cannot fix by itself.
2. One desktop test, `DesignReviewTest.largeTextTour`, times out.

A check that is already red cannot tell you whether your change broke something. Worse, the usual reflex (run the auto-formatter) rewrites those 35 unrelated files in the middle of someone else's change.

This plan makes both green. It was tried on a clean copy of commit `a9f8f8ba` on 2026-11-09: with exactly the changes below, `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest` ended in `BUILD SUCCESSFUL`.

Two other commands also fail at that commit. They are **not** fixed here, because the right fix is a decision for the maintainer. They are described under "Known failures this plan leaves alone" so that you recognise them.

## Current state

### The lint error

`shared/src/androidMain/kotlin/com/bernaferrari/changedetection/background/MonitoringBatch.kt:37`:

```kotlin
        state = state.copy(remaining = state.remaining - site.id, retryNeeded = state.retryNeeded || result is SyncResult.Failed && result.error.isRetryable)
```

The checker (ktlint rule `standard:mixed-condition-operators`) refuses `a || b && c` without parentheses. In Kotlin `&&` binds tighter than `||`, so the line already means `a || (b && c)`. Adding the parentheses changes nothing about what the code does.

### The unformatted files

`./gradlew :shared:spotlessCheck` prints `The following files had format violations:` followed by the first file and a diff. The differences are line breaks and indentation only (for example `if (x) { a();return false }` on one line becomes three lines).

### The failing test

`shared/src/desktopTest/kotlin/com/bernaferrari/changedetection/review/DesignReviewTest.kt:303-305` (inside the private function `tour`):

```kotlin
        onNodeWithText("Settings").performClick()
        waitForText("About")
        onNodeWithText("About").performScrollTo().performClick()
```

`largeTextTour` runs this tour with the text 1.8 times larger. The Settings screen is a lazy list: rows that are far below the visible area are not built until the list is scrolled. At normal text size the "About" row is close enough to exist. At 1.8 times it is not, so `waitForText("About")` waits ten seconds and fails with `ComposeTimeoutException ... DesignReviewTest.kt:389`.

The list is in `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/settings/SettingsScreen.kt:131`:

```kotlin
            LazyColumn(Modifier.widthIn(max = 720.dp).fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp)) {
```

Its items have keys. The "About" row is inside the item with key `"help"` (`item(key = "help") {`, around line 189). The list has no test tag, so a test cannot scroll it by key. Other lists in the app do have one (`home-list`, `page-details`, `about-list`), and this same test file scrolls them with `onNodeWithTag("home-list").performScrollToKey("row-exam")`. The file already has a helper `waitForTag(tag: String)`.

## Commands you will need

| Purpose | Command | Expected when done |
| --- | --- | --- |
| Format check | `./gradlew :shared:spotlessCheck` | `BUILD SUCCESSFUL` |
| Auto-format | `./gradlew :shared:spotlessApply` | `BUILD SUCCESSFUL` |
| The repaired test | `./gradlew :shared:desktopTest --tests '*DesignReviewTest*'` | `BUILD SUCCESSFUL` |
| Full gate | `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest` | `BUILD SUCCESSFUL` |

The build needs JDK 21 and Android SDK platform 37.2. The full gate takes about five minutes.

## Known failures this plan leaves alone

Do not try to fix these. If a later plan tells you to run one of these commands, this is what it does at commit `a9f8f8ba`:

| Command | What happens | Why |
| --- | --- | --- |
| `./gradlew :shared:compileKotlinIosSimulatorArm64` | Five errors, all in `shared/src/iosMain/kotlin/com/bernaferrari/changedetection/browser/IosPageCapture.kt`, starting with `Unresolved reference 'kmpwright'` | The iOS build of the `kmpwright-core` 0.0.2 library was made with Kotlin 2.5.0-Beta1; this project uses Kotlin 2.4.21, which cannot read it. The maintainer owns that library and must choose: republish it with Kotlin 2.4, or move this project to Kotlin 2.5 |
| `./gradlew :webDemo:wasmJsBrowserDistribution` | Kotlin compiles, then `:kotlinWasmStoreYarnLock` fails with `Lock file was changed` | The lock file the build generates lacks the `sqlite-wasm-worker` entries that the committed `kotlin-js-store/wasm/yarn.lock` has. **Do not run `kotlinWasmUpgradeYarnLock`**: it was tried, it removes those entries, and the build then fails later in webpack. Not diagnosed further |

Until the maintainer settles these, use `./gradlew :webDemo:compileKotlinWasmJs` to prove the web demo's Kotlin still compiles (it passes at `a9f8f8ba`).

## Scope

**In scope:**

- `shared/src/androidMain/kotlin/com/bernaferrari/changedetection/background/MonitoringBatch.kt` (one pair of parentheses)
- Whatever `./gradlew :shared:spotlessApply` rewrites, formatting only (Step 2)
- `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/settings/SettingsScreen.kt` (one import, one test tag)
- `shared/src/desktopTest/kotlin/com/bernaferrari/changedetection/review/DesignReviewTest.kt` (three added lines)
- `plans/README.md` (status column only)

**Out of scope:**

- Any change to what code does. If the formatter's diff shows anything other than line breaks, indentation, blank lines and trailing commas, STOP.
- `IosPageCapture.kt`, `gradle/libs.versions.toml`, `kotlin-js-store/`.
- Formatter or lint configuration (`build.gradle.kts`, any `.editorconfig`). Do not suppress the rule.

## Git workflow

- Branch from `main`: `git switch -c advisor/000-make-the-gates-green`
- Two commits, so the large mechanical one can be reviewed (or skipped) by itself:
  1. Steps 1 and 2: `Code is formatted the way the format check expects`
  2. Step 3: `The large-text tour scrolls to About before looking for it`
- Do not push, merge, or open a pull request.

## Steps

### Step 0: Confirm the starting state

Make sure the working tree is clean first: `git status --short` must print nothing. If it prints files, STOP and report; a formatter run on top of someone's unfinished work would mix the two.

**Verify:** `./gradlew :shared:spotlessCheck`
Expected: `BUILD FAILED`, with `The following files had format violations:`. (If it already passes, skip Steps 1 and 2.)

### Step 1: Fix the one lint error by hand

In `MonitoringBatch.kt`, change line 37 to:

```kotlin
        state = state.copy(remaining = state.remaining - site.id, retryNeeded = state.retryNeeded || (result is SyncResult.Failed && result.error.isRetryable))
```

Only the parentheses around `result is SyncResult.Failed && result.error.isRetryable` are new.

### Step 2: Run the formatter once, over everything

**Verify:** `./gradlew :shared:spotlessApply`
Expected: `BUILD SUCCESSFUL`. (Without Step 1 it fails with `There were 1 lint error(s)`.)

**Verify:** `./gradlew :shared:spotlessCheck`
Expected: `BUILD SUCCESSFUL`.

**Verify:** `git status --short | wc -l`
Expected: a number between 30 and 45 (it was 36 to 38 on trial runs), every path ending in `.kt` under `shared/src/`.

Look through `git diff` quickly. Every hunk must be a re-wrapping of the same code: a one-line `if (...) { a();return x }` split over lines, arguments moved to one per line, indentation changed, blank lines added between `when` branches. If any hunk adds, removes or changes an identifier, a literal or an operator (other than your parentheses from Step 1), STOP.

Commit (first commit).

### Step 3: Let the tour reach "About" at large text sizes

In `SettingsScreen.kt`:

1. Add the import `import androidx.compose.ui.platform.testTag` directly below the existing line `import androidx.compose.ui.Modifier`.
2. Change the `LazyColumn(` line quoted in "Current state" to:

```kotlin
            LazyColumn(Modifier.widthIn(max = 720.dp).fillMaxSize().testTag("settings-list"), contentPadding = PaddingValues(bottom = 40.dp)) {
```

In `DesignReviewTest.kt`, in the function `tour`, change the three lines quoted in "Current state" to:

```kotlin
        onNodeWithText("Settings").performClick()
        waitForTag("settings-list")
        // At large text sizes the Help group starts below the first screen, where a lazy list has not built it yet.
        onNodeWithTag("settings-list").performScrollToKey("help")
        waitForText("About")
        onNodeWithText("About").performScrollTo().performClick()
```

**Verify:** `./gradlew :shared:desktopTest --tests '*DesignReviewTest*'`
Expected: `BUILD SUCCESSFUL`. This runs all the tours (normal, dark, large text, wide), so it also shows the change did not disturb the others.

### Step 4: The full gate

**Verify:** `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest`
Expected: `BUILD SUCCESSFUL`.

**Verify:** `./gradlew :webDemo:compileKotlinWasmJs`
Expected: `BUILD SUCCESSFUL`.

Commit (second commit). Set this plan's row in `plans/README.md` to `DONE`.

## Test plan

No new test. The repaired `DesignReviewTest.largeTextTour` is the test: it fails before Step 3 and passes after. The formatter run is proven harmless by the whole suite passing in Step 4.

## Done criteria

- [ ] `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest` is `BUILD SUCCESSFUL`.
- [ ] `rg -n 'testTag\("settings-list"\)' shared/src/commonMain` prints exactly one line.
- [ ] `git log --oneline -2` shows the two commits described under "Git workflow".
- [ ] No file outside "In scope" changed, and `kotlin-js-store/wasm/yarn.lock` is untouched (`git diff --stat a9f8f8ba..HEAD -- kotlin-js-store` prints nothing).

## STOP conditions

- The working tree is not clean at Step 0.
- After Step 1, `spotlessApply` still reports a lint error. Report the file, line and rule; do not suppress it.
- The formatter's diff contains anything that is not layout (Step 2).
- After Step 3, `largeTextTour` still fails. Report the line number in the failure and attach `build/desktop-review/v6/zz-failure.png` (the test saves a picture of the screen when it fails). Do not raise timeouts.
- Any test other than `largeTextTour` fails at Step 4 that did not fail before you started.

## Maintenance notes

- After this plan, a red format check means the last change introduced it. `./gradlew :shared:spotlessApply` is then safe to run again, because it only touches what that change left unformatted.
- This plan moves lines in about 36 files. Other plans in this folder quote code with line numbers taken before it; they say to find code by its text.
- The iOS compile error and the web demo lock file (see "Known failures this plan leaves alone") are listed as open questions for the maintainer in `plans/README.md`.
