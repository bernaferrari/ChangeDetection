import com.bernaferrari.buildsrc.Libs2
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
    compileSdkVersion(33)

    defaultConfig {
        applicationId = "com.bernaferrari.changedetection"
        minSdkVersion(21)
        targetSdkVersion(33)
        versionCode = 34
        versionName = "2.31"
        multiDexEnabled = true
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
    buildFeatures.dataBinding = true
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

    kotlinOptions.jvmTarget = "1.8"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation(project(":base"))
    implementation(project(":base-android"))

    implementation(project(":diffutils"))
    implementation(project(":repo"))

    // Kotlin
    implementation(Libs2.Kotlin.stdlib)
    implementation(Libs2.Coroutines.core)
    implementation(Libs2.Coroutines.rx2)
    implementation(Libs2.Coroutines.android)

    // Epoxy
    implementation(Libs2.Epoxy.epoxy)
    implementation(Libs2.Epoxy.dataBinding)
    implementation(Libs2.Epoxy.paging)
    kapt(Libs2.Epoxy.processor)

    // MvRx
    implementation(Libs2.MvRx.main)

    // Glide
    implementation(Libs2.Glide.glide)

    // Dagger
    implementation(Libs2.Dagger.dagger)
    kapt(Libs2.Dagger.compiler)

    implementation(Libs2.Dagger.androidSupport)
    kapt(Libs2.Dagger.androidProcessor)

    compileOnly(Libs2.AssistedInject.annotationDagger2)
    kapt(Libs2.AssistedInject.processorDagger2)


    // AndroidX
    implementation(Libs2.Google.material)
    implementation(Libs2.AndroidX.coreKtx)
    implementation(Libs2.AndroidX.constraintlayout)
    implementation(Libs2.AndroidX.appcompat)
    implementation(Libs2.AndroidX.recyclerview)

    implementation(Libs2.AndroidX.Lifecycle.liveDataKtx)
    implementation(Libs2.AndroidX.Lifecycle.viewModel)

    implementation(Libs2.AndroidX.Navigation.navigationUi)
    implementation(Libs2.AndroidX.Navigation.navigationFragment)

    annotationProcessor(Libs2.AndroidX.Room.compiler)
    kapt(Libs2.AndroidX.Room.compiler)
    implementation(Libs2.AndroidX.Room.runtime)
    implementation(Libs2.AndroidX.Room.roomktx)
    implementation(Libs2.AndroidX.Work.runtimeKtx)
    implementation(Libs2.AndroidX.Work.rxJava)
    implementation(Libs2.AndroidX.Paging.runtimeKtx)
    implementation(Libs2.AndroidX.browser)

    // Logging
    implementation(Libs2.logger)

    // RX
    implementation(Libs2.RxJava.rxJava)
    implementation(Libs2.RxJava.rxAndroid)
    implementation(Libs2.RxJava.rxKotlin)
    implementation(Libs2.RxJava.rxRelay)
    implementation(Libs2.RxJava.rxkPrefs)

    // Glide
    implementation(Libs2.Glide.glide)
    kapt(Libs2.Glide.compiler)

    // Others
    implementation(Libs2.jsoup)
    implementation(Libs2.MaterialDialogs.core)
    implementation(Libs2.MaterialDialogs.input)
    implementation(Libs2.MaterialDialogs.bottomsheets)
    implementation(Libs2.notify)

    // UI
    implementation(Libs2.alerter)

    implementation(Libs2.Komprehensions.rxJava)

    debugImplementation(Libs2.LeakCanary.no_op)
    debugImplementation(Libs2.LeakCanary.no_op)
    releaseImplementation(Libs2.LeakCanary.no_op)

    implementation("com.davemorrissey.labs:subsampling-scale-image-view:3.10.0")

    // Iconics
    implementation("com.mikepenz:iconics-core:3.1.0@aar")
    implementation("com.mikepenz:community-material-typeface:2.0.46.1@aar")
    implementation("com.mikepenz:google-material-typeface:3.0.1.2.original@aar")

    // About
    implementation("com.github.daniel-stoneuk:material-about-library:2.4.2")


    // RecyclerView
    val groupie = "2.4.0"
    implementation("com.xwray:groupie:$groupie")
    implementation("com.yarolegovich:discrete-scrollview:1.4.9")
    implementation("com.xwray:groupie-kotlin-android-extensions:$groupie")

    // Internal
    implementation(Libs2.stetho)
    implementation(Libs2.okHttp)
    implementation(Libs2.okio)
    implementation("org.apache.commons:commons-text:1.8")


    // Others
    implementation(Libs2.threeTenAndroid)
    implementation(Libs2.timeAgo)
    testImplementation(Libs2.junit)
}
