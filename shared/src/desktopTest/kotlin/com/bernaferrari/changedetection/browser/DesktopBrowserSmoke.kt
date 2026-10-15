package com.bernaferrari.changedetection.browser

import com.bernaferrari.changedetection.ui.comparison.secureSnapshotDocument
import com.bernaferrari.changedetection.detection.selectableTextDocument
import com.bernaferrari.changedetection.detection.RegionViewport
import com.bernaferrari.changedetection.detection.regionViewportFromNavigation
import com.bernaferrari.changedetection.detection.selectionDocumentWithInitialRegion
import com.bernaferrari.changedetection.detection.selectionDocumentWithWatchedAreas
import com.bernaferrari.changedetection.detection.selectionFromNavigation
import com.multiplatform.webview.web.WebContent
import java.nio.file.Files
import java.io.File
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.swing.JFrame
import javax.swing.SwingUtilities
import kotlin.system.exitProcess
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Explicit native integration task, separate from the headless Compose test suite. */
object DesktopBrowserSmoke {
    private suspend fun verifyLiveChooser() {
        val server = com.sun.net.httpserver.HttpServer.create(java.net.InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val bytes = "<title>Live chooser</title><p>Current page</p>".toByteArray()
            exchange.responseHeaders.set("Content-Type", "text/html")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        val client = checkNotNull(dev.datlag.kcef.KCEF.newClientOrNullBlocking()) {
            "Live chooser cannot create a KCEF client after runtime initialization"
        }
        val loaded = CompletableDeferred<Unit>()
        client.addLoadHandler(object : org.cef.handler.CefLoadHandlerAdapter() {
            override fun onLoadEnd(browser: org.cef.browser.CefBrowser, frame: org.cef.browser.CefFrame, httpStatusCode: Int) {
                if (frame.isMain) loaded.complete(Unit)
            }
        })
        var browser: dev.datlag.kcef.KCEFBrowser? = null
        var window: JFrame? = null
        try {
            SwingUtilities.invokeAndWait {
                browser = client.createBrowser("http://127.0.0.1:${server.address.port}/catalog")
                window = JFrame("Live chooser smoke check").apply {
                    add(requireNotNull(browser).uiComponent)
                    setSize(480, 360)
                    isVisible = true
                }
            }
            withTimeout(15_000) { loaded.await() }
            val url = withTimeout(5_000) { requireNotNull(browser).evaluateJavaScript("(function(){ history.pushState({}, '', '/product/b'); return window.location.href; })()") }
            check(url?.contains("/product/b") == true) { "Live chooser could not report its current SPA address: $url" }
            println("PASS KCEF live chooser loads, runs JavaScript and reports SPA navigation")
        } finally {
            SwingUtilities.invokeAndWait {
                browser?.dispose()
                window?.dispose()
                client.dispose()
            }
            server.stop(0)
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        var result = 1
        val directory = Files.createTempDirectory("change-detection-browser-smoke").toFile()
        try {
            check(org.cef.CefApp::class.java.module.name == "jcef") { "Run with a JetBrains Runtime containing JCEF" }
            SwingUtilities.invokeAndWait { }
            runBlocking {
                DesktopBrowserRuntime.start(directory)
                val state = withTimeout(15_000) {
                    DesktopBrowserRuntime.state.first { it == DesktopBrowserRuntime.State.Ready || it == DesktopBrowserRuntime.State.Failed }
                }
                check(state == DesktopBrowserRuntime.State.Ready) { "Browser runtime failed to initialize" }
                verifyLiveChooser()
                var savedViewport = RegionViewport()
                repeat(5) { index ->
                    val expected = "Saved version ${index + 1} — café"
                    val loaded = CompletableDeferred<Unit>()
                    val loadCount = java.util.concurrent.atomic.AtomicInteger()
                    val longContent = if (index >= 3) (1..80).joinToString("") { "<p>Long page paragraph $it</p>" } else ""
                    val html = "<html><head><style>.schedule { background: #f4f1e6; padding: 28px; } .schedule h1 { font-family: Georgia, serif; font-weight: 400; } #stock { border-left: 3px solid #31442b; padding-left: 12px; }</style><style media='print'>body { display: none; }</style></head><body class='schedule'><main><h1>$expected</h1>$longContent<p id='stock'>Available: 3</p><p id='noise'>Changing countdown</p><script>document.body.textContent='UNSAFE'</script></main></body></html>"
                    val selection = if (index >= 2) {
                        val document = selectableTextDocument(html)
                        selectionDocumentWithWatchedAreas(document, listOf(document.regions.first { it.tag == "h1" }.selector), listOf("#noise"))
                    } else {
                        null
                    }
                    val selected = CompletableDeferred<String>()
                    val scrolled = CompletableDeferred<RegionViewport>()
                    val content = WebContent.Data(if (index == 4) selectionDocumentWithInitialRegion(requireNotNull(selection), "#stock", savedViewport) else selection?.selectionHtml ?: secureSnapshotDocument(html))
                    lateinit var session: DesktopBrowserSession
                    lateinit var window: JFrame
                    SwingUtilities.invokeAndWait {
                        session = DesktopBrowserSession.create(content, disablePopups = true, onNavigationRequest = { url ->
                            if (selection != null) {
                                regionViewportFromNavigation(url)?.takeIf { it.y > 0 }?.let { scrolled.complete(it) }
                                selectionFromNavigation(url, selection.regions)?.let { selected.complete(it.selector) }
                                url.startsWith("data:text/html") || url == "about:blank"
                            } else {
                                true
                            }
                        }) {
                            loadCount.incrementAndGet()
                            loaded.complete(Unit)
                        }
                        window = JFrame("Change Detection browser smoke check").apply {
                            add(session.browser.uiComponent)
                            setSize(480, 360)
                            isVisible = true
                        }
                    }
                    try {
                        withTimeout(15_000) { loaded.await() }
                        val text = CompletableDeferred<String>()
                        session.browser.getText { text.complete(it) }
                        val actual = withTimeout(5_000) { text.await() }
                        check(expected in actual && "UNSAFE" !in actual) { "Saved content was not rendered correctly" }
                        session.browser.devToolsClient.use { devTools ->
                            val expression = "getComputedStyle(document.body).backgroundColor === 'rgb(244, 241, 230)' && getComputedStyle(document.body).paddingLeft === '28px' && getComputedStyle(document.querySelector('h1')).fontFamily.includes('Georgia') && getComputedStyle(document.querySelector('#stock')).borderLeftWidth === '3px' && document.body.offsetHeight >= window.innerHeight"
                            val params = kotlinx.serialization.json.buildJsonObject { put("expression", kotlinx.serialization.json.JsonPrimitive(expression)) }.toString()
                            val response = devTools.executeDevToolsMethod("Runtime.evaluate", params).get(5, TimeUnit.SECONDS)
                            check(Json.parseToJsonElement(response).jsonObject.getValue("result").jsonObject.getValue("value").jsonPrimitive.content == "true") { "Self-contained source styling or print media isolation was lost" }
                        }
                        println("PASS source layout, font, body class and media-scoped stylesheet rendering")
                        if (index == 3) {
                            session.browser.devToolsClient.use { devTools ->
                                devTools.executeDevToolsMethod("Runtime.evaluate", "{\"expression\":\"window.scrollTo(0, 600)\"}").get(5, TimeUnit.SECONDS)
                            }
                            savedViewport = withTimeout(5_000) { scrolled.await() }
                            check(savedViewport.y == 600) { "Scroll position did not reach the native bridge" }
                        }
                        if (index == 4) {
                            val restored = withTimeout(5_000) { scrolled.await() }
                            check(restored == savedViewport) { "Reloaded browser lost the viewport: $restored versus $savedViewport" }
                            println("PASS native scroll reporting and restored viewport after browser recreation")
                        }
                        if (selection != null) {
                            val candidate = selection.regions.indexOfFirst { it.selector == "#stock" }
                            check(candidate >= 0)
                            session.browser.devToolsClient.use { devTools ->
                                val expression = "document.querySelector('[data-cd-region=\"$candidate\"]').click()"
                                val params = kotlinx.serialization.json.buildJsonObject { put("expression", kotlinx.serialization.json.JsonPrimitive(expression)) }.toString()
                                devTools.executeDevToolsMethod("Runtime.evaluate", params).get(5, TimeUnit.SECONDS)
                            }
                            check(withTimeout(5_000) { selected.await() } == "#stock") { "Rendered selection did not reach native confirmation state" }
                            session.browser.devToolsClient.use { devTools ->
                                val expression = "document.querySelectorAll('[data-cd-watched][aria-pressed=true]').length === 1 && document.querySelectorAll('[data-cd-selected][aria-pressed=true]').length === 1 && getComputedStyle(document.querySelector('#noise')).textDecorationLine.includes('line-through') && document.querySelector('#noise').offsetHeight > 0"
                                val params = kotlinx.serialization.json.buildJsonObject { put("expression", kotlinx.serialization.json.JsonPrimitive(expression)) }.toString()
                                val response = devTools.executeDevToolsMethod("Runtime.evaluate", params).get(5, TimeUnit.SECONDS)
                                check(Json.parseToJsonElement(response).jsonObject.getValue("result").jsonObject.getValue("value").jsonPrimitive.content == "true") { "Retained area lost its highlight after selecting another area" }
                            }
                            println("PASS trusted selection script, native navigation bridge and retained-area highlight and ignored-area geometry")
                            if (index == 3) {
                                fun evaluate(expression: String): String = session.browser.devToolsClient.use { devTools ->
                                    val params = kotlinx.serialization.json.buildJsonObject { put("expression", kotlinx.serialization.json.JsonPrimitive(expression)) }.toString()
                                    devTools.executeDevToolsMethod("Runtime.evaluate", params).get(5, TimeUnit.SECONDS)
                                }
                                evaluate("window.__originalBody = document.body; document.querySelector('#stock').focus({preventScroll:true}); window.scrollTo(0,600)")
                                val updated = selectionDocumentWithWatchedAreas(selectableTextDocument(html), listOf("#noise"), listOf("#stock"))
                                val update = com.bernaferrari.changedetection.detection.regionHighlightUpdate(updated, "#noise")
                                SwingUtilities.invokeAndWait { session.browser.executeJavaScript(update.script(), session.browser.url, 0) }
                                withTimeout(5_000) {
                                    while (true) {
                                        val response = evaluate("document.querySelector('#stock').hasAttribute('data-cd-ignored') && !document.querySelector('#noise').hasAttribute('data-cd-ignored') && document.querySelector('#noise').hasAttribute('data-cd-watched') && document.querySelector('#noise').hasAttribute('data-cd-selected') && !document.querySelector('h1').hasAttribute('data-cd-watched') && document.activeElement.id === 'stock' && window.scrollY === 600 && window.__originalBody === document.body")
                                        if (Json.parseToJsonElement(response).jsonObject.getValue("result").jsonObject.getValue("value").jsonPrimitive.content == "true") break
                                        kotlinx.coroutines.delay(50)
                                    }
                                }
                                check(loadCount.get() == 1) { "Highlight update reloaded the document" }
                                println("PASS live native highlight update preserves document, focus and scroll without reload")
                            }
                        }
                        if (index == 0 || index == 2) {
                            val capture = session.browser.devToolsClient.use { devTools ->
                                devTools.executeDevToolsMethod("Page.captureScreenshot", "{\"format\":\"png\"}").get(5, TimeUnit.SECONDS)
                            }
                            val bytes = Base64.getDecoder().decode(Json.parseToJsonElement(capture).jsonObject.getValue("data").jsonPrimitive.content)
                            val image = javax.imageio.ImageIO.read(bytes.inputStream())
                            check(image.width > 100 && image.height > 100) { "Native rendered capture is empty" }
                            val output = File(System.getProperty("changedetection.browserSmoke.output"))
                            output.mkdirs()
                            File(output, if (selection == null) "native-browser-smoke.png" else "native-region-selection.png").writeBytes(bytes)
                        }
                        println("PASS saved HTML loaded and read back, session ${index + 1}")
                    } finally {
                        SwingUtilities.invokeAndWait {
                            session.close()
                            session.close() // Disposal must be idempotent.
                            window.dispose()
                        }
                    }
                }
            }
            result = 0
        } catch (failure: Throwable) {
            failure.printStackTrace()
        } finally {
            org.cef.CefApp.getInstanceIfAny()?.dispose()
            directory.deleteRecursively()
        }
        exitProcess(result)
    }
}
