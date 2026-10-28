# Development

## Build it yourself

This is a Kotlin Multiplatform project with a shared Compose UI. Use the checked-in Gradle wrapper.

Install an Android SDK with platform **37.2**, then set `ANDROID_HOME` or add `sdk.dir=/path/to/Android/sdk` to `local.properties`. The SDK is needed during Gradle configuration even for Desktop tasks. CI starts with Java 21; Gradle can provision the toolchains configured by the project. Desktop tasks request JetBrains JDK 25.

```sh
# Android debug APK → androidApp/build/outputs/apk/debug/
./gradlew :androidApp:assembleDebug

# Desktop app
./gradlew :shared:run

# Browser demo
./gradlew :webDemo:wasmJsBrowserDevelopmentRun
```

For iOS, open `iosApp/iosApp.xcodeproj` in Xcode. The marketing site lives in `website/` and uses pnpm; see its package scripts for development and builds.

## Check a change

```sh
./gradlew :shared:testAndroidHostTest :androidApp:lintDebug
./gradlew :shared:spotlessCheck :shared:desktopTest

# With an Android emulator or device connected
./gradlew :shared:connectedAndroidDeviceTest
```

The UI tour used for the README is reproducible with:

```sh
./gradlew :shared:desktopTest --tests '*DesignReviewTest.readmeTour'
```

It captures the real shared app UI at phone dimensions with local fixture content. See [screenshot provenance](screenshots/README.md) for the source images and composition.

## Contribute

Bug reports are most useful with the app version, platform, steps to reproduce, and a public page URL when possible. Describe what you expected and what happened. Remove private page content, cookies, and account details from logs or screenshots.

For a fix, add a regression test that fails first, keep the change focused, and run the relevant checks above. Documentation, translations, accessibility improvements, and reproducible test cases are welcome too.


Localized store artwork is maintained in [Screenshot Studio](../store-listing/README.md): shared layout in `store-listing/gallery.json`, words in `store-listing/<locale>/copy.json`, and native captures plus UI layout files in `store-listing/<locale>/raw/`. The editor compares languages and saves copy; strict export rejects English fallbacks and unresolved callouts. English remains the README image source.
