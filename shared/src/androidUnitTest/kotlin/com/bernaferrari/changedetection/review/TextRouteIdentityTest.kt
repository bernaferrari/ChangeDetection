package com.bernaferrari.changedetection.review

import com.bernaferrari.changedetection.repo.CaptureMode
import com.bernaferrari.changedetection.repo.ColorGroup
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.source.SitesDataSource
import com.bernaferrari.changedetection.repo.source.SitesRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/** Review-only proof: kept out of the live production checkout. */
class TextRouteIdentityTest {
    @Test fun androidRenderedTextRoutesMustNotBeRejectedAsDuplicates() = runTest {
        val source = mock<SitesDataSource>()
        val saved = Site("Route A", "https://example.test/#/product/a", 0L, "route-a", ColorGroup(0, 0), true, false).copy(captureMode = CaptureMode.TEXT)
        whenever(source.getSites()).thenReturn(listOf(saved))
        assertNull("Android TEXT uses a browser, so route B is a different page", SitesRepository(source).findDuplicate("https://example.test/#/product/b", CaptureMode.TEXT))
    }

    @Test fun screenshotBrowserRoutesAlreadyRemainDistinctControl() = runTest {
        val source = mock<SitesDataSource>()
        val saved = Site("Route A", "https://example.test/#/product/a", 0L, "route-a", ColorGroup(0, 0), true, false).copy(captureMode = CaptureMode.SCREENSHOT)
        whenever(source.getSites()).thenReturn(listOf(saved))
        assertNull(SitesRepository(source).findDuplicate("https://example.test/#/product/b", CaptureMode.SCREENSHOT))
    }
}
