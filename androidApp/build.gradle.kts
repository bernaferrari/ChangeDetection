
import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import java.util.Properties

plugins {
    alias(libs.plugins.spotless)
    alias(libs.plugins.android.application)
    alias(libs.plugins.play.publisher)
    alias(libs.plugins.compose)
}

val releaseKeystorePropertiesFile = providers.environmentVariable("CHANGEDETECTION_SIGNING_PROPERTIES").orNull?.let { rootProject.file(it) }
val releaseKeystoreProperties = Properties()

if (releaseKeystorePropertiesFile?.isFile == true) {
    releaseKeystorePropertiesFile.inputStream().use(releaseKeystoreProperties::load)
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

val playPublishingEnabled = providers.gradleProperty("playPublishing").map { it.toBooleanStrict() }.orElse(false).get()
val playCredentialsFile = providers.environmentVariable("CHANGEDETECTION_PLAY_CREDENTIALS").orNull?.let { rootProject.file(it) }
if (playPublishingEnabled) {
    check(hasReleaseSigningConfig) { "Set CHANGEDETECTION_SIGNING_PROPERTIES to a valid external signing configuration before publishing." }
    check(playCredentialsFile?.isFile == true) { "Set CHANGEDETECTION_PLAY_CREDENTIALS to an external Play service-account JSON file before publishing." }
}

play {
    enabled.set(playPublishingEnabled)
    if (playCredentialsFile != null) serviceAccountCredentials.set(playCredentialsFile)
    defaultToAppBundles.set(true)
    track.set("internal")
    releaseStatus.set(ReleaseStatus.DRAFT)
}

android {
    namespace = "com.bernaferrari.changedetection"
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        applicationId = "com.bernaferrari.changedetection"
        minSdk = 26
        targetSdk = 37
        versionCode = 36
        versionName = "3.0"
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
}

dependencies {
    implementation(platform(libs.koin.bom))
    implementation(project(":shared"))
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
