/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)

    alias(libs.plugins.compose)
}

val releaseKeystorePropertiesFile = providers.environmentVariable("CHANGEDETECTION_SIGNING_PROPERTIES").orNull?.let { rootProject.file(it) }
val releaseKeystoreProperties = Properties()

if (releaseKeystorePropertiesFile?.isFile == true) {
    FileInputStream(releaseKeystorePropertiesFile).use(releaseKeystoreProperties::load)
}

val releaseKeystoreFile =
    releaseKeystoreProperties
        .getProperty("storeFile")
        ?.takeIf { it.isNotBlank() }
        ?.let(::file)
val hasReleaseSigningConfig =
    releaseKeystoreFile?.isFile == true &&
        listOf("keyAlias", "keyPassword", "storePassword").all { property ->
            !releaseKeystoreProperties.getProperty(property).isNullOrBlank()
        }

android {
    namespace = "com.bernaferrari.changedetection"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.bernaferrari.changedetection"
        minSdk = 30
        targetSdk = 37
        versionCode = 34
        versionName = "2.31"
    }

    signingConfigs {
        if (hasReleaseSigningConfig) {
            register("release") {
                keyAlias = releaseKeystoreProperties.getProperty("keyAlias")
                keyPassword = releaseKeystoreProperties.getProperty("keyPassword")
                storeFile = releaseKeystoreFile
                storePassword = releaseKeystoreProperties.getProperty("storePassword")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
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
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true

            setProguardFiles(
                listOf(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    file("proguard-rules.pro"),
                ),
            )
            if (hasReleaseSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                logger.warn("Release signing credentials are unavailable; release artifacts will be unsigned.")
            }
        }
    }

    lint {
        abortOnError = true
    }
}

dependencies {
    implementation(platform(libs.koin.bom))
    implementation(project(":composeApp"))
    implementation(platform(libs.androidx.compose.bom))

    // Koin for dependency injection
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.koin.workmanager)

    // Android Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.work.runtime.ktx)

    // Compose
    implementation(libs.androidx.compose.material3.window.size)

    // Glance AppWidget
    implementation(libs.androidx.glance)
    implementation(libs.androidx.glance.appwidget)

    // Logging
    implementation(libs.napier)
}
