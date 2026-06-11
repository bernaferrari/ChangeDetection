/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.util

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document

/**
 * HTML sanitizer to prevent XSS attacks when rendering untrusted content.
 *
 * This sanitizer removes dangerous elements and attributes while preserving
 * safe HTML for rendering.
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

        // Remove event handlers from all elements
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

            // Remove javascript: protocol from href and src
            listOf("href", "src", "data").forEach { attr ->
                if (element.hasAttr(attr)) {
                    val attrValue = element.attr(attr)
                    if (attrValue.trim().lowercase().startsWith("javascript:")) {
                        element.removeAttr(attr)
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
}
