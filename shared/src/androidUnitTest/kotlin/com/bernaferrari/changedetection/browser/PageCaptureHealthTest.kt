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
        assertRejected { health.validate(old) }
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
        assertRejected { h.validate() }
        h.begin(page)
        h.started(page)
        h.request(other, true)
        val redirect = h.ssl(other, 3, listOf(3))
        assertEquals(TlsRequestKind.MAIN_DOCUMENT, redirect.requestKind)
        assertEquals(TlsUrlRelation.DIFFERENT_ORIGIN, redirect.relation)
        assertFalse(redirect.committed)
        assertRejected { h.validate() }
        val json = Json.encodeToString(listOf(sub, redirect))
        for (secret in listOf("private.invalid", "other.invalid", "SECRET", "PASSWORD", "https://", "cookie", "token")) assertFalse(secret, json.contains(secret))
    }

    @Test fun unknownAndMixedRequestsStayUnknownAndBoundsAreEnforced() = runTest {
        val h = PageCaptureHealth()
        h.begin("https://main.invalid")
        assertEquals(TlsRequestKind.UNKNOWN, h.ssl("https://missing.invalid", 3, listOf(3)).requestKind)
        h.request("https://mixed.invalid", true)
        h.request("https://mixed.invalid", false)
        assertEquals(TlsRequestKind.UNKNOWN, h.ssl("https://mixed.invalid", 3, listOf(3)).requestKind)
        repeat(500) {
            h.request("https://$it.invalid", false)
            h.ssl("https://$it.invalid", 3, listOf(3))
        }
        assertEquals(8, h.observations().size)
        assertEquals(TlsRequestKind.UNKNOWN, h.ssl("https://499.invalid", 3, listOf(3)).requestKind)
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

    private suspend fun assertRejected(block: suspend () -> Unit) {
        try {
            block()
            fail("Expected strict TLS rejection")
        } catch (_: SSLException) { }
    }
}
