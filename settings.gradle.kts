pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    // Kotlin/Wasm adds the Node.js distribution repository needed for browser builds.
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
    }
}

include(":app")
include(":composeApp")
include(":sqliteWasmWorker")
include(":webApp")

// The Android browser adapter uses the local Webwright development build. Keeping this as a
// composite preserves the shared source-level browser implementation used by the native apps.
includeBuild("../../kotlin/webwright") {
    dependencySubstitution {
        substitute(module("com.bernaferrari.kmpwright:kmpwright-core")).using(project(":kmpwright-core"))
    }
}
