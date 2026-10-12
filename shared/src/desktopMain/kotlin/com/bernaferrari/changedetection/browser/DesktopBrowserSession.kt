package com.bernaferrari.changedetection.browser

import com.multiplatform.webview.web.WebContent
import org.cef.CefApp
import org.cef.CefClient
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLifeSpanHandlerAdapter
import org.cef.handler.CefLoadHandlerAdapter
import java.util.Base64
import java.util.concurrent.atomic.AtomicBoolean

/** Owns the native client and browser together, including partially failed construction. */
internal class DesktopBrowserSession private constructor(
    private val client: CefClient,
    val browser: CefBrowser,
) : AutoCloseable {
    private val closed = AtomicBoolean(false)

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        try {
            browser.close(true)
        } finally {
            client.dispose()
        }
    }

    companion object {
        fun create(content: WebContent, disablePopups: Boolean, onNavigationRequest: ((String) -> Boolean)? = null, onLoaded: () -> Unit): DesktopBrowserSession {
            val client = CefApp.getInstance().createClient()
            try {
                if (onNavigationRequest != null) {
                    client.addRequestHandler(object : org.cef.handler.CefRequestHandlerAdapter() {
                        override fun onBeforeBrowse(browser: CefBrowser?, frame: CefFrame?, request: org.cef.network.CefRequest?, userGesture: Boolean, isRedirect: Boolean): Boolean = request?.url?.let { !onNavigationRequest(it) } ?: true
                    })
                }
                client.addLoadHandler(object : CefLoadHandlerAdapter() {
                    override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                        if (frame?.isMain == true) onLoaded()
                    }
                })
                if (disablePopups) {
                    client.addLifeSpanHandler(object : CefLifeSpanHandlerAdapter() {
                        override fun onBeforePopup(browser: CefBrowser?, frame: CefFrame?, targetUrl: String?, targetFrameName: String?): Boolean = true
                    })
                }
                val url = when (content) {
                    is WebContent.Data -> "data:text/html;charset=utf-8;base64," + Base64.getEncoder().encodeToString(content.data.toByteArray(Charsets.UTF_8))
                    is WebContent.Url -> content.url
                    else -> error("Unsupported browser content")
                }
                val browser = client.createBrowser(url, false, false)
                val session = DesktopBrowserSession(client, browser)
                try {
                    browser.createImmediately()
                } catch (failure: Throwable) {
                    browser.close(true)
                    throw failure
                }
                return session
            } catch (failure: Throwable) {
                client.dispose()
                throw failure
            }
        }
    }
}
