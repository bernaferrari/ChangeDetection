package com.bernaferrari.changedetection.ui.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import okio.Path.Companion.toPath
import com.bernaferrari.changedetection.model.ThemeMode
import com.bernaferrari.changedetection.model.AppPalette
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.lifecycle.viewModelScope
import com.bernaferrari.changedetection.background.MonitoringSchedule
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsLoadingTest {
    @Test fun clearingHistoryReportsFailureAndRetriesWithoutOverlappingWork() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = mock<DataStore<Preferences>>()
        whenever(store.data).thenReturn(flowOf(mutablePreferencesOf()))
        val snapshots = mock<SnapsRepository>()
        whenever(snapshots.observeStorageBytes()).thenReturn(flowOf(100L))
        whenever(snapshots.observeOlderVersionBytes()).thenReturn(flowOf(50L))
        val release = CompletableDeferred<Unit>()
        var attempts = 0
        doSuspendableAnswer {
            attempts++
            if (attempts == 1) {
                release.await()
                throw java.io.IOException("private storage error")
            }
            Unit
        }.whenever(snapshots).clearOlderVersions()
        val model = SettingsViewModel(store, mock(), snapshots, mock { on { scope } doReturn backgroundScope })
        val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect() }
        try {
            runCurrent()
            model.clearOlderVersions()
            model.clearOlderVersions()
            runCurrent()
            assertEquals(1, attempts)
            assertTrue(model.uiState.value.clearingOlderVersions)
            release.complete(Unit)
            runCurrent()
            assertFalse(model.uiState.value.clearingOlderVersions)
            assertTrue(model.uiState.value.clearOlderVersionsFailed)
            assertTrue(model.uiState.value.isLoaded)
            model.clearOlderVersions()
            runCurrent()
            assertEquals(2, attempts)
            assertFalse(model.uiState.value.clearOlderVersionsFailed)
            assertFalse(model.uiState.value.clearingOlderVersions)
        } finally {
            collection.cancelAndJoin()
            model.viewModelScope.coroutineContext[Job]?.cancelAndJoin()
            Dispatchers.resetMain()
        }
    }

    @Test fun historyClearingSurvivesClosingSettings() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val snapshots = mock<SnapsRepository>()
        val release = CompletableDeferred<Unit>()
        var finished = false
        doSuspendableAnswer {
            release.await()
            finished = true
            Unit
        }.whenever(snapshots).clearOlderVersions()
        val model = SettingsViewModel(mock(), mock(), snapshots, mock { on { scope } doReturn backgroundScope })
        try {
            model.clearOlderVersions()
            runCurrent()
            model.viewModelScope.coroutineContext[Job]?.cancelAndJoin()
            assertFalse(finished)
            release.complete(Unit)
            runCurrent()
            assertTrue(finished)
        } finally {
            release.complete(Unit)
            model.viewModelScope.coroutineContext[Job]?.cancelAndJoin()
            Dispatchers.resetMain()
        }
    }

    @Test fun storageFailureAndRetryKeepPreferencesLoadedAndSubscribed() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val preferences = MutableStateFlow<Preferences>(mutablePreferencesOf(MonitoringSchedule.INTERVAL to 10080L))
        val failStorage = CompletableDeferred<Unit>()
        var preferenceSubscriptions = 0
        var storageSubscriptions = 0
        val store = mock<DataStore<Preferences>>()
        val snapshots = mock<SnapsRepository>().also { whenever(it.observeOlderVersionBytes()).thenReturn(kotlinx.coroutines.flow.flowOf(0L)) }
        whenever(store.data).thenReturn(preferences.onStart { preferenceSubscriptions++ })
        whenever(snapshots.observeStorageBytes()).thenReturn(
            flow {
                storageSubscriptions++
                if (storageSubscriptions == 1) {
                    emit(500L)
                    failStorage.await()
                    error("private database diagnostic")
                }
                emit(4_194_304L)
                awaitCancellation()
            },
        )
        val model = SettingsViewModel(store, mock(), snapshots, mock { on { scope } doReturn backgroundScope })
        val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect() }
        try {
            runCurrent()
            assertEquals(500L, model.uiState.value.storageBytes)
            failStorage.complete(Unit)
            runCurrent()
            assertTrue(model.uiState.value.isLoaded)
            assertFalse(model.uiState.value.loadFailed)
            assertTrue(model.uiState.value.storageLoadFailed)
            assertNull(model.uiState.value.storageBytes)
            assertEquals(10080L, model.uiState.value.syncInterval)
            preferences.value = mutablePreferencesOf(MonitoringSchedule.INTERVAL to 15L, SettingsViewModel.THEME_MODE_KEY to ThemeMode.DARK.name)
            runCurrent()
            assertEquals(15L, model.uiState.value.syncInterval)
            assertEquals(ThemeMode.DARK, model.uiState.value.themeMode)
            model.retryStorageLoading()
            runCurrent()
            assertTrue(model.uiState.value.isLoaded)
            assertFalse(model.uiState.value.storageLoadFailed)
            assertEquals(4_194_304L, model.uiState.value.storageBytes)
            assertEquals(15L, model.uiState.value.syncInterval)
            assertEquals(1, preferenceSubscriptions)
            assertEquals(2, storageSubscriptions)
        } finally {
            collection.cancelAndJoin()
            model.viewModelScope.coroutineContext[Job]?.cancelAndJoin()
            Dispatchers.resetMain()
        }
    }

    @Test fun failedPreferenceWriteRetriesAndNewerChoiceReplacesOldRetry() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        var fail = true
        val persisted = MutableStateFlow<Preferences>(mutablePreferencesOf())
        val store = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = persisted
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
                if (fail) throw java.io.IOException("private write failure")
                return transform(persisted.value).also { persisted.value = it }
            }
        }
        val model = SettingsViewModel(store, mock(), mock(), mock { on { scope } doReturn backgroundScope })
        try {
            model.updateThemeMode(ThemeMode.DARK)
            runCurrent()
            assertTrue(model.writeFailed.value)
            assertNull(persisted.value[SettingsViewModel.THEME_MODE_KEY])
            fail = false
            model.retryPreferenceWrite()
            runCurrent()
            assertFalse(model.writeFailed.value)
            assertEquals(ThemeMode.DARK.name, persisted.value[SettingsViewModel.THEME_MODE_KEY])
            fail = true
            model.updateAppPalette(AppPalette.BLUE)
            runCurrent()
            assertTrue(model.writeFailed.value)
            fail = false
            model.updateAppPalette(AppPalette.GREEN)
            runCurrent()
            model.retryPreferenceWrite()
            runCurrent()
            assertFalse(model.writeFailed.value)
            assertEquals(AppPalette.GREEN.name, persisted.value[SettingsViewModel.APP_PALETTE_KEY])
        } finally {
            model.viewModelScope.coroutineContext[Job]?.cancelAndJoin()
            Dispatchers.resetMain()
        }
    }

    @Test fun appearanceWritesSurviveClosingAndReopeningThePreferenceStore() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val directory = kotlin.io.path.createTempDirectory("appearance-persistence").toFile()
        val path = java.io.File(directory, "settings.preferences_pb").absolutePath.toPath()
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = PreferenceDataStoreFactory.createWithPath(scope = firstScope, produceFile = { path })
        val snapshots = mock<SnapsRepository>().also { whenever(it.observeOlderVersionBytes()).thenReturn(kotlinx.coroutines.flow.flowOf(0L)) }
        val model = SettingsViewModel(store, mock(), snapshots, mock { on { scope } doReturn backgroundScope })
        try {
            model.updateThemeMode(ThemeMode.DARK)
            store.data.first { it[SettingsViewModel.THEME_MODE_KEY] == ThemeMode.DARK.name }
            model.updateAppPalette(AppPalette.BLUE)
            store.data.first { it[SettingsViewModel.APP_PALETTE_KEY] == AppPalette.BLUE.name }
            model.viewModelScope.coroutineContext[Job]?.cancelAndJoin()
            firstScope.coroutineContext[Job]?.cancelAndJoin()
            val reopened = PreferenceDataStoreFactory.createWithPath(scope = secondScope, produceFile = { path }).data.first()
            assertEquals(ThemeMode.DARK.name, reopened[SettingsViewModel.THEME_MODE_KEY])
            assertEquals(AppPalette.BLUE.name, reopened[SettingsViewModel.APP_PALETTE_KEY])
        } finally {
            model.viewModelScope.coroutineContext[Job]?.cancelAndJoin()
            firstScope.coroutineContext[Job]?.cancelAndJoin()
            secondScope.coroutineContext[Job]?.cancelAndJoin()
            Dispatchers.resetMain()
            directory.deleteRecursively()
        }
    }

    @Test fun failedPreferencesCanBeRetriedWithoutPublishingDefaults() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        var attempts = 0
        val store = mock<DataStore<Preferences>>()
        val snapshots = mock<SnapsRepository>().also { whenever(it.observeOlderVersionBytes()).thenReturn(kotlinx.coroutines.flow.flowOf(0L)) }
        whenever(store.data).thenReturn(
            flow {
                if (attempts++ == 0) error("private filesystem diagnostic")
                emit(mutablePreferencesOf(MonitoringSchedule.INTERVAL to 10080L))
                awaitCancellation()
            },
        )
        whenever(snapshots.observeStorageBytes()).thenReturn(flow { awaitCancellation() })
        val model = SettingsViewModel(store, mock(), snapshots, mock { on { scope } doReturn backgroundScope })
        val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect() }
        try {
            runCurrent()
            assertFalse(model.uiState.value.isLoaded)
            assertTrue(model.uiState.value.loadFailed)
            model.retryLoading()
            runCurrent()
            assertTrue(model.uiState.value.isLoaded)
            assertFalse(model.uiState.value.loadFailed)
            assertEquals(10080L, model.uiState.value.syncInterval)
            assertNull(model.uiState.value.storageBytes)
            assertEquals(2, attempts)
        } finally {
            collection.cancelAndJoin()
            model.viewModelScope.coroutineContext[Job]?.cancelAndJoin()
            Dispatchers.resetMain()
        }
    }

    @Test fun preferencesBecomeEditableWithoutWaitingForStorageSize() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val preferences = MutableSharedFlow<Preferences>()
        val store = mock<DataStore<Preferences>>()
        val snapshots = mock<SnapsRepository>().also { whenever(it.observeOlderVersionBytes()).thenReturn(kotlinx.coroutines.flow.flowOf(0L)) }
        whenever(store.data).thenReturn(preferences)
        whenever(snapshots.observeStorageBytes()).thenReturn(flow { awaitCancellation() })
        val model = SettingsViewModel(store, mock(), snapshots, mock { on { scope } doReturn backgroundScope })
        val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect() }
        try {
            runCurrent()
            assertFalse(model.uiState.value.isLoaded)
            preferences.emit(mutablePreferencesOf(MonitoringSchedule.INTERVAL to 10080L, MonitoringSchedule.ENABLED to false))
            runCurrent()
            assertTrue(model.uiState.value.isLoaded)
            assertEquals(10080L, model.uiState.value.syncInterval)
            assertNull(model.uiState.value.storageBytes)
            assertFalse(model.uiState.value.backgroundSync)
        } finally {
            collection.cancelAndJoin()
            model.viewModelScope.coroutineContext[Job]?.cancelAndJoin()
            Dispatchers.resetMain()
        }
    }
}
