package com.bernaferrari.changedetection.localBrowser

import com.bernaferrari.changedetection.repo.ContentTypeInfo
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.SnapsDataSource
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.nio.charset.Charset
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class LocalBrowserViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test fun `switching snapshots ignores the cancelled load's error`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = mock<SnapsRepository>()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        doSuspendableAnswer {
            withContext(NonCancellable) {
                entered.complete(Unit)
                release.await()
                throw java.io.IOException("The old snapshot read failed")
            }
        }.whenever(repository).getSnapById("old")
        whenever(repository.getSnapById("new")).thenReturn(snapshot("new", "text/plain"))
        whenever(repository.getSnapContent("new")).thenReturn("New content".encodeToByteArray())
        val model = LocalBrowserViewModel(repository)
        try {
            model.fetchData("old")
            entered.await()
            val oldLoads = model.viewModelScope.coroutineContext.job.children.toList()
            model.fetchData("new")
            model.state.first { it.content == "New content" }
            release.complete(Unit)
            oldLoads.joinAll()
            assertEquals("New content", model.state.value.content)
            assertFalse(model.state.value.hasError)
        } finally {
            release.complete(Unit)
            model.viewModelScope.coroutineContext.job.cancelAndJoin()
        }
    }

    @Test fun `reset does not become an error when the previous read is cancelled`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = mock<SnapsRepository>()
        val entered = CompletableDeferred<Unit>()
        whenever(repository.getSnapById("old")).doSuspendableAnswer {
            entered.complete(Unit)
            awaitCancellation()
        }
        val model = LocalBrowserViewModel(repository)
        try {
            model.fetchData("old")
            entered.await()
            val oldLoads = model.viewModelScope.coroutineContext.job.children.toList()
            model.reset()
            oldLoads.joinAll()
            assertEquals(BrowserState(), model.state.value)
        } finally {
            model.viewModelScope.coroutineContext.job.cancelAndJoin()
        }
    }

    @Test fun `retry clears the error while the next read is loading`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = mock<SnapsRepository>()
        var attempts = 0
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        doSuspendableAnswer {
            if (++attempts == 1) throw java.io.IOException("Retry this read")
            entered.complete(Unit)
            release.await()
            snapshot("page", "text/plain")
        }.whenever(repository).getSnapById("page")
        whenever(repository.getSnapContent("page")).thenReturn("Recovered content".encodeToByteArray())
        val model = LocalBrowserViewModel(repository)
        try {
            model.fetchData("page")
            model.state.first { it.hasError }
            model.fetchData("page")
            entered.await()
            assertTrue(model.state.value.isLoading)
            assertFalse("Retry must show loading rather than the old error", model.state.value.hasError)
            assertNull(model.state.value.bytes)
            release.complete(Unit)
            model.state.first { it.content == "Recovered content" }
        } finally {
            release.complete(Unit)
            model.viewModelScope.coroutineContext.job.cancelAndJoin()
        }
    }

    @Test fun `structured text snapshots are readable in the saved viewer`() = runTest(mainDispatcherRule.dispatcher) {
        val content = "Available: café, €19"
        for (type in listOf("application/json", "application/ld+json", "application/x-ndjson", "application/json-seq", "application/xml", "application/rss+xml", "application/xhtml+xml", "application/javascript")) {
            val repository = SnapsRepository(FakeSnapsDataSource(snapshot("page", type), content.encodeToByteArray()))
            val model = LocalBrowserViewModel(repository)
            try {
                model.fetchData("page")
                model.state.first { !it.isLoading && it.bytes != null }
                assertEquals(type, content, model.state.value.content)
            } finally {
                model.viewModelScope.coroutineContext.job.cancelAndJoin()
            }
        }
    }

    private fun snapshot(id: String, type: String) = Snap(id, "site", 1L, type, "UTF-8", 24)

    @Test
    fun `fetchData decodes snapshot using stored charset`() = runTest(mainDispatcherRule.dispatcher) {
        val charsetName = "ISO-8859-1"
        val originalHtml = "Olá, mundo ç à"
        val bytes = originalHtml.toByteArray(Charset.forName(charsetName))

        val snap =
            Snap(
                snapId = "snap-1",
                siteId = "site",
                timestamp = 0L,
                contentType = "text/html; charset=$charsetName",
                contentCharset = charsetName,
                contentSize = bytes.size,
            )

        val repository = SnapsRepository(FakeSnapsDataSource(snap, bytes))
        assertEquals(snap, repository.getSnapById("snap-1"))
        val viewModel = LocalBrowserViewModel(repository)

        viewModel.fetchData("snap-1")
        val state = viewModel.state.first { it.bytes != null && !it.isLoading }
        assertFalse("State after fetch: $state", state.isLoading)
        assertEquals(originalHtml, state.content)
        assertEquals("text/html", state.mimeType)
        assertEquals(charsetName, state.charset)
        viewModel.viewModelScope.coroutineContext.job.cancelAndJoin()
    }

    private class FakeSnapsDataSource(
        private val snap: Snap?,
        private val content: ByteArray,
    ) : SnapsDataSource {
        override fun observeStorageBytes() = kotlinx.coroutines.flow.flowOf(0L)
        override fun observeOlderVersionBytes() = kotlinx.coroutines.flow.flowOf(0L)
        override suspend fun clearOlderVersions() = Unit
        override suspend fun deleteSiteWithSnaps(siteId: String) = Unit
        override suspend fun getContentTypeInfo(siteId: String): List<ContentTypeInfo> = emptyList()

        override suspend fun getMostRecentSnap(siteId: String): Snap? = null

        override suspend fun getAllMostRecentSnaps(): Map<String, Snap> = emptyMap()

        override fun getSnaps(siteId: String) = flowOf(emptyList<Snap>())

        override fun observeSnapsTable() = flowOf(false)

        override fun getSnapsFiltered(
            siteId: String,
            filter: String,
        ) = flowOf(emptyList<Snap>())

        override suspend fun getAllSnapsForSite(siteId: String): List<Snap> = emptyList()

        override suspend fun getSnapById(snapId: String): Snap? = snap?.takeIf { it.snapId == snapId }

        override suspend fun getSnapContent(snapId: String): ByteArray = content

        override suspend fun getLastNSnapsForSiteId(
            siteId: String,
            limit: Int,
        ): List<Snap>? = emptyList()

        override suspend fun getSnapPair(
            originalId: String,
            newId: String,
        ): Pair<Pair<Snap, ByteArray>, Pair<Snap, ByteArray>>? = null

        override suspend fun deleteSnap(snapId: String) = Unit

        override suspend fun pruneSnaps(siteId: String) = Unit

        override suspend fun getSingleSnapPair(id: String): ByteArray = ByteArray(0)

        override suspend fun getLastContentHash(siteId: String): String? = null

        override suspend fun getLastProcessedContentHash(siteId: String): String? = null
    }

    class MainDispatcherRule(
        val dispatcher: TestDispatcher = kotlinx.coroutines.test.StandardTestDispatcher(),
    ) : TestWatcher() {
        override fun starting(description: Description) {
            Dispatchers.setMain(dispatcher)
        }

        override fun finished(description: Description) {
            Dispatchers.resetMain()
        }
    }
}
