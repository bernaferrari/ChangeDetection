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
        substitute(module("com.webwright:webwright-core")).using(project(":webwright-core"))
        substitute(module("com.webwright:webwright-compose")).using(project(":webwright-compose"))
    }
}

include(":app")
include(":composeApp")
