package com.bernaferrari.changedetection.ui.logs

import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import com.bernaferrari.changedetection.ui.detail.SavedPagePreview
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*

class HistoryPreviewContentTest {
    private val repository: SnapsRepository = mock()
    private val current = Snap("after", "page", 2L, "text/html", "UTF-8", 100)
    private val previous = current.copy(snapId = "before", timestamp = 1L)

    @Test fun `passage finds changes beyond the page introduction using the exact saved pair`() = runTest {
        val introduction = "<p>Unchanged introduction</p>".repeat(200)
        whenever(repository.getSnapContent("before")).thenReturn("$introduction<p>Workshop starts at 09:00</p>".encodeToByteArray())
        whenever(repository.getSnapContent("after")).thenReturn("$introduction<p>Workshop starts at 10:00</p>".encodeToByteArray())
        val preview = loadHistoryPreview(current, repository) { previous } as HistoryPreviewContent.Passage
        assertTrue(preview.before.contains("09:00"))
        assertTrue(preview.after.contains("10:00"))
        assertTrue(preview.before.length <= 242)
        assertTrue(preview.after.length <= 242)
        verify(repository).getSnapContent("before")
        verify(repository).getSnapContent("after")
        verifyNoMoreInteractions(repository)
    }

    @Test fun `missing preceding file retains the current capture`() = runTest {
        whenever(repository.getSnapContent("after")).thenReturn("<p>Current content</p>".encodeToByteArray())
        whenever(repository.getSnapContent("before")).thenThrow(IllegalStateException("Pruned"))
        assertEquals(HistoryPreviewContent.Capture(SavedPagePreview.Text("Current content")), loadHistoryPreview(current, repository) { previous })
    }

    @Test fun `oversized preceding capture is not read`() = runTest {
        whenever(repository.getSnapContent("after")).thenReturn("<p>Current content</p>".encodeToByteArray())
        loadHistoryPreview(current, repository) { previous.copy(contentSize = 1_048_577) }
        verify(repository).getSnapContent("after")
        verifyNoMoreInteractions(repository)
    }

    @Test fun `cancellation propagates during predecessor reads`() = runTest {
        whenever(repository.getSnapContent("after")).thenReturn("<p>Current content</p>".encodeToByteArray())
        whenever(repository.getSnapContent("before")).thenThrow(CancellationException("Scrolled away"))
        try {
            loadHistoryPreview(current, repository) { previous }
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
    }

    @Test fun `unchanged preceding lines cannot push the actual change out of the preview`() {
        val prefix = "Morning sessions\n09:00 — Doors open\n"
        assertEquals(
            HistoryPreviewContent.Passage("10:00 — Opening session", "10:30 — Opening session"),
            firstChangedPassage(prefix + "10:00 — Opening session\nWorkshops", prefix + "10:30 — Opening session\nWorkshops"),
        )
    }

    @Test fun `insertion deletion and unchanged readable text remain honest`() {
        assertNull(firstChangedPassage("Same", "Same"))
        assertEquals(HistoryPreviewContent.Passage("", "Added"), firstChangedPassage("", "Added"))
        assertEquals(HistoryPreviewContent.Passage("Removed", ""), firstChangedPassage("Removed", ""))
        assertEquals(HistoryPreviewContent.Passage("Tickets sold out", "Tickets available"), firstChangedPassage("Tickets sold out", "Tickets available"))
    }
}
