/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.util

import com.fleeksoft.ksoup.Ksoup

/**
 * HTML sanitizer to prevent XSS attacks when rendering untrusted content.
 *
 * This sanitizer removes dangerous elements and attributes while preserving
 * safe HTML for rendering.
 *
 * In addition to XSS hardening, [sanitize] strips per-request dynamic noise so that
 * pages embedding CSRF nonces or cache-busting timestamps do not produce false-positive
 * content changes when hashed (see [ContentHasher.hashHtmlContent]). Exactly the following
 * is stripped, conservatively and only at the attribute level (text nodes are never altered):
 *
 * - Dynamic-noise attributes removed from every element:
 *   `nonce`, `data-csrf`, `data-token`, `csrf-token`, `integrity`.
 * - Cache-busting query parameters removed from `href` and `src` attribute URLs:
 *   parameters named `ts`, `t`, `timestamp`, `nonce`, `cb`, or `v` whose value is purely
 *   numeric (`^[0-9]+$`). Non-numeric values (e.g. `v=2.3` version strings) are preserved,
 *   and URLs without a query string are untouched.
 *
 * Both removals keep the sanitized output safe for rendering; they only drop attributes
 * and URL parameters that carry no user-visible content.
 */
object HtmlSanitizer {
    /**
     * Sanitize HTML content to prevent XSS attacks.
     * Uses a whitelist approach to only allow safe tags and attributes.
     */
    fun sanitize(html: String): String = try {
        val doc = Ksoup.parse(html)

        // Remove script and style tags
        doc.select("script").remove()
        doc.select("style").remove()
        doc.select("iframe").remove()
        doc.select("object").remove()
        doc.select("embed").remove()
        doc.select("link").remove()

        // Per-request dynamic noise: CSRF/nonce/integrity attributes change on every fetch
        // and must not influence rendered output or content hashes.
        doc.select("*").forEach { element -> noiseAttributes.forEach { element.removeAttr(it) } }

        val allElements = doc.select("*")
        val eventHandlers =
            listOf(
                "onerror",
                "onload",
                "onclick",
                "onmouseover",
                "onmouseout",
                "onkeydown",
                "onkeyup",
                "onfocus",
                "onblur",
                "onchange",
                "onsubmit",
                "ondblclick",
                "onmouseenter",
                "onmouseleave",
            )

        allElements.forEach { element ->
            // Remove event handler attributes
            eventHandlers.forEach { handler ->
                if (element.hasAttr(handler)) {
                    element.removeAttr(handler)
                }
            }

            listOf("href", "src").forEach { attr ->
                if (element.hasAttr(attr)) {
                    val attrValue = element.attr(attr)
                    if (attrValue.trim().lowercase().startsWith("javascript:")) {
                        element.removeAttr(attr)
                        return@forEach
                    }

                    // Strip cache-busting query params so per-fetch timestamps/nonces do not
                    // change the hash. Safe for rendering: only the URL query is rewritten.
                    val cleaned = stripCacheBustingParams(attrValue)
                    if (cleaned != attrValue) {
                        element.attr(attr, cleaned)
                    }
                }
            }
        }

        // Return cleaned HTML body
        doc.body().html()
    } catch (e: Exception) {
        // Return original on error (or empty string if safer)
        html
    }

    /** Attributes carrying per-request tokens that must not affect hashing or rendering. */
    private val noiseAttributes =
        listOf(
            "nonce",
            "data-csrf",
            "data-token",
            "csrf-token",
            "integrity",
        )

    /** Query parameter names commonly used for cache busting. */
    private val cacheBustingParams = setOf("ts", "t", "timestamp", "nonce", "cb", "v")

    /** A value that looks like a timestamp/counter rather than meaningful content. */
    private val numericValue = Regex("""^\d+$""")

    /**
     * Removes cache-busting query parameters (numeric-valued [cacheBustingParams])
     * from a URL string. Leaves URLs without a query, empty queries, non-matching
     * parameters, and fragments untouched.
     */
    private fun stripCacheBustingParams(url: String): String {
        val queryStart = url.indexOf('?')
        if (queryStart == -1) return url

        val fragmentStart = url.indexOf('#', queryStart)
        val query =
            if (fragmentStart == -1) url.substring(queryStart + 1) else url.substring(queryStart + 1, fragmentStart)
        if (query.isEmpty()) return url

        val keptParams =
            query
                .split('&')
                .filter { it.isNotEmpty() }
                .filterNot { param ->
                    val name = param.substringBefore('=').lowercase()
                    val value = param.substringAfter('=', "")
                    name in cacheBustingParams && numericValue.matches(value)
                }

        val prefix = url.substring(0, queryStart)
        val fragment = if (fragmentStart == -1) "" else url.substring(fragmentStart)
        return if (keptParams.isEmpty()) prefix + fragment else "$prefix?${keptParams.joinToString("&")}$fragment"
    }
}
