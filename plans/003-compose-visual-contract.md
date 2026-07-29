# Plan 003: Establish one Compose visual language

> **Executor instructions**: Follow every step and preserve existing user changes. This is a consistency refactor, not a visual clone of SDKMonitor or QuietGuard. Retain Change Detection's dynamic Material You / Material Expressive identity. Update the plan status in `plans/README.md` when done.
>
> **Drift check (run first)**: `git diff --stat cff7931..HEAD -- composeApp/src/commonMain/kotlin/com/bernaferrari/changedetection/ui composeApp/src/androidMain/kotlin/com/bernaferrari/changedetection/ui/theme composeApp/src/iosMain/kotlin/com/bernaferrari/changedetection/ui/theme`

## Status

- **Priority**: P1
- **Effort**: L
- **Risk**: MED — broad UI changes can alter accessibility, touch targets, and state-dependent surfaces.
- **Depends on**: `plans/001-reproducible-android-verification.md`
- **Category**: tech-debt, tests
- **Planned at**: commit `cff7931`, 2026-07-28

## Why this matters

Change Detection has the ingredients of a polished design—dynamic colors, Material Expressive theme, shared rows, empty states, and motion—but currently uses two systems at once. `DesignConstants.kt` calls its `Spacing`, `CornerRadius`, and `ExpressiveSpring` compatibility layers temporary while newer code uses `MaterialTheme` defaults. This makes screens feel individually designed rather than like one product, and makes every later polish change costlier.

## Current state

- `composeApp/src/androidMain/kotlin/com/bernaferrari/changedetection/ui/theme/ChangeDetectionTheme.android.kt:31-53` builds dynamic Android or MaterialKolor color schemes and passes `MotionScheme.expressive()`, `Shapes`, and `Typography` to `MaterialExpressiveTheme`.
- `composeApp/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/theme/DesignConstants.kt:9-14` says new UI should use MaterialTheme typography/shapes/defaults, while old UI retains custom token objects temporarily.
- Custom `ExpressiveSpring` is still used across `DialogBackgroundSync.kt`, `TurboScreen.kt`, `SettingsScreen.kt`, `LogComponents.kt`, and shared empty/dialog components. `TurboScreen.kt` additionally uses `CornerRadius` directly for changing shapes.
- `MainScreen.kt` and `SiteRow.kt` are the most appropriate current exemplars: Material surfaces, `MaterialTheme.shapes.medium`, semantic colors, 64dp rows, and restrained haptics.
- The comparable products look cohesive because their repeated cards, navigation, spacing, and motion are governed by a smaller set of shared primitives. Do not copy their colors or layout.

## Commands you will need

| Purpose | Command | Expected on success |
|---|---|---|
| Host tests | `./gradlew :composeApp:testAndroidHostTest` | `BUILD SUCCESSFUL` |
| Android debug build | `./gradlew :app:assembleDebug` | `BUILD SUCCESSFUL` |
| Kotlin formatting check | `./gradlew spotlessCheck` | `BUILD SUCCESSFUL` |
| Source inventory | `rg -n 'Spacing\\.|CornerRadius\\.|ExpressiveSpring\\.' composeApp/src -g '*.kt'` | count decreases only where replacement is deliberate |

## Scope

**In scope**:

- `composeApp/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/theme/DesignConstants.kt`
- `composeApp/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/theme/Theme.kt` and `Type.kt` if required by the new contract
- `composeApp/src/androidMain/kotlin/com/bernaferrari/changedetection/ui/theme/ChangeDetectionTheme.android.kt`
- `composeApp/src/iosMain/kotlin/com/bernaferrari/changedetection/ui/theme/ChangeDetectionTheme.ios.kt`
- Shared component primitives in `composeApp/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/components/`
- `ui/main/MainScreen.kt`, `SiteRow.kt`, `SiteOptionsBottomSheet.kt`, `ui/settings/SettingsScreen.kt`, `ThemeModeToggle.kt`, and `DialogBackgroundSync.kt`

**Out of scope**:

- Major extraction of `ComparisonScreen.kt` and `TurboScreen.kt`; Plan 004 owns that.
- Browser/WebView overlays and platform browser behavior.
- New color-palette customization, branding changes, or replacing dynamic color.

## Git workflow

- Use a branch such as `codex/003-compose-visual-contract` after preserving the dirty worktree.
- Make commits by coherent unit: shared contract, main/list adoption, settings adoption, then tests.
- Do not push unless instructed.

## Steps

### Step 1: Write the visual contract before migrating call sites

Keep `MaterialExpressiveTheme` as the source of truth. Add a small shared Compose design contract only for gaps Material does not cover consistently in this app: semantic horizontal/content spacing, standard list/card density, and a limited set of semantic motion choices that honor the theme motion scheme. Make it available from both Android and iOS without platform checks.

Do not add a second parallel palette, generic `Utils`, or raw `Color` values. Naming must describe intent (`screenContent`, `listItemGap`, `emphasizedActionMotion`), not arbitrary sizes (`mediumPlus2`). Remove compatibility comments only after all in-scope consumers have migrated.

**Verify**: compile common and Android sources with `./gradlew :app:assembleDebug` → `BUILD SUCCESSFUL`.

### Step 2: Turn repeated surface patterns into shared components

Extract or refine small shared components for a settings/list surface, section header, standard trailing action, and status presentation only where at least two screens need the same semantics. Preserve public composable APIs where instrumented tests or navigation call them. Use `SiteRow.kt` as the appearance baseline: Material surface colors, theme shapes, 48dp-or-larger touch targets, one clear primary line and one subdued secondary line.

Do not make a generic mega-card with dozens of boolean flags. Prefer a composable slot API or distinct narrow components when behaviors differ.

**Verify**: `rg -n 'Surface\(|RoundedCornerShape|Color\.'` on migrated components shows colors/shapes come from the theme or the new semantic contract, not duplicated literals.

### Step 3: Migrate Main and Settings as reference surfaces

Apply the contract to `MainScreen`, `SiteRow`, settings groups, theme selector, toggle/action items, and background-sync dialog. Eliminate duplicate local press-scale/spring implementations when the new shared primitive expresses the same interaction. Keep accessibility labels, content descriptions, focus behavior, strings, haptic feedback, and existing actions unchanged.

For the settings screen, preserve the current grouping and preferences; polish density and hierarchy without inventing analytics-style dashboard content that does not serve a setting.

**Verify**: run Android host tests and manually inspect light/dark plus dynamic/non-dynamic color on an emulator. Every interactive control has a visible selected/disabled state and a touch target at least 48dp.

### Step 4: Retire only migrated compatibility tokens

After callers in this plan use the new contract, remove obsolete aliases from `DesignConstants.kt`. Do not delete `ExpressiveSpring` or `CornerRadius` entries still used by Plan 004's out-of-scope surfaces; instead document them as legacy until that migration completes. Ensure no unused imports remain.

**Verify**: `./gradlew spotlessCheck :composeApp:testAndroidHostTest :app:assembleDebug` → `BUILD SUCCESSFUL`.

## Test plan

- Extend or add Compose tests for the migrated public components: selected theme mode, enabled/disabled settings toggle, visible status label for each `SiteStatus`, and accessibility content descriptions for trailing actions.
- Use `composeApp/src/androidInstrumentedTest/kotlin/com/bernaferrari/changedetection/ui/main/QuickMonitorUiTest.kt` for Compose rule structure and resource-string lookup.
- Retain existing unit behavior tests; this plan must not alter persistence, sync scheduling, or navigation semantics.

## Done criteria

- [ ] Theme remains dynamic and Material Expressive on Android.
- [ ] Main/list/settings use one documented set of semantic spacing, surface, and motion decisions.
- [ ] No new raw visual constants or unscoped animation specifications are added in migrated files.
- [ ] Obsolete compatibility tokens are removed only when zero in-scope callers remain.
- [ ] Host tests, debug build, and Spotless check pass.

## STOP conditions

- Stop if the common theme cannot expose a required token consistently on iOS and Android; report the platform limitation rather than adding Android-only visual behavior to common UI.
- Stop if a component needs more than two feature flags to become shared; keep separate named components and report the abstraction boundary.
- Stop if visual changes require changing stored settings, navigation routes, or sync behavior; those are out of scope.

## Maintenance notes

- New screens must use the visual contract or a Material default; they must not introduce new ad-hoc `object` token sets.
- Reviewers should check light/dark/dynamic color, reduced-motion behavior where available, keyboard/focus, and touch-target sizes—not only screenshot appearance.
