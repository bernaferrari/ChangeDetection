import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose) // Kotlin Compose plugin required for Kotlin 2.0+
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidLibrary {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
        androidResources {
            enable = true
        }
        namespace = "com.bernaferrari.changedetection.library"
        compileSdk = 36
        minSdk = 30
        packaging {
            resources {
                excludes += "/META-INF/{AL2.0,LGPL2.1}"
            }
        }
        lint {
            abortOnError = false
        }
        withHostTest { }
        withDeviceTest { }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.stdlib)
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.androidx.room.runtime)
            implementation(libs.sqlite.bundled)
            // Networking (Ktor)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            // HTML Parsing (Ksoup - Jsoup for KMP)
            implementation(libs.ksoup)
            // Date/Time (KMP)
            implementation(libs.kotlinx.datetime)
            // Compose Multiplatform UI
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.materialIconsExtended)
            implementation(libs.androidx.graphics.shapes)
            
            // Logging (Napier)
            implementation(libs.napier)
            
            // Paging (KMP)
            implementation(libs.androidx.paging.common)
            implementation(libs.androidx.paging.compose)
            implementation(libs.androidx.room.paging)
            
            // AndroidX Lifecycle ViewModel (KMP)
            implementation(libs.jetbrains.androidx.lifecycle.viewmodel)
            implementation(libs.jetbrains.androidx.lifecycle.viewmodel.compose)
            implementation(libs.jetbrains.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.fast.kotlin.diff.core)
            
            // Navigation 3 (KMP-compatible runtime)
            implementation(libs.jetbrains.androidx.navigation3.runtime)
            implementation(libs.jetbrains.androidx.navigation3.ui)
            implementation(libs.webview.multiplatform)
            
            // Adaptive
            implementation(libs.jetbrains.compose.material3.adaptive.navigation3)
            
            // Core
            implementation(libs.jetbrains.androidx.savedstate)
            implementation(libs.jetbrains.androidx.window.core)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.workmanager)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.work.runtime.ktx)
            implementation(libs.timeago)
            // Navigation 3 UI (Android-specific)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.startup)

            // Compose (already provided by compose multiplatform in commonMain)
            implementation(libs.androidx.compose.ui.tooling.preview)
            implementation(libs.androidx.compose.material3.window.size)

            // Coil for image loading
            implementation(libs.coil.compose)
            implementation(libs.coil.core)

            // DataStore for Preferences
            implementation(libs.androidx.datastore.preferences)





            // UI Utils
            implementation(libs.androidx.palette.ktx)
            implementation(libs.material.kolor)

            // Browser
            implementation(libs.androidx.browser)

            // Web scraping and networking
            implementation(libs.jsoup)
            implementation(libs.okhttp)
            implementation(libs.okio)

            // Text processing
            implementation(libs.commons.text)

            // Image handling
            implementation(libs.subsampling.scale.imageview)

            // Glance AppWidget
            implementation(libs.androidx.glance)
            implementation(libs.androidx.glance.appwidget)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.material.kolor)
        }

        // Host (JVM) tests
        val androidHostTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.mockito.core)
                implementation(libs.mockito.kotlin)
            }
        }

        // Device (instrumented) tests
        val androidDeviceTest by getting {
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

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
}
