pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

includeBuild("../../kotlin/webwright") {
    dependencySubstitution {
        substitute(module("com.bernaferrari.kmpwright:kmpwright-core")).using(project(":kmpwright-core"))
    }
}

include(":app")
include(":composeApp")
