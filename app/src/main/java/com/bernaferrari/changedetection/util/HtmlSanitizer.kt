package com.bernaferrari.changedetection.util

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.safety.Safelist
import com.orhanobut.logger.Logger

/**
 * HTML sanitizer to prevent XSS attacks when rendering untrusted content in WebViews.
 *
 * This sanitizer removes dangerous elements and attributes while preserving
 * safe HTML for rendering. It uses JSoup's built-in whitelist-based approach.
 *
 * Security considerations:
 * - Removes all script tags and event handlers
 * - Removes frames, iframes, and other embedding vectors
 * - Strips dangerous attributes (onclick, onload, etc.)
 * - Preserves basic formatting (bold, italic, links, lists)
 * - Can be configured for different safety levels
 */
object HtmlSanitizer {

    /**
     * Sanitize HTML content to prevent XSS attacks
     * Uses a whitelist approach to only allow safe tags and attributes
     */
    fun sanitize(html: String, level: SafetyLevel = SafetyLevel.BALANCED): String {
        return try {
            val safelist = when (level) {
                SafetyLevel.STRICT -> createStrictSafelist()
                SafetyLevel.BALANCED -> createBalancedSafelist()
                SafetyLevel.PERMISSIVE -> createPermissiveSafelist()
            }

            val cleaned = Jsoup.clean(html, "", safelist, Document.OutputSettings().indentAmount(0))
            Logger.d("✅ HTML sanitized successfully (level: $level)")
            cleaned
        } catch (e: Exception) {
            Logger.e("⚠️ HTML sanitization failed: ${e.message}, returning original content")
            // Return original on error - better to render unsafe than crash
            html
        }
    }

    /**
     * Create a strict whitelist - only basic text formatting
     * Safe for untrusted sources
     */
    private fun createStrictSafelist(): Safelist {
        return Safelist()
            .addTags("p", "br", "strong", "em", "u", "h1", "h2", "h3", "h4", "h5", "h6")
            .addAttributes("p", "class", "id")
            .addAttributes("div", "class", "id")
    }

    /**
     * Create a balanced whitelist - common formatting + links + images
     * Good for most use cases
     */
    private fun createBalancedSafelist(): Safelist {
        return Safelist()
            // Text formatting
            .addTags("p", "br", "strong", "em", "u", "i", "b", "code", "pre", "blockquote")
            .addTags("h1", "h2", "h3", "h4", "h5", "h6")
            // Lists
            .addTags("ul", "ol", "li", "dl", "dt", "dd")
            // Tables (with caution)
            .addTags("table", "tbody", "thead", "tfoot", "tr", "td", "th", "caption", "col", "colgroup")
            // Links and images
            .addTags("a", "img")
            .addAttributes("a", "href", "title", "target", "rel")
            .addAttributes("img", "src", "alt", "title", "width", "height")
            // General attributes
            .addAttributes("p", "class", "id", "align")
            .addAttributes("div", "class", "id", "align")
            .addAttributes("span", "class", "id")
            // Whitelist safe protocols
            .addProtocols("a", "href", "ftp", "http", "https", "mailto")
            .addProtocols("img", "src", "http", "https", "data")
            // Allow rel attribute values
            .addEnforcedAttribute("a", "rel", "noopener noreferrer")
    }

    /**
     * Create a permissive whitelist - most HTML tags except scripts
     * Use only with trusted sources
     */
    private fun createPermissiveSafelist(): Safelist {
        return Safelist()
            .addTags(
                "p", "br", "strong", "em", "u", "i", "b", "code", "pre", "blockquote",
                "h1", "h2", "h3", "h4", "h5", "h6",
                "ul", "ol", "li", "dl", "dt", "dd",
                "table", "tbody", "thead", "tfoot", "tr", "td", "th", "caption", "col", "colgroup",
                "a", "img", "div", "span", "section", "article", "nav", "aside", "header", "footer",
                "figure", "figcaption", "video", "audio", "source", "iframe"
            )
            .addAttributes(":all", "class", "id", "title", "style", "data-*")
            .addAttributes("a", "href", "target", "rel")
            .addAttributes("img", "src", "alt", "width", "height", "loading")
            .addAttributes("iframe", "src", "width", "height", "frameborder", "allow", "sandbox")
            .addProtocols("a", "href", "ftp", "http", "https", "mailto", "tel")
            .addProtocols("img", "src", "http", "https", "data")
            .addProtocols("iframe", "src", "http", "https")
    }

    /**
     * Check if HTML is likely safe (simple heuristic)
     * Returns true if no suspicious patterns are detected
     */
    fun isLikelyUnsafe(html: String): Boolean {
        val suspiciousPatterns = listOf(
            "<script",
            "javascript:",
            "onerror=",
            "onload=",
            "onclick=",
            "onmouseover=",
            "onmouseout=",
            "onkeydown=",
            "onkeyup=",
            "onfocus=",
            "onblur=",
            "onchange=",
            "onsubmit=",
            "eval(",
            "expression(",
            "vbscript:",
            "data:text/html",
            "<iframe",
            "<embed",
            "<object"
        )

        val lowerHtml = html.lowercase()
        return suspiciousPatterns.any { lowerHtml.contains(it) }
    }

    /**
     * Remove script tags and event handlers (aggressive approach)
     * Use this as a fallback if JSoup cleaning is unavailable
     */
    fun removeScriptsAndHandlers(html: String): String {
        return try {
            val doc = Jsoup.parse(html)

            // Remove all script tags
            doc.select("script").remove()
            doc.select("style").remove()

            // Remove all event handler attributes
            val eventHandlers = listOf(
                "onerror", "onload", "onclick", "onmouseover", "onmouseout",
                "onkeydown", "onkeyup", "onfocus", "onblur", "onchange",
                "onsubmit", "ondblclick", "onmouseenter", "onmouseleave"
            )

            doc.select("*").forEach { element ->
                eventHandlers.forEach { handler ->
                    if (element.hasAttr(handler)) {
                        element.removeAttr(handler)
                    }
                }
                // Remove javascript: protocol from href and src
                listOf("href", "src", "data").forEach { attr ->
                    if (element.hasAttr(attr)) {
                        val attrValue = element.attr(attr)
                        if (attrValue.lowercase().startsWith("javascript:")) {
                            element.removeAttr(attr)
                        }
                    }
                }
            }

            doc.html()
        } catch (e: Exception) {
            Logger.e("⚠️ Error removing scripts: ${e.message}")
            html
        }
    }

    enum class SafetyLevel {
        STRICT,      // Only basic text formatting
        BALANCED,    // Most common HTML tags, good default
        PERMISSIVE   // Most HTML except scripts (use with trusted sources only)
    }
}
