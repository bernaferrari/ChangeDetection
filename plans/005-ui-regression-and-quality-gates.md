# Plan 005: Add focused UI regression coverage

> **Executor instructions**: Preserve existing user changes. Add behavior-focused tests, not brittle pixel snapshots. Follow existing Android Compose test conventions and update `plans/README.md` when done.
>
> **Drift check (run first)**: `git diff --stat cff7931..HEAD -- composeApp/src/androidUnitTest composeApp/src/androidInstrumentedTest composeApp/build.gradle.kts README.md`

## Status

- **Priority**: P2
- **Effort**: M
- **Risk**: LOW — tests and verification only; test setup changes must not alter release behavior.
- **Depends on**: `plans/001-reproducible-android-verification.md`, `plans/003-compose-visual-contract.md`, `plans/004-decompose-complex-compose-screens.md`
- **Category**: tests, dx
- **Planned at**: commit `cff7931`, 2026-07-28

## Why this matters

Change Detection has more tests than the two comparison projects, but its most complex and visually revised Compose surfaces remain lightly protected. The recent working-tree pass changes main, Turbo, settings, comparison, logs, local browser, and navigation in one sweep. A small set of semantic UI tests will make future polishing safe without tying the project to fragile screenshots or implementation-private layouts.

## Current state

- `composeApp/build.gradle.kts` configures `androidHostTest` with JUnit, Ktor mock, coroutines-test, Mockito, and Mockito-Kotlin; `androidDeviceTest` has Compose UI testing dependencies.
- Existing unit tests include `ComparisonDiffTest.kt`, `SmartMonitoringEngineTest.kt`, `SyncEngineTest.kt`, and `LocalBrowserViewModelTest.kt`.
- `QuickMonitorUiTest.kt:19-88` uses `createAndroidComposeRule<ComponentActivity>()`, app resources through `R.string`, `setContent`, `onNodeWithText`, and content-description assertions. It is the required structural pattern for new Compose tests.
- `README.md` lists host tests but currently lacks a concise verification matrix that says which tests protect visual state versus sync/domain logic.

## Commands you will need

| Purpose | Command | Expected on success |
|---|---|---|
| Focused host tests | `./gradlew :composeApp:testAndroidHostTest --tests '*ComparisonDiffTest*'` | `BUILD SUCCESSFUL` |
| Host suite | `./gradlew :composeApp:testAndroidHostTest` | `BUILD SUCCESSFUL` |
| Device test compilation | `./gradlew :composeApp:compileDebugAndroidTestKotlin` | `BUILD SUCCESSFUL` |
| Debug app | `./gradlew :app:assembleDebug` | `BUILD SUCCESSFUL` |
| Formatting | `./gradlew spotlessCheck` | `BUILD SUCCESSFUL` |

## Scope

**In scope**:

- Focused tests in `composeApp/src/androidUnitTest/kotlin/com/bernaferrari/changedetection/ui/`
- Focused Compose tests in `composeApp/src/androidInstrumentedTest/kotlin/com/bernaferrari/changedetection/ui/`
- `composeApp/build.gradle.kts` only if a test dependency is demonstrably missing
- `README.md` verification section

**Out of scope**:

- Production UI or ViewModel behavior changes.
- Golden-image/pixel screenshot infrastructure, network-dependent tests, and real website monitoring.
- Testing private implementation details instead of public composable behavior.

## Git workflow

- Use a branch such as `codex/005-ui-regression-and-quality-gates` after preserving the current dirty worktree.
- Commit test additions separately from README documentation.

## Steps

### Step 1: Map stable public UI contracts

Before adding tests, list the visible contracts changed by Plans 003–004: main empty/loading/populated states; site row status; settings theme selection; settings actions/toggles; comparison empty/single/diff state; Turbo empty/stopped/active state; and bottom navigation selection. Only choose components that expose stable strings, roles, or content descriptions.

**Verify**: each selected contract maps to a public composable and a stable resource string/content description; do not test animation timing or modifier tree shape.

### Step 2: Add semantic Compose tests

Using `QuickMonitorUiTest.kt` as the exact test style, add tests that set each target composable to representative synthetic state and assert:

- Empty states show their primary and secondary actions.
- Each site status exposes readable text/icon content description.
- Theme selection announces the selected mode and only invokes its supplied callback.
- Comparison's empty/single/diff states expose the right action/content label.
- Turbo stopped and active variants expose mutually exclusive start/stop actions.

Keep fixture data in the test file unless it is shared by three or more tests; then add a narrowly named fixture helper under the test tree.

**Verify**: device test Kotlin compilation succeeds and each new test has a meaningful assertion beyond “component composes.”

### Step 3: Characterize pure UI-adjacent logic

Add host tests for the comparison helpers extracted in Plan 004 and any state-to-label mapper introduced by Plan 003. Cover empty, normal, and error/edge cases. Use `ComparisonDiffTest.kt` and the existing Arrange/Act/Assert naming style.

**Verify**: focused and full host test commands succeed.

### Step 4: Publish a concise verification matrix

Update the README's build-and-test section with a compact table: host tests (domain and pure UI logic), device/Compose tests (semantic UI contracts), debug assembly, and web lint/build. Mention that tests use synthetic local data and should not depend on live monitored websites.

**Verify**: every command in the README is present in the repository configuration and succeeds locally where the environment provides its prerequisites.

## Test plan

- Happy-path and alternative visual state for every selected screen contract.
- Callback regression tests for user actions exposed by extracted composables.
- Pure comparison helper edge cases: empty/identical/added/removed/non-ASCII/minimal HTML.
- Manual smoke: run app on a test device after debug assembly and navigate through all bottom-navigation destinations.

## Done criteria

- [ ] New tests assert user-visible semantics and callbacks for main, settings, comparison, and Turbo contracts.
- [ ] Pure diff/state helper tests cover named edge cases.
- [ ] No test requires network access, a real monitored URL, or personal data.
- [ ] Host tests, Android test compilation, debug assemble, and Spotless check pass.
- [ ] README accurately maps verification commands to coverage.

## STOP conditions

- Stop if a target cannot be rendered without directly constructing a production ViewModel/repository; report the missing UI boundary rather than adding test-only production hooks.
- Stop if an assertion relies on animation timing, pixel color, or private modifier structure.
- Stop if adding a dependency duplicates one already in the version catalog or changes production runtime dependencies.

## Maintenance notes

- Add one semantic test when a new state is added to an existing stateful surface.
- Keep screenshot-based review manual unless the project deliberately adopts a stable cross-platform golden-test system later; pixel snapshots are not a substitute for accessibility and behavior contracts.
