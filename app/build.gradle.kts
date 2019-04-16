import com.bernaferrari.buildsrc.Libs
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

    implementation(project(":base"))
    implementation(project(":base-android"))

    implementation(project(":diffutils"))
    implementation(project(":repo"))

    // Kotlin
    implementation(Libs.Kotlin.stdlib)
    implementation(Libs.Coroutines.core)
    implementation(Libs.Coroutines.android)

    // Epoxy
    implementation(Libs.Epoxy.epoxy)
    implementation(Libs.Epoxy.dataBinding)
    implementation(Libs.Epoxy.paging)
    kapt(Libs.Epoxy.processor)

    // MvRx
    implementation(Libs.MvRx.main)

    // Glide
    implementation(Libs.Glide.glide)

    // Dagger
    implementation(Libs.Dagger.dagger)
    kapt(Libs.Dagger.compiler)

    implementation(Libs.Dagger.androidSupport)
    kapt(Libs.Dagger.androidProcessor)

    compileOnly(Libs.AssistedInject.annotationDagger2)
    kapt(Libs.AssistedInject.processorDagger2)


    // AndroidX
    implementation(Libs.Google.material)
    implementation(Libs.AndroidX.coreKtx)
    implementation(Libs.AndroidX.constraintlayout)
    implementation(Libs.AndroidX.appcompat)
    implementation(Libs.AndroidX.recyclerview)

    implementation(Libs.AndroidX.Lifecycle.extensions)
    implementation(Libs.AndroidX.Navigation.navigationUi)
    implementation(Libs.AndroidX.Navigation.navigationFragment)

    kapt(Libs.AndroidX.Room.compiler)
    implementation(Libs.AndroidX.Room.runtime)
    implementation(Libs.AndroidX.Room.roomktx)
    implementation(Libs.AndroidX.Work.runtimeKtx)
    implementation(Libs.AndroidX.Work.rxJava)
    implementation(Libs.AndroidX.Paging.runtimeKtx)
    implementation(Libs.AndroidX.browser)

    // Logging
    implementation(Libs.logger)

    // RX
    implementation(Libs.RxJava.rxJava)
    implementation(Libs.RxJava.rxAndroid)
    implementation(Libs.RxJava.rxKotlin)
    implementation(Libs.RxJava.rxRelay)
    implementation(Libs.RxJava.rxkPrefs)

    // Glide
    implementation(Libs.Glide.glide)
    kapt(Libs.Glide.compiler)

    // Others
    implementation(Libs.jsoup)
    implementation(Libs.materialDialogs)
    implementation(Libs.notify)

    // UI
    implementation(Libs.alerter)

    implementation(Libs.Komprehensions.rxJava)

    debugImplementation(Libs.LeakCanary.main)
    debugImplementation(Libs.LeakCanary.support)
    releaseImplementation(Libs.LeakCanary.no_op)

    implementation("com.davemorrissey.labs:subsampling-scale-image-view:3.10.0")

    // Iconics
    implementation("com.mikepenz:iconics-core:3.1.0@aar")
    implementation("com.mikepenz:community-material-typeface:2.0.46.1@aar")
    implementation("com.mikepenz:google-material-typeface:3.0.1.2.original@aar")

    // About
    implementation("com.github.daniel-stoneuk:material-about-library:2.4.2")


    // RecyclerView
    val groupie = "2.3.0"
    implementation("com.xwray:groupie:$groupie")
    implementation("com.yarolegovich:discrete-scrollview:1.4.9")
    implementation("com.xwray:groupie-kotlin-android-extensions:$groupie")

    // Internal
    implementation(Libs.stetho)
    implementation(Libs.okHttp)

    // Others
    implementation(Libs.threeTenAndroid)
    implementation(Libs.timeAgo)
    testImplementation(Libs.junit)
}
