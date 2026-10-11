/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.jetbrains.compose)
}

kotlin {
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "changedetection-demo.js"
                sourceMaps = false
            }
        }
        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(project(":composeApp"))
            implementation(project(":sqliteWasmWorker"))
            implementation(libs.jetbrains.compose.runtime)
            implementation(libs.jetbrains.compose.foundation)
            implementation(libs.jetbrains.compose.material3)
            implementation(libs.jetbrains.compose.ui)
            implementation(libs.jetbrains.compose.material.icons.extended)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.material.kolor)
        }
    }
}
