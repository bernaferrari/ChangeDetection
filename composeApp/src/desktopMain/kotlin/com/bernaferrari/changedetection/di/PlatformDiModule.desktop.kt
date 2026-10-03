/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.bernaferrari.changedetection.addedit.CapturePreviewer
import com.bernaferrari.changedetection.addedit.HtmlCapturePreviewer
import com.bernaferrari.changedetection.background.DesktopMonitoringManager
import com.bernaferrari.changedetection.background.MonitoringManager
import com.bernaferrari.changedetection.platform.ShareManager
import com.bernaferrari.changedetection.repo.source.local.ChangeDatabase
import com.bernaferrari.changedetection.repo.source.local.getRoomDatabase
import com.bernaferrari.changedetection.sync.ChangeNotifier
import com.bernaferrari.changedetection.sync.NoOpChangeNotifier
import com.bernaferrari.changedetection.sync.SyncEngine
import com.bernaferrari.changedetection.util.*
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import okio.Path.Companion.toPath
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
actual class PlatformDiModule {
    @Single fun capturePreviewer(engine: SyncEngine): CapturePreviewer = HtmlCapturePreviewer(engine)

    @Single fun database(): ChangeDatabase = getRoomDatabase(
        Room.databaseBuilder<ChangeDatabase>(name = File(desktopDataDirectory(), "change_detection.db").absolutePath),
        BundledSQLiteDriver(),
    )

    @Single fun dataStore(): DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
        produceFile = { File(desktopDataDirectory(), "settings.preferences_pb").absolutePath.toPath() },
    )

    @Single fun monitoringManager(engine: SyncEngine, dataStore: DataStore<Preferences>, applicationScope: ApplicationScope): MonitoringManager = DesktopMonitoringManager(engine, dataStore, applicationScope.scope)

    @Single fun fileStorage(): FileStorage = DesktopFileStorage()

    @Single fun changeNotifier(): ChangeNotifier = NoOpChangeNotifier

    @Single fun shareManager(): ShareManager = object : ShareManager {
        override fun shareText(text: String, title: String) {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
        }
        override fun shareFile(fileName: String, text: String, title: String) {
            val target = File.createTempFile("change-detection-", "-" + File(fileName).name)
            target.writeText(text)
            Desktop.getDesktop().open(target)
        }
    }
}
