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
        maven { setUrl("https://jitpack.io") }
    }
}

plugins {
    `kotlin-dsl`
    id("io.gitlab.arturbosch.detekt") version "1.0.0-RC12"
}

tasks.named<Delete>("clean").configure {
    delete(rootProject.buildDir)
}

//detekt {
//    toolVersion = "1.0.0.RC12"                             // Version of the Detekt CLI that will be used. When unspecified the latest detekt version found will be used. Override to stay on the same version.
//    input = files(                                        // The directories where detekt looks for input files. Defaults to `files("src/main/java", "src/main/kotlin")`.
//        "src/main/kotlin",
//        "gensrc/main/kotlin"
//    )
//    parallel = false                                      // Runs detekt in parallel. Can lead to speedups in larger projects. `false` by default.
//    config = files("path/to/config.yml")                  // Define the detekt configuration(s) you want to use. Defaults to the default detekt configuration.
//    baseline = file("path/to/baseline.xml")               // Specifying a baseline file. All findings stored in this file in subsequent runs of detekt.
//    disableDefaultRuleSets = false                        // Disables all default detekt rulesets and will only run detekt with custom rules defined in `plugins`. `false` by default.
//    plugins = "other/optional/ruleset.jar"                // Additional jar file containing custom detekt rules.
//    debug = false                                         // Adds debug output during task execution. `false` by default.
//    reports {
//        xml {
//            enabled = true                                // Enable/Disable XML report (default: true)
//            destination = file("build/reports/detekt.xml")  // Path where XML report will be stored (default: `build/reports/detekt/detekt.xml`)
//        }
//        html {
//            enabled = true                                // Enable/Disable HTML report (default: true)
//            destination = file("build/reports/detekt.html") // Path where HTML report will be stored (default: `build/reports/detekt/detekt.html`)
//        }
//    }
//}


//detekt {
//    version = "1.0.0.RC8"
//    defaultProfile {
//        input = file("app/")
//        filters = ".*/resources/.*,.*/build/.*"
//        config = file("default-detekt-config.yml")
//    }
//}


subprojects {
    tasks.withType(Javadoc::class.java).all { enabled = false }
}