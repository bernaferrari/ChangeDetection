# Plan 012: Fix the visibly broken French, Russian, Indonesian and Arabic strings, and measure how many more there are

> **Executor instructions**
> You are implementing one small, self-contained change to translation files. You have not seen the audit that produced this plan; everything you need is in this file.
> 1. Run the drift check first: `git diff --stat a9f8f8ba..HEAD -- shared/src/commonMain/composeResources/values-fr shared/src/commonMain/composeResources/values-ru shared/src/commonMain/composeResources/values-id shared/src/commonMain/composeResources/values-ar`. Other plans add new strings at the end of these files, which is fine. Find each string by its `name`, not by line number. If a string's current text differs from the "Current" column below, leave that string alone and report it.
> 2. Follow the steps in order and run each **Verify** command.
> 3. Touch only the files listed under "In scope".
> 4. If a STOP condition applies, stop and report. Do not improvise.
> 5. When finished, set this plan's row in `plans/README.md` to `DONE`.

## Status

| Field | Value |
| --- | --- |
| Priority | P3 |
| Effort | S (1 hour) |
| Risk of the fix | Low. Seventeen strings in four files |
| Depends on | Nothing |
| Category | Localisation quality |
| Planned at | commit `a9f8f8ba`, 2026-11-09 |

## Why this matters

The app ships in twelve languages. Eight of them read naturally in the samples checked. Four files (French, Russian, Indonesian, Arabic) contain strings that are raw machine translation and are wrong in ways any native speaker notices at once:

- Russian "Nothing saved yet" reads as "Haven't rescued anything yet". Russian "watchlist" was translated as "list of clocks".
- French and Russian error titles speak in the first person ("I couldn't open this page").
- The note about Turbo and the schedule is ungrammatical in all four ("Turbo sessions se déroulent elles-mêmes et ne pas le modifier").
- Indonesian uses the Malay word "Jadual" where Indonesian says "Jadwal".

These appear on main screens (the empty Settings storage row, the browser error state, the tablet layout's empty pane, the schedule screen). For a product whose store listing leads with being local and trustworthy, visibly broken text in four languages undermines that for those users.

**Scope honesty.** The audit sampled these files; it did not review all of their roughly 640 strings each. The strings below were read and are certainly wrong. The samples suggest many older strings in these four files have the same origin. This plan fixes the confirmed ones and adds a measuring step so the maintainer can decide whether to commission a full review of the four files. It does not attempt a full re-translation.

## Current state

All files are under `shared/src/commonMain/composeResources/`. Each of these four languages keeps all its strings in one file: `values-fr/strings.xml`, `values-ru/strings.xml`, `values-id/strings.xml`, `values-ar/strings.xml`.

The English originals:

| Name | English (file) |
| --- | --- |
| `checks_own_schedule_note` | This schedule applies to all active pages. Turbo sessions run on their own and don’t change it. (`values/strings_v6.xml:229`) |
| `select_site_canvas_supporting` | Choose a page from the watchlist. Its newest version opens here without taking you away from the rest. (`values/strings.xml:27`) |
| `local_browser_error_title` | Couldn’t open this page (`values/strings.xml:182`) |
| `settings_storage_empty` | Nothing saved yet (`values/strings_v6.xml:248`) |
| `settings_group_app` | App (`values/strings_v6.xml:241`). The heading of a group of rows in Settings. French and Russian only |

Good translations of the first string, for reference on tone (these are correct and must not be changed):

- German: "Dieser Zeitplan gilt für alle aktiven Seiten. Turbo läuft unabhängig davon und ändert ihn nicht."
- Spanish: "Esta programación se aplica a todas las páginas activas. Las sesiones Turbo funcionan por separado y no la cambian."

Conventions in these files: French addresses the user as "vous"; Russian as "вы"; apostrophes are the typographic `’`; "Turbo" is a product name and stays in Latin letters.

A test, `StringResourcesTest` (in `shared/src/desktopTest`), fails if a string name is defined twice in one language folder.

## Commands you will need

| Purpose | Command | Expected |
| --- | --- | --- |
| XML is well formed | `xmllint --noout shared/src/commonMain/composeResources/values-{fr,ru,id,ar}/strings.xml` | no output |
| String rules | `./gradlew :shared:desktopTest --tests '*StringResourcesTest'` | `BUILD SUCCESSFUL` |
| Resources compile | `./gradlew :shared:testAndroidHostTest` | `BUILD SUCCESSFUL` |

If `xmllint` is not installed: `python3 -c "import sys, xml.dom.minidom as m; [m.parse(f) for f in sys.argv[1:]]" <files>`.

The build needs JDK 21 and Android SDK platform 37.2.

## Before you start: the state of the build

Checked on 2026-11-09 at commit `a9f8f8ba`. Read this before the steps; it overrides any line below that expects otherwise.

1. **Plan 000 must be done first.** Before you change anything, run `./gradlew :shared:spotlessCheck`. If it fails, Plan 000 (`plans/000-make-the-gates-green.md`) has not landed: STOP and report that. Do not run `spotlessApply` to get past it; on an unprepared tree it rewrites about 36 unrelated files. Once Plan 000 has landed, the test and format commands in this plan behave as described, and `spotlessApply` only touches what you changed.
2. **Two commands fail for reasons no plan fixes.** `./gradlew :shared:compileKotlinIosSimulatorArm64` stops with five errors, all in `shared/src/iosMain/kotlin/com/bernaferrari/changedetection/browser/IosPageCapture.kt` (the first is `Unresolved reference 'kmpwright'`; a library was built with a newer Kotlin than this project uses). `./gradlew :webDemo:wasmJsBrowserDistribution` compiles the Kotlin and then stops at `:kotlinWasmStoreYarnLock` with `Lock file was changed`. Wherever this plan expects one of them to print `BUILD SUCCESSFUL`, the bar is instead: for iOS, **those same five errors and no others**; for the web demo, run `./gradlew :webDemo:compileKotlinWasmJs` and expect `BUILD SUCCESSFUL`. Never run `kotlinWasmUpgradeYarnLock`. If either command has since been repaired and passes, that is fine.
3. **Line numbers may have moved.** Two changes landed after this plan was written. Plan 000 re-formats about 36 files (layout only). The "failure experience" change (`plans/failure-experience-changes.md`) added code to `SyncEngine.kt`, `SitesDao.kt`, `Site.kt`, `CheckFailure.kt`, `DiagnosticTrace.kt`, `PageDetailScreen.kt`, `MainViewModel.kt`, `LegacyMigration.kt`, `DatabaseFactory.kt`, `AppContent.kt`, `AndroidChangeNotifier.kt`, the four `PlatformDiModule` files and the string files. Find code by the text this plan quotes, not by line number. A quoted block that differs only in line breaks or indentation is the same code. The drift check at the top of this plan will list those files as changed; that alone is not a reason to stop. Stop only if the quoted code is gone or does something different.

## Scope

**In scope:**

- `shared/src/commonMain/composeResources/values-fr/strings.xml`
- `shared/src/commonMain/composeResources/values-ru/strings.xml`
- `shared/src/commonMain/composeResources/values-id/strings.xml`
- `shared/src/commonMain/composeResources/values-ar/strings.xml`
- `plans/README.md` (status column only)

**Out of scope:**

- Every other string in these files, even ones you believe are wrong. List them in your report (Step 3) instead of changing them.
- The other eight languages and the English files.
- String names, order, or placeholders such as `%1$s`.

## Git workflow

- Branch from `main`: `git switch -c advisor/012-fix-broken-translations`
- Commit subjects are plain sentences. Suggested: `Broken French, Russian, Indonesian and Arabic strings read correctly`.
- Do not push, merge, or open a pull request.

## Steps

### Step 1: Replace the confirmed strings

For each row, find the `<string name="...">` element in the named file and replace only its text. The "Current" column is what the file contains at the planned commit.

**French, `values-fr/strings.xml`**

| Name | Current | Replace with |
| --- | --- | --- |
| `checks_own_schedule_note` | Ce calendrier s’applique à toutes les pages actives. Turbo sessions se déroulent elles-mêmes et ne pas le modifier. | Ce calendrier s’applique à toutes les pages actives. Les sessions Turbo fonctionnent séparément et ne le modifient pas. |
| `select_site_canvas_supporting` | Choisissez une page de la liste d’observations. sa dernière version s’ouvre ici sans vous enlever du reste. | Choisissez une page dans la liste. Sa dernière version s’ouvre ici, sans quitter le reste. |
| `local_browser_error_title` | Je ne pouvais pas ouvrir cette page | Impossible d’ouvrir cette page |
| `settings_storage_empty` | Rien n’a encore été sauvé | Rien d’enregistré pour l’instant |
| `settings_group_app` | App pour | Application |

**Russian, `values-ru/strings.xml`**

| Name | Current | Replace with |
| --- | --- | --- |
| `checks_own_schedule_note` | Этот график распространяется на все активные страницы. Turbo сеансов проходит самостоятельно и не меняйте его. | Это расписание действует для всех активных страниц. Сеансы Turbo работают отдельно и не меняют его. |
| `select_site_canvas_supporting` | Выберите страницу из списка часов. ее новейшая версия открывается здесь, не отнимая вас от остальных. | Выберите страницу из списка. Её последняя версия откроется здесь, а список останется на месте. |
| `local_browser_error_title` | Я не могу открыть эту страницу | Не удалось открыть эту страницу |
| `settings_storage_empty` | Ничего еще не спас | Пока ничего не сохранено |
| `settings_group_app` | Апп | Приложение |

**Indonesian, `values-id/strings.xml`**

| Name | Current | Replace with |
| --- | --- | --- |
| `checks_own_schedule_note` | Jadual ini berlaku untuk semua halaman aktif. Turbo sesi berjalan sendiri dan tidak mengubahnya. | Jadwal ini berlaku untuk semua halaman aktif. Sesi Turbo berjalan terpisah dan tidak mengubahnya. |
| `select_site_canvas_supporting` | Pilih halaman dari daftar penonton. versi terbaru dibuka di sini tanpa mengambil Anda jauh dari yang lain. | Pilih halaman dari daftar. Versi terbarunya terbuka di sini tanpa meninggalkan daftar. |
| `settings_storage_empty` | Belum ada yang diselamatkan | Belum ada yang disimpan |

(The Indonesian `local_browser_error_title`, "Tidak bisa membuka halaman ini", is correct. Leave it.)

**Arabic, `values-ar/strings.xml`**

| Name | Current | Replace with |
| --- | --- | --- |
| `checks_own_schedule_note` | هذا الجدول الزمني ينطبق على جميع الصفحات النشطة. Turbo جلسة تعمل من تلقاء نفسها ولا تغيرها. | ينطبق هذا الجدول على جميع الصفحات النشطة. جلسات Turbo تعمل بشكل مستقل ولا تغيّره. |
| `select_site_canvas_supporting` | اختر صفحة من قائمة المشاهدين.تفتح أحدث نسختها هنا دون أن تأخذك بعيدا عن الباقي. | اختر صفحة من القائمة. يُفتح أحدث إصدار منها هنا دون مغادرة القائمة. |
| `local_browser_error_title` | لا أستطيع فتح هذه الصفحة | تعذّر فتح هذه الصفحة |
| `settings_storage_empty` | لا شيء تم إنقاذه حتى الآن | لم يُحفظ شيء بعد |

The replacements were written for this plan and have not been checked by native speakers. They fix clear errors, but say in your final report that a native review is still advisable.

**Verify:** each of these must print nothing:

```bash
rg -n "ne pas le modifier|Je ne pouvais pas|encore été sauvé|liste d’observations|>App pour<" shared/src/commonMain/composeResources/values-fr/strings.xml
rg -n "списка часов|Ничего еще не спас|Я не могу открыть|не меняйте его|>Апп<" shared/src/commonMain/composeResources/values-ru/strings.xml
rg -n "Jadual|diselamatkan|daftar penonton" shared/src/commonMain/composeResources/values-id/strings.xml
rg -n "قائمة المشاهدين|إنقاذه|لا أستطيع فتح" shared/src/commonMain/composeResources/values-ar/strings.xml
```

If `Jadual` still matches in another Indonesian string, do not change that string; include it in the Step 3 report.

### Step 2: Check the files

**Verify:** `xmllint --noout shared/src/commonMain/composeResources/values-{fr,ru,id,ar}/strings.xml`
Expected: no output.

**Verify:** `git diff --stat`
Expected: four files changed, with 5, 5, 3 and 4 changed lines (fr, ru, id, ar): seventeen insertions and seventeen deletions in total.

**Verify:** `./gradlew :shared:desktopTest --tests '*StringResourcesTest' :shared:testAndroidHostTest`
Expected: `BUILD SUCCESSFUL`.

### Step 3: Measure how widespread the problem is (report only, change nothing)

Machine translation leaves mechanical traces that can be counted without knowing the language. Run these and put the **counts and the first ten matching lines of each** in your final report:

```bash
# A sentence that starts with a lower-case letter after a full stop.
for l in fr ru id de es it pt; do echo "$l: $(rg -c '\. [a-zа-яё]' shared/src/commonMain/composeResources/values-$l/ | paste -sd' ' -)"; done

# A full stop glued to the next word.
for l in fr ru id ar; do echo "$l: $(rg -c '[^.0-9]\.[^ .<0-9"…]' shared/src/commonMain/composeResources/values-$l/strings.xml)"; done

# First-person phrasing in error text (the app does not speak as "I" in English).
rg -n -c "Je ne |Je n’|J’ai " shared/src/commonMain/composeResources/values-fr/strings.xml
rg -n -c "Я не |Я " shared/src/commonMain/composeResources/values-ru/strings.xml
```

German, Spanish, Italian and Portuguese are included in the first command as a baseline: they are believed to be good, so their counts show how many false positives the pattern has.

Do not fix what these searches find. The purpose is to give the maintainer a number.

## Test plan

- `StringResourcesTest` (existing) proves no name was duplicated or lost.
- Well-formedness check on the four XML files.
- No automated test can judge translation quality; the report from Step 3 and a native review are the follow-up.

## Done criteria

- [ ] The four searches in Step 1 print nothing (except a possibly reported extra `Jadual`).
- [ ] `git diff --stat` shows exactly the four translation files (plus `plans/README.md`), seventeen lines changed.
- [ ] `./gradlew :shared:desktopTest --tests '*StringResourcesTest' :shared:testAndroidHostTest` is `BUILD SUCCESSFUL`.
- [ ] The final report contains the Step 3 counts and sample lines, and the note that replacements need native review.

## STOP conditions

- A string in the tables is missing from its file, or its current text is different from the "Current" column. Skip it and report; stop entirely if more than three are different, because the files have been reworked since this plan was written.
- An XML file fails to parse after your edit.

## Maintenance notes

- The likely real fix is a full review of `values-fr`, `values-ru`, `values-id` and `values-ar` by native speakers or a dedicated translation pass with review. Step 3's numbers are the input for that decision. It is deliberately not part of this plan: rewriting about 2,500 strings without review would trade one kind of error for another.
- `StringResourcesTest` only requires Spanish, Italian and Portuguese to be complete. If the four languages here are ever judged not good enough to ship, removing a language folder makes the app fall back to English for it, which is a legitimate option.
- When new English strings are added, translate them from the final English text with the product terms in mind ("Turbo" stays as is; "watchlist" is the list of watched pages, not viewers or clocks).
