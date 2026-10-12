package com.bernaferrari.changedetection.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

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
data class PageDetailDestination(val siteId: String) : NavKey

@Serializable
data class PageTurboDestination(val siteId: String) : NavKey

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
data class ComparisonDestination(
    val siteId: String,
    val initialSnapId: String? = null,
    val showHistory: Boolean = false,
) : NavKey

/** Serialization contract used by Navigation 3 to restore the complete back stack. */
val appNavSavedStateConfiguration =
    SavedStateConfiguration {
        serializersModule =
            SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(MainDestination::class, MainDestination.serializer())
                    subclass(PageDetailDestination::class, PageDetailDestination.serializer())
                    subclass(PageTurboDestination::class, PageTurboDestination.serializer())
                    subclass(TurboDestination::class, TurboDestination.serializer())
                    subclass(LogsDestination::class, LogsDestination.serializer())
                    subclass(SettingsDestination::class, SettingsDestination.serializer())
                    subclass(AddEditDestination::class, AddEditDestination.serializer())
                    subclass(EditSiteDestination::class, EditSiteDestination.serializer())
                    subclass(AboutDestination::class, AboutDestination.serializer())
                    subclass(HowItWorksDestination::class, HowItWorksDestination.serializer())
                    subclass(BrowserDestination::class, BrowserDestination.serializer())
                    subclass(ComparisonDestination::class, ComparisonDestination.serializer())
                }
            }
    }
