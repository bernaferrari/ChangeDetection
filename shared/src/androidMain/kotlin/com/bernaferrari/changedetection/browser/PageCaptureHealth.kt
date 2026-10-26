package com.bernaferrari.changedetection.browser

import com.bernaferrari.changedetection.diagnostics.*
import kotlinx.coroutines.currentCoroutineContext
import java.net.URI
import javax.net.ssl.SSLException

/**
 * One document's TLS health, shared by interactive capture and persistence. No WebView reference.
 * URL observations are bounded, memory-only, and used solely for classification.
 *
 * Certificate errors are always refused. A failed main document blocks capture immediately.
 * Refused subresources use the same watched-content checks as HTTP failures, so an optional
 * tracker cannot invalidate a healthy page, while missing captured assets still prevent saving.
 */
internal class PageCaptureHealth {
    private var generation = 0L
    private var requested: String? = null
    private var started: String? = null
    private var committed: String? = null
    private var finished = false
    private var blockingSinceFinish = false

    /** A main-frame request seen after the last commit: a navigation (or its redirect) is still in flight. */
    private var mainPending = false
    private val mainRequests = LinkedHashSet<String>()
    private val subRequests = LinkedHashSet<String>()
    private val blockingFailures = mutableListOf<DiagnosticTlsContext>()
    private val refusedResources = mutableListOf<DiagnosticTlsContext>()
    private var blocked = 0
    private val resourceErrors = LinkedHashSet<String>()
    private val watchedDependencies = LinkedHashSet<String>()
    private var dependencyOverflow = false
    private var resourceErrorOverflow = false

    // Fragments select content within a resource; browsers omit them from HTTP requests.
    // Keep queries and encoded '#' characters: those can identify different server resources.
    private fun resourceKey(url: String) = url.substringBefore('#')

    @Synchronized fun resourceError(url: String) {
        val key = resourceKey(url)
        if (key in resourceErrors) return
        if (key.length <= 4096 && resourceErrors.size < 512) {
            resourceErrors += key
        } else {
            resourceErrorOverflow = true
        }
    }

    @Synchronized fun dependencies(urls: List<String>) {
        // Retain this document's dependency union so another preview cannot revoke an older preview's checks.
        for (url in urls) {
            val key = resourceKey(url)
            if (key in watchedDependencies) continue
            if (key.length <= 4096 && watchedDependencies.size < 512) {
                watchedDependencies += key
            } else {
                dependencyOverflow = true
            }
        }
    }

    @Synchronized private fun incompleteResources() = resourceErrorOverflow || resourceErrors.any { it in watchedDependencies } ||
        (dependencyOverflow && resourceErrors.isNotEmpty())

    @Synchronized fun begin(url: String?) {
        generation++
        requested = bounded(url)
        started = null
        committed = null
        finished = false
        blockingSinceFinish = false
        mainPending = false
        mainRequests.clear()
        subRequests.clear()
        blockingFailures.clear()
        refusedResources.clear()
        blocked = 0
        resourceErrors.clear()
        watchedDependencies.clear()
        dependencyOverflow = false
        resourceErrorOverflow = false
    }

    @Synchronized fun started(url: String?) {
        // Some SSL callbacks precede onPageStarted. Never clear a blocking failure here.
        // A later, completed navigation can recover without poisoning the next page.
        if (finished && !blockingSinceFinish) begin(url)
        started = bounded(url)
        committed = null
        finished = false
    }

    @Synchronized fun committed(url: String?) {
        committed = bounded(url)
        mainPending = false
    }

    @Synchronized fun finished() {
        finished = true
        blockingSinceFinish = false
    }

    @Synchronized fun request(url: String, main: Boolean) {
        if (url.length > 4096) return
        if (main) {
            mainPending = true
            // Main-frame requests are few (the page and its redirects), and losing one would hide a blocking failure.
            if (mainRequests.size < MAX_MAIN) mainRequests += url
        } else if (subRequests.size < MAX_SUB) {
            subRequests += url
        }
    }

    @Synchronized fun ssl(url: String?, primary: Int, codes: List<Int>): DiagnosticTlsContext {
        val inMain = url != null && url in mainRequests
        val inSub = url != null && url in subRequests
        val kind = when {
            inMain && !inSub -> TlsRequestKind.MAIN_DOCUMENT
            inSub && !inMain -> TlsRequestKind.SUBRESOURCE
            else -> TlsRequestKind.UNKNOWN // Mixed observations and unobserved requests are ambiguous.
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
        val blocking = when (kind) {
            TlsRequestKind.MAIN_DOCUMENT -> true

            TlsRequestKind.SUBRESOURCE -> false

            // An unseen redirect may be the page itself until a document has committed.
            TlsRequestKind.UNKNOWN ->
                relation == TlsUrlRelation.UNKNOWN || relation == TlsUrlRelation.REQUESTED ||
                    relation == TlsUrlRelation.STARTED || relation == TlsUrlRelation.COMMITTED ||
                    committed == null || mainPending || (inMain && inSub)
        }
        val context = DiagnosticTlsContext(primary.coerceIn(0, 5), codes.filter { it in 0..5 }.distinct().sorted().take(6), kind, relation, started != null, committed != null, finished, blocking)
        if (blocking) {
            blockingSinceFinish = true
            if (blockingFailures.size < MAX_FAILURES) blockingFailures += context
        } else {
            blocked++
            url?.let(::resourceError)
            if (refusedResources.size < MAX_FAILURES) refusedResources += context
        }
        return context
    }

    @Synchronized fun epoch() = generation

    /** Blocking failures first: they decide validity and are never crowded out by refused resources. */
    @Synchronized fun observations() = blockingFailures + refusedResources

    /** A refused main document is fatal; resource completeness is checked against captured dependencies. */
    @Synchronized fun hasBlockingFailure() = blockingFailures.isNotEmpty()

    /** How many sub-resources were refused for this document (they are missing from the capture). */
    @Synchronized fun blockedResources() = blocked

    @Synchronized fun valid(epoch: Long) = epoch == generation && !hasBlockingFailure()

    /** Called again after asynchronous capture and immediately before baseline promotion. */
    suspend fun validate(epoch: Long = epoch()) {
        // Snapshot atomically: do not attribute a newer document's TLS/resource errors to an older capture.
        val state = synchronized(this) {
            when {
                epoch != generation -> Validation(CaptureIssue.DOCUMENT_CHANGED, document = DiagnosticDocumentContext(started != null, committed != null, finished, mainPending))
                incompleteResources() -> Validation(CaptureIssue.RESOURCE_FAILED)
                hasBlockingFailure() -> Validation(tls = observations())
                else -> Validation()
            }
        }
        state.document?.let { currentCoroutineContext()[DiagnosticSpan]?.documentChanged(it) }
        state.issue?.let { throw CaptureNotReadyException(it) }
        if (state.tls.isNotEmpty()) {
            state.tls.forEach { currentCoroutineContext()[DiagnosticSpan]?.tls(it) }
            throw SSLException("The page could not be captured with valid TLS")
        }
    }
    private data class Validation(val issue: CaptureIssue? = null, val tls: List<DiagnosticTlsContext> = emptyList(), val document: DiagnosticDocumentContext? = null)
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

    private companion object {
        const val MAX_MAIN = 32
        const val MAX_SUB = 256
        const val MAX_FAILURES = 8
    }
}

/** Callback threads share this weak registry; discarded WebViews and their health can expire. */
internal object WebViewCaptureHealth {
    private val pages = java.util.WeakHashMap<android.webkit.WebView, PageCaptureHealth>()

    @Synchronized fun of(view: android.webkit.WebView) = pages.getOrPut(view) { PageCaptureHealth() }
}
