/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.detection

import com.bernaferrari.changedetection.util.readableSnapshotText
import com.fleeksoft.ksoup.Ksoup

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
        if (include.isNotBlank() && matches.isEmpty()) throw TextRegionException(TextRegionFailure.REGION_MISSING)
        // Select outermost surviving subtrees before mutation, retaining document order.
        val roots = matches.filter { element ->
            generateSequence(element.parent()) { it.parent() }.none { it in matches }
        }
        excluded.forEach { it.remove() }
        val result = if (include.isBlank()) document.outerHtml() else roots.joinToString("\n") { it.outerHtml() }
        if (readableSnapshotText(result, true).isBlank()) throw TextRegionException(TextRegionFailure.REGION_MISSING)
        return result
    }

    private fun validateSelector(selector: String) {
        if (selector.isBlank()) return
        // Regex pseudos are not needed for region selection and can block a capture thread.
        if (selector.length > 4_096 || selector.contains(Regex(":matches(?:Own|WholeText|WholeOwnText)?\\s*\\(", RegexOption.IGNORE_CASE))) {
            throw TextRegionException(TextRegionFailure.INVALID_SELECTOR)
        }
        try {
            Ksoup.parse("").select(selector.trim())
        } catch (invalid: com.fleeksoft.ksoup.select.Selector.SelectorParseException) {
            throw TextRegionException(TextRegionFailure.INVALID_SELECTOR, invalid)
        }
    }
}

enum class TextRegionFailure { INVALID_SELECTOR, REGION_MISSING, HTML_REQUIRED }

class TextRegionException(val failure: TextRegionFailure, cause: Throwable? = null) :
    IllegalArgumentException(
        when (failure) {
            TextRegionFailure.INVALID_SELECTOR -> "Use a valid CSS selector without regular-expression selectors."
            TextRegionFailure.REGION_MISSING -> "The selected region was not found. Check the selection or preview the page again."
            TextRegionFailure.HTML_REQUIRED -> "Region selection requires an HTML response."
        },
        cause,
    )
