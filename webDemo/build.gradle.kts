@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    alias(libs.plugins.spotless)
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
            implementation(project(":shared"))
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

dependencies {
    // Material 3 alpha resolves newer Compose UI; its bundled Skiko runtime must match.
    val skikoRuntime = variantOf(libs.skiko.wasm.js) { classifier("skiko-runtime") }
    configurations.matching { it.name == "COMPOSE_SKIKO_JS_WASM_RUNTIME" }.configureEach {
        dependencies.addLater(skikoRuntime)
    }
}
