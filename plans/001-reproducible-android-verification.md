# Plan 001: Restore reproducible Android verification

> **Executor instructions**: Follow this plan step by step. Run every verification command and confirm the expected result before moving on. The repository has pre-existing, uncommitted user edits. Do not reset, stash, discard, format, stage, or commit any unrelated file. When done, update this plan's status in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat cff7931..HEAD -- settings.gradle.kts README.md .github/workflows/verify.yml`
> If any in-scope file changed since this plan was written, compare the excerpts below with the live code. Stop if the required build behavior is already being changed independently.

## Status

- **Priority**: P1
- **Effort**: M
- **Risk**: MED — composite substitution affects which browser implementation is compiled locally.
- **Depends on**: none
- **Category**: dx, migration, tests
- **Planned at**: commit `cff7931`, 2026-07-28

## Why this matters

The documented command `./gradlew :composeApp:testAndroidHostTest` currently fails before compilation. `settings.gradle.kts` substitutes two `webwright-*` project paths, while the sibling `../../kotlin/webwright` checkout exposes them as `kmpwright-*`. A project whose stated test command cannot initialize is not safely refactorable or release-ready. This plan makes the local checkout work while keeping CI and contributors without the sibling checkout able to use the published dependency declared in the version catalog.

## Current state

- `settings.gradle.kts` configures the optional-looking sibling checkout unconditionally, but it is actually mandatory:

  ```kotlin
  // settings.gradle.kts:17-21
  includeBuild("../../kotlin/webwright") {
      dependencySubstitution {
          substitute(module("com.webwright:webwright-core")).using(project(":webwright-core"))
          substitute(module("com.webwright:webwright-compose")).using(project(":webwright-compose"))
      }
  }
  ```

- The sibling project's `settings.gradle.kts:25-29` has `rootProject.name = "kmpwright"` and includes `:kmpwright-core`, `:kmpwright-compose`, and `:kmpwright-test` — not the paths named above.
- `gradle/libs.versions.toml:169` declares the published module `com.webwright:webwright-core:1.3.0`; `composeApp/build.gradle.kts:139` consumes it. Therefore the composite build is a local-development convenience, not the only valid dependency source.
- `README.md:37-51` tells contributors that the sibling checkout is required and lists `./gradlew :app:assembleDebug` and `./gradlew :composeApp:testAndroidHostTest` as validation.
- Formatting uses root Spotless configuration in `build.gradle.kts`; Kotlin test dependencies are already configured in `composeApp/build.gradle.kts` under `androidHostTest`.

## Commands you will need

| Purpose | Command | Expected on success |
|---|---|---|
| Drift check | `git diff --stat cff7931..HEAD -- settings.gradle.kts README.md .github/workflows/verify.yml` | Inspect only; no unexpected conflicting change |
| Host tests | `./gradlew :composeApp:testAndroidHostTest` | `BUILD SUCCESSFUL` |
| Android compile/package | `./gradlew :app:assembleDebug` | `BUILD SUCCESSFUL` and a debug APK under `app/build/outputs/apk/` |
| Web lint | `cd web && node_modules/.bin/eslint .` | exit 0 |
| Web production build | `cd web && node_modules/.bin/next build` | exit 0 and static routes `/`, `/docs` |

## Scope

**In scope**:

- `settings.gradle.kts`
- `README.md`
- `.github/workflows/verify.yml` (create only if no equivalent workflow exists)

**Out of scope**:

- `composeApp/build.gradle.kts`, dependency versions, and application source code.
- Any files under `../../kotlin/webwright`; it is a separate repository.
- The user's existing working-tree changes outside this plan.

## Git workflow

- Work on a clean purpose-specific branch such as `codex/001-reproducible-android-verification` only after preserving the existing dirty worktree.
- Match recent commit style, for example `Fix publishability verification issues`.
- Do not push or open a pull request unless explicitly instructed.

## Steps

### Step 1: Make the Webwright composite checkout optional and correct

In `settings.gradle.kts`, create a `File` reference to `../../kotlin/webwright`. Include the composite build only when that directory exists. Inside its `dependencySubstitution`, retain the module coordinates currently used by Change Detection, but point `project(...)` to `:kmpwright-core` and `:kmpwright-compose` — the exact projects exposed by the sibling checkout.

When the directory is absent, do not add an alternate local project dependency: allow Gradle to resolve `com.webwright:webwright-core:1.3.0` from the normal configured repositories. Do not hard-code an absolute developer path.

**Verify**: with the sibling checkout present, run `./gradlew :composeApp:testAndroidHostTest` → `BUILD SUCCESSFUL`. Temporarily verify only with a non-mutating settings-level approach that an absent checkout does not cause settings evaluation to fail; do not rename or delete the sibling repository.

### Step 2: Correct contributor instructions

Update `README.md` so it says the sibling Webwright checkout is optional for local source substitution. State that builds otherwise resolve the pinned Maven dependency. Keep the existing app and web commands, and add the exact host-test command as the required local Kotlin verification command.

Document the Webwright-project-name requirement only if it remains useful to contributors; do not expose local machine-specific paths beyond the already documented relative checkout relationship.

**Verify**: `rg -n 'webwright-core|webwright-compose|kmpwright-core|kmpwright-compose|testAndroidHostTest' README.md settings.gradle.kts` → occurrences accurately describe the corrected mapping.

### Step 3: Add a narrow CI verification workflow if absent

If `.github/workflows/` has no existing equivalent, create `.github/workflows/verify.yml`. It must run on pull requests and pushes, check out the repository, set up JDK 17, run `./gradlew :composeApp:testAndroidHostTest :app:assembleDebug`, set up the package-manager version declared by the web project, install web dependencies using the lockfile, and run `pnpm lint` plus `pnpm build` in `web/`.

The workflow must not rely on `../../kotlin/webwright`; that validates the published-dependency path. If pnpm's configured supply-chain policy requires an explicit approved-build configuration, use the repository's existing package-manager policy mechanism rather than weakening security or adding `--ignore-scripts` blindly.

**Verify**: inspect workflow YAML with `git diff --check` → no whitespace errors. If `actionlint` is installed, run `actionlint .github/workflows/verify.yml` → exit 0; otherwise record that GitHub is the remaining workflow parser.

### Step 4: Run all local gates

Run the host tests, debug assemble, direct local ESLint, and direct Next production build from the command table. The direct web executables are intentional local diagnostics because the current `pnpm lint` wrapper may stop at the external build-approval policy before invoking ESLint.

**Verify**: every command in the table succeeds. If only `pnpm lint` fails at a policy preflight, report its exact policy requirement separately; do not claim the lint source failed.

## Test plan

- The pre-existing host unit suite must run from the real settings graph, especially `ComparisonDiffTest`, `SmartMonitoringEngineTest`, and `LocalBrowserViewModelTest`.
- The debug assembly gate verifies Android-specific Webwright compilation.
- The Next build verifies the marketing site typechecks and statically renders.

## Done criteria

- [ ] `./gradlew :composeApp:testAndroidHostTest` succeeds with the sibling Webwright checkout.
- [ ] `./gradlew :app:assembleDebug` succeeds.
- [ ] Settings evaluation does not require a sibling checkout when the published dependency is available.
- [ ] `README.md` accurately describes both dependency modes.
- [ ] A CI workflow exists or an equivalent existing workflow covers Android host tests, debug assembly, web lint, and web build.
- [ ] No unrelated dirty file was modified or staged.

## STOP conditions

- Stop if `:kmpwright-core` does not publish the package API imported by Change Detection under `com.webwright.core`; changing application imports or the sibling repository is out of scope.
- Stop if the published `com.webwright:webwright-core:1.3.0` cannot resolve after skipping the composite build; report the repository-resolution error rather than changing versions speculatively.
- Stop if an existing CI workflow already implements the requested gates with different conventions; review and minimally amend it instead of creating a duplicate.

## Maintenance notes

- Any future Webwright rename must update both this composite substitution and the README in the same pull request.
- Keep the test command in the README executable; do not treat a downloaded Gradle distribution or an uninitialized composite checkout as a passing verification baseline.
