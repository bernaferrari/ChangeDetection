import io.gitlab.arturbosch.detekt.detekt

buildscript {
    repositories {
        google()
        jcenter()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:3.5.0-alpha11")
        classpath(kotlin("gradle-plugin", version = "1.3.30"))
        classpath("android.arch.navigation:navigation-safe-args-gradle-plugin:1.0.0")
        classpath("com.github.ben-manes:gradle-versions-plugin:0.21.0")
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
    id("io.gitlab.arturbosch.detekt") version "1.0.0-RC14"
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.buildDir)
}

detekt {
    version = "1.0.0.RC8"
    input = files("app/")
    filters = ".*/resources/.*,.*/build/.*"
    config = files("default-detekt-config.yml")
}

subprojects {
    tasks.withType<Javadoc>().configureEach { isEnabled = false }
}
