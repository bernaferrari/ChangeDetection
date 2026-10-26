package com.bernaferrari.changedetection.browser

import com.bernaferrari.changedetection.diagnostics.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import javax.net.ssl.SSLException

class PageCaptureHealthTest {
    @Test fun earlyMainFailureSurvivesStartedAndFinishedAndRecoveryInvalidatesOldCapture() = runTest {
        val health = PageCaptureHealth()
        val url = "https://private.invalid/path?token=SECRET"
        health.begin(url)
        val old = health.epoch()
        health.request(url, true)
        val tls = health.ssl(url, 3, listOf(3, 1))
        health.started(url)
        health.finished()
        assertEquals(TlsRequestKind.MAIN_DOCUMENT, tls.requestKind)
        assertEquals(TlsUrlRelation.REQUESTED, tls.relation)
        assertFalse(tls.navigationStarted)
        assertRejected { health.validate(old) }
        health.begin(url)
        health.started(url)
        health.committed(url)
        health.finished()
        health.validate()
        assertTrue(runCatching { health.validate(old) }.exceptionOrNull() is CaptureNotReadyException)
    }

    @Test fun subresourceAndRedirectClassificationsUseRequestEvidenceNotUrlEquality() = runTest {
        val h = PageCaptureHealth()
        val page = "https://private.invalid/path?token=SECRET"
        val other = "https://other.invalid/private?cookie=PASSWORD"
        h.begin(page)
        h.started(page)
        h.committed(page)
        h.finished()
        h.request(other, false)
        val sub = h.ssl(other, 3, listOf(3, 0, 3, 99))
        assertEquals(TlsRequestKind.SUBRESOURCE, sub.requestKind)
        assertEquals(TlsUrlRelation.DIFFERENT_ORIGIN, sub.relation)
        assertEquals(listOf(0, 3), sub.errors)
        assertTrue(sub.committed && sub.finished)
        // Refused optional content does not invalidate the main document.
        assertFalse(sub.blocking)
        h.validate()
        assertEquals(1, h.blockedResources())
        h.begin(page)
        h.started(page)
        h.request(other, true)
        val redirect = h.ssl(other, 3, listOf(3))
        assertEquals(TlsRequestKind.MAIN_DOCUMENT, redirect.requestKind)
        assertEquals(TlsUrlRelation.DIFFERENT_ORIGIN, redirect.relation)
        assertFalse(redirect.committed)
        assertTrue(redirect.blocking)
        assertRejected { h.validate() }
        val json = Json.encodeToString(listOf(sub, redirect))
        for (secret in listOf("private.invalid", "other.invalid", "SECRET", "PASSWORD", "https://", "cookie", "token")) assertFalse(secret, json.contains(secret))
    }

    @Test fun unknownAndMixedRequestsStayUnknownAndBlockUntilThePageCommits() = runTest {
        val h = PageCaptureHealth()
        h.begin("https://main.invalid")
        val missing = h.ssl("https://missing.invalid", 3, listOf(3))
        assertEquals(TlsRequestKind.UNKNOWN, missing.requestKind)
        // A server redirect is never intercepted, so an unseen URL before commit may be the page itself.
        assertTrue(missing.blocking)
        h.request("https://mixed.invalid", true)
        h.request("https://mixed.invalid", false)
        assertEquals(TlsRequestKind.UNKNOWN, h.ssl("https://mixed.invalid", 3, listOf(3)).requestKind)
        assertRejected { h.validate() }
    }

    @Test fun optionalSubresourceRedirectDoesNotInvalidateCommittedDocument() = runTest {
        // The shared report: the page loaded and committed, then a resource it pulled in (redirected to a host with an
        // untrusted certificate, so never seen by shouldInterceptRequest) failed TLS 350 ms later.
        val h = PageCaptureHealth()
        val page = "https://news.invalid/"
        h.begin(page)
        h.request(page, true)
        h.started(page)
        h.committed(page)
        val epoch = h.epoch()
        h.request("https://cdn.invalid/pixel.gif", false)
        val late = h.ssl("https://tracker.invalid/pixel.gif", 3, listOf(3))
        h.finished()
        assertEquals(TlsRequestKind.UNKNOWN, late.requestKind)
        assertEquals(TlsUrlRelation.DIFFERENT_ORIGIN, late.relation)
        assertFalse(late.blocking)
        h.validate(epoch)
    }

    @Test fun anUnseenUrlDuringANewMainFrameNavigationBlocks() = runTest {
        val h = PageCaptureHealth()
        val page = "https://news.invalid/"
        h.begin(page)
        h.request(page, true)
        h.started(page)
        h.committed(page)
        // Script navigates; the request is seen, then its server redirect lands on an untrusted host.
        h.request("https://news.invalid/next", true)
        assertTrue(h.ssl("https://evil.invalid/next", 3, listOf(3)).blocking)
        assertRejected { h.validate() }
    }

    @Test fun boundsKeepBlockingFailuresAndLimitRefusedResources() = runTest {
        val h = PageCaptureHealth()
        h.begin("https://main.invalid")
        h.request("https://main.invalid", true)
        h.started("https://main.invalid")
        h.committed("https://main.invalid")
        repeat(500) {
            h.request("https://$it.invalid", false)
            assertFalse(h.ssl("https://$it.invalid", 3, listOf(3)).blocking)
        }
        assertEquals(500, h.blockedResources())
        assertEquals(8, h.observations().size)
        h.validate()
        // Main-frame evidence survives any number of sub-resources.
        h.request("https://main.invalid/again", true)
        assertTrue(h.ssl("https://main.invalid/again", 3, listOf(3)).blocking)
        assertTrue(h.observations().first().blocking)
        assertRejected { h.validate() }
    }

    @Test fun callbackAfterCaptureReadinessRevokesBaselineValidation() = runTest {
        val h = PageCaptureHealth()
        h.begin("https://main.invalid")
        h.started("https://main.invalid")
        h.finished()
        val epoch = h.epoch()
        h.validate(epoch)
        h.ssl("https://late.invalid", 3, listOf(3))
        assertRejected { h.validate(epoch) }
        h.started("https://late.invalid")
        assertRejected { h.validate(epoch) }
    }

    @Test fun refusedOptionalResourceDoesNotInvalidateTheDocument() = runTest {
        val h = PageCaptureHealth()
        h.begin("https://page.test")
        h.request("https://tracker.test/pixel", false)
        assertFalse(h.ssl("https://tracker.test/pixel", 3, listOf(3)).blocking)
        h.dependencies(listOf("https://page.test/hero.png"))
        h.validate()
    }

    @Test fun refusedWatchedResourceStillRejectsTheCapture() = runTest {
        val h = PageCaptureHealth()
        h.begin("https://page.test")
        h.request("https://cdn.test/hero.png", false)
        h.ssl("https://cdn.test/hero.png", 3, listOf(3))
        h.dependencies(listOf("https://cdn.test/hero.png"))
        val error = runCatching { h.validate() }.exceptionOrNull()
        assertTrue(error is CaptureNotReadyException)
        assertEquals(CaptureIssue.RESOURCE_FAILED, (error as CaptureNotReadyException).issue)
    }

    private suspend fun assertRejected(block: suspend () -> Unit) {
        try {
            block()
            fail("Expected strict TLS rejection")
        } catch (_: SSLException) { }
    }
}
