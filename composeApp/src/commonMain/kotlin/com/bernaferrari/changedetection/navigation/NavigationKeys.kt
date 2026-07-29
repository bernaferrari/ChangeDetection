/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Navigation keys for Navigation 3.
 * Each key represents a unique destination in the app.
 * Shared across all platforms.
 */

// Main tabs
@Serializable
data object MainDestination : NavKey

@Serializable
data object TurboDestination : NavKey

@Serializable
data object LogsDestination : NavKey

@Serializable
data object SettingsDestination : NavKey

// Full-screen destinations
@Serializable
data class AddEditDestination(val initialUrl: String? = null) : NavKey

@Serializable
data class EditSiteDestination(val siteId: String) : NavKey

@Serializable
data object AboutDestination : NavKey

@Serializable
data object HowItWorksDestination : NavKey

@Serializable
data class BrowserDestination(
    val snapId: String = "",
    val url: String = "",
) : NavKey

@Serializable
data class ComparisonDestination(val siteId: String) : NavKey
