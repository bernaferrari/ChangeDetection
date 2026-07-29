# Plan 004: Decompose the comparison and Turbo screens

> **Executor instructions**: Preserve user edits and behavior. Extract components around stable feature boundaries; do not rewrite the diff engine or Turbo service. Update `plans/README.md` when done.
>
> **Drift check (run first)**: `git diff --stat cff7931..HEAD -- composeApp/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/comparison composeApp/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/main/TurboScreen.kt composeApp/src/androidUnitTest/kotlin/com/bernaferrari/changedetection/ui`

## Status

- **Priority**: P2
- **Effort**: L
- **Risk**: MED — selection and monitoring state must remain exact.
- **Depends on**: `plans/003-compose-visual-contract.md`
- **Category**: tech-debt, tests
- **Planned at**: commit `cff7931`, 2026-07-28

## Why this matters

The two most user-visible specialized screens contain too many unrelated responsibilities. `ComparisonScreen.kt` is 1,011 lines, owning top-level state, empty/single/diff layouts, selector chips, HTML rendering, similarity calculation, keyword extraction, and JavaScript generation. `TurboScreen.kt` is 835 lines with a screen controller, site cards, hero, actions, empty state, and indicator. This makes visual polish risky and hides product logic in UI files.

## Current state

- `ComparisonScreen.kt` declares `ComparisonScreen` at line 47; `SingleSnapshotState`, `DiffFirstContent`, `VersionSelector`, `SimpleDiffView`, and `RenderedHtmlComparison` are all private in the same file. Algorithmic helpers `calculateSimilarity`, `extractChangedKeywords`, and `buildHighlighterJs` appear at lines 946–991.
- `TurboScreen.kt` declares `TurboScreen` at line 121 and contains `SiteRowCard`, `TurboHero`, `StartStopFab`, `EmptyState`, and `CircularCheckIndicator` in the same 835-line file.
- `ComparisonDiffTest.kt` already exists under `composeApp/src/androidUnitTest/kotlin/com/bernaferrari/changedetection/ui/comparison/`; retain and extend it rather than creating an unrelated test convention.
- Plan 003 supplies the visual primitives that every extracted component must use.

## Commands you will need

| Purpose | Command | Expected on success |
|---|---|---|
| Focused unit tests | `./gradlew :composeApp:testAndroidHostTest --tests '*ComparisonDiffTest*'` | `BUILD SUCCESSFUL` |
| Full host tests | `./gradlew :composeApp:testAndroidHostTest` | `BUILD SUCCESSFUL` |
| Debug build | `./gradlew :app:assembleDebug` | `BUILD SUCCESSFUL` |
| Formatting | `./gradlew spotlessCheck` | `BUILD SUCCESSFUL` |

## Scope

**In scope**:

- `composeApp/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/comparison/ComparisonScreen.kt`
- New narrow files under `composeApp/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/comparison/`
- `composeApp/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/main/TurboScreen.kt`
- New narrow files under `composeApp/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/main/`
- Focused test files under `composeApp/src/androidUnitTest/kotlin/com/bernaferrari/changedetection/ui/`

**Out of scope**:

- `ImageComparisonView.kt`, platform image-diff actuals, Webwright APIs, repositories, view models, foreground service, WorkerHelper, and navigation contracts.
- Redesigning product behavior, adding a new comparison mode, changing Turbo intervals, or migrating to another navigation library.

## Git workflow

- Use a branch such as `codex/004-decompose-complex-compose-screens` after preserving dirty work.
- Commit extraction without behavior change before any small visual cleanup, so review can separate mechanical movement from intended design changes.

## Steps

### Step 1: Create explicit UI-state boundaries for comparison

Keep `ComparisonScreen` as a small route-level orchestrator: collect state, select which high-level state to show, own navigation callbacks, and assemble the screen. Move visual states into purpose-named files such as `ComparisonEmptyStates.kt`, `ComparisonVersionSelector.kt`, `TextDiffContent.kt`, and `RenderedHtmlDiff.kt`. Move pure similarity/keyword/highlighter helpers to a non-Compose file only if they have no UI dependencies.

Pass immutable render models and callbacks into extracted components. Do not pass `ComparisonViewModel` into every child; this is the boundary that makes the UI testable and lets composition remain obvious.

**Verify**: `ComparisonScreen.kt` is a route-level file with no embedded JavaScript string and no similarity/keyword algorithm. Focused comparison tests pass.

### Step 2: Extract Turbo presentation from its controller

Keep `TurboScreen` responsible for state collection and routing start/stop/selection events. Move `TurboHero`, the selected-site list/card, primary action/fab, and empty content to individual files with narrow stateless APIs. Reuse Plan 003's shared status, motion, surface, and spacing conventions; preserve existing string resources and `contentDescription`s.

Make selection and running state explicit in data/parameters. Do not rely on captured mutable state inside extracted composables.

**Verify**: Turbo start/stop, selection, duration, and empty-state behavior compile unchanged; no UI child needs direct access to the view model.

### Step 3: Isolate pure comparison computation and characterize it

For `calculateSimilarity`, `extractChangedKeywords`, and generated highlighter JavaScript, first add or retain characterization tests for empty, identical, insertion-only, removal-only, malformed/minimal HTML, and non-ASCII content. Preserve current output before moving each helper. Avoid changing the algorithm as part of this refactor.

**Verify**: focused unit tests pass before and after the move with identical expected outputs.

### Step 4: Run full verification and a manual state matrix

Run the complete host test suite, debug build, and Spotless check. On a device/emulator manually inspect comparison empty, one-snapshot, text diff, rendered-HTML diff, image diff entry, Turbo empty, Turbo configured-but-stopped, and Turbo active states in both light and dark themes.

**Verify**: commands exit 0; state-matrix notes identify no behavior regression.

## Test plan

- Extend `ComparisonDiffTest.kt` with the pure-helper characterization cases above.
- Add Compose tests for public extracted state components using `QuickMonitorUiTest.kt` as a structure example: required title/actions are displayed and selected-version state is announced correctly.
- Verify that callbacks trigger exactly once for refresh/start/stop/selection actions using injected lambdas and counters where the Compose test environment supports it.

## Done criteria

- [ ] `ComparisonScreen.kt` no longer combines route state, visual states, pure diff logic, and JavaScript construction.
- [ ] `TurboScreen.kt` no longer combines controller, hero, cards, action, and empty content.
- [ ] Extracted composables use the Plan 003 visual contract and accept narrow inputs/callbacks.
- [ ] Characterization tests cover the moved pure diff helpers.
- [ ] Host tests, debug build, and Spotless check pass.

## STOP conditions

- Stop if moving a helper changes public comparison output; restore behavior and report the coupling.
- Stop if an extraction requires changing ViewModel, repository, service, navigation, or platform-browser APIs.
- Stop if a new component needs many unrelated callbacks; re-evaluate the state boundary rather than recreating a giant parameter list.

## Maintenance notes

- Future comparison modes should be new state/content files, not branches appended to `ComparisonScreen`.
- Future Turbo visual changes should be local to the presentation component; background-monitoring behavior remains behind the existing view model/service boundary.
