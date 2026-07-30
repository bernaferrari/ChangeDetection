/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bernaferrari.changedetection.background.MonitoringManager
import com.bernaferrari.changedetection.repo.ComparisonMethod
import com.bernaferrari.changedetection.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

data class SettingsUiState(
    val backgroundSync: Boolean = false,
    val syncInterval: Long = 60,
    val requireWifi: Boolean = false,
    val requireCharging: Boolean = false,
    val requireBatteryNotLow: Boolean = false,
    val requireDeviceIdle: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val comparisonMethod: ComparisonMethod = ComparisonMethod.HTML,
    val browserDelayMs: Long = 5000,
)

@KoinViewModel
class SettingsViewModel(
    private val dataStore: DataStore<Preferences>,
    private val monitoringManager: MonitoringManager,
) : ViewModel() {
    companion object {
        val BACKGROUND_SYNC_KEY = booleanPreferencesKey("background_sync")
        val SYNC_INTERVAL_KEY = longPreferencesKey("sync_interval")
        val REQUIRE_WIFI_KEY = booleanPreferencesKey("require_wifi")
        val REQUIRE_CHARGING_KEY = booleanPreferencesKey("require_charging")
        val REQUIRE_BATTERY_NOT_LOW_KEY = booleanPreferencesKey("require_battery_not_low")
        val REQUIRE_DEVICE_IDLE_KEY = booleanPreferencesKey("require_device_idle")
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val COMPARISON_METHOD_KEY = stringPreferencesKey("comparison_method")
        val BROWSER_DELAY_KEY = longPreferencesKey("browser_delay_ms")
    }

    val uiState: StateFlow<SettingsUiState> =
        dataStore.data
            .map { preferences ->
                SettingsUiState(
                    backgroundSync = preferences[BACKGROUND_SYNC_KEY] ?: false,
                    syncInterval = preferences[SYNC_INTERVAL_KEY] ?: 60,
                    requireWifi = preferences[REQUIRE_WIFI_KEY] ?: false,
                    requireCharging = preferences[REQUIRE_CHARGING_KEY] ?: false,
                    requireBatteryNotLow = preferences[REQUIRE_BATTERY_NOT_LOW_KEY] ?: false,
                    requireDeviceIdle = preferences[REQUIRE_DEVICE_IDLE_KEY] ?: false,
                    themeMode = preferences[THEME_MODE_KEY]
                        ?.let(ThemeMode::fromStoredName)
                        ?: ThemeMode.SYSTEM,
                    comparisonMethod =
                    runCatching {
                        ComparisonMethod.valueOf(preferences[COMPARISON_METHOD_KEY] ?: ComparisonMethod.HTML.name)
                    }.getOrElse { ComparisonMethod.HTML },
                    browserDelayMs = preferences[BROWSER_DELAY_KEY] ?: 5000,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = SettingsUiState(),
            )

    fun updateBackgroundSync(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[BACKGROUND_SYNC_KEY] = enabled
            }

            if (enabled) {
                monitoringManager.updateWorkerWithConstraints(dataStore)
            } else {
                monitoringManager.cancelWork()
            }
        }
    }

    fun updateSyncInterval(interval: Long) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[SYNC_INTERVAL_KEY] = interval
            }
            monitoringManager.updateWorkerWithConstraints(dataStore)
        }
    }

    fun updateWifiConstraint(enabled: Boolean) {
        updateConstraint(REQUIRE_WIFI_KEY, enabled)
    }

    fun updateChargingConstraint(enabled: Boolean) {
        updateConstraint(REQUIRE_CHARGING_KEY, enabled)
    }

    fun updateBatteryNotLowConstraint(enabled: Boolean) {
        updateConstraint(REQUIRE_BATTERY_NOT_LOW_KEY, enabled)
    }

    fun updateDeviceIdleConstraint(enabled: Boolean) {
        updateConstraint(REQUIRE_DEVICE_IDLE_KEY, enabled)
    }

    private fun updateConstraint(
        key: Preferences.Key<Boolean>,
        enabled: Boolean,
    ) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[key] = enabled
            }
            monitoringManager.updateWorkerWithConstraints(dataStore)
        }
    }

    fun updateThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[THEME_MODE_KEY] = themeMode.name
            }
        }
    }

    fun updateComparisonMethod(method: ComparisonMethod) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[COMPARISON_METHOD_KEY] = method.name
            }
        }
    }

    fun updateBrowserDelay(delayMs: Long) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[BROWSER_DELAY_KEY] = delayMs
            }
        }
    }
}
