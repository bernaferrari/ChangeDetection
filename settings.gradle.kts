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

// A composite's Android/Kotlin plugins must match the consumer's stable toolchain.
val consumerVersions = providers.fileContents(layout.settingsDirectory.file("gradle/libs.versions.toml")).asText.get()
fun consumerVersion(key: String): String = requireNotNull(Regex("(?m)^$key\\s*=\\s*\"([^\"]+)\"").find(consumerVersions)).groupValues[1]
gradle.startParameter.projectProperties = gradle.startParameter.projectProperties + mapOf(
    "kmpwrightConsumerAgp" to consumerVersion("androidGradlePlugin"),
    "kmpwrightConsumerKotlin" to consumerVersion("kotlin"),
    "kmpwrightConsumerSerialization" to consumerVersion("kotlinxSerializationJson"),
)

includeBuild(providers.gradleProperty("kmpwrightPath").orElse("/Users/bernardoferrari/Documents/kotlin/webwright").get()) {
    dependencySubstitution {
        substitute(module("io.github.bernaferrari:kmpwright-core")).using(project(":kmpwright-core"))
    }
}
