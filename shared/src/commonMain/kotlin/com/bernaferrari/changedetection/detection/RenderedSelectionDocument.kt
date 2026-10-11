/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.detection

import com.bernaferrari.changedetection.ui.comparison.secureSnapshotDocument
import com.bernaferrari.changedetection.util.ContentHasher
import com.fleeksoft.ksoup.Ksoup
import kotlin.io.encoding.Base64

private const val selectionOrigin = "https://selection.changedetection.invalid/"

internal fun selectionDocumentWithInitialRegion(document: SelectableTextDocument, selector: String?, viewport: RegionViewport = RegionViewport()): String {
    val html = requireNotNull(document.selectionHtml)
    val index = document.regions.indexOfFirst { it.selector == selector }
    if (index < 0 && viewport == RegionViewport()) return html
    val parsed = Ksoup.parse(html)
    parsed.outputSettings().prettyPrint(false)
    parsed.body().attr("data-cd-scroll-x", viewport.x.toString()).attr("data-cd-scroll-y", viewport.y.toString())
    parsed.select("[data-cd-region=$index]").singleOrNull()?.apply {
        attr("data-cd-selected", "true")
        attr("aria-pressed", "true")
    }
    return parsed.outerHtml()
}

/** Retained areas keep their highlight while the active candidate changes. */
internal fun selectionDocumentWithWatchedAreas(document: SelectableTextDocument, selectors: List<String>, ignored: List<String> = emptyList()): SelectableTextDocument {
    if ((selectors.isEmpty() && ignored.isEmpty()) || document.selectionHtml == null) return document
    val parsed = Ksoup.parse(document.selectionHtml)
    parsed.outputSettings().prettyPrint(false)
    val source = document.sourceHtml?.let(Ksoup::parse)
    val included = if (source != null && selectors.isNotEmpty()) {
        try {
            source.select(selectors.joinToString(", ")).toSet()
        } catch (_: com.fleeksoft.ksoup.select.Selector.SelectorParseException) {
            emptySet()
        }
    } else {
        emptySet()
    }
    if (source?.body() in included) parsed.body().attr("data-cd-watched", "true")
    document.regions.forEachIndexed { index, region ->
        if (region.selector in selectors || source?.select(region.selector)?.singleOrNull() in included) {
            parsed.select("[data-cd-region=$index]").singleOrNull()?.apply {
                attr("data-cd-watched", "true")
                attr("aria-pressed", "true")
            }
        }
    }
    val excluded = if (source != null && ignored.isNotEmpty()) {
        try {
            source.select(ignored.joinToString(", ")).toSet()
        } catch (_: com.fleeksoft.ksoup.select.Selector.SelectorParseException) {
            emptySet()
        }
    } else {
        emptySet()
    }
    document.regions.forEachIndexed { index, region ->
        val element = source?.select(region.selector)?.singleOrNull()
        if (region.selector in ignored || generateSequence(element) { it.parent() }.any { it in excluded }) {
            parsed.select("[data-cd-region=$index]").singleOrNull()?.attr("data-cd-ignored", "true")
        }
    }
    return document.copy(selectionHtml = parsed.outerHtml())
}

/** Only opaque candidate indices cross the browser boundary; source selectors remain native data. */
internal fun selectionFromNavigation(url: String, regions: List<SelectableTextRegion>): SelectableTextRegion? {
    if (!url.startsWith(selectionOrigin)) return null
    val message = url.removePrefix(selectionOrigin)
    val index = if (message.startsWith("pick/")) {
        if (regionViewportFromNavigation(url) == null) return null
        message.split('/')[1]
    } else {
        message
    }
    if (index.isEmpty() || index.any { it !in '0'..'9' }) return null
    return index.toIntOrNull()?.let(regions::getOrNull)
}

internal fun renderedSelectionDocument(html: String, regions: List<SelectableTextRegion>): String {
    val source = Ksoup.parse(html)
    // Never trust a source page to supply our candidate IDs or selection state.
    source.select("*").forEach { element ->
        element.attributes().asList().map { it.key }.filter { it.startsWith("data-cd-", true) }.forEach(element::removeAttr)
    }
    regions.forEachIndexed { index, region ->
        source.select(region.selector).singleOrNull()?.apply {
            attr("data-cd-region", index.toString())
            attr("tabindex", "0")
            attr("role", "button")
            attr("aria-pressed", "false")
        }
    }
    val safe = secureSnapshotDocument(source.outerHtml())
    val hash = ContentHasher.hashContent(selectionScript.encodeToByteArray()).chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    val cspHash = Base64.encode(hash)
    return safe.replace("default-src 'none';", "default-src 'none'; script-src 'sha256-$cspHash';")
        .replace(" navigate-to 'none'", "")
        .replace("</head>", "<style>$selectionStyle</style></head>")
        .replace("</body>", "<script>$selectionScript</script></body>")
}

private val selectionStyle = """
    [data-cd-region] { cursor: pointer; }
    [data-cd-region]:focus-visible { outline: 2px solid #6352ca; outline-offset: 3px; }
    [data-cd-watched] { outline: 2px dashed #6352ca; outline-offset: 3px; background-color: #eeeafb !important; color: #272d2a !important; }
    [data-cd-selected] { outline: 3px solid #6352ca; outline-offset: 3px; border-radius: 8px; background-color: #eeeafb !important; color: #272d2a !important; }
    [data-cd-ignored], [data-cd-ignored] * { background-color: #e2e3df !important; color: #626760 !important; text-decoration: line-through; }
    [data-cd-ignored] { outline: 2px dashed #626760; outline-offset: 3px; border-radius: 8px; }
    [data-cd-ignored][data-cd-selected] { outline: 3px solid #626760; }
""".trimIndent()

private val selectionScript = """
    (() => {
      let pendingScroll = null;
      function coordinates() {
        return [window.scrollX, window.scrollY].map(value => Math.max(0, Math.min(10000000, Math.round(value)))).join('/');
      }
      function restoreViewport() {
        const x = Number(document.body.getAttribute('data-cd-scroll-x') || 0);
        const y = Number(document.body.getAttribute('data-cd-scroll-y') || 0);
        window.scrollTo(x, y);
      }
      window.addEventListener('load', restoreViewport, {once: true});
      window.addEventListener('scroll', () => {
        if (pendingScroll !== null) return;
        pendingScroll = requestAnimationFrame(() => {
          pendingScroll = null;
          window.location.href = '$selectionOrigin' + 'view/' + coordinates();
        });
      }, {passive: true});
      function select(event) {
        if (event.type === 'keydown' && event.key !== 'Enter' && event.key !== ' ') return;
        const target = event.target.closest('[data-cd-region]');
        if (!target) return;
        event.preventDefault();
        event.stopPropagation();
        document.querySelectorAll('[data-cd-selected]').forEach(node => {
          node.removeAttribute('data-cd-selected'); node.setAttribute('aria-pressed', node.hasAttribute('data-cd-watched') ? 'true' : 'false');
        });
        target.setAttribute('data-cd-selected', 'true');
        target.setAttribute('aria-pressed', 'true');
        if (pendingScroll !== null) { cancelAnimationFrame(pendingScroll); pendingScroll = null; }
        window.location.href = '$selectionOrigin' + 'pick/' + target.getAttribute('data-cd-region') + '/' + coordinates();
      }
      document.addEventListener('click', select, true);
      document.addEventListener('keydown', select, true);
    })();
""".trimIndent()
