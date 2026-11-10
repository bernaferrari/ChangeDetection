package com.bernaferrari.changedetection.ui.kit

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.size.Dimension
import coil3.size.Size
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapshotThumbnailFetcherTest {
    private val png: ByteArray = ByteArrayOutputStream().also { ImageIO.write(BufferedImage(64, 256, BufferedImage.TYPE_INT_RGB), "png", it) }.toByteArray()

    private fun request(snapId: String, read: suspend (String) -> ByteArray?) = ImageRequest.Builder(PlatformContext.INSTANCE)
        .data(SnapshotThumbnail(snapId))
        .fetcherFactory(SnapshotThumbnailFetcher.Factory(read))
        .memoryCacheKey("top-$snapId-32")
        .size(Size(Dimension(32), Dimension.Undefined))
        .build()

    @Test fun aCachedThumbnailIsNotReadFromStorageAgain() = runBlocking {
        val loader = ImageLoader(PlatformContext.INSTANCE)
        val reads = mutableListOf<String>()
        val read: suspend (String) -> ByteArray? = { id ->
            reads += id
            png
        }
        try {
            assertTrue(loader.execute(request("snap-1", read)) is SuccessResult)
            assertTrue(loader.execute(request("snap-1", read)) is SuccessResult)
            assertEquals("the second request must come from the memory cache", listOf("snap-1"), reads)

            assertTrue(loader.execute(request("snap-2", read)) is SuccessResult)
            assertEquals(listOf("snap-1", "snap-2"), reads)
        } finally {
            loader.shutdown()
        }
    }

    @Test fun aSnapshotThatCannotBeReadIsAnErrorResultNotACrash() = runBlocking {
        val loader = ImageLoader(PlatformContext.INSTANCE)
        try {
            assertTrue(loader.execute(request("missing") { null }) is ErrorResult)
            assertTrue(loader.execute(request("broken") { error("file is gone") }) is ErrorResult)
        } finally {
            loader.shutdown()
        }
    }
}
