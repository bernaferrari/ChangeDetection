package com.bernaferrari.changedetection.backup

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bernaferrari.changedetection.model.AppPalette
import com.bernaferrari.changedetection.model.ColorStyle
import com.bernaferrari.changedetection.model.ThemeMode
import com.bernaferrari.changedetection.repo.ComparisonMethod

/**
 * The app settings a backup carries. Anything not listed here (running Turbo sessions, batch cursors) is runtime
 * state and must never travel between devices; an imported file can only set these names, with checked values.
 */
internal object BackupSettings {
    private val booleans = listOf("background_sync", "require_wifi", "require_charging", "require_battery_not_low", "require_device_idle", "notification_image_previews")

    /** Name to the range an imported value must fall in. */
    private val longs = mapOf("sync_interval" to 15L..10_080L, "browser_delay_ms" to 0L..60_000L)
    private val strings = mapOf(
        "theme_mode" to ThemeMode.entries,
        "app_palette" to AppPalette.entries,
        "color_style" to ColorStyle.entries,
        "comparison_method" to ComparisonMethod.entries,
    )

    fun read(preferences: Preferences): Map<String, String> = buildMap {
        booleans.forEach { name -> preferences[booleanPreferencesKey(name)]?.let { put(name, it.toString()) } }
        longs.keys.forEach { name -> preferences[longPreferencesKey(name)]?.let { put(name, it.toString()) } }
        strings.keys.forEach { name -> preferences[stringPreferencesKey(name)]?.let { put(name, it) } }
    }

    fun apply(values: Map<String, String>, target: MutablePreferences) {
        booleans.forEach { name -> values[name]?.toBooleanStrictOrNull()?.let { target[booleanPreferencesKey(name)] = it } }
        longs.forEach { (name, range) -> values[name]?.toLongOrNull()?.takeIf { it in range }?.let { target[longPreferencesKey(name)] = it } }
        strings.forEach { (name, allowed) ->
            values[name]?.takeIf { value -> allowed.any { it.name == value } }?.let { target[stringPreferencesKey(name)] = it }
        }
    }
}
