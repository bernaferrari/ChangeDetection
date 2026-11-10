# Plan 001: Stop the database from silently erasing itself when a migration is missing

> **Executor instructions**
> You are implementing one small, self-contained change. You have not seen the audit that produced this plan; everything you need is in this file.
> 1. Run the drift check first: `git diff --stat a9f8f8ba..HEAD -- shared/src/commonMain/kotlin/com/bernaferrari/changedetection/repo/source/local/ shared/src/desktopTest/kotlin/com/bernaferrari/changedetection/repo/SchemaUpgradeTest.kt`. If it prints anything, re-read the "Current state" excerpts against the real files before editing. If they no longer match, STOP and report.
> 2. Follow the steps in order. Run each **Verify** command and compare with the expected result before moving on.
> 3. Touch only the files listed under "In scope".
> 4. If a STOP condition applies, stop and report. Do not improvise.
> 5. When finished, set this plan's row in `plans/README.md` to `DONE`.

## Status

| Field | Value |
| --- | --- |
| Priority | P1 |
| Effort | S (about 1 hour) |
| Risk of the fix | Low. Narrows a builder option; one new test |
| Depends on | Nothing |
| Category | Bug (data loss) |
| Planned at | commit `a9f8f8ba`, 2026-11-09 |

## Why this matters

The app keeps every watched page and its whole history in one Room database on the device. There is no account and no cloud copy, so the database is the only copy.

The database is built with `fallbackToDestructiveMigration(true)`. That tells Room: "if you cannot find a migration from the version on disk to the current version, drop every table and start empty". The intent, per the comment in the code, is narrower: only *retired development schemas* (versions that never reached the public) may start fresh.

The blanket option also covers the public versions. If a future release bumps the schema to version 7 and the 6 → 7 migration is forgotten, mistyped, or not registered, every user's watchlist and history is erased on first launch, with no error. The only trace is a `DATABASE_RESET` diagnostics event that nobody sees.

After this change a missing migration from a public version makes the app fail loudly at startup (which the test suite and any manual run will catch before release) instead of deleting user data.

## Current state

`shared/src/commonMain/kotlin/com/bernaferrari/changedetection/repo/source/local/DatabaseFactory.kt:13-25`:

```kotlin
fun getRoomDatabase(
    builder: RoomDatabase.Builder<ChangeDatabase>,
    driver: SQLiteDriver,
    migrations: Array<out Migration> = emptyArray(),
): ChangeDatabase = builder
    .setDriver(driver)
    // Preserve the original public release. Retired development schemas may start fresh.
    .addMigrations(MIGRATION_1_4, MIGRATION_4_5, MIGRATION_5_6, *migrations)
    .fallbackToDestructiveMigration(true)
    .addCallback(snapshotCleanupCallback)
    .addCallback(databaseDiagnosticsCallback)
    .setQueryCoroutineContext(Dispatchers.Default)
    .build()
```

Facts you need:

- The current schema version is 6 (`ChangeDatabase.kt:15`, `version = 6`). It was 5 at commit `a9f8f8ba`; the "failure experience" change (see "Before you start") added three columns to `sites` and the 5 → 6 migration.
- Public versions are 1 (every release from 2019 to 2024), 4, 5 and 6. Versions 2 and 3 only existed in development builds. This is documented at the top of `LegacyMigration.kt:8-22`.
- `MIGRATION_1_4`, `MIGRATION_4_5` and `MIGRATION_5_6` are defined in `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/repo/source/local/LegacyMigration.kt` (same package as `DatabaseFactory.kt`).
- No caller passes the `migrations` argument today. All callers use `getRoomDatabase(builder, driver)`.
- The web demo (`shared/src/wasmJsMain/.../DatabaseFactory.wasmJs.kt`) sets its own blanket fallback on the builder before calling `getRoomDatabase`. That is fine (the demo holds sample data only) and must stay as it is.
- An existing test pins the intended behaviour for retired schemas. `shared/src/desktopTest/kotlin/com/bernaferrari/changedetection/repo/SchemaUpgradeTest.kt:26-51`, test `retiredDevelopmentSchemasStartFresh`, sets `PRAGMA user_version` to each of `2, 3, 10` and asserts the database opens empty. Version 10 is *higher* than the current version 6, so it is a downgrade. This test must keep passing.

Room 3.0.3 offers the narrower options (confirmed in the library sources):

```kotlin
public fun fallbackToDestructiveMigrationOnDowngrade(dropAllTables: Boolean = true): Builder<T>
public fun fallbackToDestructiveMigrationFrom(dropAllTables: Boolean = true, vararg startVersions: Int): Builder<T>
```

## Commands you will need

| Purpose | Command | Expected |
| --- | --- | --- |
| Run the one test class | `./gradlew :shared:desktopTest --tests '*SchemaUpgradeTest'` | `BUILD SUCCESSFUL` |
| Format check | `./gradlew :shared:spotlessCheck` | `BUILD SUCCESSFUL` |
| Auto-format (if the check fails) | `./gradlew :shared:spotlessApply` | `BUILD SUCCESSFUL` |
| Full gate | `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest` | `BUILD SUCCESSFUL` |
| iOS still compiles (macOS only) | `./gradlew :shared:compileKotlinIosSimulatorArm64` | `BUILD SUCCESSFUL` |
| Web demo still compiles | `./gradlew :webDemo:wasmJsBrowserDistribution` | `BUILD SUCCESSFUL` |

The build needs JDK 21 and Android SDK platform 37.2 (set `ANDROID_HOME`), even for desktop tasks.

## Before you start: the state of the build

Checked on 2026-11-09 at commit `a9f8f8ba`. Read this before the steps; it overrides any line below that expects otherwise.

1. **Plan 000 must be done first.** Before you change anything, run `./gradlew :shared:spotlessCheck`. If it fails, Plan 000 (`plans/000-make-the-gates-green.md`) has not landed: STOP and report that. Do not run `spotlessApply` to get past it; on an unprepared tree it rewrites about 36 unrelated files. Once Plan 000 has landed, the test and format commands in this plan behave as described, and `spotlessApply` only touches what you changed.
2. **Two commands fail for reasons no plan fixes.** `./gradlew :shared:compileKotlinIosSimulatorArm64` stops with five errors, all in `shared/src/iosMain/kotlin/com/bernaferrari/changedetection/browser/IosPageCapture.kt` (the first is `Unresolved reference 'kmpwright'`; a library was built with a newer Kotlin than this project uses). `./gradlew :webDemo:wasmJsBrowserDistribution` compiles the Kotlin and then stops at `:kotlinWasmStoreYarnLock` with `Lock file was changed`. Wherever this plan expects one of them to print `BUILD SUCCESSFUL`, the bar is instead: for iOS, **those same five errors and no others**; for the web demo, run `./gradlew :webDemo:compileKotlinWasmJs` and expect `BUILD SUCCESSFUL`. Never run `kotlinWasmUpgradeYarnLock`. If either command has since been repaired and passes, that is fine.
3. **Line numbers may have moved.** Two changes landed after this plan was written. Plan 000 re-formats about 36 files (layout only). The "failure experience" change (`plans/failure-experience-changes.md`) added code to `SyncEngine.kt`, `SitesDao.kt`, `Site.kt`, `CheckFailure.kt`, `DiagnosticTrace.kt`, `PageDetailScreen.kt`, `MainViewModel.kt`, `LegacyMigration.kt`, `DatabaseFactory.kt`, `AppContent.kt`, `AndroidChangeNotifier.kt`, the four `PlatformDiModule` files and the string files. Find code by the text this plan quotes, not by line number. A quoted block that differs only in line breaks or indentation is the same code. The drift check at the top of this plan will list those files as changed; that alone is not a reason to stop. Stop only if the quoted code is gone or does something different.

## Scope

**In scope (the only files you may change):**

- `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/repo/source/local/DatabaseFactory.kt`
- `shared/src/desktopTest/kotlin/com/bernaferrari/changedetection/repo/SchemaUpgradeTest.kt`
- `plans/README.md` (status column only)

**Out of scope (do not touch):**

- `LegacyMigration.kt`, `ChangeDatabase.kt`, anything under `shared/schemas/`. Do not change the schema version.
- `shared/src/wasmJsMain/.../DatabaseFactory.wasmJs.kt`. The demo keeps its blanket fallback.
- `shared/src/androidMain/.../LegacyDatabaseImport.kt`.
- The existing tests in `SchemaUpgradeTest.kt`. Add a test; do not edit the others.

## Git workflow

- Branch from `main`: `git switch -c advisor/001-narrow-destructive-database-fallback`
- Commit subjects in this repo are plain sentences, not Conventional Commits. Examples from the log: "Refactor change detection flow and simplify implementation", "Settings opens without a Loading flash". Suggested subject: `A missing migration no longer erases a public database`.
- Do not push, merge, or open a pull request.

## Steps

### Step 1: Make the migration list replaceable (no behaviour change yet)

The test in Step 2 must be able to open the database *without* any migration from version 4. Today `getRoomDatabase` always adds all three migrations, so first turn the `migrations` parameter from "extra migrations" into "the full list".

In `DatabaseFactory.kt`, add this above `getRoomDatabase`:

```kotlin
/** Every migration between schema versions that reached the public. */
internal val PUBLIC_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_4, MIGRATION_4_5, MIGRATION_5_6)
```

Change the parameter default from `migrations: Array<out Migration> = emptyArray()` to `migrations: Array<out Migration> = PUBLIC_MIGRATIONS`, and change `.addMigrations(MIGRATION_1_4, MIGRATION_4_5, MIGRATION_5_6, *migrations)` to `.addMigrations(*migrations)`. Leave `.fallbackToDestructiveMigration(true)` in place for now.

**Verify:** `./gradlew :shared:desktopTest --tests '*SchemaUpgradeTest'`
Expected: `BUILD SUCCESSFUL`, five tests pass (nothing changed in behaviour).

### Step 2: Write the failing test

Add this test to `SchemaUpgradeTest.kt`, directly after `retiredDevelopmentSchemasStartFresh` (ends at line 51). It reuses the private helpers already in the file (`withDatabaseFile`, `exec`, `queryAll`). Add `import com.bernaferrari.changedetection.repo.source.local.MIGRATION_1_4` next to the other imports at the top of the file.

```kotlin
    @Test fun aPublicSchemaWithoutAMigrationPathIsNeverErased() = runBlocking {
        withDatabaseFile { path, driver ->
            getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = path), driver).apply {
                siteDao().getSitesList()
                close()
            }
            driver.open(path).use { connection ->
                connection.exec("INSERT INTO sites (title,url,timestamp,siteId,isSuccessful,isBrowser,threshold,isRead,isSyncEnabled,isNotificationEnabled,notes,colors,constraints) VALUES ('Page','https://example.test',123,'page',1,0,0,0,1,1,'notes','0,0','false,false,false,false')")
                // Pretend this file is the public version 4, then open it with no migration from version 4 registered.
                connection.exec("PRAGMA user_version = 4")
            }
            val failure = runCatching {
                getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = path), driver, migrations = arrayOf(MIGRATION_1_4)).apply {
                    try {
                        siteDao().getSitesList()
                    } finally {
                        close()
                    }
                }
            }.exceptionOrNull()
            assertNotNull("a public schema with no migration path must refuse to open, not start fresh", failure)
            driver.open(path).use { connection ->
                assertEquals("the page must still be on disk", "1", connection.queryAll("SELECT count(*) FROM sites").trim())
                assertEquals("4", connection.queryAll("PRAGMA user_version").trim())
            }
        }
    }
```

**Verify:** `./gradlew :shared:desktopTest --tests '*SchemaUpgradeTest'`
Expected now: the build **fails**, and the only failing test is `aPublicSchemaWithoutAMigrationPathIsNeverErased`, with the message "a public schema with no migration path must refuse to open, not start fresh". This is the bug: with the blanket fallback, Room found no migration from version 4, dropped every table and opened an empty database without any error. The other five tests pass.

### Step 3: Narrow the fallback

In `DatabaseFactory.kt`, make `PUBLIC_MIGRATIONS` and the `getRoomDatabase` function read as follows (this replaces the function you edited in Step 1):

```kotlin
/** Every migration between schema versions that reached the public. */
internal val PUBLIC_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_4, MIGRATION_4_5, MIGRATION_5_6)

/** Schema versions that only ever existed in development builds, so a database at one of them may start fresh. */
internal val RETIRED_DEVELOPMENT_SCHEMAS = intArrayOf(2, 3)

fun getRoomDatabase(
    builder: RoomDatabase.Builder<ChangeDatabase>,
    driver: SQLiteDriver,
    migrations: Array<out Migration> = PUBLIC_MIGRATIONS,
): ChangeDatabase = builder
    .setDriver(driver)
    .addMigrations(*migrations)
    // Only retired development schemas and downgrades start fresh. A public version with no migration
    // path must fail to open, because a silent reset would erase the only copy of the user's history.
    .fallbackToDestructiveMigrationFrom(true, *RETIRED_DEVELOPMENT_SCHEMAS)
    .fallbackToDestructiveMigrationOnDowngrade(true)
    .addCallback(snapshotCleanupCallback)
    .addCallback(databaseDiagnosticsCallback)
    .setQueryCoroutineContext(Dispatchers.Default)
    .build()
```

Also update the KDoc on `databaseDiagnosticsCallback` (lines 27-31): change "and the destructive fallback above, which otherwise wipes history without a trace" to "and a destructive reset of a retired development schema or a downgrade".

Do not add or remove imports; `Migration` is already imported.

**Verify:** `./gradlew :shared:desktopTest --tests '*SchemaUpgradeTest'`
Expected: `BUILD SUCCESSFUL`, all six tests pass, including the existing `retiredDevelopmentSchemasStartFresh` (versions 2 and 3 are covered by `fallbackToDestructiveMigrationFrom`; version 10 is covered by the downgrade fallback) and `versionFiveUpgradeKeepsPagesAndStartsFailureStreaksEmpty` (the real 5 → 6 upgrade).

### Step 4: Confirm nothing else depended on the old behaviour

**Verify:** `rg -n "fallbackToDestructiveMigration\(" shared/src`
Expected: exactly one match, in `shared/src/wasmJsMain/kotlin/com/bernaferrari/changedetection/repo/source/local/DatabaseFactory.wasmJs.kt`.

**Verify:** `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest`
Expected: `BUILD SUCCESSFUL`. If `spotlessCheck` fails, run `./gradlew :shared:spotlessApply` and re-run.

**Verify:** `./gradlew :webDemo:wasmJsBrowserDistribution` (and on macOS `./gradlew :shared:compileKotlinIosSimulatorArm64`)
Expected: `BUILD SUCCESSFUL`.

## Test plan

- New test `aPublicSchemaWithoutAMigrationPathIsNeverErased` in `SchemaUpgradeTest.kt` (Step 2). It fails after Step 2 and passes after Step 3.
- Existing tests that must stay green: `release22DatabaseUpgrades`, `releases2019To2024DatabaseUpgrades`, `retiredDevelopmentSchemasStartFresh`, `versionFiveUpgradeKeepsPagesAndStartsFailureStreaksEmpty`, `historyQueriesUseTheirIndexWithoutSorting`, and everything in `Schema5DurabilityTest`.

## Done criteria

- [ ] `rg -n "fallbackToDestructiveMigration\(" shared/src/commonMain` prints nothing.
- [ ] `rg -n "fallbackToDestructiveMigrationFrom|fallbackToDestructiveMigrationOnDowngrade" shared/src/commonMain` prints two lines, both in `DatabaseFactory.kt`.
- [ ] `./gradlew :shared:desktopTest --tests '*SchemaUpgradeTest'` passes with six tests.
- [ ] `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest` is `BUILD SUCCESSFUL`.
- [ ] `git status --short` lists only the in-scope files.

## STOP conditions

Stop and report (do not work around) if:

- The new test passes at the end of Step 2, before you narrow the fallback. That means the premise of this plan is wrong.
- `fallbackToDestructiveMigrationFrom` or `fallbackToDestructiveMigrationOnDowngrade` does not compile. Report the exact error; do not go back to the blanket fallback.
- `retiredDevelopmentSchemasStartFresh` fails after Step 3.
- After Step 3 the new test fails on the row count or `user_version` assertion (the open failed, but the file was still modified).
- Any caller of `getRoomDatabase` turns out to pass a `migrations` argument (`rg -n "getRoomDatabase\(" shared/src | rg "migrations"`). The default changed meaning from "extra migrations" to "the full list".

## Maintenance notes

- Every future schema bump must add its migration to `PUBLIC_MIGRATIONS` and a case to `SchemaUpgradeTest`. With this change, forgetting to do so fails the test suite and crashes at startup instead of erasing data. That is the intended trade.
- The downgrade fallback stays destructive. Android blocks installing an older build over a newer one, so this only affects development devices.
- Development builds once used version numbers up to 10. Version 6 became a real public schema in November 2026 (the failure-streak columns), so a development device still holding a *retired* version 6 database fails Room's schema check; the same will happen for 7 to 10 as real versions reach them. That affects the maintainer's own devices only; clearing app data fixes it.
- Reviewers should check that no new code path calls the blanket `fallbackToDestructiveMigration` outside the web demo.
