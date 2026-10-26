package com.bernaferrari.changedetection.ui.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bernaferrari.changedetection.background.MonitoringManager
import com.bernaferrari.changedetection.background.MonitoringSchedule
import com.bernaferrari.changedetection.ui.theme.supportsDynamicColors
import com.bernaferrari.changedetection.model.AppPalette
import com.bernaferrari.changedetection.repo.ComparisonMethod
import com.bernaferrari.changedetection.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

data class SettingsUiState(
    val isLoaded: Boolean = true,
    val loadFailed: Boolean = false,
    val storageBytes: Long? = null,
    val storageLoadFailed: Boolean = false,
    val clearingOlderVersions: Boolean = false,
    val clearOlderVersionsFailed: Boolean = false,
    /** What clearing older versions would free; 0 when every page is already down to its latest change. */
    val clearableBytes: Long = 0,
    val backgroundSync: Boolean = true,
    val syncInterval: Long = MonitoringSchedule.DEFAULT_INTERVAL_MINUTES,
    val requireWifi: Boolean = false,
    val requireCharging: Boolean = false,
    val requireBatteryNotLow: Boolean = false,
    val requireDeviceIdle: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appPalette: AppPalette = AppPalette.defaultFor(supportsDynamicColors()),
    val comparisonMethod: ComparisonMethod = ComparisonMethod.SCREENSHOT,
    val browserDelayMs: Long = 5000,
    val notificationImages: Boolean = false,
)

@KoinViewModel
class SettingsViewModel(
    private val dataStore: DataStore<Preferences>,
    private val monitoringManager: MonitoringManager,
    private val snapshots: com.bernaferrari.changedetection.repo.source.SnapsRepository,
    private val applicationScope: com.bernaferrari.changedetection.util.ApplicationScope,
) : ViewModel() {
    companion object {
        val BACKGROUND_SYNC_KEY = MonitoringSchedule.ENABLED
        val SYNC_INTERVAL_KEY = MonitoringSchedule.INTERVAL
        val REQUIRE_WIFI_KEY = MonitoringSchedule.UNMETERED
        val REQUIRE_CHARGING_KEY = MonitoringSchedule.CHARGING
        val REQUIRE_BATTERY_NOT_LOW_KEY = MonitoringSchedule.BATTERY_NOT_LOW
        val REQUIRE_DEVICE_IDLE_KEY = booleanPreferencesKey("require_device_idle")
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val APP_PALETTE_KEY = stringPreferencesKey("app_palette")
        val COMPARISON_METHOD_KEY = stringPreferencesKey("comparison_method")
        val BROWSER_DELAY_KEY = longPreferencesKey("browser_delay_ms")
    }

    private val reload = MutableStateFlow(0)
    private val reloadStorage = MutableStateFlow(0)
    fun retryStorageLoading() {
        reloadStorage.value += 1
    }

    private data class StorageSummary(val bytes: Long? = null, val clearable: Long = 0, val failed: Boolean = false)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val storageSummary = reloadStorage.flatMapLatest {
        // Each total starts on its own: the size shows as soon as it is known, not when the slower one is.
        snapshots.observeStorageBytes().map<Long, Long?> { it }.onStart { emit(null) }
            .combine(snapshots.observeOlderVersionBytes().onStart { emit(0L) }) { bytes, clearable -> StorageSummary(bytes = bytes, clearable = clearable) }
            .onStart { emit(StorageSummary()) }
            .catch { error ->
                appLog.w("Could not load history storage size", error)
                emit(StorageSummary(failed = true))
            }
    }

    private enum class ClearState { Idle, Clearing, Failed }
    private val clearState = MutableStateFlow(ClearState.Idle)

    /** Survives leaving Settings mid-clear; repeated taps cannot start overlapping deletions. */
    fun clearOlderVersions() {
        val previous = clearState.value
        if (previous == ClearState.Clearing || !clearState.compareAndSet(previous, ClearState.Clearing)) return
        applicationScope.scope.launch {
            try {
                snapshots.clearOlderVersions()
                clearState.value = ClearState.Idle
                retryStorageLoading()
            } catch (error: CancellationException) {
                clearState.value = ClearState.Idle
                throw error
            } catch (error: Exception) {
                appLog.w("Could not clear older versions", error)
                clearState.value = ClearState.Failed
                retryStorageLoading()
            }
        }
    }

    fun retryLoading() {
        reload.value += 1
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<SettingsUiState> = reload.flatMapLatest {
        dataStore.data
            .combine(storageSummary) { preferences, storage ->
                SettingsUiState(
                    storageBytes = storage.bytes,
                    storageLoadFailed = storage.failed,
                    clearableBytes = storage.clearable,
                    backgroundSync = MonitoringSchedule.from(preferences).enabled,
                    syncInterval = MonitoringSchedule.from(preferences).intervalMinutes,
                    requireWifi = preferences[REQUIRE_WIFI_KEY] ?: false,
                    requireCharging = preferences[REQUIRE_CHARGING_KEY] ?: false,
                    requireBatteryNotLow = preferences[REQUIRE_BATTERY_NOT_LOW_KEY] ?: false,
                    requireDeviceIdle = preferences[REQUIRE_DEVICE_IDLE_KEY] ?: false,
                    themeMode = preferences[THEME_MODE_KEY]
                        ?.let(ThemeMode::fromStoredName)
                        ?: ThemeMode.SYSTEM,
                    appPalette = AppPalette.fromStoredName(preferences[APP_PALETTE_KEY], supportsDynamicColors()),
                    comparisonMethod =
                    runCatching {
                        ComparisonMethod.valueOf(preferences[COMPARISON_METHOD_KEY] ?: ComparisonMethod.SCREENSHOT.name)
                    }.getOrElse { ComparisonMethod.SCREENSHOT },
                    browserDelayMs = preferences[BROWSER_DELAY_KEY] ?: 5000,
                    notificationImages = preferences[com.bernaferrari.changedetection.sync.NotificationPreferences.IMAGE_PREVIEWS] ?: false,
                )
            }.onStart { emit(SettingsUiState(isLoaded = false)) }
            .catch { error ->
                appLog.w("Could not load settings", error)
                emit(SettingsUiState(isLoaded = false, loadFailed = true))
            }
    }.combine(clearState) { state, clear ->
        state.copy(clearingOlderVersions = clear == ClearState.Clearing, clearOlderVersionsFailed = clear == ClearState.Failed)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(isLoaded = false),
    )

    suspend fun saveMonitoringSettings(settings: SettingsUiState) = kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
        check(settings.isLoaded) { "Settings have not loaded" }
        require(settings.syncInterval in MonitoringSchedule.MIN_INTERVAL_MINUTES..MonitoringSchedule.MAX_INTERVAL_MINUTES)
        dataStore.edit { preferences ->
            preferences[BACKGROUND_SYNC_KEY] = settings.backgroundSync
            preferences[SYNC_INTERVAL_KEY] = settings.syncInterval
            preferences[REQUIRE_WIFI_KEY] = settings.requireWifi
            preferences[REQUIRE_CHARGING_KEY] = settings.requireCharging
            preferences[REQUIRE_BATTERY_NOT_LOW_KEY] = settings.requireBatteryNotLow
        }
        monitoringManager.updateSchedule()
    }

    private val writeMutex = Mutex()
    private var writeGeneration = 0L
    private var failedWrite: (suspend (MutablePreferences) -> Unit)? = null
    private val _writeFailed = MutableStateFlow(false)
    val writeFailed: StateFlow<Boolean> = _writeFailed

    fun retryPreferenceWrite() {
        failedWrite?.let(::writePreference)
    }

    private fun writePreference(change: suspend (MutablePreferences) -> Unit) {
        val generation = ++writeGeneration
        failedWrite = null
        _writeFailed.value = false
        viewModelScope.launch {
            writeMutex.withLock {
                try {
                    dataStore.edit(change)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    appLog.w("Could not save preference", error)
                    if (generation == writeGeneration) {
                        failedWrite = change
                        _writeFailed.value = true
                    }
                }
            }
        }
    }

    fun updateThemeMode(themeMode: ThemeMode) = writePreference { it[THEME_MODE_KEY] = themeMode.name }
    fun updateAppPalette(palette: AppPalette) = writePreference { it[APP_PALETTE_KEY] = palette.name }
    fun updateComparisonMethod(method: ComparisonMethod) = writePreference { it[COMPARISON_METHOD_KEY] = method.name }
    fun updateNotificationImages(enabled: Boolean) = writePreference { it[com.bernaferrari.changedetection.sync.NotificationPreferences.IMAGE_PREVIEWS] = enabled }
    fun updateBrowserDelay(delayMs: Long) = writePreference { it[BROWSER_DELAY_KEY] = delayMs }
}

private val appLog = com.bernaferrari.changedetection.logging.AppLog.tagged("SettingsViewModel")
