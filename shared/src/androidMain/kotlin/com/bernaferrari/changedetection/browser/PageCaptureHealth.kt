package com.bernaferrari.changedetection.browser

import com.bernaferrari.changedetection.diagnostics.*
import kotlinx.coroutines.currentCoroutineContext
import java.net.URI
import javax.net.ssl.SSLException

/** One document's health, shared by interactive capture and persistence. No WebView reference.
 * URL observations are bounded, memory-only, and used solely for conservative classification.
 */
internal class PageCaptureHealth {
    private var generation = 0L
    private var requested: String? = null
    private var started: String? = null
    private var committed: String? = null
    private var finished = false
    private var sslSinceFinish = false
    private val requests = linkedMapOf<String, Int>()
    private val failures = mutableListOf<DiagnosticTlsContext>()

    @Synchronized fun begin(url: String?) {
        generation++
        requested = bounded(url)
        started = null
        committed = null
        finished = false
        sslSinceFinish = false
        requests.clear()
        failures.clear()
    }

    @Synchronized fun started(url: String?) {
        // Some SSL callbacks precede onPageStarted. Never clear their failure here.
        // A later, completed navigation can recover without poisoning the next page.
        if (finished && !sslSinceFinish) begin(url)
        started = bounded(url)
        committed = null
        finished = false
    }

    @Synchronized fun committed(url: String?) {
        committed = bounded(url)
    }

    @Synchronized fun finished() {
        finished = true
        sslSinceFinish = false
    }

    @Synchronized fun request(url: String, main: Boolean) {
        if (url.length > 4096) return
        if (requests.size < 128 || requests.containsKey(url)) requests[url] = (requests[url] ?: 0) or if (main) 1 else 2
    }

    @Synchronized fun ssl(url: String?, primary: Int, codes: List<Int>): DiagnosticTlsContext {
        sslSinceFinish = true
        val kind = when (requests[url]) {
            1 -> TlsRequestKind.MAIN_DOCUMENT
            2 -> TlsRequestKind.SUBRESOURCE
            else -> TlsRequestKind.UNKNOWN // Mixed observations and evicted/unobserved requests are ambiguous.
        }
        val relation = when {
            url.isNullOrBlank() -> TlsUrlRelation.UNKNOWN

            url == committed -> TlsUrlRelation.COMMITTED

            url == started -> TlsUrlRelation.STARTED

            url == requested -> TlsUrlRelation.REQUESTED

            else -> {
                val a = origin(url)
                val b = origin(committed ?: started ?: requested)
                when {
                    a == null || b == null -> TlsUrlRelation.UNKNOWN
                    a == b -> TlsUrlRelation.SAME_ORIGIN
                    else -> TlsUrlRelation.DIFFERENT_ORIGIN
                }
            }
        }
        val context = DiagnosticTlsContext(primary.coerceIn(0, 5), codes.filter { it in 0..5 }.distinct().sorted().take(6), kind, relation, started != null, committed != null, finished)
        if (failures.size < 8) failures += context
        return context
    }

    @Synchronized fun epoch() = generation

    @Synchronized fun observations() = failures.toList()

    @Synchronized fun valid(epoch: Long) = epoch == generation && failures.isEmpty()

    /** Called again after asynchronous capture and immediately before baseline promotion. */
    suspend fun validate(epoch: Long = epoch()) {
        if (!valid(epoch)) {
            observations().forEach { currentCoroutineContext()[DiagnosticSpan]?.tls(it) }
            throw SSLException("The page could not be captured with valid TLS")
        }
    }
    private fun bounded(url: String?) = url?.takeIf { it.length <= 4096 }
    private fun origin(url: String?): Triple<String, String, Int>? = runCatching {
        val uri = URI(url ?: return null)
        val scheme = uri.scheme?.lowercase() ?: return null
        val host = uri.host?.lowercase() ?: return null
        Triple(
            scheme,
            host,
            if (uri.port >= 0) {
                uri.port
            } else if (scheme == "https") {
                443
            } else {
                80
            },
        )
    }.getOrNull()
}

/** Callback threads share this weak registry; discarded WebViews and their health can expire. */
internal object WebViewCaptureHealth {
    private val pages = java.util.WeakHashMap<android.webkit.WebView, PageCaptureHealth>()

    @Synchronized fun of(view: android.webkit.WebView) = pages.getOrPut(view) { PageCaptureHealth() }
}
