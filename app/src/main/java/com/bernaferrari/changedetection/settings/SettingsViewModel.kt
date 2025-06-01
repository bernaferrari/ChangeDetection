package com.bernaferrari.changedetection.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bernaferrari.changedetection.WorkerHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val lightMode: Boolean = true,
    val colorBySdk: Boolean = true,
    val showSystemApps: Boolean = true,
    val backgroundSync: Boolean = false,
    val orderBySdk: Boolean = true,
    val syncInterval: Long = 60,
    val requireWifi: Boolean = false,
    val requireCharging: Boolean = false,
    val requireBatteryNotLow: Boolean = false,
    val requireDeviceIdle: Boolean = false,
    val themeMode: String = "MATERIAL_YOU"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val workerHelper: WorkerHelper
) : ViewModel() {

    companion object {
        val LIGHT_MODE_KEY = booleanPreferencesKey("light_mode")
        val COLOR_BY_SDK_KEY = booleanPreferencesKey("color_by_sdk")
        val SHOW_SYSTEM_APPS_KEY = booleanPreferencesKey("show_system_apps")
        val BACKGROUND_SYNC_KEY = booleanPreferencesKey("background_sync")
        val ORDER_BY_SDK_KEY = booleanPreferencesKey("order_by_sdk")
        val SYNC_INTERVAL_KEY = longPreferencesKey("sync_interval")
        val REQUIRE_WIFI_KEY = booleanPreferencesKey("require_wifi")
        val REQUIRE_CHARGING_KEY = booleanPreferencesKey("require_charging")
        val REQUIRE_BATTERY_NOT_LOW_KEY = booleanPreferencesKey("require_battery_not_low")
        val REQUIRE_DEVICE_IDLE_KEY = booleanPreferencesKey("require_device_idle")
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    }

    val uiState: StateFlow<SettingsUiState> = dataStore.data.map { preferences ->
        SettingsUiState(
            lightMode = preferences[LIGHT_MODE_KEY] ?: true,
            colorBySdk = preferences[COLOR_BY_SDK_KEY] ?: true,
            showSystemApps = preferences[SHOW_SYSTEM_APPS_KEY] ?: true,
            backgroundSync = preferences[BACKGROUND_SYNC_KEY] ?: false,
            orderBySdk = preferences[ORDER_BY_SDK_KEY] ?: true,
            syncInterval = preferences[SYNC_INTERVAL_KEY] ?: 60,
            requireWifi = preferences[REQUIRE_WIFI_KEY] ?: false,
            requireCharging = preferences[REQUIRE_CHARGING_KEY] ?: false,
            requireBatteryNotLow = preferences[REQUIRE_BATTERY_NOT_LOW_KEY] ?: false,
            requireDeviceIdle = preferences[REQUIRE_DEVICE_IDLE_KEY] ?: false,
            themeMode = preferences[THEME_MODE_KEY] ?: "MATERIAL_YOU"
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun updateLightMode(lightMode: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[LIGHT_MODE_KEY] = lightMode
            }
        }
    }

    fun updateColorBySdk(colorBySdk: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[COLOR_BY_SDK_KEY] = colorBySdk
            }
        }
    }

    fun updateShowSystemApps(showSystemApps: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[SHOW_SYSTEM_APPS_KEY] = showSystemApps
            }
        }
    }

    fun updateBackgroundSync(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[BACKGROUND_SYNC_KEY] = enabled
            }
            
            if (enabled) {
                workerHelper.updateWorkerWithConstraints(dataStore)
            } else {
                workerHelper.cancelWork()
            }
        }
    }

    fun updateSyncInterval(interval: Long) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[SYNC_INTERVAL_KEY] = interval
            }
            workerHelper.updateWorkerWithConstraints(dataStore)
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

    private fun updateConstraint(key: Preferences.Key<Boolean>, enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[key] = enabled
            }
            workerHelper.updateWorkerWithConstraints(dataStore)
        }
    }

    fun updateOrderBySdk(orderBySdk: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[ORDER_BY_SDK_KEY] = orderBySdk
            }
        }
    }

    fun updateThemeMode(themeMode: String) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[THEME_MODE_KEY] = themeMode
            }
        }
    }
}
