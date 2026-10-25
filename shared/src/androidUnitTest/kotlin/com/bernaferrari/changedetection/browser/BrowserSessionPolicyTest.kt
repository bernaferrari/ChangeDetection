package com.bernaferrari.changedetection.browser

import com.bernaferrari.changedetection.error.NetworkError
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class BrowserSessionPolicyTest {
    @Test fun aRedirectIntoVisibleSignInDoesNotBecomeMonitoredContent() {
        val result = runCatching { validateBrowserSessionState("https://example.test/account", "[\"https://example.test/login\",true]") }
        assertTrue(result.exceptionOrNull() is NetworkError)
        assertEquals(401, (result.exceptionOrNull() as NetworkError).code)
    }
    @Test fun anIntentionallyWatchedLoginPageIsNotRejected() {
        validateBrowserSessionState("https://example.test/login", "[\"https://example.test/login\",true]")
    }
    @Test fun ordinaryRedirectAndAndroidWrappedCallbacksRemainSupported() {
        validateBrowserSessionState("https://example.test/old", Json.encodeToString("[\"https://example.test/new\",false]"))
    }
    @Test fun sameAddressLoginWallIsExplicitlyNotGuessed() {
        validateBrowserSession("https://example.test/account", "https://example.test/account", true)
    }
}
