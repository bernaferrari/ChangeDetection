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

/** A cacheable page, a login cookie and a broken subresource on a private ephemeral port. */
private class CaptureServer {
    val reply = AtomicReference(Reply())
    val requests = AtomicReference(emptyList<String>())
    private val listener = socket(AF_INET, SOCK_STREAM, 0)
    val port: Int

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
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.convert(), 0u)) {
            while (true) {
                val client = accept(listener, null, null)
                if (client < 0) break
                try {
                    serve(client)
                } finally {
                    platform.posix.close(client)
                }
            }
        }
    }

    private fun serve(client: Int) {
        val request = StringBuilder()
        memScoped {
            val one = alloc<IntVar>().apply { value = 1 }
            setsockopt(client, SOL_SOCKET, SO_NOSIGPIPE, one.ptr, sizeOf<IntVar>().convert())
            val buffer = allocArray<ByteVar>(4096)
            while (!request.contains("\r\n\r\n")) {
                val length = recv(client, buffer, 4096.convert(), 0)
                if (length <= 0) return
                request.append(buffer.readBytes(length.toInt()).decodeToString())
            }
        }
        while (true) {
            val before = requests.value
            if (requests.compareAndSet(before, before + request.toString())) break
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
        }
    }

    fun close() {
        shutdown(listener, SHUT_RDWR)
        platform.posix.close(listener)
    }
}
