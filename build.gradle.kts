import io.gitlab.arturbosch.detekt.detekt

buildscript {
    repositories {
        google()
        jcenter()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:4.1.0")
        classpath(kotlin("gradle-plugin", version = "1.3.70"))
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.2.1")
        classpath("com.github.ben-manes:gradle-versions-plugin:0.28.0")
    }
}

allprojects {
    repositories {
        google()
        jcenter()
        maven("https://jitpack.io")
        maven("https://kotlin.bintray.com/kotlinx/")
    }
}

plugins {
    id("io.gitlab.arturbosch.detekt") version "1.6.0"
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.buildDir)
}

detekt {
    version = "1.6.0"
    input = files("app/")
    config = files("default-detekt-config.yml")
}

subprojects {
    tasks.withType<Javadoc>().configureEach { isEnabled = false }
}
