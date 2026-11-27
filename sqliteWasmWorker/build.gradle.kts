@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    alias(libs.plugins.spotless)
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
        }
        wasmJsMain.dependencies {
            implementation(npm("sqlite-wasm-worker", layout.projectDirectory.dir("worker").asFile))
            implementation(libs.kotlinx.browser)
        }
    }
}
