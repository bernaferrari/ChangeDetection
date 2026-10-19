package com.bernaferrari.changedetection.preview

import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ChangePreviewRepositoryTest {
    private val before = Snap("before", "page", 1L, "text/html", "UTF-8", 20)
    private val after = before.copy(snapId = "after", timestamp = 2L)

    @Test fun simultaneousConsumersReadAndBuildTheSamePairOnlyOnce() = runBlocking {
        val repository = mock<SnapsRepository>()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val reads = AtomicInteger()
        whenever(repository.getSnapContent("before")).thenAnswer {
            reads.incrementAndGet()
            entered.countDown()
            check(release.await(5, TimeUnit.SECONDS))
            "<p>Before offer</p>".encodeToByteArray()
        }
        whenever(repository.getSnapContent("after")).thenReturn("<p>New offer</p>".encodeToByteArray())
        val previews = ChangePreviewRepository(repository)
        val first = async { previews.between(before, after) }
        withContext(Dispatchers.Default) { check(entered.await(5, TimeUnit.SECONDS)) }
        val second = async(start = CoroutineStart.UNDISPATCHED) { previews.between(before, after) }
        delay(100)
        release.countDown()
        assertEquals(first.await(), second.await())
        assertEquals("Home and Activity must share one build", 1, reads.get())
    }

    @Test fun transientMissingFileCanRecoverInsteadOfCachingFailureForever() = runBlocking {
        val repository = mock<SnapsRepository>()
        whenever(repository.getSnapContent("before")).thenThrow(IllegalStateException("File temporarily unavailable")).thenReturn("<p>Before offer</p>".encodeToByteArray())
        whenever(repository.getSnapContent("after")).thenReturn("<p>New offer</p>".encodeToByteArray())
        val previews = ChangePreviewRepository(repository)
        assertEquals(ChangePreview.Unavailable, previews.between(before, after))
        assertTrue(previews.between(before, after) is ChangePreview.Words)
    }

    @Test fun interruptedBuildCanBeRetriedAndSuccessfulBuildsStayCached() = runBlocking {
        val repository = mock<SnapsRepository>()
        whenever(repository.getSnapContent("before")).thenThrow(CancellationException("Left screen")).thenReturn("<p>Before offer</p>".encodeToByteArray())
        whenever(repository.getSnapContent("after")).thenReturn("<p>New offer</p>".encodeToByteArray())
        val previews = ChangePreviewRepository(repository)
        try {
            previews.between(before, after)
            fail("Must propagate cancellation")
        } catch (_: CancellationException) { }
        val ready = previews.between(before, after)
        assertTrue(ready is ChangePreview.Words)
        assertSame(ready, previews.between(before, after))
        verify(repository, times(2)).getSnapContent("before")
        verify(repository, times(1)).getSnapContent("after")
        Unit
    }
}
