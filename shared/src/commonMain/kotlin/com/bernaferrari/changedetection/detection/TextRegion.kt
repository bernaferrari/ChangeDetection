package com.bernaferrari.changedetection.detection

import com.bernaferrari.changedetection.util.readableSnapshotText
import com.fleeksoft.ksoup.Ksoup

internal const val MAX_TEXT_SELECTOR_LENGTH = 4_096

/** Selects stored HTML content without executing JavaScript or evaluating CSS visibility. */
data class TextRegion(val include: String = "", val exclude: String = "") {
    val isEmpty: Boolean get() = include.isBlank() && exclude.isBlank()

    /** Syntax validation is separate from matching: a valid selector can disappear later. */
    fun validate() {
        validateSelector(include)
        validateSelector(exclude)
    }

    /**
     * Returns the selected markup for both preview and capture. Missing include matches never
     * fall back to the whole page, and ignored elements never enter the saved fragment.
     * Without rules the source is returned exactly, preserving existing monitor behavior.
     */
    fun extractHtml(content: String, isHtml: Boolean): String {
        if (isEmpty) return content
        validate()
        if (!isHtml) throw TextRegionException(TextRegionFailure.HTML_REQUIRED)
        val document = Ksoup.parse(content)
        // Capture source, not presentation indentation: deep pages can otherwise expand manyfold.
        document.outputSettings().prettyPrint(false)
        // Resolve both rules against the original tree: removals change nth-child and :has.
        val excluded = if (exclude.isBlank()) emptySet() else document.select(exclude.trim()).toSet()
        val matches = if (include.isBlank()) {
            emptySet()
        } else {
            document.select(include.trim()).filter { element ->
                generateSequence(element) { it.parent() }.none { it in excluded }
            }.toSet()
        }
        if (include.isNotBlank() && matches.isEmpty()) throw TextRegionException(TextRegionFailure.REGION_MISSING, issue = if (document.select(include.trim()).isEmpty()) TextRegionIssue.NO_MATCH else TextRegionIssue.EXCLUDED)
        // Select outermost surviving subtrees before mutation, retaining document order.
        val roots = matches.filter { element ->
            generateSequence(element.parent()) { it.parent() }.none { it in matches }
        }
        excluded.forEach { it.remove() }
        val result = if (include.isBlank()) document.outerHtml() else roots.joinToString("\n") { it.outerHtml() }
        if (readableSnapshotText(result, true).isBlank()) throw TextRegionException(TextRegionFailure.REGION_MISSING, issue = TextRegionIssue.EMPTY_TEXT)
        return result
    }

    private fun validateSelector(selector: String) {
        if (selector.isBlank()) return
        // Accept only the shared static/browser CSS surface. Ignore quoted attribute values
        // when checking names, so a literal ':contains(...)' value remains valid CSS.
        val syntax = buildString {
            var quote: Char? = null
            var escaped = false
            for (c in selector) {
                when {
                    escaped -> { append(' '); escaped = false }
                    c == '\\' -> { append(' '); escaped = true }
                    quote != null -> { if (c == quote) quote = null; append(' ') }
                    c == '\'' || c == '"' -> { quote = c; append(' ') }
                    else -> append(c)
                }
            }
        }
        val nonBrowserPseudo = Regex(":(?:contains(?:Own|Data|WholeText|WholeOwnText)?|matches(?:Own|WholeText|WholeOwnText)?|eq|lt|gt|matchText)\\b", RegexOption.IGNORE_CASE)
        if (selector.length > MAX_TEXT_SELECTOR_LENGTH || nonBrowserPseudo.containsMatchIn(syntax)) {
            throw TextRegionException(TextRegionFailure.INVALID_SELECTOR)
        }
        try {
            Ksoup.parse("").select(selector.trim())
        } catch (invalid: com.fleeksoft.ksoup.select.Selector.SelectorParseException) {
            throw TextRegionException(TextRegionFailure.INVALID_SELECTOR, invalid)
        }
    }
}

enum class TextRegionIssue { NO_MATCH, EXCLUDED, EMPTY_TEXT, NOT_VISIBLE, INVALID_SYNTAX, NON_HTML, MULTIPLE_MATCHES }

enum class TextRegionFailure { INVALID_SELECTOR, REGION_MISSING, HTML_REQUIRED }

class TextRegionException(val failure: TextRegionFailure, cause: Throwable? = null, val issue: TextRegionIssue? = null) :
    IllegalArgumentException(
        if (issue == TextRegionIssue.MULTIPLE_MATCHES) "Screenshot monitoring supports one selected area. Choose one element or switch to Text." else when (failure) {
            TextRegionFailure.INVALID_SELECTOR -> "Use a valid CSS selector without regular-expression selectors."
            TextRegionFailure.REGION_MISSING -> "The selected region was not found. Check the selection or preview the page again."
            TextRegionFailure.HTML_REQUIRED -> "Region selection requires an HTML response."
        },
        cause,
    )
