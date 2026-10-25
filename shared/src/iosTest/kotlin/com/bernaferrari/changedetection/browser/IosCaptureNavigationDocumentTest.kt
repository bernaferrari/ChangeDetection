@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.bernaferrari.changedetection.browser

import kotlinx.cinterop.*
import kotlinx.cinterop.ByteVar
import kotlinx.coroutines.*
import platform.CoreGraphics.CGRectMake
import platform.Foundation.*
import platform.WebKit.*
import platform.darwin.*
import platform.posix.*
import kotlin.concurrent.AtomicReference
import kotlin.coroutines.resume
import kotlin.test.*

/** Real WebKit contracts; run the test binary inside a simulator app bundle. */
class IosCaptureNavigationDocumentTest {
    @Test fun requestsRefreshContentWithoutDiscardingTheBrowserSession() = runWebKitTest {
        val server = CaptureServer()
        val first = browser()
        val second = browser()
        try {
            val url = "http://127.0.0.1:${server.port}/page"
            assertEquals(NSURLRequestReloadIgnoringLocalCacheData, captureRequest(NSURL.URLWithString(url)!!).cachePolicy)
            IosPageCapture.loadPage(first, url)
            assertEquals("Original", evaluate(first, "document.body.textContent"))
            assertTrue(evaluate(first, "document.cookie").contains("captureSession=retained"))
            server.reply.value = Reply(body = "Fresh content")
            IosPageCapture.loadPage(second, url)
            assertEquals("Fresh content", evaluate(second, "document.body.textContent"))
            assertEquals(2, server.requests.value.size, "A cacheable page must reach the server on every check")
            assertTrue(server.requests.value.last().contains("captureSession=retained"), "Freshness must preserve login cookies")
        } finally {
            first.stopLoading()
            second.stopLoading()
            server.close()
        }
    }

    @Test fun mainDocumentErrorsFailTheCheckButSubresourceErrorsDoNot() = runWebKitTest {
        val server = CaptureServer()
        val web = browser()
        val recovered = browser()
        try {
            val url = "http://127.0.0.1:${server.port}/page"
            server.reply.value = Reply(status = "503 Service Unavailable", body = "Temporarily unavailable")
            val error = assertFailsWith<com.bernaferrari.changedetection.error.NetworkError> { IosPageCapture.loadPage(web, url) }
            assertEquals(503, error.code)
            assertTrue(error.message.orEmpty().contains("HTTP 503"))
            server.reply.value = Reply(body = "Recovered<img src='/broken.png'>")
            // Every production capture owns a fresh WebView, including recovery after a failed load.
            web.stopLoading()
            IosPageCapture.loadPage(recovered, url)
            assertEquals("Recovered", evaluate(recovered, "document.body.textContent"))
            assertTrue(server.requests.value.any { "GET /broken.png " in it })
        } finally {
            web.stopLoading()
            recovered.stopLoading()
            server.close()
        }
    }

    @Test fun textChecksRetainScriptCreatedRegionsAndBrowserCookies() = runWebKitTest {
        val server = CaptureServer()
        try {
            server.reply.value = Reply(body = "<script>setTimeout(function(){var p=document.createElement('p');p.id='watched';p.textContent='Available 3';document.body.appendChild(p)},350)</script>")
            val url = "http://127.0.0.1:${server.port}/page"
            val first = timedCapture { IosPageCapture.capturePage(url, 0, only = "#watched", text = true) }
            val second = timedCapture { IosPageCapture.capturePage(url, 0, only = "#watched", text = true) }
            assertEquals("text/html", first.contentType)
            val region = com.bernaferrari.changedetection.detection.TextRegion("#watched")
            assertTrue(region.extractHtml(first.bytes.decodeToString(), true).contains("Available 3"))
            assertEquals(com.bernaferrari.changedetection.util.ContentHasher.detectionHash(first.bytes, first.contentType, com.bernaferrari.changedetection.repo.CaptureMode.TEXT), com.bernaferrari.changedetection.util.ContentHasher.detectionHash(second.bytes, second.contentType, com.bernaferrari.changedetection.repo.CaptureMode.TEXT))
            assertTrue(server.requests.value.last().contains("captureSession=retained"))
        } finally {
            server.close()
        }
    }

    @Test fun metadataBelongsToItsOwnCaptureRatherThanTheMostRecentGlobalPage() = runWebKitTest {
        val server = CaptureServer()
        try {
            val url = "http://127.0.0.1:${server.port}/page"
            server.reply.value = Reply(body = "<script>document.title='First page'</script><p>First content</p>")
            val first = timedCapture { IosPageCapture.capturePage(url, 0, text = true) }
            server.reply.value = Reply(body = "<script>document.title='Second page'</script><p>Second content</p>")
            val second = timedCapture { IosPageCapture.capturePage(url, 0, text = true) }
            assertEquals("First page", first.title)
            assertEquals("Second page", second.title)
            assertTrue(first.bytes.decodeToString().contains("First content"))
            assertFalse(first.bytes.decodeToString().contains("Second content"))
        } finally {
            server.close()
        }
    }

    @Test fun multipleScreenshotMatchesFailBeforeAnyPartialCaptureCanBeSaved() = runWebKitTest {
        val server = CaptureServer()
        try {
            server.reply.value = Reply(body = "<p class='watched'>First</p><p class='watched'>Second</p>")
            val error = assertFailsWith<com.bernaferrari.changedetection.detection.TextRegionException> { timedCapture { IosPageCapture.capturePage("http://127.0.0.1:${server.port}/page", 0, only = ".watched") } }
            assertEquals(com.bernaferrari.changedetection.detection.TextRegionIssue.MULTIPLE_MATCHES, error.issue)
        } finally {
            server.close()
        }
    }

    @Test fun failedVisibleImageCannotBecomeAScreenshotAndHealthyCaptureRecovers() = runWebKitTest {
        val server = CaptureServer()
        val previousWindow = platform.UIKit.UIApplication.sharedApplication.keyWindow
        val window = platform.UIKit.UIWindow(CGRectMake(0.0, 0.0, 390.0, 844.0)).apply {
            rootViewController = platform.UIKit.UIViewController()
            makeKeyAndVisible()
        }
        try {
            val url = "http://127.0.0.1:${server.port}/page"
            server.reply.value = Reply(body = "<p>Unchanged stock</p><img src='/broken.png' style='width:120px;height:100px'>")
            val failure = assertFailsWith<CaptureNotReadyException> { timedCapture { IosPageCapture.capturePage(url, 0) } }
            assertEquals(CaptureIssue.RESOURCE_FAILED, failure.issue)
            server.reply.value = Reply(body = "<p>Unchanged stock</p>")
            val recovered = timedCapture { IosPageCapture.capturePage(url, 0) }
            assertEquals("image/png", recovered.contentType)
            assertTrue(recovered.bytes.size > 100)
            assertEquals(listOf(137, 80, 78, 71), recovered.bytes.take(4).map { it.toInt() and 255 })
        } finally {
            window.hidden = true
            previousWindow?.makeKeyAndVisible()
            server.close()
        }
    }

    @Test fun fullyExcludedWatchedAreaCannotBecomeABlankScreenshotBaseline() = runWebKitTest {
        val server = CaptureServer()
        try {
            server.reply.value = Reply(body = "<p id='watched' style='width:240px;height:120px'>Stable stock</p>")
            val failure = assertFailsWith<com.bernaferrari.changedetection.detection.TextRegionException> {
                timedCapture { IosPageCapture.capturePage("http://127.0.0.1:${server.port}/page", 0, hide = "#watched", only = "#watched") }
            }
            assertEquals(com.bernaferrari.changedetection.detection.TextRegionIssue.EXCLUDED, failure.issue)
        } finally {
            server.close()
        }
    }

    @Test fun selectedScreenshotIgnoresFailedImagesOutsideItsArea() = runWebKitTest {
        withCaptureWindow {
            val server = CaptureServer()
            try {
                server.reply.value = Reply(body = "<meta name='viewport' content='width=device-width,initial-scale=1'><p id='watched' style='width:240px;height:120px'>Stable stock</p><img src='/broken.png' style='width:120px;height:100px'>")
                val shot = timedCapture { IosPageCapture.capturePage("http://127.0.0.1:${server.port}/page", 0, only = "#watched") }
                assertTrue(shot.bytes.size > 100)
                assertEquals("image/png", shot.contentType)
                assertTrue(server.requests.value.any { "GET /broken.png " in it }, "The unrelated failed image must actually have loaded")
            } finally {
                server.close()
            }
        }
    }

    @Test fun fullPageScreenshotIgnoresFailedOrLazyImagesBelowItsCaptureBudget() = runWebKitTest {
        withCaptureWindow {
            val server = CaptureServer()
            try {
                server.reply.value = Reply(body = "<meta name='viewport' content='width=device-width,initial-scale=1'><p>Stable stock</p><div style='height:20000px'></div><img loading='lazy' src='/broken.png' style='width:120px;height:100px'>")
                val shot = timedCapture { IosPageCapture.capturePage("http://127.0.0.1:${server.port}/page", 0) }
                assertTrue(shot.bytes.size > 100)
                assertEquals("image/png", shot.contentType)
            } finally {
                server.close()
            }
        }
    }

    @Test fun readinessUsesOnlyFontFacesNeededByCapturedText() = runWebKitTest {
        val server = CaptureServer()
        val web = browser()
        try {
            server.reply.value = Reply(
                body = """
                <meta name='viewport' content='width=device-width,initial-scale=1'>
                <p id='watched' style='width:240px;height:120px;font:400 24px CaptureFixture,serif'>Stable stock</p>
                <script>
                var good=new FontFace('CaptureFixture','local("Helvetica")',{weight:'400'});
                var bad=new FontFace('CaptureFixture','url("/broken.png")',{weight:'900'});
                document.fonts.add(good);document.fonts.add(bad);
                Promise.allSettled([good.load(),bad.load()]).then(function(){window.fontFixtureReady=true;});
                </script>
                """.trimIndent(),
            )
            IosPageCapture.loadPage(web, "http://127.0.0.1:${server.port}/page")
            withTimeout(5_000) { while (evaluate(web, "window.fontFixtureReady ? 'ready' : 'waiting'") != "ready") delay(50) }
            assertEquals("loaded", evaluate(web, "good.status"))
            assertEquals("error", evaluate(web, "bad.status"))
            val script = RenderedCaptureScripts.resources("#watched", 6_000.0)
            suspend fun fontState(): String = withTimeout(5_000) {
                var state = evaluate(web, script).split(',')[2]
                while (state.toInt() > 0) {
                    delay(50)
                    state = evaluate(web, script).split(',')[2]
                }
                state
            }
            assertEquals("0", fontState(), "A failed unused 900 face must not poison healthy 400 text")
            evaluate(web, "document.getElementById('watched').style.fontWeight='900';0")
            val usedFontState = fontState()
            assertEquals("-1", usedFontState, "A failed font actually used by the watched text must still block capture")
        } finally {
            web.stopLoading()
            server.close()
        }
    }

    private suspend fun withCaptureWindow(block: suspend () -> Unit) {
        val previous = platform.UIKit.UIApplication.sharedApplication.keyWindow
        val window = platform.UIKit.UIWindow(CGRectMake(0.0, 0.0, 390.0, 844.0)).apply {
            rootViewController = platform.UIKit.UIViewController()
            makeKeyAndVisible()
        }
        try {
            block()
        } finally {
            window.hidden = true
            previous?.makeKeyAndVisible()
        }
    }

    @Test fun idleAcceptedClientCannotBlockASecondRequest() = runWebKitTest {
        val server = CaptureServer()
        var idle = -1
        var active = -1
        try {
            withContext(Dispatchers.Default) {
                idle = connectFixture(server.port)
                withTimeout(1_000) { while (server.acceptedIds().isEmpty()) delay(5) }
                val idleId = server.acceptedIds().single()
                active = connectFixture(server.port)
                val request = "GET /page HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n".encodeToByteArray()
                request.usePinned { check(send(active, it.addressOf(0), request.size.convert(), 0) == request.size.toLong()) }
                val response = memScoped {
                    val buffer = allocArray<ByteVar>(4096)
                    val count = recv(active, buffer, 4096.convert(), 0)
                    check(count > 0) { "The second fixture request must complete while the first is idle" }
                    buffer.readBytes(count.toInt()).decodeToString()
                }
                assertTrue(response.startsWith("HTTP/1.1 200 OK"))
                assertTrue(idleId in server.acceptedIds(), "The idle client must remain open until the second response")
            }
        } finally {
            if (idle >= 0) platform.posix.close(idle)
            if (active >= 0) platform.posix.close(active)
            server.close()
        }
    }

    private fun browser() = WKWebView(
        CGRectMake(0.0, 0.0, 390.0, 844.0),
        WKWebViewConfiguration().apply {
            websiteDataStore = WKWebsiteDataStore.defaultDataStore()
        },
    )

    private suspend fun evaluate(web: WKWebView, script: String): String = suspendCancellableCoroutine { continuation ->
        web.evaluateJavaScript(script) { result, error ->
            if (continuation.isActive) {
                if (error != null) {
                    continuation.resumeWith(Result.failure(IllegalStateException(error.localizedDescription)))
                } else {
                    continuation.resume(result.toString())
                }
            }
        }
    }
}

private fun runWebKitTest(block: suspend () -> Unit) {
    assertTrue(NSThread.isMainThread)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val completed = CompletableDeferred<Unit>()
    scope.launch(start = CoroutineStart.UNDISPATCHED) {
        try {
            withTimeout(20_000) { block() }
            completed.complete(Unit)
        } catch (error: Throwable) {
            completed.completeExceptionally(error)
        }
    }
    val started = kotlin.time.TimeSource.Monotonic.markNow()
    try {
        while (!completed.isCompleted && started.elapsedNow().inWholeMilliseconds < 25_000) {
            NSRunLoop.mainRunLoop.runUntilDate(NSDate.dateWithTimeIntervalSinceNow(0.01))
        }
        assertTrue(completed.isCompleted, "WebKit requires a simulator app host")
        runBlocking { completed.await() }
    } finally {
        scope.cancel()
    }
}

private data class Reply(val status: String = "200 OK", val body: String = "Original")

private suspend fun <T> timedCapture(block: suspend () -> T): T {
    val started = kotlin.time.TimeSource.Monotonic.markNow()
    println("IOS_CAPTURE_TEST start")
    try {
        return block()
    } finally {
        println("IOS_CAPTURE_TEST end elapsedMs=${started.elapsedNow().inWholeMilliseconds}")
    }
}

private fun configureFixtureSocket(fd: Int) = memScoped {
    // Darwin may inherit O_NONBLOCK from the listener; accepted clients use bounded blocking I/O.
    val flags = fcntl(fd, F_GETFL, 0)
    check(flags >= 0 && fcntl(fd, F_SETFL, flags and O_NONBLOCK.inv()) == 0)
    val one = alloc<IntVar>().apply { value = 1 }
    check(setsockopt(fd, SOL_SOCKET, SO_NOSIGPIPE, one.ptr, sizeOf<IntVar>().convert()) == 0)
    val timeout = alloc<timeval>().apply {
        tv_sec = 3
        tv_usec = 0
    }
    check(setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, timeout.ptr, sizeOf<timeval>().convert()) == 0)
    check(setsockopt(fd, SOL_SOCKET, SO_SNDTIMEO, timeout.ptr, sizeOf<timeval>().convert()) == 0)
}

private fun connectFixture(port: Int): Int {
    val fd = socket(AF_INET, SOCK_STREAM, 0)
    check(fd >= 0)
    try {
        configureFixtureSocket(fd)
        memScoped {
            val address = alloc<sockaddr_in>()
            address.sin_family = AF_INET.convert()
            address.sin_port = (((port and 255) shl 8) or ((port shr 8) and 255)).convert()
            address.sin_addr.s_addr = 0x0100007Fu
            check(connect(fd, address.ptr.reinterpret(), sizeOf<sockaddr_in>().convert()) == 0)
        }
        return fd
    } catch (error: Throwable) {
        platform.posix.close(fd)
        throw error
    }
}

/** Concurrent, bounded loopback fixture; never log request headers, cookies or page contents. */
private class CaptureServer {
    val reply = AtomicReference(Reply())
    val requests = AtomicReference(emptyList<String>())
    private val lock = NSLock()
    private val clients = mutableMapOf<Int, Int>()
    private var nextId = 0
    private var closing = false
    private var events = 0
    private val started = kotlin.time.TimeSource.Monotonic.markNow()
    private val listener = socket(AF_INET, SOCK_STREAM, 0)
    val port: Int

    private fun <T> locked(block: () -> T): T {
        lock.lock()
        try {
            return block()
        } finally {
            lock.unlock()
        }
    }
    private fun event(message: String) = locked {
        if (events++ < 64) println("IOS_FIXTURE elapsedMs=${started.elapsedNow().inWholeMilliseconds} $message")
    }
    fun acceptedIds(): Set<Int> = locked { clients.values.toSet() }

    init {
        check(listener >= 0)
        port = memScoped {
            val address = alloc<sockaddr_in>()
            address.sin_family = AF_INET.convert()
            address.sin_port = 0u
            address.sin_addr.s_addr = 0x0100007Fu
            check(bind(listener, address.ptr.reinterpret(), sizeOf<sockaddr_in>().convert()) == 0)
            check(listen(listener, 8) == 0)
            val length = alloc<socklen_tVar>().apply { value = sizeOf<sockaddr_in>().convert() }
            check(getsockname(listener, address.ptr.reinterpret(), length.ptr) == 0)
            val network = address.sin_port.toInt()
            ((network and 0xff) shl 8) or ((network shr 8) and 0xff)
        }
        check(fcntl(listener, F_SETFL, O_NONBLOCK) == 0)
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.convert(), 0u)) {
            while (true) {
                // Nonblocking accept and ownership registration share the close lock, preventing fd reuse races.
                val accepted = locked {
                    if (closing) {
                        null
                    } else {
                        val fd = accept(listener, null, null)
                        if (fd < 0) Pair(-1, -1) else Pair(fd, ++nextId).also { clients[fd] = it.second }
                    }
                } ?: break
                val (client, id) = accepted
                if (client < 0) {
                    usleep(5_000u)
                    continue
                }
                event("accept id=$id")
                dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.convert(), 0u)) {
                    try {
                        configureFixtureSocket(client)
                        serve(client, id)
                    } finally {
                        // Only this worker closes its accepted fd; close() only shuts it down under the same lock.
                        locked {
                            clients.remove(client)
                            platform.posix.close(client)
                        }
                        event("close id=$id")
                    }
                }
            }
        }
    }

    private fun serve(client: Int, id: Int) {
        val request = StringBuilder()
        val readStarted = kotlin.time.TimeSource.Monotonic.markNow()
        memScoped {
            val buffer = allocArray<ByteVar>(4096)
            while (!request.contains("\r\n\r\n")) {
                if (request.length >= 8192 || readStarted.elapsedNow().inWholeMilliseconds >= 4_000) return
                val length = recv(client, buffer, minOf(4096, 8192 - request.length).convert(), 0)
                if (length <= 0) {
                    event("read-ended id=$id")
                    return
                }
                request.append(buffer.readBytes(length.toInt()).decodeToString())
            }
        }
        event("request id=$id headerBytes=${request.length}")
        while (true) {
            val before = requests.value
            if (requests.compareAndSet(before, (before + request.toString()).takeLast(32))) break
        }
        val response = if (request.startsWith("GET /broken.png ")) Reply("503 Service Unavailable", "") else reply.value
        val bytes = ("<html><body>${response.body}</body></html>").encodeToByteArray()
        val message = (
            "HTTP/1.1 ${response.status}\r\nContent-Type: text/html; charset=utf-8\r\n" +
                "Cache-Control: max-age=3600\r\nSet-Cookie: captureSession=retained; Path=/\r\n" +
                "Content-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
            ).encodeToByteArray() + bytes
        message.usePinned { pinned ->
            var offset = 0
            while (offset < message.size) {
                val sent = send(client, pinned.addressOf(offset), (message.size - offset).convert(), 0)
                if (sent <= 0) break
                offset += sent.toInt()
            }
            event("response id=$id bytes=$offset")
        }
    }

    fun close() = locked {
        if (!closing) {
            closing = true
            shutdown(listener, SHUT_RDWR)
            platform.posix.close(listener)
            clients.keys.forEach { shutdown(it, SHUT_RDWR) }
        }
    }
}
