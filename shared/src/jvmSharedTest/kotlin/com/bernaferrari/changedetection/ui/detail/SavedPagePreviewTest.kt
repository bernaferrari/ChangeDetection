/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.detail

import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*

class SavedPagePreviewTest {
    private val repository: SnapsRepository = mock()
    private val snap = Snap("saved-version", "page", 1L, "text/html", "UTF-8", 10)

    @Test fun `saved HTML excerpt removes scripts and bounds the rendered text`() = runTest {
        whenever(repository.getSnapContent(snap.snapId)).thenReturn("<html><head><title>Hidden title</title></head><body><script>secretScript()</script><p>Saved offer</p><p>${"Long page ".repeat(1000)}</p></body></html>".encodeToByteArray())
        val preview = loadSavedPagePreview(snap, repository) as SavedPagePreview.Text
        assertTrue(preview.excerpt.startsWith("Saved offer"))
        assertFalse(preview.excerpt.contains("secretScript"))
        assertFalse(preview.excerpt.contains("Hidden title"))
        assertEquals(1200, preview.excerpt.length)
        verify(repository).getSnapContent("saved-version")
        verifyNoMoreInteractions(repository)
    }

    @Test fun `missing file produces an explicit unavailable preview`() = runTest {
        whenever(repository.getSnapContent(snap.snapId)).thenThrow(IllegalStateException("Pruned file"))
        assertEquals(SavedPagePreview.Unavailable, loadSavedPagePreview(snap, repository))
    }

    @Test fun `leaving the page cancels loading rather than presenting a failure`() = runTest {
        whenever(repository.getSnapContent(snap.snapId)).thenThrow(CancellationException("Left page"))
        try {
            loadSavedPagePreview(snap, repository)
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
    }

    @Test fun `binary documents do not decode a large PDF as text`() = runTest {
        assertEquals(SavedPagePreview.Document, loadSavedPagePreview(snap.copy(contentType = "application/pdf"), repository))
        verifyNoInteractions(repository)
    }
}
