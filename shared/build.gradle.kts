@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.spotless)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.koin.compiler)
    alias(libs.plugins.androidx.room)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    jvm("desktop") {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
    }

    android {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
        androidResources {
            enable = true
        }
        namespace = "com.bernaferrari.changedetection.library"
        compileSdk {
            version = release(37) {
                minorApiLevel = 2
            }
        }
        minSdk = 26
        // Test source sets are opt-in for the Android KMP library plugin.
        withHostTest { }
        withDeviceTest { }
    }

    wasmJs {
        browser()
        useEsModules()
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.kotlin.stdlib)
            implementation(libs.koin.core)
            implementation(libs.koin.annotations)
            implementation(libs.koin.core.viewmodel)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.androidx.room.runtime)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ksoup)
            implementation(libs.kotlinx.datetime)
            implementation(libs.jetbrains.compose.runtime)
            implementation(libs.jetbrains.compose.foundation)
            implementation(libs.jetbrains.compose.material3)
            implementation(libs.jetbrains.compose.ui)
            implementation(libs.jetbrains.compose.components.resources)
            implementation(libs.jetbrains.compose.material.icons.extended)
            implementation(libs.androidx.graphics.shapes)

            implementation(libs.napier)

            implementation(libs.androidx.paging.common)
            implementation(libs.androidx.paging.compose)
            implementation(libs.androidx.room.paging)

            implementation(libs.jetbrains.androidx.lifecycle.viewmodel)
            implementation(libs.jetbrains.androidx.lifecycle.viewmodel.compose)
            implementation(libs.jetbrains.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.datastore.preferences.core)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.jetbrains.androidx.navigation3.ui)
            implementation(libs.androidx.lifecycle.viewmodel.navigation3)
            implementation(libs.webview.multiplatform)

            implementation(libs.jetbrains.compose.material3.adaptive.navigation3)
            implementation(libs.jetbrains.compose.material3.adaptive.navigation.suite)

            implementation(libs.jetbrains.androidx.savedstate)
            implementation(libs.jetbrains.androidx.window.core)
            implementation(libs.coil.compose)
            implementation(libs.coil.core)
            implementation(libs.coil.network.ktor3)
        }
        androidMain.dependencies {
            implementation(libs.kmpwright.core)
            implementation(project.dependencies.platform(libs.androidx.compose.bom))
            implementation(libs.koin.android)
            implementation(libs.koin.compose)
            implementation(libs.koin.workmanager)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.work.runtime.ktx)
            implementation(libs.androidx.navigation3.runtime)
            implementation(libs.androidx.navigation3.ui)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.startup)

            implementation(libs.androidx.compose.ui.tooling.preview)
            implementation(libs.androidx.compose.material3.window.size)

            implementation(libs.androidx.datastore.preferences)
            implementation(libs.sqlite.bundled)

            implementation(libs.androidx.palette.ktx)
            implementation(libs.material.kolor)

            implementation(libs.androidx.browser)

            implementation(libs.androidx.glance)
            implementation(libs.androidx.glance.appwidget)
        }
        getByName("desktopMain") {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.material.kolor)
                implementation(libs.sqlite.bundled)
            }
        }
        getByName("desktopTest") {
            kotlin.srcDir("src/jvmSharedTest/kotlin")
            dependencies {
                implementation(libs.jetbrains.compose.ui.test.junit4)
                implementation(libs.junit)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.mockito.core)
                implementation(libs.mockito.kotlin)
            }
        }
        iosMain.dependencies {
            implementation(libs.kmpwright.core)
            implementation(libs.ktor.client.darwin)
            implementation(libs.material.kolor)
            implementation(libs.sqlite.bundled)
        }
        wasmJsMain.dependencies {
            implementation(project(":sqliteWasmWorker"))
            implementation(libs.material.kolor)
            implementation(libs.okio)
        }

        getByName("androidHostTest") {
            kotlin.srcDir("src/androidUnitTest/kotlin")
            kotlin.srcDir("src/jvmSharedTest/kotlin")
            dependencies {
                implementation(libs.junit)
                implementation(libs.ktor.client.mock)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.mockito.core)
                implementation(libs.mockito.kotlin)
            }
        }

        getByName("androidDeviceTest") {
            kotlin.srcDir("src/androidInstrumentedTest/kotlin")
            dependencies {
                implementation(libs.androidx.test.core)
                implementation(libs.androidx.compose.ui.test.junit4)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

compose.resources {
    packageOfResClass = "com.bernaferrari.changedetection.shared.generated.resources"
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    listOf("Android", "Desktop", "IosSimulatorArm64", "IosArm64", "WasmJs").forEach { target ->
        add("ksp$target", libs.androidx.room.compiler)
    }
}

val desktopJavaLauncher = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(25))
    vendor.set(JvmVendorSpec.JETBRAINS)
}
val isMac = providers.systemProperty("os.name").get().contains("Mac", ignoreCase = true)

compose.desktop {
    application {
        mainClass = "com.bernaferrari.changedetection.DesktopMainKt"
        javaHome = desktopJavaLauncher.get().metadata.installationPath.asFile.absolutePath
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "Change Detection"
            packageVersion = "2.31.0"
            modules("java.sql", "java.net.http", "jdk.unsupported", "jdk.crypto.ec")
            macOS {
                bundleID = "com.bernaferrari.changedetection.desktop"
                minimumSystemVersion = "12.0"
                iconFile.set(project.file("src/desktopMain/icons/AppIcon.icns"))
                infoPlist {
                    extraKeysRawXml = "<key>CFBundleIconName</key><string>AppIcon</string>"
                }
            }
            windows {
                iconFile.set(project.file("src/desktopMain/icons/AppIcon.ico"))
                shortcut = true
                menu = true
            }
            linux {
                iconFile.set(project.file("src/desktopMain/composeResources/drawable/app_icon.png"))
                packageName = "changedetection"
                shortcut = true
            }
        }
    }
}

if (isMac) {
    val macIconResources = layout.buildDirectory.dir("macIcon/Resources")
    val compileMacIcon = tasks.register<Exec>("compileMacIcon") {
        group = "distribution"
        description = "Compile the shared Icon Composer artwork for macOS"
        val artwork = rootProject.file("iosApp/iosApp/AppIcon.icon")
        inputs.dir(artwork)
        outputs.dir(macIconResources)
        val output = macIconResources.get().asFile
        doFirst { output.mkdirs() }
        commandLine(
            "xcrun", "actool", artwork.absolutePath,
            "--compile", output.absolutePath,
            "--platform", "macosx", "--minimum-deployment-target", "12.0",
            "--app-icon", "AppIcon",
            "--output-partial-info-plist", output.resolve("icon-info.plist").absolutePath,
        )
    }
    tasks.withType<AbstractJPackageTask>().configureEach {
        if (targetFormat == TargetFormat.AppImage) {
            dependsOn(compileMacIcon)
            inputs.dir(macIconResources)
            // jpackage copies Contents/Resources before signing, preserving the layered icon.
            freeArgs.addAll("--app-content", macIconResources.get().asFile.absolutePath)
        }
    }
}

// JCEF embeds native AWT peers on Desktop (including the Hot Reload JavaExec task).
val browserJvmArgs = listOf(
    "--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
    "--add-opens=java.desktop/java.awt.peer=ALL-UNNAMED",
) + if (isMac) {
    listOf(
        "--add-opens=java.desktop/sun.lwawt=ALL-UNNAMED",
        "--add-opens=java.desktop/sun.lwawt.macosx=ALL-UNNAMED",
    )
} else {
    emptyList()
}
tasks.withType<JavaExec>().configureEach { jvmArgs(browserJvmArgs) }
compose.desktop.application.jvmArgs += browserJvmArgs

// Opt-in: exercises the real bundled native browser, not a mocked WebView or semantic tree.
tasks.register<JavaExec>("desktopBrowserSmoke") {
    group = "verification"
    description = "Load and reopen sanitized saved HTML using the bundled JCEF runtime"
    val testCompilation = kotlin.targets.getByName("desktop").compilations.getByName("test")
    dependsOn(testCompilation.compileTaskProvider)
    classpath(testCompilation.output.allOutputs, testCompilation.runtimeDependencyFiles)
    mainClass.set("com.bernaferrari.changedetection.browser.DesktopBrowserSmoke")
    systemProperty("changedetection.browserSmoke.output", rootProject.layout.buildDirectory.dir("desktop-review").get().asFile.absolutePath)
    javaLauncher.set(desktopJavaLauncher)
    if (isMac) {
        systemProperty("apple.awt.UIElement", "true")
    }
}
