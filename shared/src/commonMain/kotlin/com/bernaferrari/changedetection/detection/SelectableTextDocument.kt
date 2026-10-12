package com.bernaferrari.changedetection.detection

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Source-backed choices for text monitoring, never a claim about browser-rendered visibility. */
data class SelectableTextRegion(val selector: String, val text: String, val tag: String, val depth: Int, val parentSelector: String? = null)

data class SelectableTextDocument(val regions: List<SelectableTextRegion>, val limited: Boolean, val selectionHtml: String? = null, val sourceHtml: String? = null)

/**
 * Keep selectors anchored to the original tree: removing scripts/hidden nodes before calculating
 * sibling positions would make the saved rule select different content on the next check.
 */
suspend fun selectableTextDocument(html: String): SelectableTextDocument {
    val document = Ksoup.parse(html)
    val regions = mutableListOf<SelectableTextRegion>()
    val selectorsByElement = mutableMapOf<Element, String>()
    val pending = ArrayDeque<Pair<Element, Int>>()
    pending.add(document.body() to 0)
    var visited = 0
    var limited = false
    while (pending.isNotEmpty()) {
        currentCoroutineContext().ensureActive()
        if (++visited > 5_000 || regions.size == 300) {
            limited = true
            break
        }
        val (element, depth) = pending.removeLast()
        if (element.tagName() in ignoredTags || element.hasAttr("hidden") || element.attr("aria-hidden").equals("true", true)) continue
        if (depth > 32) {
            limited = true
            continue
        }
        if (element.tagName() in selectableTags) {
            val text = element.text().trim()
            if (text.isNotEmpty()) {
                val preferred = element.cssSelector()
                val selector = if (preferred.length <= 512 && document.select(preferred).singleOrNull() === element) {
                    preferred
                } else {
                    // Duplicate IDs and classes must not silently select multiple regions.
                    generateSequence(element) { it.parent()?.takeUnless { parent -> parent.tagName() == "#root" } }
                        .toList().asReversed().joinToString(" > ") { "${it.tagName()}:nth-child(${it.elementSiblingIndex() + 1})" }
                }
                if (selector.length <= 512 && document.select(selector).singleOrNull() === element) {
                    val parentSelector = generateSequence(element.parent()) { it.parent() }.firstNotNullOfOrNull { selectorsByElement[it] }
                    regions += SelectableTextRegion(selector, text.take(1_200), element.tagName(), depth, parentSelector)
                    selectorsByElement[element] = selector
                }
            }
        }
        for (child in element.children().asReversed()) pending.add(child to depth + 1)
    }
    return SelectableTextDocument(regions, limited, renderedSelectionDocument(html, regions), html)
}

private val ignoredTags = setOf("script", "style", "template", "noscript", "svg", "head")
private val selectableTags = setOf("main", "article", "section", "div", "p", "h1", "h2", "h3", "h4", "h5", "h6", "li", "table", "tr", "td", "th", "span", "dl", "dt", "dd", "blockquote", "pre")
