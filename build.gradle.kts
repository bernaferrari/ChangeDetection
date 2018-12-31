import org.gradle.internal.impldep.org.testng.reporters.XMLUtils.xml

buildscript {
    repositories {
        google()
        jcenter()
    }

    val kotlinVersion = "1.3.11"

    dependencies {
        classpath("gradle.plugin.com.boxfuse.client:gradle-plugin-publishing:5.0.3")
        classpath("com.android.tools.build:gradle:3.4.0-alpha09")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("android.arch.navigation:navigation-safe-args-gradle-plugin:1.0.0-alpha09")
        classpath("com.github.ben-manes:gradle-versions-plugin:0.20.0")
    }
}

allprojects {
    repositories {
        google()
        jcenter()
        maven("https://jitpack.io")
    }
}

plugins {
    id("io.gitlab.arturbosch.detekt") version "1.0.0-RC12"
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
