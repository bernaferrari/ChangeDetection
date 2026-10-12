package com.bernaferrari.changedetection.background

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey

/** The same scheduling contract is used by setup, settings, status and platform execution. */
data class MonitoringSchedule(
    val enabled: Boolean = true,
    val intervalMinutes: Long = DEFAULT_INTERVAL_MINUTES,
    val unmeteredOnly: Boolean = false,
    val chargingOnly: Boolean = false,
    val batteryNotLow: Boolean = false,
) {
    init {
        require(intervalMinutes in MIN_INTERVAL_MINUTES..MAX_INTERVAL_MINUTES)
    }

    companion object {
        const val MIN_INTERVAL_MINUTES = 15L
        const val MAX_INTERVAL_MINUTES = 7 * 24 * 60L
        const val DEFAULT_INTERVAL_MINUTES = 60L
        val ENABLED = booleanPreferencesKey("background_sync")
        val INTERVAL = longPreferencesKey("sync_interval")
        val UNMETERED = booleanPreferencesKey("require_wifi")
        val CHARGING = booleanPreferencesKey("require_charging")
        val BATTERY_NOT_LOW = booleanPreferencesKey("require_battery_not_low")

        fun from(preferences: Preferences) = MonitoringSchedule(
            enabled = preferences[ENABLED] ?: true,
            intervalMinutes = (preferences[INTERVAL] ?: DEFAULT_INTERVAL_MINUTES)
                .coerceIn(MIN_INTERVAL_MINUTES, MAX_INTERVAL_MINUTES),
            unmeteredOnly = preferences[UNMETERED] ?: false,
            chargingOnly = preferences[CHARGING] ?: false,
            batteryNotLow = preferences[BATTERY_NOT_LOW] ?: false,
        )
    }
}
