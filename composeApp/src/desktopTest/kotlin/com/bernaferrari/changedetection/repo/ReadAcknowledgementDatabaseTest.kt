/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.repo

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.bernaferrari.changedetection.repo.source.local.ChangeDatabase
import com.bernaferrari.changedetection.repo.source.local.getRoomDatabase
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ReadAcknowledgementDatabaseTest {
    @Test fun concreteCaptureGuardPreservesNewerUpdatesAndOtherPageFields() = runBlocking {
        val directory = Files.createTempDirectory("read-ack-test").toFile()
        val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = directory.resolve("test.db").path), BundledSQLiteDriver())
        try {
            val sites = db.siteDao()
            val snaps = db.snapsDao()
            val site = Site("Program", "https://example.test", 1L, "", "page", 0 to 0).copy(isRead = false, notes = "Keep my note")
            sites.insertSite(site)
            snaps.insertSnap(Snap("capture-a", "page", 10L, "text/html", "UTF-8", 20))
            assertEquals(1, sites.markSiteRead("page", "capture-a"))
            assertEquals(0, sites.markSiteRead("page", "capture-a"))
            assertTrue(sites.getSiteById("page")!!.isRead)
            assertEquals(1, sites.undoSiteRead("page", "capture-a"))
            assertFalse(sites.getSiteById("page")!!.isRead)
            assertEquals(1, sites.markSiteRead("page", "capture-a"))
            // Same-timestamp captures still have a deterministic newest identity.
            snaps.insertSnap(Snap("capture-b", "page", 10L, "text/html", "UTF-8", 20))
            sites.insertSite(site.copy(isRead = false, timestamp = 20L))
            assertEquals(0, sites.markSiteRead("page", "capture-a"))
            assertFalse(sites.getSiteById("page")!!.isRead)
            assertEquals(1, sites.markSiteRead("page", "capture-b"))
            assertEquals(0, sites.undoSiteRead("page", "capture-a"))
            assertTrue(sites.getSiteById("page")!!.isRead)
            assertEquals("Keep my note", sites.getSiteById("page")!!.notes)
            assertEquals(20L, sites.getSiteById("page")!!.timestamp)
        } finally {
            db.close()
            directory.deleteRecursively()
        }
    }
}
