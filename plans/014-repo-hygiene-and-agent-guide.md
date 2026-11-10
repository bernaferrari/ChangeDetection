# Plan 014: Fix two git-ignore traps and add a root guide for contributors and coding agents

> **Executor instructions**
> You are implementing one small, self-contained housekeeping change. You have not seen the audit that produced this plan; everything you need is in this file.
> 1. Run the drift check first: `git diff --stat a9f8f8ba..HEAD -- .gitignore docs/DEVELOPMENT.md tools/ iosApp/iosApp.xcodeproj` and `ls AGENTS.md`. If `AGENTS.md` already exists at the repo root, STOP and report (someone wrote one; merging is the maintainer's call). If `.gitignore` changed, check that the excerpt below is still present.
> 2. Follow the steps in order and run each **Verify** command.
> 3. Touch only the files listed under "In scope".
> 4. If a STOP condition applies, stop and report. Do not improvise.
> 5. When finished, set this plan's row in `plans/README.md` to `DONE`.

## Status

| Field | Value |
| --- | --- |
| Priority | P3 |
| Effort | S (1 hour) |
| Risk of the fix | Low. No application code changes |
| Depends on | Nothing. If Plan 001 has landed, one sentence in the guide applies as written; if not, it still states the intended rule |
| Category | Repository hygiene / developer experience |
| Planned at | commit `a9f8f8ba`, 2026-11-09 |

## Why this matters

Three small things make the repository harder to work in than it needs to be.

1. **`.gitignore` ignores `gradle.properties`, but the root `gradle.properties` is tracked and needed.** The rule's comment says the file "may contain secrets". It does not (it holds JVM memory settings and worker limits), and the rule has two bad effects: it tells contributors that secrets belong in that file, and it silently ignores a `gradle.properties` in any *new* Gradle module, so that module would build locally and fail for everyone else.
2. **Personal Xcode state is committed.** Three files under `xcuserdata/bernardoferrari.xcuserdatad/` are tracked. Two are per-user bookkeeping that changes whenever Xcode is opened. The third is the project's only scheme, stored in the per-user location, so a contributor with a different user name does not get it. Xcode's convention is a *shared* scheme under `xcshareddata/`.
3. **There is no root `AGENTS.md`.** `docs/DEVELOPMENT.md` is a good human guide to building. It does not state the project's working rules, which today can only be learned by reading code: where a new UI string must go so a test does not fail, how cancellation is handled, how commits are worded, what must never be added (analytics, cloud services). Coding agents read `AGENTS.md` at the repository root by convention; without it each one rediscovers or violates these rules.

## Current state

`.gitignore:108-112`:

```gitignore
# Kotlin
.kotlin/

# Gradle properties (may contain secrets)
gradle.properties
```

`git ls-files gradle.properties` prints `gradle.properties` (it is tracked). Its contents are build settings only.

`git ls-files | rg xcuserdata` prints:

```text
iosApp/iosApp.xcodeproj/project.xcworkspace/xcuserdata/bernardoferrari.xcuserdatad/xcschemes/xcschememanagement.plist
iosApp/iosApp.xcodeproj/xcuserdata/bernardoferrari.xcuserdatad/xcschemes/iosApp.xcscheme
iosApp/iosApp.xcodeproj/xcuserdata/bernardoferrari.xcuserdatad/xcschemes/xcschememanagement.plist
```

There is no `iosApp/iosApp.xcodeproj/xcshareddata/` directory, and `.gitignore` has no `xcuserdata` rule.

`tools/` contains one script, `tools/test-ios-browser.sh`. `docs/DEVELOPMENT.md` has a section "Before you push" listing these commands:

```bash
./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest
./gradlew :shared:compileKotlinIosSimulatorArm64 :androidApp:lintDebug :androidApp:assembleRelease
./gradlew :webDemo:wasmJsBrowserDistribution
(cd website && pnpm lint && pnpm build)
```

Facts for the guide (each verified in the repository at the planned commit):

- Test source sets: `shared/src/androidUnitTest` and `shared/src/jvmSharedTest` both compile into the Android host test task `:shared:testAndroidHostTest`; `shared/src/desktopTest` and `shared/src/jvmSharedTest` compile into `:shared:desktopTest`; `shared/src/androidInstrumentedTest` is the device test set, compiled by `:shared:assembleAndroidDeviceTest` and run by `:shared:connectedAndroidDeviceTest`.
- `StringResourcesTest` (desktop) requires every name in `shared/src/commonMain/composeResources/values/strings_v7.xml` to exist in `values-es`, `values-it` and `values-pt`, and forbids a name being defined twice within one language folder.
- Spanish, Italian and Portuguese mirror the English file names; the other eight languages keep everything in one `strings.xml`.
- Recent commit subjects are plain sentences such as "Settings opens without a Loading flash", not `feat:`/`fix:` prefixes (`git log --oneline -15`).
- Room schemas are exported to `shared/schemas/`; migrations live in `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/repo/source/local/` and are tested by `SchemaUpgradeTest` (desktop).
- The formatter is Spotless with ktlint: `./gradlew :shared:spotlessApply`.

## Commands you will need

| Purpose | Command | Expected |
| --- | --- | --- |
| Shell syntax check | `bash -n tools/verify.sh` | no output |
| Quick gate through the new script | `tools/verify.sh` | ends with `BUILD SUCCESSFUL` |
| What git tracks | `git ls-files \| rg "xcuserdata\|xcshareddata"` | see Step 2 |

The build needs JDK 21 and Android SDK platform 37.2.

## Before you start: the state of the build

Checked on 2026-11-09 at commit `a9f8f8ba`. Read this before the steps; it overrides any line below that expects otherwise.

1. **Plan 000 must be done first.** Before you change anything, run `./gradlew :shared:spotlessCheck`. If it fails, Plan 000 (`plans/000-make-the-gates-green.md`) has not landed: STOP and report that. Do not run `spotlessApply` to get past it; on an unprepared tree it rewrites about 36 unrelated files. Once Plan 000 has landed, the test and format commands in this plan behave as described, and `spotlessApply` only touches what you changed.
2. **Two commands fail for reasons no plan fixes.** `./gradlew :shared:compileKotlinIosSimulatorArm64` stops with five errors, all in `shared/src/iosMain/kotlin/com/bernaferrari/changedetection/browser/IosPageCapture.kt` (the first is `Unresolved reference 'kmpwright'`; a library was built with a newer Kotlin than this project uses). `./gradlew :webDemo:wasmJsBrowserDistribution` compiles the Kotlin and then stops at `:kotlinWasmStoreYarnLock` with `Lock file was changed`. Wherever this plan expects one of them to print `BUILD SUCCESSFUL`, the bar is instead: for iOS, **those same five errors and no others**; for the web demo, run `./gradlew :webDemo:compileKotlinWasmJs` and expect `BUILD SUCCESSFUL`. Never run `kotlinWasmUpgradeYarnLock`. If either command has since been repaired and passes, that is fine.
3. **Line numbers may have moved.** Two changes landed after this plan was written. Plan 000 re-formats about 36 files (layout only). The "failure experience" change (`plans/failure-experience-changes.md`) added code to `SyncEngine.kt`, `SitesDao.kt`, `Site.kt`, `CheckFailure.kt`, `DiagnosticTrace.kt`, `PageDetailScreen.kt`, `MainViewModel.kt`, `LegacyMigration.kt`, `DatabaseFactory.kt`, `AppContent.kt`, `AndroidChangeNotifier.kt`, the four `PlatformDiModule` files and the string files. Find code by the text this plan quotes, not by line number. A quoted block that differs only in line breaks or indentation is the same code. The drift check at the top of this plan will list those files as changed; that alone is not a reason to stop. Stop only if the quoted code is gone or does something different.

## Scope

**In scope:**

- `.gitignore`
- `iosApp/iosApp.xcodeproj/xcuserdata/**` and `iosApp/iosApp.xcodeproj/project.xcworkspace/xcuserdata/**` (untrack; move the scheme)
- New: `iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/iosApp.xcscheme` (moved, not rewritten)
- New: `AGENTS.md` (repo root)
- New: `tools/verify.sh`
- `docs/DEVELOPMENT.md` (two added lines)
- `plans/README.md` (status column only)

**Out of scope:**

- `gradle.properties` contents.
- `.editorconfig`. Do not add one: ktlint reads it, and it could change what Spotless accepts across the whole codebase.
- `iosApp/project.yml` and the contents of the Xcode project file.
- CI workflows.
- Any source code.

## Git workflow

- Branch from `main`: `git switch -c advisor/014-repo-hygiene-and-agent-guide`
- One commit per step. Commit subjects are plain sentences, for example `gradle.properties is no longer listed as ignored`, `The iOS scheme is shared and personal Xcode state is untracked`, `A root guide states the project's working rules`.
- Do not push, merge, or open a pull request.

## Steps

### Step 1: Remove the misleading ignore rule

In `.gitignore`, replace

```gitignore
# Gradle properties (may contain secrets)
gradle.properties
```

with

```gitignore
# gradle.properties is tracked on purpose: build settings only. Secrets live outside the repo
# (see "Good to know" in docs/DEVELOPMENT.md) or in local.properties, which is ignored.
```

First confirm that `local.properties` really is ignored: `git check-ignore -v local.properties` must print a rule. If it prints nothing, add a line `local.properties` under the new comment.

**Verify:** `git check-ignore -v --no-index gradle.properties`
Expected: no output (exit code 1), meaning no rule matches it any more.

**Verify:** `git status --short`
Expected: only ` M .gitignore`. If other, previously ignored `gradle.properties` files now show up as untracked (for example inside `website/` or a build directory), do not add them; list them in your report.

### Step 2: Share the scheme and untrack personal Xcode state

```bash
mkdir -p iosApp/iosApp.xcodeproj/xcshareddata/xcschemes
git mv iosApp/iosApp.xcodeproj/xcuserdata/bernardoferrari.xcuserdatad/xcschemes/iosApp.xcscheme iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/iosApp.xcscheme
git rm --cached iosApp/iosApp.xcodeproj/xcuserdata/bernardoferrari.xcuserdatad/xcschemes/xcschememanagement.plist
git rm --cached iosApp/iosApp.xcodeproj/project.xcworkspace/xcuserdata/bernardoferrari.xcuserdatad/xcschemes/xcschememanagement.plist
```

`git rm --cached` stops tracking a file and leaves it on disk, so the maintainer's local Xcode state is untouched.

Add to `.gitignore`, in the section that already lists macOS files (`.DS_Store` and so on), or at the end if you cannot find it:

```gitignore
# Per-user Xcode state. Schemes that everyone needs live in xcshareddata/.
xcuserdata/
```

**Verify:** `git ls-files | rg "xcuserdata|xcshareddata"`
Expected: exactly one line, `iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/iosApp.xcscheme`.

**Verify (macOS with Xcode only):** `xcodebuild -list -project iosApp/iosApp.xcodeproj`
Expected: the output lists a scheme named `iosApp`. If Xcode is not installed, say so in your report.

### Step 3: Add the verification script

Create `tools/verify.sh`:

```bash
#!/usr/bin/env bash
# Runs the checks from "Before you push" in docs/DEVELOPMENT.md.
#   tools/verify.sh        format check and JVM tests (a few minutes)
#   tools/verify.sh full   adds iOS compile (macOS), Android lint and release build, web demo, website
set -euo pipefail
cd "$(dirname "$0")/.."

./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest

if [ "${1:-}" = "full" ]; then
  if [ "$(uname)" = "Darwin" ]; then
    ./gradlew :shared:compileKotlinIosSimulatorArm64
  fi
  ./gradlew :androidApp:lintDebug :androidApp:assembleRelease
  ./gradlew :webDemo:wasmJsBrowserDistribution
  (cd website && pnpm lint && pnpm typecheck && pnpm build)
fi
```

Make it executable: `chmod +x tools/verify.sh`, then `git add tools/verify.sh` and confirm the mode with `git ls-files -s tools/verify.sh` (the line must start with `100755`).

**Verify:** `bash -n tools/verify.sh` prints nothing.

**Verify:** `tools/verify.sh`
Expected: ends with `BUILD SUCCESSFUL`.

### Step 4: Write the root guide

Create `AGENTS.md` at the repository root with exactly this content:

````markdown
# Working in this repository

Change Detection watches web pages on the user's own device and shows what changed. It is a Kotlin Multiplatform app
with one Compose UI. Android ships on Google Play; desktop and iOS are development builds; the browser demo uses sample
data. Setup, tool versions and the module map are in [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md).

## Check your work

```bash
tools/verify.sh        # format check and JVM tests: run before every commit
tools/verify.sh full   # adds iOS compile, Android lint and release build, web demo, website
./gradlew :shared:spotlessApply                      # fix formatting
./gradlew :shared:desktopTest --tests '*ClassName'   # one test class
```

Device tests need an emulator: `./gradlew :shared:connectedAndroidDeviceTest`. At minimum they must compile:
`./gradlew :shared:assembleAndroidDeviceTest`.

## Where tests go

| Folder under `shared/src/` | Runs in | Use it for |
| --- | --- | --- |
| `jvmSharedTest` | both tasks below | Pure logic with no platform types |
| `androidUnitTest` | `:shared:testAndroidHostTest` | Logic with mocks (Mockito); no Robolectric, so no Android framework calls |
| `desktopTest` | `:shared:desktopTest` | Real Room database, Compose UI tests, anything needing a real file system |
| `androidInstrumentedTest` | `:shared:connectedAndroidDeviceTest` | WebView, WorkManager, anything that needs Android itself |

Write the failing test first when fixing a bug. Do not edit an existing test to make a change pass without saying why.

## Rules that are easy to break

- **Privacy is the product.** No accounts, analytics, crash reporting, ads, or any network destination other than the
  pages the user watches. Do not add an SDK or a server call. User data leaves the device only through an action the
  user takes (share sheet, export).
- **Never lose user history.** Database changes need a migration and a test in `SchemaUpgradeTest`. Exported schemas
  live in `shared/schemas/`. A database from a released version must never be opened destructively.
- **Saved pages are untrusted input.** HTML from a watched site goes through `secureSnapshotDocument` before it is
  shown, and viewers must block navigation.
- **Cancellation propagates.** In `suspend` code, put `catch (e: CancellationException) { throw e }` before
  `catch (e: Exception)`, and do not wrap suspending calls in `runCatching`.
- **Strings.** New UI text goes at the end of `shared/src/commonMain/composeResources/values/strings_v7.xml`. Add the
  same names to `values-es/strings_v7.xml`, `values-it/strings_v7.xml` and `values-pt/strings_v7.xml` (a test fails
  otherwise), and to `strings.xml` in the other eight language folders. Never define a name twice in one folder.
  Use the typographic apostrophe `’` in translated text.
- **Chinese, Hindi and Bengali use bundled subset fonts.** A translated string with a character the font lacks fails
  `LocalizationTest`, which names the character. Reword the string; extending a font is described in
  `third_party/notosanssc-subsets.md`.
- **Platform-only UI** uses `@Composable expect fun` in `commonMain` with `actual ... = Unit` on platforms that do not
  have it. `DiagnosticsRow` is the example to copy.
- **Secrets stay out of the repository.** Signing and publishing read files named by environment variables; see
  "Good to know" in `docs/DEVELOPMENT.md`.

## Style

- Formatting is enforced by Spotless (ktlint). Run `spotlessApply` instead of formatting by hand.
- Comments say why, not what. Many functions carry a one-line KDoc stating the rule they protect; keep that habit.
- Commit subjects are plain sentences that describe the result for the user, for example
  "Settings opens without a Loading flash". No `feat:` or `fix:` prefixes.
- Keep changes small and focused. Do not reformat or rename code you are not changing.
````

Then check two statements in it against the repository, and correct the guide (not the repository) if either is wrong:

- `rg -n "class SchemaUpgradeTest" shared/src` prints a match.
- `rg -n "fun secureSnapshotDocument" shared/src/commonMain` prints a match.

Then run the two commands that were failing when this plan was written (see "Before you start", point 2): `./gradlew :shared:compileKotlinIosSimulatorArm64` and `./gradlew :webDemo:wasmJsBrowserDistribution`. If both pass, the guide is complete. For each one that still fails, add a section at the end of `AGENTS.md`, listing only the commands that fail:

```markdown
## Checks that fail today

`tools/verify.sh full` stops at these. They fail without any change of yours; do not try to fix them as part of other work.

- `./gradlew :shared:compileKotlinIosSimulatorArm64`: five errors in `IosPageCapture.kt` (a library built with a newer Kotlin). Your change is fine if there are no other errors.
- `./gradlew :webDemo:wasmJsBrowserDistribution`: stops at `kotlinWasmStoreYarnLock`. Run `./gradlew :webDemo:compileKotlinWasmJs` instead. Do not run `kotlinWasmUpgradeYarnLock`.

Remove this section when they pass.
```

### Step 5: Point the human guide at the new files

In `docs/DEVELOPMENT.md`, in the section "Before you push", add this line directly above the code block:

```markdown
`tools/verify.sh full` runs everything below except the device tests; `tools/verify.sh` runs the first line only.
```

and add this line at the end of the "Good to know" list:

```markdown
- Working rules for contributors and coding agents (tests, strings, privacy, commit style) are in [AGENTS.md](../AGENTS.md).
```

**Verify:** `rg -n "verify.sh|AGENTS.md" docs/DEVELOPMENT.md` prints two lines.

## Test plan

No application behaviour changes, so there are no new tests. The checks are: the script runs green, git tracks exactly one Xcode scheme in the shared location, and `gradle.properties` is no longer matched by an ignore rule.

## Done criteria

- [ ] `git check-ignore -v --no-index gradle.properties` prints nothing.
- [ ] `git ls-files | rg "xcuserdata|xcshareddata"` prints only the shared scheme.
- [ ] `rg -n "^xcuserdata/" .gitignore` prints one line.
- [ ] `git ls-files -s tools/verify.sh` starts with `100755`, and `tools/verify.sh` ends with `BUILD SUCCESSFUL`.
- [ ] `AGENTS.md` exists at the root and the two checks in Step 4 pass.
- [ ] `git status --short` lists only in-scope files.

## STOP conditions

- `AGENTS.md` already exists at the root.
- `gradle.properties` turns out to contain a credential (a password, token or key). Do not print it. Report the line number and the kind of value; the fix is then rotation and removal, which is a different task.
- `xcodebuild -list` no longer shows the `iosApp` scheme after Step 2.
- Removing the ignore rule surfaces untracked `gradle.properties` files you cannot explain.

## Maintenance notes

- `AGENTS.md` is only useful while it is true. Update it in the same change that alters a rule it states (for example when `strings_v7.xml` is succeeded by a `strings_v8.xml`).
- If the iOS project is regenerated from `iosApp/project.yml` with XcodeGen, check that the shared scheme is still produced; XcodeGen can define schemes in `project.yml`, which would be the durable home for it.
- The files under `xcuserdata/` remain in git history. They contain no secrets, so no history rewrite is needed.
