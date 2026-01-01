import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose) // Kotlin Compose plugin required for Kotlin 2.0+
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

kotlin {
    @Suppress("DEPRECATION")
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    
    listOf(
        iosX64(),
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
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
            // Compose Multiplatform UI
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.materialIconsExtended)
            
            // Logging (Napier)
            implementation(libs.napier)
            
            // Paging (KMP)
            implementation(libs.androidx.paging.common)
            implementation(libs.androidx.room.paging)
            
            // AndroidX Lifecycle ViewModel (KMP)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.koin.compose.viewmodel)
            implementation("io.github.bernaferrari:fast-kotlin-diff-core:1.0.0")
            
            // Navigation 3 (KMP-compatible runtime)
            implementation(libs.androidx.navigation3.runtime)
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
            // Paging Compose (Android-only)
            implementation(libs.androidx.paging.compose)
            // Navigation 3 UI (Android-specific)
            implementation(libs.androidx.navigation3.ui)
            implementation(libs.androidx.lifecycle.viewmodel.navigation3)
            implementation(libs.androidx.compose.material3.adaptive.navigation3)

            // Android Core
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.startup)

            // Compose (already provided by compose multiplatform in commonMain)
            implementation(libs.androidx.compose.ui.tooling.preview)
            implementation(libs.androidx.compose.material3.window.size)
            implementation(libs.androidx.lifecycle.viewmodel.compose)

            // Coil for image loading
            implementation(libs.coil.compose)
            implementation(libs.coil.core)

            // DataStore for Preferences
            implementation(libs.androidx.datastore.preferences)

            // Logging
            implementation(libs.logger)

            // Security - Encrypted SharedPreferences
            implementation(libs.androidx.security.crypto)

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
            implementation(libs.ktor.client.ios)
        }

        // Unit tests
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.mockito.core)
                implementation(libs.mockito.kotlin)
            }
        }

        // Instrumented tests
        val androidInstrumentedTest by getting {
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

android {
    namespace = "com.bernaferrari.changedetection"
    compileSdk = 36

    defaultConfig {
        minSdk = 30
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        named("release") {
            isMinifyEnabled = false
        }
    }

    lint {
        abortOnError = false
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
    add("kspIosX64", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
}
