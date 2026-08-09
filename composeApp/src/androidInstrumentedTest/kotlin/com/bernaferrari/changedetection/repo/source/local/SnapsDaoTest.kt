/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.repo.source.local

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bernaferrari.changedetection.repo.ConstraintsRequired
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the current KMP Room schema through Android's real SQLite implementation. */
@RunWith(AndroidJUnit4::class)
class SnapsDaoTest {
    private lateinit var database: ChangeDatabase
    private lateinit var snapsDao: SnapsDao
    private lateinit var sitesDao: SitesDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = getRoomDatabase(
            Room.inMemoryDatabaseBuilder<ChangeDatabase>(context = context),
            BundledSQLiteDriver(),
        )
        snapsDao = database.snapsDao()
        sitesDao = database.siteDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun latestSnapshotsReturnsOneNewestSnapshotPerSite() = runBlocking {
        sitesDao.insertSite(site("garden"))
        sitesDao.insertSite(site("library"))
        snapsDao.insertSnap(snap("garden-old", "garden", 1_000))
        snapsDao.insertSnap(snap("garden-new", "garden", 3_000))
        snapsDao.insertSnap(snap("library-only", "library", 2_000))

        val latest = snapsDao.getAllMostRecentSnaps()

        assertEquals(listOf("garden-new", "library-only"), latest.map(Snap::snapId))
    }

    @Test
    fun latestSnapshotsIsEmptyForAnEmptyDatabase() = runBlocking {
        assertTrue(snapsDao.getAllMostRecentSnaps().isEmpty())
    }

    private fun site(id: String) = Site(
        title = "Synthetic $id",
        url = "https://$id.example",
        timestamp = 0,
        id = id,
        isSuccessful = true,
        isBrowser = false,
        threshold = 0,
        isRead = false,
        isSyncEnabled = true,
        isNotificationEnabled = true,
        notes = "",
        colors = 0xFF00897B.toInt() to 0xFF26A69A.toInt(),
        constraints = ConstraintsRequired(false, false, false, false),
    )

    private fun snap(id: String, siteId: String, timestamp: Long) = Snap(
        snapId = id,
        siteId = siteId,
        timestamp = timestamp,
        contentType = "text/html",
        contentCharset = "UTF-8",
        contentSize = 1,
    )
}
