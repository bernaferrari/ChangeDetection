package com.bernaferrari.changedetection.browser

import android.webkit.CookieManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Signing out in Settings must leave no cookie behind for any site. */
@RunWith(AndroidJUnit4::class)
class SiteSessionsTest {
    @Test
    fun clearingSiteSessionsRemovesEveryCookie() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val stored = CountDownLatch(1)
        instrumentation.runOnMainSync {
            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setCookie("https://one.example.test", "session=first; Max-Age=3600")
                setCookie("https://two.example.test", "session=second; Max-Age=3600") { stored.countDown() }
            }
        }
        assertTrue(stored.await(10, TimeUnit.SECONDS))
        assertNotNull(CookieManager.getInstance().getCookie("https://one.example.test"))
        assertNotNull(CookieManager.getInstance().getCookie("https://two.example.test"))

        val cleared = CountDownLatch(1)
        instrumentation.runOnMainSync { clearSiteSessions(instrumentation.targetContext) { cleared.countDown() } }

        assertTrue("the completion callback must run", cleared.await(10, TimeUnit.SECONDS))
        assertNull(CookieManager.getInstance().getCookie("https://one.example.test"))
        assertNull(CookieManager.getInstance().getCookie("https://two.example.test"))
    }
}
