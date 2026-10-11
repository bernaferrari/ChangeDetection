/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.bernaferrari.changedetection.background.MonitoringManager
import com.bernaferrari.changedetection.background.SyncJobInfo
import com.bernaferrari.changedetection.platform.ShareManager
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.source.local.ChangeDatabase
import com.bernaferrari.changedetection.repo.source.local.createWasmDatabase
import com.bernaferrari.changedetection.sync.ChangeNotifier
import com.bernaferrari.changedetection.sync.NoOpChangeNotifier
import com.bernaferrari.changedetection.util.FileStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
actual class PlatformDiModule {
    @Single
    fun capturePreviewer(engine: com.bernaferrari.changedetection.sync.SyncEngine): com.bernaferrari.changedetection.addedit.CapturePreviewer = com.bernaferrari.changedetection.addedit.HtmlCapturePreviewer(engine)

    @Single
    fun database(): ChangeDatabase = createWasmDatabase()

    @Single
    fun dataStore(): DataStore<Preferences> = DemoPreferencesStore()

    @Single
    fun monitoringManager(): MonitoringManager = DemoMonitoringManager

    @Single
    fun shareManager(): ShareManager = DemoShareManager

    @Single
    fun fileStorage(): FileStorage = DemoFileStorage()

    @Single
    fun changeNotifier(): ChangeNotifier = NoOpChangeNotifier
}

/** The browser demo has no platform preference file; state lives for the lifetime of the tab. */
private class DemoPreferencesStore : DataStore<Preferences> {
    private val mutex = Mutex()
    private val state = MutableStateFlow(emptyPreferences())

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = mutex.withLock {
        transform(state.value).also { state.value = it }
    }
}

private object DemoMonitoringManager : MonitoringManager {
    override val quickMonitorState = kotlinx.coroutines.flow.MutableStateFlow(com.bernaferrari.changedetection.background.TurboState())
    override fun getSyncStatusFlow(): Flow<List<SyncJobInfo>> = emptyFlow()
    override fun reloadSite(site: Site) = Unit
    override fun startQuickMonitor(siteIds: List<String>, intervalSeconds: Int, durationMinutes: Int) = Unit
    override fun stopQuickMonitor() = Unit
    override suspend fun updateSchedule() = Unit
    override fun cancelWork() = Unit
}

private object DemoShareManager : ShareManager {
    override fun shareText(text: String, title: String) = Unit
    override fun shareFile(fileName: String, text: String, title: String) = Unit
}

private class DemoFileStorage : FileStorage {
    private val files = mutableMapOf<String, ByteArray>()
    override fun writeBytes(fileName: String, bytes: ByteArray) {
        files[fileName] = bytes.copyOf()
    }
    override fun readBytes(fileName: String): ByteArray = files[fileName]?.copyOf() ?: ByteArray(0)
    override fun deleteFile(fileName: String): Boolean = files.remove(fileName) != null
    override fun fileExists(fileName: String): Boolean = fileName in files
}
