/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    wasmJs {
        browser()
        useEsModules()
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.androidx.sqlite.web)
            implementation(npm("sqlite-wasm-worker", layout.projectDirectory.dir("worker").asFile))
        }
        wasmJsMain.dependencies {
            implementation(libs.kotlinx.browser)
        }
    }
}
