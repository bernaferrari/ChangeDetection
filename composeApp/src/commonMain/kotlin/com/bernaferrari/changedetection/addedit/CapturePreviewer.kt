/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.addedit

import io.ktor.http.hostWithPortIfSpecified

import com.bernaferrari.changedetection.repo.CaptureMode
import com.bernaferrari.changedetection.repo.ColorGroup
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.sync.SyncEngine

sealed interface CapturePreview {
    data class Html(val text: String, val totalBytes: Int, val selectableDocument: com.bernaferrari.changedetection.detection.SelectableTextDocument? = null, val pageTitle: String? = null, val pageIcon: String? = null) : CapturePreview
    data class Image(val bytes: ByteArray, val pageTitle: String? = null, val pageIcon: String? = null) : CapturePreview
}

/** A read-only sample: no monitor creation, baseline commit, scheduling or notification. */
fun interface CapturePreviewer {
    suspend fun capture(url: String, mode: CaptureMode): CapturePreview
    suspend fun capture(url: String, mode: CaptureMode, region: com.bernaferrari.changedetection.detection.TextRegion): CapturePreview {
        check(region.isEmpty) { "Region preview is unavailable" }
        return capture(url, mode)
    }
}

internal fun previewSite(url: String) = Site(null, url, 0L, "capture-preview", ColorGroup(0, 0), true, false)

class HtmlCapturePreviewer(private val engine: SyncEngine) : CapturePreviewer {
    override suspend fun capture(url: String, mode: CaptureMode): CapturePreview = capture(url, mode, com.bernaferrari.changedetection.detection.TextRegion())

    override suspend fun capture(url: String, mode: CaptureMode, region: com.bernaferrari.changedetection.detection.TextRegion): CapturePreview {
        check(mode != CaptureMode.SCREENSHOT) { "Screenshot monitoring is unavailable on this platform" }
        return when (val result = engine.fetchFromServer(previewSite(url))) {
            is SyncEngine.FetchResult.Error -> error(result.errorResult?.userMessage ?: result.message)

            is SyncEngine.FetchResult.Success -> {
                check(result.data.isNotEmpty()) { "The page returned no content" }
                check(mode != CaptureMode.TEXT || !result.contentType.startsWith("image/", true)) { "This address returns an image. Choose screenshots or source monitoring." }
                if (result.contentType.startsWith("image/", true)) {
                    CapturePreview.Image(result.data)
                } else {
                    CapturePreview.Html(
                        (
                            if (mode == CaptureMode.TEXT) {
                                com.bernaferrari.changedetection.util.readableSnapshotText(
                                    com.bernaferrari.changedetection.detection.prepareTextCapture(result.data, result.contentType, region).data.decodeToString(),
                                    result.contentType.contains("html", true),
                                )
                            } else if (mode == CaptureMode.SOURCE) {
                                result.data.decodeToString()
                            } else {
                                engine.parseHtml(result.data.decodeToString())
                            }
                            ).take(12_000),
                        result.data.size,
                        if (mode == CaptureMode.TEXT && result.contentType.contains("html", true)) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                                com.bernaferrari.changedetection.detection.selectableTextDocument(result.data.decodeToString())
                            }
                        } else {
                            null
                        },
                        pageTitle = if (result.contentType.contains("html", true)) htmlTitle(result.data.decodeToString()) else null,
                        pageIcon = if (result.contentType.contains("html", true)) htmlIcon(result.data.decodeToString(), url) else null,
                    )
                }
            }
        }
    }
}

private val titleTag = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val ogTitle = Regex("<meta[^>]+property=[\"']og:title[\"'][^>]*content=([\"'])(.*?)\\1", RegexOption.IGNORE_CASE)

/** The page's own name: Open Graph title first (usually cleanest), then `<title>`; cleaned to be a good default. */
internal fun htmlTitle(html: String): String? {
    val head = html.take(600_000)
    val raw = ogTitle.find(head)?.groupValues?.get(2) ?: titleTag.find(head)?.groupValues?.get(1) ?: return null
    return cleanTitle(com.fleeksoft.ksoup.Ksoup.parse(raw).text())
}

private val linkTag = Regex("<link\\b[^>]*>", RegexOption.IGNORE_CASE)
private fun attr(tag: String, name: String) = Regex("\\b$name\\s*=\\s*([\"'])(.*?)\\1", RegexOption.IGNORE_CASE).find(tag)?.groupValues?.get(2)

/** Same choice as [com.bernaferrari.changedetection.browser.PageCleanup.ICON], from raw HTML. */
internal fun htmlIcon(html: String, pageUrl: String): String? {
    val base = io.ktor.http.Url(pageUrl)
    val origin = "${base.protocol.name}://${base.hostWithPortIfSpecified}"
    var best: String? = null
    var score = -1
    linkTag.findAll(html.take(600_000)).forEach { match ->
        val tag = match.value
        val rel = attr(tag, "rel")?.lowercase() ?: return@forEach
        val href = attr(tag, "href")?.takeIf { it.isNotBlank() } ?: return@forEach
        if ("icon" !in rel || "mask" in rel || href.substringBefore('?').endsWith(".svg", true) || attr(tag, "type")?.contains("svg") == true) return@forEach
        val apple = "apple" in rel
        val size = attr(tag, "sizes")?.substringBefore('x')?.toIntOrNull() ?: if (apple) 180 else 16
        val s = (if (apple) 1000 else 0) + minOf(size, 512)
        if (s > score) {
            score = s
            best = resolveUrl(origin, base.encodedPath, href)
        }
    }
    return best ?: "$origin/favicon.ico"
}

private fun resolveUrl(origin: String, path: String, href: String): String = when {
    href.startsWith("http://") || href.startsWith("https://") -> href
    href.startsWith("//") -> origin.substringBefore("//") + href
    href.startsWith("/") -> origin + href
    else -> origin + path.substringBeforeLast('/', "") + "/" + href
}.replace("&amp;", "&")

/** "BBC News - Breaking news, video and…" reads better as "BBC News"; short titles stay whole. */
internal fun cleanTitle(title: String): String? {
    val text = title.replace(Regex("\\s+"), " ").trim()
    // "BBC News - Breaking news, video and…" reads better as "BBC News"; short titles stay whole.
    val short = if (text.length > 28) text.split(" - ", " | ", " – ", " — ", " · ").first().trim() else text
    return short.takeIf { it.length >= 2 }?.let { if (it.length > 60) it.take(60).substringBeforeLast(' ') + "…" else it }
}
