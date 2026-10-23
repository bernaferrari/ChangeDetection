plugins {
    alias(libs.plugins.gradle.versions)
    alias(libs.plugins.version.catalog.update)
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.play.publisher) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.koin.compiler) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.spotless) apply false
}

subprojects {
    plugins.withId("com.diffplug.spotless") {
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
                targetExclude("build/**")
                ktlint(libs.versions.ktlint.get()).editorConfigOverride(ktlintOverrides)
            }
            kotlinGradle {
                target("*.gradle.kts")
                targetExclude("build/**")
                ktlint(libs.versions.ktlint.get()).editorConfigOverride(ktlintOverrides)
            }
        }
    }
}
