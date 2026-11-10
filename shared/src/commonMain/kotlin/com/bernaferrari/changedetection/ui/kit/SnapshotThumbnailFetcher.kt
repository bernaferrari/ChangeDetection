package com.bernaferrari.changedetection.ui.kit

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import okio.Buffer

/** Names a saved screenshot to the image loader without holding its bytes. */
internal data class SnapshotThumbnail(val snapId: String)

/**
 * Reads a saved screenshot only when the image loader actually has to decode it. A thumbnail already in the
 * loader's memory cache is served without touching storage, and no composable keeps the full file in memory.
 */
internal class SnapshotThumbnailFetcher(
    private val thumbnail: SnapshotThumbnail,
    private val options: Options,
    private val read: suspend (snapId: String) -> ByteArray?,
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        val bytes = read(thumbnail.snapId) ?: return null
        return SourceFetchResult(
            source = ImageSource(source = Buffer().apply { write(bytes) }, fileSystem = options.fileSystem),
            mimeType = null,
            dataSource = DataSource.DISK,
        )
    }

    class Factory(private val read: suspend (snapId: String) -> ByteArray?) : Fetcher.Factory<SnapshotThumbnail> {
        override fun create(data: SnapshotThumbnail, options: Options, imageLoader: ImageLoader): Fetcher = SnapshotThumbnailFetcher(data, options, read)
    }
}
