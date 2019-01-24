import org.jetbrains.kotlin.config.KotlinCompilerVersion
import java.io.FileInputStream
import java.util.*

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-android-extensions")
    id("kotlin-kapt")
    id("com.github.ben-manes.versions")
}

androidExtensions {
    isExperimental = true
}

android {

    compileSdkVersion(28)

    defaultConfig {
        applicationId = "com.bernaferrari.changedetection"
        minSdkVersion(21)
        targetSdkVersion(28)
        versionCode = 32
        versionName = "2.2"
    }

    signingConfigs {
        register("release") {
            val keystorePropertiesFile = file("../ci-dummies/upload-keystore.properties")

            if (!keystorePropertiesFile.exists()) {
                logger.warn("Release builds may not work: signing config not found.")
                return@register
            }

            val keystoreProperties = Properties()
            keystoreProperties.load(FileInputStream(keystorePropertiesFile))

            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
        }
    }

    lintOptions.isAbortOnError = false
    dataBinding.isEnabled = true
    kapt.correctErrorTypes = true

    buildTypes {
        named("release") {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true

            setProguardFiles(
                listOf(
                    getDefaultProguardFile("proguard-android.txt"),
                    file("proguard-rules.pro")
                )
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation(project(":diffutils"))

    // RX
    implementation("io.reactivex.rxjava2:rxjava:2.2.5")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.0")

    
    // Coroutines
    val coroutinesVersion = "1.1.0"
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")


    // Jetpack
    implementation("com.google.android.material:material:1.0.0")
    implementation("androidx.appcompat:appcompat:1.0.2")
    implementation("androidx.core:core:1.0.1")
    implementation("androidx.recyclerview:recyclerview:1.0.0")
    implementation("androidx.annotation:annotation:1.0.1")
    implementation("androidx.core:core-ktx:1.0.1")

    // Android Architecture Components
    val navigationVersion = "1.0.0-alpha11"
    implementation("android.arch.navigation:navigation-ui-ktx:$navigationVersion")
    implementation("android.arch.navigation:navigation-fragment-ktx:$navigationVersion")

    val lifecycleVersion = "2.0.0"
    implementation("androidx.lifecycle:lifecycle-extensions:$lifecycleVersion")

    val roomVersion = "2.0.0"
    kapt("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-runtime:$roomVersion")

    val workVersion = "1.0.0-alpha13"
    implementation("android.arch.work:work-runtime-ktx:$workVersion")

    val pagingVersion = "2.0.0"
    implementation("androidx.paging:paging-runtime:$pagingVersion")

    // Logging
    implementation("com.orhanobut:logger:2.2.0")

    // UI
    implementation("io.karn:notify:1.1.0")
    implementation("com.tapadoo.android:alerter:4.0.0")
    implementation("com.afollestad.material-dialogs:core:2.0.0-rc7")
    implementation("com.davemorrissey.labs:subsampling-scale-image-view:3.10.0")


    // Glide
    val glide = "4.8.0"
    implementation("com.github.bumptech.glide:glide:$glide")
    implementation("com.github.bumptech.glide:recyclerview-integration:$glide")
    kapt("com.github.bumptech.glide:compiler:$glide")


    // Iconics
    implementation("com.mikepenz:iconics-core:3.1.0@aar")
    implementation("com.mikepenz:community-material-typeface:2.0.46.1@aar")
    implementation("com.mikepenz:google-material-typeface:3.0.1.2.original@aar")


    // About
    implementation ("com.github.daniel-stoneuk:material-about-library:2.4.2")


    // RecyclerView
    val groupie = "2.3.0"
    implementation("com.xwray:groupie:$groupie")
    implementation("com.yarolegovich:discrete-scrollview:1.4.9")
    implementation("com.xwray:groupie-kotlin-android-extensions:$groupie")


    implementation("androidx.multidex:multidex:2.0.1")
    implementation("androidx.constraintlayout:constraintlayout:1.1.3")


    // Internal
    implementation("org.jsoup:jsoup:1.11.3")
    implementation("com.facebook.stetho:stetho:1.5.0")
    implementation("com.squareup.okhttp3:okhttp:3.12.1")
    kotlin("stdlib", KotlinCompilerVersion.VERSION)


    // Dagger
    val dagger = "2.20"
    implementation("com.google.dagger:dagger:$dagger")
    kapt("com.google.dagger:dagger-compiler:$dagger")


    // Others
    implementation("com.jakewharton.threetenabp:threetenabp:1.1.1")
    implementation("com.github.marlonlom:timeago:4.0.1")
    testImplementation("junit:junit:4.12")
}
