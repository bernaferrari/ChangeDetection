plugins {
    alias(libs.plugins.gradle.versions)
    alias(libs.plugins.version.catalog.update)
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.play.publisher) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.koin.compiler) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.spotless) apply false
}

val sourceCopyrightHeader =
    """
    /*
     * Copyright (c) 2024 ChangeDetection Contributors
     * Licensed under the Apache License, Version 2.0
     */
    """.trimIndent() + "\n"

subprojects {
    apply(plugin = "com.diffplug.spotless")
    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        val ktlintOverrides =
            mapOf(
                "ktlint_standard_backing-property-naming" to "disabled",
                "ktlint_standard_function-naming" to "disabled",
                "ktlint_standard_import-ordering" to "disabled",
                "ktlint_standard_max-line-length" to "disabled",
                "ktlint_standard_no-consecutive-comments" to "disabled",
                "ktlint_standard_no-wildcard-imports" to "disabled",
                "ktlint_standard_property-naming" to "disabled",
                "ktlint_standard_value-parameter-comment" to "disabled",
            )

        kotlin {
            target("**/*.kt")
            targetExclude("${layout.buildDirectory}/**/*.kt")
            ktlint().editorConfigOverride(ktlintOverrides)
            licenseHeader(sourceCopyrightHeader)
        }
        kotlinGradle {
            target("*.gradle.kts")
            targetExclude("${layout.buildDirectory}/**/*.kt")
            ktlint().editorConfigOverride(ktlintOverrides)
            // Look for the first line that doesn't have a block comment (assumed to be the license)
            licenseHeader(sourceCopyrightHeader, "(^(?![\\/ ]\\*).*$)")
        }
    }
}
