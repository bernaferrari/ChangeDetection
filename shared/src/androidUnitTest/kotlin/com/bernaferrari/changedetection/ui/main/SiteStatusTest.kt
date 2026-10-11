/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.main

import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.repo.Snap
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SiteStatusTest {
    @Test
    fun `first saved version is a baseline not a change`() {
        assertFalse(siteAndSnap(snapshotCount = 1).hasDetectedChange())
    }

    @Test
    fun `later version from latest successful check is a change`() {
        assertTrue(siteAndSnap(snapshotCount = 2).hasDetectedChange())
    }

    @Test
    fun `unread change does not expire with time`() {
        assertTrue(siteAndSnap(snapshotCount = 2, timestamp = 1L).hasDetectedChange())
    }

    @Test
    fun `opening comparison clears the change state`() {
        assertFalse(siteAndSnap(snapshotCount = 2, isRead = true).hasDetectedChange())
    }

    @Test
    fun `failed or paused checks do not hide saved unread changes`() {
        val changed = siteAndSnap(snapshotCount = 2)
        assertTrue(changed.copy(site = changed.site.copy(isSuccessful = false)).hasDetectedChange())
        assertTrue(changed.copy(site = changed.site.copy(isSyncEnabled = false)).hasDetectedChange())
        assertFalse(changed.copy(site = changed.site.copy(isSuccessful = false, isRead = true)).hasDetectedChange())
    }

    private fun siteAndSnap(
        snapshotCount: Int,
        timestamp: Long = 1_000_000L,
        isRead: Boolean = false,
    ): SiteAndLastSnap {
        val site = Site(
            title = "Example",
            url = "https://example.com",
            timestamp = timestamp,
            tags = "",
            id = "site-1",
            colors = 0xFF000000.toInt() to 0xFFFFFFFF.toInt(),
        ).copy(isRead = isRead)
        val snap = Snap(
            snapId = "snap-$snapshotCount",
            siteId = site.id,
            timestamp = timestamp,
            contentType = "text/html",
            contentCharset = "UTF-8",
            contentSize = 10,
        )
        return SiteAndLastSnap(site = site, snap = snap, snapshotCount = snapshotCount)
    }
}
