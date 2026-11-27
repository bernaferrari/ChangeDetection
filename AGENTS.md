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
