package com.bernaferrari.changedetection.image

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import org.junit.Before
import androidx.compose.ui.test.junit4.v2.createComposeRule
import java.util.concurrent.atomic.AtomicInteger

class CropBitmapCacheTest {
    @get:Rule val compose = createComposeRule()

    @Before fun graphics() {
        compose.setContent {}
    }

    @Test fun concurrentConsumersDecodeTheSameCropOnce() = runBlocking {
        val count = AtomicInteger()
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val cache = CropBitmapCache(decode = { _, _, _, _, _, _ ->
            count.incrementAndGet()
            started.complete(Unit)
            release.await()
            ImageBitmap(2, 2)
        })
        val bytes = byteArrayOf(1, 2, 3)
        val a = async { cache.get(bytes, 0f, 0f, 1f, 1f, 100) }
        started.await()
        val b = async { cache.get(bytes, 0f, 0f, 1f, 1f, 100) }
        delay(30)
        release.complete(Unit)
        assertSame(a.await(), b.await())
        assertEquals(1, count.get())
    }

    @Test fun cancelledOwnerDoesNotPoisonAnotherConsumerOrCacheAPartialResult() = runBlocking {
        val count = AtomicInteger()
        val started = CompletableDeferred<Unit>()
        val cache = CropBitmapCache(decode = { _, _, _, _, _, _ ->
            if (count.incrementAndGet() == 1) {
                started.complete(Unit)
                awaitCancellation()
            }
            ImageBitmap(2, 2)
        })
        val bytes = byteArrayOf(4)
        val first = async { cache.get(bytes, 0f, 0f, 1f, 1f, 100) }
        started.await()
        val second = async { cache.get(bytes, 0f, 0f, 1f, 1f, 100) }
        first.cancelAndJoin()
        assertNotNull(second.await())
        assertEquals(2, count.get())
    }

    @Test fun capturesDifferingOnlyBetweenOldHashSamplesNeverSharePixels() = runBlocking {
        val count = AtomicInteger()
        val cache = CropBitmapCache(decode = { _, _, _, _, _, _ ->
            count.incrementAndGet()
            ImageBitmap(2, 2)
        })
        val a = ByteArray(8192)
        val b = a.copyOf().also { it[1] = 1 }
        assertNotSame(cache.get(a, 0f, 0f, 1f, 1f, 100), cache.get(b, 0f, 0f, 1f, 1f, 100))
        assertEquals(2, count.get())
    }

    @Test fun pixelBudgetEvictsOldCropsAndCancellationIsNotUnavailable() = runBlocking {
        val count = AtomicInteger()
        val cache = CropBitmapCache(maxPixels = 4, decode = { _, _, _, _, _, _ ->
            count.incrementAndGet()
            ImageBitmap(2, 2)
        })
        cache.get(byteArrayOf(1), 0f, 0f, 1f, 1f, 100)
        cache.get(byteArrayOf(2), 0f, 0f, 1f, 1f, 100)
        assertEquals(4L, cache.retainedPixels())
        cache.get(byteArrayOf(1), 0f, 0f, 1f, 1f, 100)
        assertEquals(3, count.get())
        val cancelled = CropBitmapCache(decode = { _, _, _, _, _, _ -> throw CancellationException("stop") })
        try {
            cancelled.get(byteArrayOf(1), 0f, 0f, 1f, 1f, 100)
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
    }
}
