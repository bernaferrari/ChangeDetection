# Plan 016: Add "Backup" to Settings on Android: save pages and settings, save everything, restore

> **Executor instructions**
> You are adding one Settings entry and the screen behind it. You have not seen the audit or the conversation that produced this plan; everything you need is in this file.
> 1. **Plan 015 must be DONE first.** Check: `rg -n "class BackupService|class ZipBackupSource|class ManifestOnlySource" shared/src` must print three lines. If it does not, STOP.
> 2. Run the drift check: `git diff --stat a9f8f8ba..HEAD -- shared/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/settings/SettingsScreen.kt shared/src/androidMain/kotlin/com/bernaferrari/changedetection/diagnostics/DiagnosticsEntry.android.kt shared/src/androidMain/res/xml/ PRIVACY.md`. If `SettingsScreen.kt` changed, confirm the "app" group quoted under "Current state" is still there before editing.
> 3. Follow the steps in order and run each **Verify** command.
> 4. Touch only the files listed under "In scope".
> 5. If a STOP condition applies, stop and report. Do not improvise.
> 6. When finished, set this plan's row in `plans/README.md` to `DONE`.

## Status

| Field | Value |
| --- | --- |
| Priority | Direction (most-requested missing feature) |
| Effort | M (about a day, mostly strings and a device check) |
| Risk | Low to medium. The engine is already tested; this plan adds file pickers and wording |
| Depends on | Plan 015 |
| Category | Feature |
| Planned at | commit `a9f8f8ba`, 2026-11-09 |

## Why this matters

Plan 015 built an engine that can write and read backups, but nobody can reach it. This plan puts it in front of people: **Settings → App → Backup**, with three actions.

| Action | What the person gets |
| --- | --- |
| **Save pages and settings** | A small `.json` file with every watched page, its options, and the app's settings. No saved versions. This is the first, default choice |
| **Save everything** | A `.zip` with the same content plus every saved version, for moving to a new phone with history intact |
| **Restore from a file** | Accepts either file. Adds the pages that are not on the device yet; changes nothing that is already there |

The file goes where the person chooses in the system file picker (Downloads, Drive, a USB stick). The app needs no storage permission for this.

## Current state

**Where the row goes.** `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/settings/SettingsScreen.kt:163-188`, the "app" group. Its last row today:

```kotlin
                        SavedVersionsRow(uiState, viewModel::retryStorageLoading, viewModel::clearOlderVersions, showDivider = onNotificationSettings != null)
                    }
                }
```

**How a platform-only Settings row is done here.** The row is declared in common code and implemented per platform; platforms without the feature render nothing. The exemplar is `DiagnosticsRow`:

- `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/diagnostics/DiagnosticsEntry.kt:9`: `@Composable expect fun DiagnosticsRow()`
- `shared/src/desktopMain/.../diagnostics/DiagnosticsEntry.desktopMain.kt:7`, `iosMain/.../DiagnosticsEntry.iosMain.kt:7`, `wasmJsMain/.../DiagnosticsEntry.wasmJsMain.kt:7`: `@Composable actual fun DiagnosticsRow() = Unit`
- `shared/src/androidMain/.../diagnostics/DiagnosticsEntry.android.kt:38-49`:

```kotlin
@Composable
actual fun DiagnosticsRow() {
    var show by remember { mutableStateOf(false) }
    val context = LocalContext.current
    com.bernaferrari.changedetection.ui.kit.GroupRow(
        context.getString(R.string.diagnostics_export),
        modifier = Modifier.testTag("export-diagnostics"),
        icon = androidx.compose.material.icons.Icons.Outlined.BugReport,
        onClick = { show = true },
    )
    if (show) DiagnosticsDialog(null, onClose = { show = false })
}
```

That file also shows the imports Android UI code uses for text: `com.bernaferrari.changedetection.localization.stringResource` and `com.bernaferrari.changedetection.shared.generated.resources.*`. The helper's signature is `stringResource(resource: StringResource, vararg args: Any): String`.

**The row component.** `ui/kit/Kit.kt:541`: `GroupRow(title: String, modifier: Modifier = Modifier, supporting: String? = null, icon: ImageVector? = null, showDivider: Boolean = false, ..., enabled: Boolean = true, onClick: (() -> Unit)? = null, ...)`.

**Reaching services from a composable.** `ui/kit/ChangePreviewViews.kt:475`: `internal inline fun <reified T : Any> optional(): T? = KoinPlatformTools.defaultContext().getOrNull()?.getOrNull<T>()`. Long-running work that must outlive a screen runs on `util/ApplicationScope.kt`: `class ApplicationScope { val scope: CoroutineScope }`, a singleton.

**After pages or schedule settings change.** `background/MonitoringManager.kt:22`: `suspend fun updateSchedule()` ("Reconcile persistent scheduling with the stored policy").

**Strings.** New English strings go at the end of `shared/src/commonMain/composeResources/values/strings_v7.xml` (before `</resources>`). Spanish, Italian and Portuguese also have a `strings_v7.xml` in `values-es`, `values-it`, `values-pt`. Arabic, Bengali, German, French, Hindi, Indonesian, Russian and Chinese each have a single `strings.xml` in `values-ar`, `values-bn`, `values-de`, `values-fr`, `values-hi`, `values-id`, `values-ru`, `values-zh`. A test (`StringResourcesTest`) fails when a locale misses a string or a name appears twice in one folder. Apostrophes in strings are the typographic `’`.

**The backup rules comment** already promises this feature. `shared/src/androidMain/res/xml/data_extraction_rules.xml:2-6`:

```xml
<!--
  Local-only privacy posture: monitored URLs and snapshot history never leave the
  device via cloud backup. Device transfer is also excluded so the full browsing
  corpus is not exported over a cable without explicit in-app export.
-->
```

**`PRIVACY.md`** is a short document of plain paragraphs, each starting with a title line (for example `How the App Functions`), ending with `Contact Us`. Another plan may have added sections (`Backups and Moving to a New Phone`, `Diagnostic Reports`); keep whatever is there.

## Commands you will need

| Purpose | Command | Expected |
| --- | --- | --- |
| Android host tests | `./gradlew :shared:testAndroidHostTest` | `BUILD SUCCESSFUL` |
| String consistency | `./gradlew :shared:desktopTest --tests '*StringResourcesTest'` | `BUILD SUCCESSFUL` |
| Full gate | `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest` | `BUILD SUCCESSFUL` |
| Other platforms compile | `./gradlew :webDemo:wasmJsBrowserDistribution` and (macOS) `./gradlew :shared:compileKotlinIosSimulatorArm64` | `BUILD SUCCESSFUL` |
| Android build | `./gradlew :androidApp:lintDebug :androidApp:assembleRelease` | `BUILD SUCCESSFUL` |
| Install for the device check | `./gradlew :androidApp:installDebug` | `BUILD SUCCESSFUL` |

## Before you start: the state of the build

Checked on 2026-11-09 at commit `a9f8f8ba`. Read this before the steps; it overrides any line below that expects otherwise.

1. **Plan 000 must be done first.** Before you change anything, run `./gradlew :shared:spotlessCheck`. If it fails, Plan 000 (`plans/000-make-the-gates-green.md`) has not landed: STOP and report that. Do not run `spotlessApply` to get past it; on an unprepared tree it rewrites about 36 unrelated files. Once Plan 000 has landed, the test and format commands in this plan behave as described, and `spotlessApply` only touches what you changed.
2. **Two commands fail for reasons no plan fixes.** `./gradlew :shared:compileKotlinIosSimulatorArm64` stops with five errors, all in `shared/src/iosMain/kotlin/com/bernaferrari/changedetection/browser/IosPageCapture.kt` (the first is `Unresolved reference 'kmpwright'`; a library was built with a newer Kotlin than this project uses). `./gradlew :webDemo:wasmJsBrowserDistribution` compiles the Kotlin and then stops at `:kotlinWasmStoreYarnLock` with `Lock file was changed`. Wherever this plan expects one of them to print `BUILD SUCCESSFUL`, the bar is instead: for iOS, **those same five errors and no others**; for the web demo, run `./gradlew :webDemo:compileKotlinWasmJs` and expect `BUILD SUCCESSFUL`. Never run `kotlinWasmUpgradeYarnLock`. If either command has since been repaired and passes, that is fine.
3. **Line numbers may have moved.** Two changes landed after this plan was written. Plan 000 re-formats about 36 files (layout only). The "failure experience" change (`plans/failure-experience-changes.md`) added code to `SyncEngine.kt`, `SitesDao.kt`, `Site.kt`, `CheckFailure.kt`, `DiagnosticTrace.kt`, `PageDetailScreen.kt`, `MainViewModel.kt`, `LegacyMigration.kt`, `DatabaseFactory.kt`, `AppContent.kt`, `AndroidChangeNotifier.kt`, the four `PlatformDiModule` files and the string files. Find code by the text this plan quotes, not by line number. A quoted block that differs only in line breaks or indentation is the same code. The drift check at the top of this plan will list those files as changed; that alone is not a reason to stop. Stop only if the quoted code is gone or does something different.
4. **Chinese, Hindi and Bengali text may only use characters the bundled fonts contain.** The app ships cut-down fonts for those three languages (`shared/src/commonMain/composeResources/font/locale_{zh,hi,bn}_*.ttf`), and `LocalizationTest` ("bundled script fonts cover every translated UI glyph") fails on any character they lack. The translations in this plan were checked against those fonts on 2026-11-09 and fit. If you alter a `values-zh`, `values-hi` or `values-bn` string, run `./gradlew :shared:desktopTest --tests '*LocalizationTest'` straight away; if it reports a missing character, choose other words. Do not edit the font files.

## Scope

**In scope:**

- `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/backup/BackupRow.kt` (new)
- `shared/src/androidMain/kotlin/com/bernaferrari/changedetection/backup/BackupRow.android.kt` (new)
- `shared/src/androidMain/kotlin/com/bernaferrari/changedetection/backup/BackupFiles.kt` (new)
- `shared/src/desktopMain/kotlin/com/bernaferrari/changedetection/backup/BackupRow.desktopMain.kt`, `shared/src/iosMain/.../backup/BackupRow.iosMain.kt`, `shared/src/wasmJsMain/.../backup/BackupRow.wasmJsMain.kt` (new, one line each)
- `shared/src/androidMain/kotlin/com/bernaferrari/changedetection/backup/ZipBackup.kt` (add one function)
- `shared/src/androidUnitTest/kotlin/com/bernaferrari/changedetection/backup/ZipBackupTest.kt` (add tests)
- `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/settings/SettingsScreen.kt` (one line)
- The twelve string files named above
- `shared/src/androidMain/res/xml/data_extraction_rules.xml` (comment only)
- `PRIVACY.md`
- `plans/README.md` (status column only)

**Out of scope:**

- Changing `BackupService` or the file format.
- Desktop, iOS and web backup screens.
- Encryption, scheduled or automatic backups, cloud accounts.
- Any storage permission in the manifest. The system file picker needs none; if you find yourself adding one, STOP.

## Git workflow

- Branch from the branch that holds Plan 015: `git switch -c advisor/016-backup-ui`
- Commit subjects are plain sentences, for example `Pages can be saved to a file and restored from Settings`.
- Do not push, merge, or open a pull request.

## Steps

### Step 1: Recognise which kind of file was picked, test first

Add to `ZipBackupTest`:

```kotlin
    @Test fun `a picked zip is read as a full backup`() {
        val source = openBackup(ByteArrayInputStream(zipOf("manifest.json" to "{}".toByteArray(), "snapshots/a" to "one".toByteArray())))

        assertEquals("manifest.json", source.nextName())
        assertArrayEquals("{}".toByteArray(), source.readCurrent(10))
        assertEquals("snapshots/a", source.nextName())
    }

    @Test fun `a picked json file is read as a pages backup`() {
        val json = "{\"format\":\"change-detection-backup\"}".toByteArray()
        val source = openBackup(ByteArrayInputStream(json))

        assertEquals("manifest.json", source.nextName())
        assertArrayEquals(json, source.readCurrent(json.size))
        assertNull(source.nextName())
    }

    @Test fun `a huge file that is not a zip is not loaded whole`() {
        val source = openBackup(ByteArrayInputStream(ByteArray(MAX_MANIFEST_BYTES + 100) { 'x'.code.toByte() }))

        assertEquals("manifest.json", source.nextName())
        assertNull("more than a manifest can hold must be refused by the reader", source.readCurrent(MAX_MANIFEST_BYTES))
    }
```

**Verify:** `./gradlew :shared:testAndroidHostTest --tests '*ZipBackupTest'` fails to compile (unresolved `openBackup`). Expected.

Add to `ZipBackup.kt`:

```kotlin
/** A picked file is either kind of backup; the first two bytes say which, and no more than a manifest's worth of a non-zip is read. */
internal fun openBackup(input: InputStream): BackupSource {
    val buffered = BufferedInputStream(input)
    buffered.mark(2)
    val head = ByteArray(2)
    val read = buffered.read(head).coerceAtLeast(0)
    buffered.reset()
    if (looksLikeZip(head.copyOf(read))) return ZipBackupSource(buffered)
    val bytes = ByteArrayOutputStream()
    val chunk = ByteArray(16 * 1024)
    while (bytes.size() <= MAX_MANIFEST_BYTES) {
        val count = buffered.read(chunk)
        if (count < 0) break
        bytes.write(chunk, 0, count)
    }
    return ManifestOnlySource(bytes.toByteArray())
}
```

**Verify:** `./gradlew :shared:testAndroidHostTest --tests '*ZipBackupTest'` passes (eight tests).

### Step 2: Strings

Append these to `values/strings_v7.xml` before `</resources>`:

```xml
    <string name="backup_title">Backup</string>
    <string name="backup_supporting">Save your pages to a file, or restore them</string>
    <string name="backup_save_pages">Save pages and settings</string>
    <string name="backup_save_pages_body">A small file. Saved versions are not included.</string>
    <string name="backup_save_everything">Save everything</string>
    <string name="backup_save_everything_body">Includes every saved version. The file can be large.</string>
    <string name="backup_restore">Restore from a file</string>
    <string name="backup_restore_body">Adds the pages that are not here yet. Nothing on this device is changed.</string>
    <string name="backup_restore_settings">Also restore app settings</string>
    <string name="backup_privacy_note">The file is not encrypted and lists the pages you watch. Sign-ins are never included.</string>
    <string name="backup_working">Working…</string>
    <string name="backup_saved">Backup saved. Pages: %1$d · Versions: %2$d</string>
    <string name="backup_restored">Pages added: %1$d · Already here: %2$d · Versions: %3$d</string>
    <string name="backup_failed">The backup could not be saved. Check the free space and try again.</string>
    <string name="backup_invalid">This file is not a Change Detection backup.</string>
    <string name="backup_too_new">This backup was made by a newer version. Update the app and try again.</string>
```

The counts are written as "Label: number" on purpose, so no plural forms are needed in any language.

Add the same sixteen names to every other locale: at the end of `strings_v7.xml` for `es`, `it`, `pt`, and at the end of `strings.xml` for `ar`, `bn`, `de`, `fr`, `hi`, `id`, `ru`, `zh`. Drafts follow. They were written by the plan's author, not by native speakers: list them in your final report as **needing native review**.

**Spanish (`values-es`)**

```xml
    <string name="backup_title">Copia de seguridad</string>
    <string name="backup_supporting">Guarda tus páginas en un archivo o restáuralas</string>
    <string name="backup_save_pages">Guardar páginas y ajustes</string>
    <string name="backup_save_pages_body">Un archivo pequeño. No incluye las versiones guardadas.</string>
    <string name="backup_save_everything">Guardar todo</string>
    <string name="backup_save_everything_body">Incluye todas las versiones guardadas. El archivo puede ser grande.</string>
    <string name="backup_restore">Restaurar desde un archivo</string>
    <string name="backup_restore_body">Añade las páginas que aún no están aquí. No se cambia nada de este dispositivo.</string>
    <string name="backup_restore_settings">Restaurar también los ajustes de la app</string>
    <string name="backup_privacy_note">El archivo no está cifrado y contiene las páginas que sigues. Los inicios de sesión nunca se incluyen.</string>
    <string name="backup_working">Trabajando…</string>
    <string name="backup_saved">Copia guardada. Páginas: %1$d · Versiones: %2$d</string>
    <string name="backup_restored">Páginas añadidas: %1$d · Ya estaban: %2$d · Versiones: %3$d</string>
    <string name="backup_failed">No se pudo guardar la copia. Comprueba el espacio libre e inténtalo de nuevo.</string>
    <string name="backup_invalid">Este archivo no es una copia de seguridad de Change Detection.</string>
    <string name="backup_too_new">Esta copia se creó con una versión más reciente. Actualiza la app e inténtalo de nuevo.</string>
```

**Italian (`values-it`)**

```xml
    <string name="backup_title">Backup</string>
    <string name="backup_supporting">Salva le tue pagine in un file o ripristinale</string>
    <string name="backup_save_pages">Salva pagine e impostazioni</string>
    <string name="backup_save_pages_body">Un file piccolo. Le versioni salvate non sono incluse.</string>
    <string name="backup_save_everything">Salva tutto</string>
    <string name="backup_save_everything_body">Include tutte le versioni salvate. Il file può essere grande.</string>
    <string name="backup_restore">Ripristina da un file</string>
    <string name="backup_restore_body">Aggiunge le pagine che non sono ancora qui. Su questo dispositivo non cambia nulla.</string>
    <string name="backup_restore_settings">Ripristina anche le impostazioni dell’app</string>
    <string name="backup_privacy_note">Il file non è cifrato ed elenca le pagine che segui. Gli accessi non sono mai inclusi.</string>
    <string name="backup_working">Operazione in corso…</string>
    <string name="backup_saved">Backup salvato. Pagine: %1$d · Versioni: %2$d</string>
    <string name="backup_restored">Pagine aggiunte: %1$d · Già presenti: %2$d · Versioni: %3$d</string>
    <string name="backup_failed">Impossibile salvare il backup. Controlla lo spazio libero e riprova.</string>
    <string name="backup_invalid">Questo file non è un backup di Change Detection.</string>
    <string name="backup_too_new">Questo backup è stato creato con una versione più recente. Aggiorna l’app e riprova.</string>
```

**Portuguese (`values-pt`)**

```xml
    <string name="backup_title">Backup</string>
    <string name="backup_supporting">Salve suas páginas em um arquivo ou restaure-as</string>
    <string name="backup_save_pages">Salvar páginas e ajustes</string>
    <string name="backup_save_pages_body">Um arquivo pequeno. As versões salvas não são incluídas.</string>
    <string name="backup_save_everything">Salvar tudo</string>
    <string name="backup_save_everything_body">Inclui todas as versões salvas. O arquivo pode ser grande.</string>
    <string name="backup_restore">Restaurar de um arquivo</string>
    <string name="backup_restore_body">Adiciona as páginas que ainda não estão aqui. Nada neste aparelho é alterado.</string>
    <string name="backup_restore_settings">Restaurar também os ajustes do app</string>
    <string name="backup_privacy_note">O arquivo não é criptografado e lista as páginas que você acompanha. Logins nunca são incluídos.</string>
    <string name="backup_working">Trabalhando…</string>
    <string name="backup_saved">Backup salvo. Páginas: %1$d · Versões: %2$d</string>
    <string name="backup_restored">Páginas adicionadas: %1$d · Já existiam: %2$d · Versões: %3$d</string>
    <string name="backup_failed">Não foi possível salvar o backup. Verifique o espaço livre e tente de novo.</string>
    <string name="backup_invalid">Este arquivo não é um backup do Change Detection.</string>
    <string name="backup_too_new">Este backup foi feito por uma versão mais nova. Atualize o app e tente de novo.</string>
```

**German (`values-de`)**

```xml
    <string name="backup_title">Sicherung</string>
    <string name="backup_supporting">Speichere deine Seiten in einer Datei oder stelle sie wieder her</string>
    <string name="backup_save_pages">Seiten und Einstellungen speichern</string>
    <string name="backup_save_pages_body">Eine kleine Datei. Gespeicherte Versionen sind nicht enthalten.</string>
    <string name="backup_save_everything">Alles speichern</string>
    <string name="backup_save_everything_body">Enthält alle gespeicherten Versionen. Die Datei kann groß werden.</string>
    <string name="backup_restore">Aus einer Datei wiederherstellen</string>
    <string name="backup_restore_body">Fügt die Seiten hinzu, die hier noch fehlen. Auf diesem Gerät wird nichts geändert.</string>
    <string name="backup_restore_settings">Auch die App-Einstellungen wiederherstellen</string>
    <string name="backup_privacy_note">Die Datei ist nicht verschlüsselt und enthält die Seiten, die du beobachtest. Anmeldungen sind nie enthalten.</string>
    <string name="backup_working">Wird ausgeführt…</string>
    <string name="backup_saved">Sicherung gespeichert. Seiten: %1$d · Versionen: %2$d</string>
    <string name="backup_restored">Hinzugefügte Seiten: %1$d · Bereits vorhanden: %2$d · Versionen: %3$d</string>
    <string name="backup_failed">Die Sicherung konnte nicht gespeichert werden. Prüfe den freien Speicher und versuche es erneut.</string>
    <string name="backup_invalid">Diese Datei ist keine Sicherung von Change Detection.</string>
    <string name="backup_too_new">Diese Sicherung stammt von einer neueren Version. Aktualisiere die App und versuche es erneut.</string>
```

**French (`values-fr`)**

```xml
    <string name="backup_title">Sauvegarde</string>
    <string name="backup_supporting">Enregistrez vos pages dans un fichier ou restaurez-les</string>
    <string name="backup_save_pages">Enregistrer les pages et les réglages</string>
    <string name="backup_save_pages_body">Un petit fichier. Les versions enregistrées ne sont pas incluses.</string>
    <string name="backup_save_everything">Tout enregistrer</string>
    <string name="backup_save_everything_body">Inclut toutes les versions enregistrées. Le fichier peut être volumineux.</string>
    <string name="backup_restore">Restaurer depuis un fichier</string>
    <string name="backup_restore_body">Ajoute les pages qui ne sont pas encore ici. Rien n’est modifié sur cet appareil.</string>
    <string name="backup_restore_settings">Restaurer aussi les réglages de l’app</string>
    <string name="backup_privacy_note">Le fichier n’est pas chiffré et contient les pages que vous suivez. Les connexions ne sont jamais incluses.</string>
    <string name="backup_working">En cours…</string>
    <string name="backup_saved">Sauvegarde enregistrée. Pages : %1$d · Versions : %2$d</string>
    <string name="backup_restored">Pages ajoutées : %1$d · Déjà présentes : %2$d · Versions : %3$d</string>
    <string name="backup_failed">Impossible d’enregistrer la sauvegarde. Vérifiez l’espace libre et réessayez.</string>
    <string name="backup_invalid">Ce fichier n’est pas une sauvegarde de Change Detection.</string>
    <string name="backup_too_new">Cette sauvegarde a été créée avec une version plus récente. Mettez l’app à jour et réessayez.</string>
```

**Russian (`values-ru`)**

```xml
    <string name="backup_title">Резервная копия</string>
    <string name="backup_supporting">Сохраните страницы в файл или восстановите их</string>
    <string name="backup_save_pages">Сохранить страницы и настройки</string>
    <string name="backup_save_pages_body">Небольшой файл. Сохранённые версии не включаются.</string>
    <string name="backup_save_everything">Сохранить всё</string>
    <string name="backup_save_everything_body">Включает все сохранённые версии. Файл может быть большим.</string>
    <string name="backup_restore">Восстановить из файла</string>
    <string name="backup_restore_body">Добавляет страницы, которых здесь ещё нет. На этом устройстве ничего не меняется.</string>
    <string name="backup_restore_settings">Также восстановить настройки приложения</string>
    <string name="backup_privacy_note">Файл не зашифрован и содержит страницы, за которыми вы следите. Данные входа никогда не включаются.</string>
    <string name="backup_working">Выполняется…</string>
    <string name="backup_saved">Копия сохранена. Страницы: %1$d · Версии: %2$d</string>
    <string name="backup_restored">Добавлено страниц: %1$d · Уже были: %2$d · Версии: %3$d</string>
    <string name="backup_failed">Не удалось сохранить копию. Проверьте свободное место и повторите попытку.</string>
    <string name="backup_invalid">Этот файл не является резервной копией Change Detection.</string>
    <string name="backup_too_new">Эта копия создана в более новой версии. Обновите приложение и повторите попытку.</string>
```

**Indonesian (`values-id`)**

```xml
    <string name="backup_title">Cadangan</string>
    <string name="backup_supporting">Simpan halaman Anda ke file, atau pulihkan</string>
    <string name="backup_save_pages">Simpan halaman dan setelan</string>
    <string name="backup_save_pages_body">File kecil. Versi tersimpan tidak disertakan.</string>
    <string name="backup_save_everything">Simpan semuanya</string>
    <string name="backup_save_everything_body">Menyertakan semua versi tersimpan. File bisa berukuran besar.</string>
    <string name="backup_restore">Pulihkan dari file</string>
    <string name="backup_restore_body">Menambahkan halaman yang belum ada di sini. Tidak ada yang diubah di perangkat ini.</string>
    <string name="backup_restore_settings">Pulihkan juga setelan aplikasi</string>
    <string name="backup_privacy_note">File tidak dienkripsi dan memuat halaman yang Anda pantau. Data login tidak pernah disertakan.</string>
    <string name="backup_working">Sedang memproses…</string>
    <string name="backup_saved">Cadangan disimpan. Halaman: %1$d · Versi: %2$d</string>
    <string name="backup_restored">Halaman ditambahkan: %1$d · Sudah ada: %2$d · Versi: %3$d</string>
    <string name="backup_failed">Cadangan tidak dapat disimpan. Periksa ruang kosong lalu coba lagi.</string>
    <string name="backup_invalid">File ini bukan cadangan Change Detection.</string>
    <string name="backup_too_new">Cadangan ini dibuat oleh versi yang lebih baru. Perbarui aplikasi lalu coba lagi.</string>
```

**Chinese (`values-zh`)**

The wording avoids the usual word for "backup" on purpose: one of its characters is not in the bundled Chinese font (see "Before you start", point 4).

```xml
    <string name="backup_title">保存与恢复</string>
    <string name="backup_supporting">将页面保存到文件，或从文件恢复</string>
    <string name="backup_save_pages">保存页面和设置</string>
    <string name="backup_save_pages_body">文件较小，不包含已保存的版本。</string>
    <string name="backup_save_everything">保存全部</string>
    <string name="backup_save_everything_body">包含所有已保存的版本，文件可能较大。</string>
    <string name="backup_restore">从文件恢复</string>
    <string name="backup_restore_body">添加此设备上还没有的页面，不会更改已有内容。</string>
    <string name="backup_restore_settings">同时恢复应用设置</string>
    <string name="backup_privacy_note">文件未加密，其中包含你关注的页面。登录信息绝不会包含在内。</string>
    <string name="backup_working">正在处理…</string>
    <string name="backup_saved">文件已保存。页面：%1$d · 版本：%2$d</string>
    <string name="backup_restored">已添加页面：%1$d · 已存在：%2$d · 版本：%3$d</string>
    <string name="backup_failed">无法保存文件。请检查可用空间后重试。</string>
    <string name="backup_invalid">此文件不是由 Change Detection 保存的。</string>
    <string name="backup_too_new">此文件来自更新版本的应用。请更新应用后重试。</string>
```

**Arabic (`values-ar`)**

```xml
    <string name="backup_title">النسخ الاحتياطي</string>
    <string name="backup_supporting">احفظ صفحاتك في ملف أو استعدها</string>
    <string name="backup_save_pages">حفظ الصفحات والإعدادات</string>
    <string name="backup_save_pages_body">ملف صغير. لا يتضمن النسخ المحفوظة.</string>
    <string name="backup_save_everything">حفظ كل شيء</string>
    <string name="backup_save_everything_body">يتضمن كل النسخ المحفوظة. قد يكون الملف كبيرًا.</string>
    <string name="backup_restore">الاستعادة من ملف</string>
    <string name="backup_restore_body">يضيف الصفحات غير الموجودة هنا. لا يتغير أي شيء على هذا الجهاز.</string>
    <string name="backup_restore_settings">استعادة إعدادات التطبيق أيضًا</string>
    <string name="backup_privacy_note">الملف غير مشفّر ويحتوي على الصفحات التي تتابعها. لا تُضمَّن بيانات تسجيل الدخول أبدًا.</string>
    <string name="backup_working">جارٍ التنفيذ…</string>
    <string name="backup_saved">تم حفظ النسخة الاحتياطية. الصفحات: %1$d · النسخ: %2$d</string>
    <string name="backup_restored">الصفحات المضافة: %1$d · الموجودة مسبقًا: %2$d · النسخ: %3$d</string>
    <string name="backup_failed">تعذّر حفظ النسخة الاحتياطية. تحقق من المساحة المتاحة ثم حاول مرة أخرى.</string>
    <string name="backup_invalid">هذا الملف ليس نسخة احتياطية من Change Detection.</string>
    <string name="backup_too_new">أُنشئت هذه النسخة بإصدار أحدث. حدّث التطبيق ثم حاول مرة أخرى.</string>
```

**Hindi (`values-hi`)**

```xml
    <string name="backup_title">बैकअप</string>
    <string name="backup_supporting">अपने पेज किसी फ़ाइल में सेव करें या उन्हें वापस लाएँ</string>
    <string name="backup_save_pages">पेज और सेटिंग सेव करें</string>
    <string name="backup_save_pages_body">छोटी फ़ाइल। सेव किए गए वर्शन शामिल नहीं हैं।</string>
    <string name="backup_save_everything">सब कुछ सेव करें</string>
    <string name="backup_save_everything_body">सेव किए गए सभी वर्शन शामिल हैं। फ़ाइल बड़ी हो सकती है।</string>
    <string name="backup_restore">फ़ाइल से वापस लाएँ</string>
    <string name="backup_restore_body">वे पेज जोड़ता है जो यहाँ अभी नहीं हैं। इस डिवाइस पर कुछ भी नहीं बदलता।</string>
    <string name="backup_restore_settings">ऐप की सेटिंग भी वापस लाएँ</string>
    <string name="backup_privacy_note">फ़ाइल एन्क्रिप्ट नहीं है और इसमें वे पेज हैं जिन पर आप नज़र रखते हैं। साइन-इन कभी शामिल नहीं होते।</string>
    <string name="backup_working">काम जारी है…</string>
    <string name="backup_saved">बैकअप सेव हो गया। पेज: %1$d · वर्शन: %2$d</string>
    <string name="backup_restored">जोड़े गए पेज: %1$d · पहले से मौजूद: %2$d · वर्शन: %3$d</string>
    <string name="backup_failed">बैकअप सेव नहीं हो सका। खाली जगह जाँचें और फिर कोशिश करें।</string>
    <string name="backup_invalid">यह फ़ाइल Change Detection का बैकअप नहीं है।</string>
    <string name="backup_too_new">यह बैकअप ऐप के नए वर्शन से बना है। ऐप अपडेट करें और फिर कोशिश करें।</string>
```

**Bengali (`values-bn`)**

```xml
    <string name="backup_title">ব্যাকআপ</string>
    <string name="backup_supporting">আপনার পেজগুলো একটি ফাইলে সেভ করুন বা ফিরিয়ে আনুন</string>
    <string name="backup_save_pages">পেজ ও সেটিংস সেভ করুন</string>
    <string name="backup_save_pages_body">ছোট একটি ফাইল। সেভ করা সংস্করণ এতে থাকে না।</string>
    <string name="backup_save_everything">সবকিছু সেভ করুন</string>
    <string name="backup_save_everything_body">সেভ করা সব সংস্করণ থাকে। ফাইলটি বড় হতে পারে।</string>
    <string name="backup_restore">ফাইল থেকে ফিরিয়ে আনুন</string>
    <string name="backup_restore_body">যে পেজগুলো এখানে নেই সেগুলো যোগ করে। এই ডিভাইসের কিছুই বদলায় না।</string>
    <string name="backup_restore_settings">অ্যাপের সেটিংসও ফিরিয়ে আনুন</string>
    <string name="backup_privacy_note">ফাইলটি এনক্রিপ্ট করা নয় এবং এতে আপনার নজরে রাখা পেজগুলো থাকে। সাইন-ইন কখনো এতে থাকে না।</string>
    <string name="backup_working">কাজ চলছে…</string>
    <string name="backup_saved">ব্যাকআপ সেভ হয়েছে। পেজ: %1$d · সংস্করণ: %2$d</string>
    <string name="backup_restored">যোগ হওয়া পেজ: %1$d · আগে থেকেই ছিল: %2$d · সংস্করণ: %3$d</string>
    <string name="backup_failed">ব্যাকআপ সেভ করা যায়নি। খালি জায়গা দেখে আবার চেষ্টা করুন।</string>
    <string name="backup_invalid">এই ফাইলটি Change Detection-এর ব্যাকআপ নয়।</string>
    <string name="backup_too_new">এই ব্যাকআপটি অ্যাপের নতুন সংস্করণে তৈরি। অ্যাপ আপডেট করে আবার চেষ্টা করুন।</string>
```

Before pasting into a locale, look at how that file already says "page", "saved version" and "settings" (`rg -n "name=\"(settings_title|home_pages|saved_versions)" shared/src/commonMain/composeResources/values-XX/`) and prefer the words the file already uses over the drafts above.

**Verify:** `./gradlew :shared:desktopTest --tests '*StringResourcesTest'`
Expected: `BUILD SUCCESSFUL`.

### Step 3: The file work

Create `shared/src/androidMain/kotlin/com/bernaferrari/changedetection/backup/BackupFiles.kt`. It owns everything that touches the picked file, away from the screen so it keeps running if the person leaves Settings.

```kotlin
package com.bernaferrari.changedetection.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.bernaferrari.changedetection.background.MonitoringManager
import com.bernaferrari.changedetection.util.ApplicationScope
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal sealed interface BackupProgress {
    data object Idle : BackupProgress
    data object Working : BackupProgress
    data class Saved(val summary: BackupSummary) : BackupProgress
    data class Restored(val summary: ImportSummary) : BackupProgress
    data object SaveFailed : BackupProgress
    data object NotABackup : BackupProgress
    data object TooNew : BackupProgress
}

/** One backup or restore at a time, kept alive by the application so leaving Settings does not cut a file short. */
internal class BackupFiles(
    private val context: Context,
    private val service: BackupService,
    private val monitoring: MonitoringManager?,
    private val application: ApplicationScope,
) {
    private val progress = MutableStateFlow<BackupProgress>(BackupProgress.Idle)
    val state: StateFlow<BackupProgress> = progress

    private val appVersion: String
        get() = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()

    fun acknowledge() {
        if (progress.value != BackupProgress.Working) progress.value = BackupProgress.Idle
    }

    fun savePages(target: Uri) = save(target) { output ->
        val bytes = service.exportPages(appVersion)
        output.write(bytes)
        BackupSummary(pages = backupJson.decodeFromString<BackupManifest>(bytes.decodeToString()).sites.size, versions = 0)
    }

    fun saveEverything(target: Uri) = save(target) { output ->
        ZipBackupSink(output).use { service.exportEverything(it, appVersion) }
    }

    private fun save(target: Uri, write: suspend (java.io.OutputStream) -> BackupSummary) = run(
        failure = BackupProgress.SaveFailed,
        // A half-written file looks like a backup and is not one; remove it.
        onFailure = { runCatching { DocumentsContract.deleteDocument(context.contentResolver, target) } },
    ) {
        val output = context.contentResolver.openOutputStream(target, "wt") ?: throw IOException("Cannot open the chosen file")
        BackupProgress.Saved(output.use { write(it) })
    }

    fun restore(from: Uri, restoreSettings: Boolean) = run(failure = BackupProgress.NotABackup) {
        val input = context.contentResolver.openInputStream(from) ?: throw IOException("Cannot open the chosen file")
        val summary = input.use { service.import(openBackup(it), restoreSettings) }
        // New pages and restored schedule settings both change what the background checks must do.
        monitoring?.updateSchedule()
        BackupProgress.Restored(summary)
    }

    private fun run(failure: BackupProgress, onFailure: () -> Unit = {}, work: suspend () -> BackupProgress) {
        if (!progress.compareAndSet(BackupProgress.Idle, BackupProgress.Working)) return
        application.scope.launch {
            progress.value = try {
                withContext(Dispatchers.IO) { work() }
            } catch (cancelled: CancellationException) {
                progress.value = BackupProgress.Idle
                throw cancelled
            } catch (tooNew: BackupTooNewException) {
                BackupProgress.TooNew
            } catch (invalid: InvalidBackupException) {
                BackupProgress.NotABackup
            } catch (error: Exception) {
                onFailure()
                failure
            }
        }
    }

    companion object {
        @Volatile private var instance: BackupFiles? = null

        /** Null only when the app's services are not running, which cannot happen on a visible Settings screen. */
        fun get(context: Context, service: BackupService?, monitoring: MonitoringManager?, application: ApplicationScope?): BackupFiles? {
            instance?.let { return it }
            if (service == null || application == null) return null
            return synchronized(this) { instance ?: BackupFiles(context.applicationContext, service, monitoring, application).also { instance = it } }
        }
    }
}
```

Notes:

- `progress.compareAndSet(Idle, Working)` means a result must be acknowledged (dialog dismissed) before another action starts; the screen in Step 4 does that.
- `"wt"` truncates an existing file the person chose to overwrite.
- If `ZipBackupSink(output).use { ... }` does not compile because the lambda is not `suspend`-friendly (`use` is inline, so it should be), write `val sink = ZipBackupSink(output); try { service.exportEverything(sink, appVersion) } finally { sink.close() }`.

**Verify:** `./gradlew :shared:testAndroidHostTest` is `BUILD SUCCESSFUL` (compiles).

### Step 4: The row and its dialog

Create `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/backup/BackupRow.kt`:

```kotlin
package com.bernaferrari.changedetection.backup

import androidx.compose.runtime.Composable

/** Settings entry for saving and restoring a backup; platforms without a file picker render nothing. */
@Composable expect fun BackupRow()
```

Create the three one-line actuals (same package, file names from "In scope"):

```kotlin
package com.bernaferrari.changedetection.backup

import androidx.compose.runtime.Composable

@Composable actual fun BackupRow() = Unit
```

Create `shared/src/androidMain/kotlin/com/bernaferrari/changedetection/backup/BackupRow.android.kt`:

```kotlin
package com.bernaferrari.changedetection.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.background.MonitoringManager
import com.bernaferrari.changedetection.localization.stringResource
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.kit.GroupRow
import com.bernaferrari.changedetection.ui.kit.optional
import com.bernaferrari.changedetection.util.ApplicationScope
import java.time.LocalDate

@Composable
actual fun BackupRow() {
    val context = LocalContext.current
    val files = remember { BackupFiles.get(context, optional<BackupService>(), optional<MonitoringManager>(), optional<ApplicationScope>()) } ?: return
    var show by rememberSaveable { mutableStateOf(false) }
    GroupRow(
        stringResource(Res.string.backup_title),
        modifier = Modifier.testTag("backup"),
        supporting = stringResource(Res.string.backup_supporting),
        icon = Icons.Outlined.SaveAlt,
        showDivider = true,
        onClick = { show = true },
    )
    if (show) BackupDialog(files, onClose = { show = false })
}

@Composable
private fun BackupDialog(files: BackupFiles, onClose: () -> Unit) {
    val progress by files.state.collectAsState()
    var restoreSettings by rememberSaveable { mutableStateOf(true) }
    val name = "change-detection-backup-${LocalDate.now()}"
    val savePages = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { it?.let(files::savePages) }
    val saveEverything = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { it?.let(files::saveEverything) }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { files.restore(it, restoreSettings) } }

    val result = when (val state = progress) {
        is BackupProgress.Saved -> stringResource(Res.string.backup_saved, state.summary.pages, state.summary.versions)
        is BackupProgress.Restored -> stringResource(Res.string.backup_restored, state.summary.pagesAdded, state.summary.pagesAlreadyHere, state.summary.versionsAdded)
        BackupProgress.SaveFailed -> stringResource(Res.string.backup_failed)
        BackupProgress.NotABackup -> stringResource(Res.string.backup_invalid)
        BackupProgress.TooNew -> stringResource(Res.string.backup_too_new)
        BackupProgress.Idle, BackupProgress.Working -> null
    }
    val working = progress == BackupProgress.Working
    val close = {
        files.acknowledge()
        onClose()
    }
    AlertDialog(
        onDismissRequest = close,
        title = { Text(stringResource(Res.string.backup_title)) },
        text = {
            Column {
                when {
                    working -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        Text(stringResource(Res.string.backup_working), Modifier.padding(start = 16.dp))
                    }
                    result != null -> Text(result, Modifier.testTag("backup-result"))
                    else -> {
                        GroupRow(stringResource(Res.string.backup_save_pages), modifier = Modifier.testTag("backup-save-pages"), supporting = stringResource(Res.string.backup_save_pages_body), onClick = { savePages.launch("$name.json") })
                        GroupRow(stringResource(Res.string.backup_save_everything), modifier = Modifier.testTag("backup-save-everything"), supporting = stringResource(Res.string.backup_save_everything_body), onClick = { saveEverything.launch("$name.zip") })
                        GroupRow(stringResource(Res.string.backup_restore), modifier = Modifier.testTag("backup-restore"), supporting = stringResource(Res.string.backup_restore_body), onClick = { restore.launch(arrayOf("application/json", "application/zip", "application/octet-stream")) })
                        Row(
                            Modifier.fillMaxWidth().toggleable(restoreSettings, role = Role.Checkbox) { restoreSettings = it }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = restoreSettings, onCheckedChange = null)
                            Text(stringResource(Res.string.backup_restore_settings), Modifier.padding(start = 12.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(Res.string.backup_privacy_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = close) { Text(stringResource(Res.string.done)) } },
    )
}
```

Things to check while writing this file:

- The button reuses the existing string `done` ("Done", `values/strings.xml:76`). Do not add a new string for it.
- `optional` is declared `internal` in `ui/kit/ChangePreviewViews.kt` and is visible here because this is the same module.
- `GroupRow` draws its own tile background (it is designed for grouped lists). If the three rows look wrong inside the dialog on the device (double padding, mismatched background), replace each with a `TextButton` holding a two-line `Column` of title and supporting text. Looks are judged in Step 6.
- While `working` is true, dismissing the dialog is allowed; the work continues and the result shows the next time the dialog opens.

In `SettingsScreen.kt`, add the row as the last entry of the "app" group, directly after the `SavedVersionsRow(...)` line:

```kotlin
                        com.bernaferrari.changedetection.backup.BackupRow()
```

(The file already calls platform rows by fully-qualified name, see `com.bernaferrari.changedetection.diagnostics.DiagnosticsRow()` a few lines below.)

**Verify:** `./gradlew :shared:spotlessApply` then `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest`
Expected: `BUILD SUCCESSFUL`. The desktop Settings tests must still pass unchanged (the row renders nothing on desktop).

**Verify:** `./gradlew :webDemo:wasmJsBrowserDistribution`, on macOS `./gradlew :shared:compileKotlinIosSimulatorArm64`, and `./gradlew :androidApp:lintDebug :androidApp:assembleRelease`
Expected: `BUILD SUCCESSFUL` for each.

### Step 5: Say it in the privacy policy and the backup rules

In `PRIVACY.md`, add this section directly before `Changes to This Privacy Policy` (and update the "Last Updated" line at the top to today's date):

```markdown
Backup Files You Create
In Settings you can save a backup file and restore it later. The file is created only when you ask, and it is saved only
where you choose. "Pages and settings" holds the addresses you watch, their options and notes, and the app's settings.
"Everything" also holds the saved versions of those pages. Neither file holds cookies or sign-ins. The file is not
encrypted: anyone who has it can read it, so keep it somewhere you trust. Change Detection never uploads it anywhere.
```

In `shared/src/androidMain/res/xml/data_extraction_rules.xml`, change the end of the comment from `without explicit in-app export.` to `without explicit in-app export (Settings, Backup).` Do not change any rule.

**Verify:** `rg -n "Backup Files You Create" PRIVACY.md` prints one line; `git diff shared/src/androidMain/res/xml/data_extraction_rules.xml` shows a comment-only change.

### Step 6: Check it on a device or emulator

File pickers cannot be driven by this repository's unit tests, so this step is a manual check. Use an emulator or phone running Android 8 or later.

1. `./gradlew :androidApp:installDebug`, open the app, add two pages (any public sites), wait until each has a first version, and trigger one more check so at least one page has two versions.
2. Settings → App → **Backup** → **Save pages and settings**. Save to Downloads. Expected: "Backup saved. Pages: 2 · Versions: 0". Open the file in a text viewer: it is readable JSON with both addresses and no page content.
3. **Save everything**. Expected: "Backup saved. Pages: 2 · Versions: N" with N ≥ 2. The file is a `.zip` whose first entry is `manifest.json`.
4. **Restore from a file**, pick the `.zip`. Expected: "Pages added: 0 · Already here: 2 · Versions: 0". The pages and their history are unchanged.
5. Delete one page in the app. Restore the `.zip` again. Expected: "Pages added: 1 · Already here: 1 · Versions: ≥ 1". The page is back with its history.
6. Clear the app's data (system settings → Apps → Change Detection → Storage → Clear storage). Open the app, change nothing, restore the `.json`. Expected: both pages appear, each showing that it is waiting for its first check; the theme and schedule from before are back.
7. Restore a file that is not a backup (any photo or text file). Expected: "This file is not a Change Detection backup." and nothing changes.
8. Rotate the device while the dialog is open, before and after an action. Expected: the dialog and its message survive.

If you cannot run a device or emulator, say so plainly in your report and leave the plan's status as `IN PROGRESS` with the note "device check not run". Do not mark it `DONE`.

## Test plan

| Check | How |
| --- | --- |
| A picked file is recognised as zip or JSON, and a huge non-backup is not loaded whole | `ZipBackupTest` (three new tests) |
| Every locale has every new string | `StringResourcesTest` |
| The row does not appear or break on desktop, iOS, web | Existing desktop Settings tests, the iOS compile, the web demo build |
| Save, restore, wrong file, rotation | Step 6, by hand |

## Done criteria

- [ ] `./gradlew :shared:testAndroidHostTest --tests '*ZipBackupTest'` passes with eight tests.
- [ ] `rg -c 'name="backup_' shared/src/commonMain/composeResources/values*/*.xml` shows 16 for exactly one file in each of the twelve locale folders.
- [ ] `rg -n "BackupRow\(\)" shared/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/settings/SettingsScreen.kt` prints one line.
- [ ] `rg -n "READ_EXTERNAL_STORAGE|WRITE_EXTERNAL_STORAGE|MANAGE_EXTERNAL_STORAGE" androidApp shared/src/androidMain` prints nothing.
- [ ] The full gate, the web demo build and the Android lint and release build are `BUILD SUCCESSFUL`.
- [ ] Step 6 was carried out and each expectation held, or the report says exactly which did not.
- [ ] `git status --short` lists only in-scope files.

## STOP conditions

- Plan 015 is not in place.
- The build asks for a storage permission, or lint reports one as missing.
- `StringResourcesTest` fails for a reason other than a missing or duplicated `backup_*` string.
- In Step 6, a restore changes or removes a page that was already on the device. That is a bug in the engine (Plan 015); report it, do not work around it here.

## Maintenance notes

- **Wording is part of the safety.** "Nothing on this device is changed" and "The file is not encrypted" are promises the engine keeps; change the text only together with the behaviour.
- **Desktop and iOS** can get the same screen by adding a file picker and a zip reader for that platform; the engine is common code.
- **Encryption** is the most likely follow-up request. It should be an option on "Save everything", with a passphrase the person types, and it needs a format version bump (see Plan 015's maintenance notes).
- Another plan (013, the privacy policy update) looks for a string named `backup_title` to decide whether this feature has landed. Keep that name.
- The translations in Step 2 were drafted without native review.
