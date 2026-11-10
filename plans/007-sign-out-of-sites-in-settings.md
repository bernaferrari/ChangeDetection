# Plan 007: Let people delete the in-app browser's saved sign-ins from Settings

> **Executor instructions**
> You are implementing one self-contained change. You have not seen the audit that produced this plan; everything you need is in this file.
> 1. Run the drift check first: `git diff --stat a9f8f8ba..HEAD -- shared/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/settings/ shared/src/androidMain/kotlin/com/bernaferrari/changedetection/browser/AndroidWebViewTools.kt shared/src/commonMain/composeResources/`. If it prints anything, compare the "Current state" excerpts with the real files. Changed string files are fine as long as none of the six new string names below already exists. If code excerpts no longer match, STOP and report.
> 2. Follow the steps in order and run each **Verify** command.
> 3. Touch only the files listed under "In scope".
> 4. If a STOP condition applies, stop and report. Do not improvise.
> 5. When finished, set this plan's row in `plans/README.md` to `DONE`.

## Status

| Field | Value |
| --- | --- |
| Priority | P2 |
| Effort | M (3 to 4 hours, mostly strings) |
| Risk of the fix | Low. One new settings row, Android only |
| Depends on | Nothing |
| Category | Privacy feature |
| Planned at | commit `a9f8f8ba`, 2026-11-09 |

## Why this matters

The app has an in-app browser. Users sign in to websites there so that background checks can read pages behind a login. Every browser view in the app shares one cookie store, so one sign-in keeps working for all later checks.

There is no way to undo that. The app never clears cookies or site storage, and a user who wants to sign out everywhere has only one option: Android's "Clear storage" for the whole app, which also deletes every watched page and its history. For an app whose pitch is "your data stays on your device and under your control", a stored session with no off switch is a gap.

This plan adds one row to Settings on Android, "Saved sign-ins", that deletes the browser's cookies and site storage after a confirmation.

## Current state

### Where the cookies are kept

`shared/src/androidMain/kotlin/com/bernaferrari/changedetection/browser/AndroidWebViewTools.kt:37-45` (inside `configureChangeDetectionWebView`):

```kotlin
    // Every WebView in the app shares one cookie jar, so signing in once in the browser keeps checks signed in.
    CookieManager.getInstance().setAcceptCookie(true)
    if (acceptThirdPartyCookies) {
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(this@configureChangeDetectionWebView, true)
        }
    }
}
```

This is the only file in the repo that mentions `CookieManager` (`rg -l "CookieManager|WebStorage" shared/src` prints only this file). `android.webkit.CookieManager` is already imported there.

### The Settings screen

`shared/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/settings/SettingsScreen.kt:163-187` is the "App" group:

```kotlin
                item(key = "app") {
                    GroupLabel(stringResource(Res.string.settings_group_app))
                    ListGroup {
                        if (onNotificationSettings != null) {
                            GroupRow(
                                title = stringResource(Res.string.settings_notifications),
                                // ...
                            )
                        }
                        if (supportsNotificationImages) {
                            // ...
                        }
                        SavedVersionsRow(uiState, viewModel::retryStorageLoading, viewModel::clearOlderVersions, showDivider = onNotificationSettings != null)
                    }
                }
```

### The pattern for an Android-only settings row

The codebase already has one row that exists only on Android. Copy its shape.

Common declaration, `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/diagnostics/DiagnosticsEntry.kt:8-9`:

```kotlin
/** The same report as a settings row, so it sits with Help instead of floating above the first section. */
@Composable expect fun DiagnosticsRow()
```

Android implementation, `shared/src/androidMain/kotlin/com/bernaferrari/changedetection/diagnostics/DiagnosticsEntry.android.kt:38-49`:

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

The other three platforms each have a one-line file, for example `shared/src/desktopMain/kotlin/com/bernaferrari/changedetection/diagnostics/DiagnosticsEntry.desktopMain.kt:7`:

```kotlin
@Composable actual fun DiagnosticsRow() = Unit
```

(the same line exists in `shared/src/iosMain/.../diagnostics/DiagnosticsEntry.iosMain.kt` and `shared/src/wasmJsMain/.../diagnostics/DiagnosticsEntry.wasmJsMain.kt`).

`GroupRow` is declared at `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/kit/Kit.kt:541`:

```kotlin
@Composable
fun GroupRow(
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    icon: ImageVector? = null,
    @Suppress("UNUSED_PARAMETER") showDivider: Boolean = false,
    // ... colours, enabled ...
    onClick: (() -> Unit)? = null,
    // ...
)
```

In Android source files, text comes from `com.bernaferrari.changedetection.localization.stringResource` with `com.bernaferrari.changedetection.shared.generated.resources.*` (see the imports at the top of `DiagnosticsEntry.android.kt`). Do not use `org.jetbrains.compose.resources.stringResource` directly.

### How strings work in this repo

- English strings for new UI go at the end of `shared/src/commonMain/composeResources/values/strings_v7.xml`, before `</resources>`.
- Spanish, Italian and Portuguese mirror that file: `values-es/strings_v7.xml`, `values-it/strings_v7.xml`, `values-pt/strings_v7.xml`. A test (`StringResourcesTest` in `shared/src/desktopTest`) **fails** if any name in the English `strings_v7.xml` is missing from one of these three folders, or if a name is defined twice within one folder.
- The other eight languages (`values-ar`, `values-bn`, `values-de`, `values-fr`, `values-hi`, `values-id`, `values-ru`, `values-zh`) keep everything in a single `strings.xml`. Add new strings at the end of that file, before `</resources>`.
- The string `cancel` ("Cancel") already exists in every language. Reuse it.
- Apostrophes in translated text use the typographic character `’`, not `'`.

### Test exemplar for device tests

`shared/src/androidInstrumentedTest/kotlin/com/bernaferrari/changedetection/webview/WebViewLifecycleTest.kt` (package `com.bernaferrari.changedetection.browser`):

```kotlin
@RunWith(AndroidJUnit4::class)
class WebViewLifecycleTest {
    @Test
    fun destroySafelyDestroysAConfiguredWebView() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        var cleanupCompleted = false

        instrumentation.runOnMainSync {
            WebView(instrumentation.targetContext).apply {
                configureChangeDetectionWebView()
                // ...
            }
            cleanupCompleted = true
        }

        assertTrue(cleanupCompleted)
    }
}
```

## Commands you will need

| Purpose | Command | Expected |
| --- | --- | --- |
| Host tests (compiles Android code) | `./gradlew :shared:testAndroidHostTest` | `BUILD SUCCESSFUL` |
| String rules | `./gradlew :shared:desktopTest --tests '*StringResourcesTest'` | `BUILD SUCCESSFUL` |
| Compile device tests | `./gradlew :shared:assembleAndroidDeviceTest` | `BUILD SUCCESSFUL` |
| Run one device test (needs an emulator or phone) | `./gradlew :shared:connectedAndroidDeviceTest -Pandroid.testInstrumentationRunnerArguments.class=com.bernaferrari.changedetection.browser.SiteSessionsTest` | `BUILD SUCCESSFUL` |
| Format check | `./gradlew :shared:spotlessCheck` | `BUILD SUCCESSFUL` |
| Auto-format | `./gradlew :shared:spotlessApply` | `BUILD SUCCESSFUL` |
| Full gate | `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest` | `BUILD SUCCESSFUL` |
| Other platforms compile | `./gradlew :webDemo:wasmJsBrowserDistribution` and (macOS) `./gradlew :shared:compileKotlinIosSimulatorArm64` | `BUILD SUCCESSFUL` |
| Android lint and release build | `./gradlew :androidApp:lintDebug :androidApp:assembleRelease` | `BUILD SUCCESSFUL` |

The build needs JDK 21 and Android SDK platform 37.2.

## Before you start: the state of the build

Checked on 2026-11-09 at commit `a9f8f8ba`. Read this before the steps; it overrides any line below that expects otherwise.

1. **Plan 000 must be done first.** Before you change anything, run `./gradlew :shared:spotlessCheck`. If it fails, Plan 000 (`plans/000-make-the-gates-green.md`) has not landed: STOP and report that. Do not run `spotlessApply` to get past it; on an unprepared tree it rewrites about 36 unrelated files. Once Plan 000 has landed, the test and format commands in this plan behave as described, and `spotlessApply` only touches what you changed.
2. **Two commands fail for reasons no plan fixes.** `./gradlew :shared:compileKotlinIosSimulatorArm64` stops with five errors, all in `shared/src/iosMain/kotlin/com/bernaferrari/changedetection/browser/IosPageCapture.kt` (the first is `Unresolved reference 'kmpwright'`; a library was built with a newer Kotlin than this project uses). `./gradlew :webDemo:wasmJsBrowserDistribution` compiles the Kotlin and then stops at `:kotlinWasmStoreYarnLock` with `Lock file was changed`. Wherever this plan expects one of them to print `BUILD SUCCESSFUL`, the bar is instead: for iOS, **those same five errors and no others**; for the web demo, run `./gradlew :webDemo:compileKotlinWasmJs` and expect `BUILD SUCCESSFUL`. Never run `kotlinWasmUpgradeYarnLock`. If either command has since been repaired and passes, that is fine.
3. **Line numbers may have moved.** Two changes landed after this plan was written. Plan 000 re-formats about 36 files (layout only). The "failure experience" change (`plans/failure-experience-changes.md`) added code to `SyncEngine.kt`, `SitesDao.kt`, `Site.kt`, `CheckFailure.kt`, `DiagnosticTrace.kt`, `PageDetailScreen.kt`, `MainViewModel.kt`, `LegacyMigration.kt`, `DatabaseFactory.kt`, `AppContent.kt`, `AndroidChangeNotifier.kt`, the four `PlatformDiModule` files and the string files. Find code by the text this plan quotes, not by line number. A quoted block that differs only in line breaks or indentation is the same code. The drift check at the top of this plan will list those files as changed; that alone is not a reason to stop. Stop only if the quoted code is gone or does something different.
4. **Chinese, Hindi and Bengali text may only use characters the bundled fonts contain.** The app ships cut-down fonts for those three languages (`shared/src/commonMain/composeResources/font/locale_{zh,hi,bn}_*.ttf`), and `LocalizationTest` ("bundled script fonts cover every translated UI glyph") fails on any character they lack. The translations in this plan were checked against those fonts on 2026-11-09 and fit. If you alter a `values-zh`, `values-hi` or `values-bn` string, run `./gradlew :shared:desktopTest --tests '*LocalizationTest'` straight away; if it reports a missing character, choose other words. Do not edit the font files.

## Scope

**In scope:**

- `shared/src/androidMain/kotlin/com/bernaferrari/changedetection/browser/AndroidWebViewTools.kt` (add one function)
- New: `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/settings/SiteSessionsRow.kt`
- New: `shared/src/androidMain/kotlin/com/bernaferrari/changedetection/ui/settings/SiteSessionsRow.android.kt`
- New: `shared/src/desktopMain/kotlin/com/bernaferrari/changedetection/ui/settings/SiteSessionsRow.desktopMain.kt`
- New: `shared/src/iosMain/kotlin/com/bernaferrari/changedetection/ui/settings/SiteSessionsRow.iosMain.kt`
- New: `shared/src/wasmJsMain/kotlin/com/bernaferrari/changedetection/ui/settings/SiteSessionsRow.wasmJsMain.kt`
- `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/settings/SettingsScreen.kt` (one added line)
- New: `shared/src/androidInstrumentedTest/kotlin/com/bernaferrari/changedetection/browser/SiteSessionsTest.kt`
- String files: `values/strings_v7.xml`, `values-es/strings_v7.xml`, `values-it/strings_v7.xml`, `values-pt/strings_v7.xml`, and `strings.xml` in `values-ar`, `values-bn`, `values-de`, `values-fr`, `values-hi`, `values-id`, `values-ru`, `values-zh` (all under `shared/src/commonMain/composeResources/`)
- `plans/README.md` (status column only)

**Out of scope:**

- `acceptThirdPartyCookies` and every other WebView setting. Do not change how cookies are accepted.
- Clearing the HTTP cache, form data or history.
- A per-site list of sign-ins. This plan is "all or nothing" on purpose.
- iOS and desktop implementations (those builds are for development; their rows stay empty).
- `PRIVACY.md` (Plan 013 owns it).

## Git workflow

- Branch from `main`: `git switch -c advisor/007-sign-out-of-sites-in-settings`
- Commit subjects are plain sentences. Suggested: `Settings can sign out of every site the in-app browser remembers`.
- Do not push, merge, or open a pull request.

## Steps

### Step 1: Add the function that clears the browser's saved sessions

In `AndroidWebViewTools.kt`, add `import android.webkit.WebStorage` and `import android.webkit.WebViewDatabase` to the imports (keep them in alphabetical order with the existing `android.webkit.*` imports), then add this function directly after `configureChangeDetectionWebView`:

```kotlin
/**
 * Forgets every sign-in the in-app browser holds: cookies, site storage and saved HTTP passwords.
 * Call on the main thread. [onCleared] runs on the main thread once cookies are gone and written to disk.
 */
internal fun clearSiteSessions(context: android.content.Context, onCleared: () -> Unit = {}) {
    WebStorage.getInstance().deleteAllData()
    WebViewDatabase.getInstance(context).clearHttpAuthUsernamePassword()
    val cookies = CookieManager.getInstance()
    cookies.removeAllCookies {
        cookies.flush()
        onCleared()
    }
}
```

**Verify:** `./gradlew :shared:testAndroidHostTest`
Expected: `BUILD SUCCESSFUL`.

### Step 2: Write the device test

Create `shared/src/androidInstrumentedTest/kotlin/com/bernaferrari/changedetection/browser/SiteSessionsTest.kt`:

```kotlin
package com.bernaferrari.changedetection.browser

import android.webkit.CookieManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Signing out in Settings must leave no cookie behind for any site. */
@RunWith(AndroidJUnit4::class)
class SiteSessionsTest {
    @Test
    fun clearingSiteSessionsRemovesEveryCookie() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val stored = CountDownLatch(1)
        instrumentation.runOnMainSync {
            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setCookie("https://one.example.test", "session=first; Max-Age=3600")
                setCookie("https://two.example.test", "session=second; Max-Age=3600") { stored.countDown() }
            }
        }
        assertTrue(stored.await(10, TimeUnit.SECONDS))
        assertNotNull(CookieManager.getInstance().getCookie("https://one.example.test"))
        assertNotNull(CookieManager.getInstance().getCookie("https://two.example.test"))

        val cleared = CountDownLatch(1)
        instrumentation.runOnMainSync { clearSiteSessions(instrumentation.targetContext) { cleared.countDown() } }

        assertTrue("the completion callback must run", cleared.await(10, TimeUnit.SECONDS))
        assertNull(CookieManager.getInstance().getCookie("https://one.example.test"))
        assertNull(CookieManager.getInstance().getCookie("https://two.example.test"))
    }
}
```

**Verify:** `./gradlew :shared:assembleAndroidDeviceTest`
Expected: `BUILD SUCCESSFUL`.

If `adb devices` lists a device or emulator, also run the single test with the command from the table. Expected: one test, passed. If no device is available, say so in your report; do not skip the compile check.

### Step 3: Add the strings

Add these six lines at the end of `shared/src/commonMain/composeResources/values/strings_v7.xml`, before `</resources>`:

```xml
    <string name="site_sessions_title">Saved sign-ins</string>
    <string name="site_sessions_body">Cookies and site data kept by the in-app browser</string>
    <string name="site_sessions_clear_title">Sign out of all sites?</string>
    <string name="site_sessions_clear_body">Cookies and site data saved by the in-app browser will be deleted from this device. Pages that need a sign-in will stop loading correctly until you sign in again.</string>
    <string name="site_sessions_clear_confirm">Sign out</string>
    <string name="site_sessions_cleared">Signed out of all sites</string>
```

First confirm none of the names exists: `rg -n 'name="site_sessions_' shared/src/commonMain/composeResources` must print nothing.

Then add the translations below at the end of each file, before `</resources>`. These translations were drafted for this plan and have **not** been reviewed by native speakers; say so in your final report so the maintainer can have them checked.

`values-es/strings_v7.xml`:

```xml
    <string name="site_sessions_title">Inicios de sesión guardados</string>
    <string name="site_sessions_body">Cookies y datos de sitios que guarda el navegador de la app</string>
    <string name="site_sessions_clear_title">¿Cerrar sesión en todos los sitios?</string>
    <string name="site_sessions_clear_body">Se eliminarán de este dispositivo las cookies y los datos de sitios guardados por el navegador de la app. Las páginas que requieren iniciar sesión dejarán de cargarse correctamente hasta que vuelvas a iniciar sesión.</string>
    <string name="site_sessions_clear_confirm">Cerrar sesión</string>
    <string name="site_sessions_cleared">Sesión cerrada en todos los sitios</string>
```

`values-it/strings_v7.xml`:

```xml
    <string name="site_sessions_title">Accessi salvati</string>
    <string name="site_sessions_body">Cookie e dati dei siti conservati dal browser dell’app</string>
    <string name="site_sessions_clear_title">Uscire da tutti i siti?</string>
    <string name="site_sessions_clear_body">I cookie e i dati dei siti salvati dal browser dell’app verranno eliminati da questo dispositivo. Le pagine che richiedono l’accesso non verranno caricate correttamente finché non accedi di nuovo.</string>
    <string name="site_sessions_clear_confirm">Esci</string>
    <string name="site_sessions_cleared">Uscita effettuata da tutti i siti</string>
```

`values-pt/strings_v7.xml`:

```xml
    <string name="site_sessions_title">Logins salvos</string>
    <string name="site_sessions_body">Cookies e dados de sites guardados pelo navegador do app</string>
    <string name="site_sessions_clear_title">Sair de todos os sites?</string>
    <string name="site_sessions_clear_body">Os cookies e os dados de sites salvos pelo navegador do app serão apagados deste dispositivo. As páginas que exigem login deixarão de carregar corretamente até você entrar de novo.</string>
    <string name="site_sessions_clear_confirm">Sair</string>
    <string name="site_sessions_cleared">Você saiu de todos os sites</string>
```

`values-de/strings.xml`:

```xml
    <string name="site_sessions_title">Gespeicherte Anmeldungen</string>
    <string name="site_sessions_body">Cookies und Websitedaten des In-App-Browsers</string>
    <string name="site_sessions_clear_title">Von allen Websites abmelden?</string>
    <string name="site_sessions_clear_body">Cookies und Websitedaten, die der In-App-Browser gespeichert hat, werden von diesem Gerät gelöscht. Seiten, die eine Anmeldung erfordern, laden erst wieder richtig, wenn du dich erneut anmeldest.</string>
    <string name="site_sessions_clear_confirm">Abmelden</string>
    <string name="site_sessions_cleared">Von allen Websites abgemeldet</string>
```

`values-fr/strings.xml`:

```xml
    <string name="site_sessions_title">Connexions enregistrées</string>
    <string name="site_sessions_body">Cookies et données de sites conservés par le navigateur intégré</string>
    <string name="site_sessions_clear_title">Se déconnecter de tous les sites ?</string>
    <string name="site_sessions_clear_body">Les cookies et les données de sites enregistrés par le navigateur intégré seront supprimés de cet appareil. Les pages qui nécessitent une connexion ne se chargeront plus correctement tant que vous ne vous serez pas reconnecté.</string>
    <string name="site_sessions_clear_confirm">Se déconnecter</string>
    <string name="site_sessions_cleared">Déconnecté de tous les sites</string>
```

`values-ru/strings.xml`:

```xml
    <string name="site_sessions_title">Сохранённые входы</string>
    <string name="site_sessions_body">Файлы cookie и данные сайтов из встроенного браузера</string>
    <string name="site_sessions_clear_title">Выйти со всех сайтов?</string>
    <string name="site_sessions_clear_body">Файлы cookie и данные сайтов, сохранённые встроенным браузером, будут удалены с этого устройства. Страницы, требующие входа, перестанут загружаться правильно, пока вы не войдёте снова.</string>
    <string name="site_sessions_clear_confirm">Выйти</string>
    <string name="site_sessions_cleared">Выполнен выход со всех сайтов</string>
```

`values-id/strings.xml`:

```xml
    <string name="site_sessions_title">Info masuk tersimpan</string>
    <string name="site_sessions_body">Cookie dan data situs yang disimpan browser dalam aplikasi</string>
    <string name="site_sessions_clear_title">Keluar dari semua situs?</string>
    <string name="site_sessions_clear_body">Cookie dan data situs yang disimpan browser dalam aplikasi akan dihapus dari perangkat ini. Halaman yang memerlukan login tidak akan dimuat dengan benar sampai Anda masuk lagi.</string>
    <string name="site_sessions_clear_confirm">Keluar</string>
    <string name="site_sessions_cleared">Sudah keluar dari semua situs</string>
```

`values-zh/strings.xml`:

```xml
    <string name="site_sessions_title">已保存的登录状态</string>
    <string name="site_sessions_body">应用内浏览器保存的 Cookie 和网站数据</string>
    <string name="site_sessions_clear_title">清除所有网站的登录状态？</string>
    <string name="site_sessions_clear_body">应用内浏览器保存的 Cookie 和网站数据将从此设备中删除。需要登录的页面在你重新登录前将无法正常加载。</string>
    <string name="site_sessions_clear_confirm">清除登录状态</string>
    <string name="site_sessions_cleared">已清除所有网站的登录状态</string>
```

`values-hi/strings.xml`:

```xml
    <string name="site_sessions_title">सहेजे गए साइन-इन</string>
    <string name="site_sessions_body">इन-ऐप ब्राउज़र में सहेजी गई कुकी और साइट डेटा</string>
    <string name="site_sessions_clear_title">सभी साइटों से साइन आउट करें?</string>
    <string name="site_sessions_clear_body">इन-ऐप ब्राउज़र में सहेजी गई कुकी और साइट डेटा इस डिवाइस से हटा दिए जाएँगे। जिन पेजों के लिए साइन-इन ज़रूरी है, वे दोबारा साइन इन करने तक ठीक से लोड नहीं होंगे।</string>
    <string name="site_sessions_clear_confirm">साइन आउट करें</string>
    <string name="site_sessions_cleared">सभी साइटों से साइन आउट हो गया</string>
```

`values-bn/strings.xml`:

```xml
    <string name="site_sessions_title">সংরক্ষিত সাইন-ইন</string>
    <string name="site_sessions_body">ইন-অ্যাপ ব্রাউজারে রাখা কুকি ও সাইটের ডেটা</string>
    <string name="site_sessions_clear_title">সব সাইট থেকে সাইন আউট করবেন?</string>
    <string name="site_sessions_clear_body">ইন-অ্যাপ ব্রাউজারে সংরক্ষিত কুকি ও সাইটের ডেটা এই ডিভাইস থেকে মুছে ফেলা হবে। যেসব পেজে সাইন-ইন লাগে, আবার সাইন ইন না করা পর্যন্ত সেগুলো ঠিকমতো লোড হবে না।</string>
    <string name="site_sessions_clear_confirm">সাইন আউট করুন</string>
    <string name="site_sessions_cleared">সব সাইট থেকে সাইন আউট করা হয়েছে</string>
```

`values-ar/strings.xml`:

```xml
    <string name="site_sessions_title">عمليات تسجيل الدخول المحفوظة</string>
    <string name="site_sessions_body">ملفات تعريف الارتباط وبيانات المواقع التي يحتفظ بها المتصفح داخل التطبيق</string>
    <string name="site_sessions_clear_title">هل تريد تسجيل الخروج من جميع المواقع؟</string>
    <string name="site_sessions_clear_body">ستُحذف من هذا الجهاز ملفات تعريف الارتباط وبيانات المواقع التي حفظها المتصفح داخل التطبيق. الصفحات التي تتطلب تسجيل الدخول لن تُحمَّل بشكل صحيح حتى تسجّل الدخول مجددًا.</string>
    <string name="site_sessions_clear_confirm">تسجيل الخروج</string>
    <string name="site_sessions_cleared">تم تسجيل الخروج من جميع المواقع</string>
```

**Verify:** `rg -c 'name="site_sessions_' shared/src/commonMain/composeResources`
Expected: twelve files, each with the count `6`.

**Verify:** `./gradlew :shared:desktopTest --tests '*StringResourcesTest'`
Expected: `BUILD SUCCESSFUL`.

### Step 4: Declare the row for every platform

Create `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/settings/SiteSessionsRow.kt`:

```kotlin
package com.bernaferrari.changedetection.ui.settings

import androidx.compose.runtime.Composable

/** Android-only: deletes the cookies and site storage that keep the in-app browser signed in. */
@Composable internal expect fun SiteSessionsRow()
```

Create the three empty implementations. Each file has the same content apart from its location:

- `shared/src/desktopMain/kotlin/com/bernaferrari/changedetection/ui/settings/SiteSessionsRow.desktopMain.kt`
- `shared/src/iosMain/kotlin/com/bernaferrari/changedetection/ui/settings/SiteSessionsRow.iosMain.kt`
- `shared/src/wasmJsMain/kotlin/com/bernaferrari/changedetection/ui/settings/SiteSessionsRow.wasmJsMain.kt`

```kotlin
package com.bernaferrari.changedetection.ui.settings

import androidx.compose.runtime.Composable

@Composable internal actual fun SiteSessionsRow() = Unit
```

Create `shared/src/androidMain/kotlin/com/bernaferrari/changedetection/ui/settings/SiteSessionsRow.android.kt`:

```kotlin
package com.bernaferrari.changedetection.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.bernaferrari.changedetection.browser.clearSiteSessions
import com.bernaferrari.changedetection.localization.stringResource
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.kit.GroupRow

@Composable
internal actual fun SiteSessionsRow() {
    val context = LocalContext.current.applicationContext
    var confirming by remember { mutableStateOf(false) }
    var cleared by remember { mutableStateOf(false) }
    GroupRow(
        title = stringResource(Res.string.site_sessions_title),
        modifier = Modifier.testTag("site-sessions"),
        supporting = stringResource(if (cleared) Res.string.site_sessions_cleared else Res.string.site_sessions_body),
        icon = Icons.AutoMirrored.Outlined.Logout,
        onClick = { confirming = true },
    )
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(stringResource(Res.string.site_sessions_clear_title)) },
            text = { Text(stringResource(Res.string.site_sessions_clear_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirming = false
                        clearSiteSessions(context) { cleared = true }
                    },
                    modifier = Modifier.testTag("site-sessions-confirm"),
                ) { Text(stringResource(Res.string.site_sessions_clear_confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text(stringResource(Res.string.cancel)) } },
        )
    }
}
```

If `Icons.AutoMirrored.Outlined.Logout` does not resolve, use `androidx.compose.material.icons.outlined.Lock` (`Icons.Outlined.Lock`) instead and note it in your report.

**Verify:** `./gradlew :shared:testAndroidHostTest`
Expected: `BUILD SUCCESSFUL`.

### Step 5: Show the row in Settings

In `SettingsScreen.kt`, in the "App" group, add one line after the `SavedVersionsRow(...)` call (line 186):

```kotlin
                        SavedVersionsRow(uiState, viewModel::retryStorageLoading, viewModel::clearOlderVersions, showDivider = onNotificationSettings != null)
                        SiteSessionsRow()
```

`SiteSessionsRow` is in the same package as `SettingsScreen`, so no import is needed.

**Verify:** `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest`
Expected: `BUILD SUCCESSFUL`. Run `./gradlew :shared:spotlessApply` first if the format check fails. The desktop UI tests render Settings; the new row is empty on desktop, so no existing screenshot or assertion should change.

**Verify:** `./gradlew :webDemo:wasmJsBrowserDistribution`, and on macOS `./gradlew :shared:compileKotlinIosSimulatorArm64`.
Expected: `BUILD SUCCESSFUL`.

**Verify:** `./gradlew :androidApp:lintDebug :androidApp:assembleRelease :shared:assembleAndroidDeviceTest`
Expected: `BUILD SUCCESSFUL`.

## Test plan

- `SiteSessionsTest.clearingSiteSessionsRemovesEveryCookie` (device test, Step 2): sets cookies for two sites, clears, asserts both are gone and the callback ran.
- `StringResourcesTest` (existing): proves the six strings exist in es, it and pt and are not duplicated.
- No host-JVM unit test is possible for `CookieManager` in this repo (there is no Robolectric). The compile check plus the device test are the coverage.
- Manual check if a device is available: Settings, tap "Saved sign-ins", confirm, and the row's second line changes to "Signed out of all sites".

## Done criteria

- [ ] `rg -n "fun clearSiteSessions" shared/src/androidMain` prints one line.
- [ ] `rg -n "SiteSessionsRow" shared/src` shows the `expect`, four `actual`s and one call in `SettingsScreen.kt`.
- [ ] `rg -c 'name="site_sessions_' shared/src/commonMain/composeResources` shows twelve files with `6` each.
- [ ] `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest` is `BUILD SUCCESSFUL`.
- [ ] `./gradlew :shared:assembleAndroidDeviceTest` is `BUILD SUCCESSFUL`.
- [ ] The report says whether the device test was run, and that the translations need native review.
- [ ] `git status --short` lists only in-scope files.

## STOP conditions

- A string name starting with `site_sessions_` already exists.
- The device test fails because cookies are still present after `removeAllCookies` reports completion.
- A desktop UI test for Settings fails after Step 5.
- You find that some other code already clears cookies or site storage.

## Maintenance notes

- After signing out, pages that depend on a login will fail or show a login page on their next check. That is the intended, explained consequence.
- Checks that are running at the moment of sign-out keep whatever their page already loaded; the next check starts signed out.
- An open question for the maintainer, deliberately **not** changed here: background captures pass `acceptThirdPartyCookies = true` (`AndroidScreenshotSyncer.kt:142`) while the interactive browser uses the default `false`. If that difference is unintended, it deserves its own change.
- If a per-site list is ever wanted, `CookieManager` has no API to enumerate sites; it would need the app to remember which hosts the user visited.
- Plan 013 updates `PRIVACY.md` and should mention this row once it exists.
