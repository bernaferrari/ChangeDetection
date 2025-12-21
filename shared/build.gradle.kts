/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    // Android target
    androidTarget()

    // iOS targets
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    // Desktop target (JVM)
    jvm("desktop")

    // Web target (Wasm)
    // wasmJs()  // Uncomment when WebAssembly support is needed

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Kotlin & Coroutines
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)

                // Ktor Client
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)

                // File access
                implementation(libs.okio)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.mockito.core)
                implementation(libs.mockito.kotlin)
            }
        }

        val androidMain by getting {
            dependencies {
                // Android specific
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.compose.material3)
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.lifecycle.runtime.compose)
                implementation(libs.androidx.lifecycle.viewmodel.compose)
                implementation(libs.androidx.navigation.compose)

                // Ktor Android
                implementation(libs.ktor.client.android)

                // SQLDelight Android
                implementation(libs.sqldelight.android.driver)

                // Room (can coexist with SQLDelight during migration)
                implementation(libs.androidx.room.runtime)

                // Jetpack Paging
                implementation(libs.androidx.paging.runtime.ktx)
                implementation(libs.androidx.paging.compose)

                // OkHttp
                implementation(libs.okhttp)

                // Work Manager
                implementation(libs.androidx.work.runtime.ktx)

                // Security
                implementation(libs.androidx.security.crypto)
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.androidx.test.core)
            }
        }

        val iosX64Main by getting {
            dependencies {
                // Ktor iOS
                implementation(libs.ktor.client.ios)

                // SQLDelight iOS
                implementation(libs.sqldelight.native.driver)
            }
        }

        val iosArm64Main by getting {
            dependencies {
                // Ktor iOS
                implementation(libs.ktor.client.ios)

                // SQLDelight iOS
                implementation(libs.sqldelight.native.driver)
            }
        }

        val iosSimulatorArm64Main by getting {
            dependencies {
                // Ktor iOS
                implementation(libs.ktor.client.ios)

                // SQLDelight iOS
                implementation(libs.sqldelight.native.driver)
            }
        }

        val desktopMain by getting {
            dependsOn(commonMain)
            dependencies {
                // Ktor Desktop (uses OkHttp)
                implementation(libs.ktor.client.okhttp)

                // SQLDelight Desktop
                implementation(libs.sqldelight.sqlite.driver)
            }
        }

    }
}

android {
    namespace = "com.bernaferrari.changedetection.shared"
    compileSdk = 36

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// SQLDelight configuration
// TODO: Configure SQLDelight after schema design
// sqldelight {
//     databases {
//         create("ChangeDetectionDatabase") {
//             packageName.set("com.bernaferrari.changedetection.database")
//             schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
//         }
//     }
// }
