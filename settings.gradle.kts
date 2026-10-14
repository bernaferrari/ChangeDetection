pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    // Kotlin/Wasm adds the Node.js distribution repository needed for browser builds.
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
        maven("https://jogamp.org/deployment/maven") {
            content { includeGroup("org.jogamp.gluegen"); includeGroup("org.jogamp.jogl") }
        }
    }
}

include(":androidApp")
include(":shared")
include(":sqliteWasmWorker")
include(":webDemo")
