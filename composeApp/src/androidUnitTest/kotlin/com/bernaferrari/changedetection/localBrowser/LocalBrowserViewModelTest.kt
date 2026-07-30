/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.localBrowser

import com.bernaferrari.changedetection.repo.ContentTypeInfo
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.Result
import com.bernaferrari.changedetection.repo.source.SnapsDataSource
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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

@OptIn(ExperimentalCoroutinesApi::class)
class LocalBrowserViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

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
        advanceUntilIdle()
        repeat(10) {
            if (!viewModel.state.value.isLoading) return@repeat
            advanceUntilIdle()
            Thread.sleep(10)
        }

        val state = viewModel.state.value
        assertFalse("State after fetch: $state", state.isLoading)
        assertEquals(originalHtml, state.content)
        assertEquals("text/html", state.mimeType)
        assertEquals(charsetName, state.charset)
    }

    private class FakeSnapsDataSource(
        private val snap: Snap?,
        private val content: ByteArray,
    ) : SnapsDataSource {
        override suspend fun getContentTypeInfo(siteId: String): List<ContentTypeInfo> = emptyList()

        override suspend fun getMostRecentSnap(siteId: String): Snap? = null

        override suspend fun getAllMostRecentSnaps(): Map<String, Snap> = emptyMap()

        override fun getSnaps(siteId: String) = flowOf(emptyList<Snap>())

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

        override suspend fun saveSnap(
            snap: Snap,
            content: ByteArray,
            threshold: Int,
        ): Result<Snap> = Result.Error()

        override suspend fun deleteAllSnaps(siteId: String) = Unit

        override suspend fun deleteSnap(snapId: String) = Unit

        override suspend fun deleteSnapsForSiteIdAndContentType(
            siteId: String,
            contentType: String,
        ) = Unit

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
