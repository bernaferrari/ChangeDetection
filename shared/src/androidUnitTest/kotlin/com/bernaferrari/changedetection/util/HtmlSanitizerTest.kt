package com.bernaferrari.changedetection.util

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [HtmlSanitizer].
 *
 * Tests cover:
 * - Dangerous element removal (script/style/iframe)
 * - Event handler attribute removal
 * - javascript: URL neutralization
 * - Per-request dynamic-noise stripping (nonce/CSRF attributes, cache-busting params)
 * - Preservation of normal links and text
 * - Idempotency
 */
class HtmlSanitizerTest {

    // ========== Dangerous Element Tests ==========

    @Test
    fun `sanitize strips script content`() {
        val html = "<p>Keep me</p><script>alert('xss')</script>"
        val result = HtmlSanitizer.sanitize(html)

        assertFalse(result.contains("<script"))
        assertFalse(result.contains("alert"))
        assertTrue(result.contains("Keep me"))
    }

    @Test
    fun `sanitize strips style content`() {
        val html = "<style>body { background: red }</style><p>Visible</p>"
        val result = HtmlSanitizer.sanitize(html)

        assertFalse(result.contains("<style"))
        assertFalse(result.contains("background"))
        assertTrue(result.contains("Visible"))
    }

    @Test
    fun `sanitize strips iframe content`() {
        val html = "<iframe src=\"https://evil.example\"></iframe><p>Safe</p>"
        val result = HtmlSanitizer.sanitize(html)

        assertFalse(result.contains("<iframe"))
        assertTrue(result.contains("Safe"))
    }

    // ========== Event Handler Tests ==========

    @Test
    fun `sanitize removes event handler attributes`() {
        val html =
            "<div onclick=\"steal()\" onerror=\"boom()\" onload=\"pwn()\" " +
                "onmouseover=\"track()\">text</div>"
        val result = HtmlSanitizer.sanitize(html)

        assertFalse(result.contains("onclick"))
        assertFalse(result.contains("onerror"))
        assertFalse(result.contains("onload"))
        assertFalse(result.contains("onmouseover"))
        assertTrue(result.contains("text"))
    }

    // ========== JavaScript URL Tests ==========

    @Test
    fun `sanitize removes javascript urls`() {
        val html = "<a href=\"javascript:alert(1)\">click</a><img src=\"javascript:alert(2)\">"
        val result = HtmlSanitizer.sanitize(html)

        assertFalse(result.contains("javascript:"))
    }

    // ========== Dynamic Noise Attribute Tests ==========

    @Test
    fun `sanitize strips nonce csrf and integrity attributes`() {
        val html =
            "<div nonce=\"abc123\" data-csrf=\"tok1\" data-token=\"tok2\" " +
                "csrf-token=\"tok3\" integrity=\"sha384-xyz\" id=\"keepme\">content</div>"
        val result = HtmlSanitizer.sanitize(html)

        assertFalse(result.contains("nonce"))
        assertFalse(result.contains("data-csrf"))
        assertFalse(result.contains("data-token"))
        assertFalse(result.contains("csrf-token"))
        assertFalse(result.contains("integrity"))
        assertTrue(result.contains("id=\"keepme\""))
        assertTrue(result.contains("content"))
    }

    // ========== Cache-Busting Parameter Tests ==========

    @Test
    fun `sanitize strips numeric cache busting params from links`() {
        val result = HtmlSanitizer.sanitize("<a href=\"https://example.com/page?ts=1727262000000&id=42\">link</a>")

        assertTrue(result.contains("href=\"https://example.com/page?id=42\""))
    }

    @Test
    fun `sanitize strips all supported cache busting param names`() {
        val html =
            "<a id=\"a\" href=\"https://e.com/?cb=123456789&x=1\"></a>" +
                "<img src=\"https://e.com/i.png?t=1699&amp;timestamp=1699000000\">" +
                "<a id=\"b\" href=\"https://e.com/p?nonce=987654321&amp;q=2\"></a>" +
                "<img src=\"https://e.com/j.png?v=555555\">"
        val result = HtmlSanitizer.sanitize(html)

        assertFalse(result.contains("cb=123456789"))
        assertFalse(result.contains("t=1699"))
        assertFalse(result.contains("timestamp=1699000000"))
        assertFalse(result.contains("nonce=987654321"))
        assertFalse(result.contains("v=555555"))
        assertTrue(result.contains("x=1"))
        assertTrue(result.contains("q=2"))
    }

    @Test
    fun `sanitize keeps non numeric values for cache busting param names`() {
        val result = HtmlSanitizer.sanitize("<a href=\"https://example.com/?v=2.3&t=latest&page=about\">link</a>")

        assertTrue(result.contains("v=2.3"))
        assertTrue(result.contains("t=latest"))
        assertTrue(result.contains("page=about"))
    }

    @Test
    fun `sanitize removes query entirely when only cache busting params remain`() {
        val result = HtmlSanitizer.sanitize("<a href=\"https://example.com/page?ts=1727262000000\">link</a>")

        assertTrue(result.contains("href=\"https://example.com/page\""))
        assertFalse(result.contains("?"))
    }

    @Test
    fun `sanitize preserves fragment when stripping cache busting params`() {
        val result = HtmlSanitizer.sanitize("<a href=\"https://example.com/p?ts=123456789#section\">link</a>")

        assertTrue(result.contains("href=\"https://example.com/p#section\""))
    }

    @Test
    fun `sanitize leaves urls without query untouched`() {
        val url = "https://example.com/plain"
        val result = HtmlSanitizer.sanitize("<a href=\"$url\">link</a>")

        assertTrue(result.contains("href=\"$url\""))
    }

    // ========== Preservation Tests ==========

    @Test
    fun `sanitize leaves normal links and text intact`() {
        val html =
            "<div><h1>Title</h1><p>Some <b>bold</b> text.</p>" +
                "<a href=\"https://example.com/docs?section=intro&amp;lang=en\">Docs</a></div>"
        val result = HtmlSanitizer.sanitize(html)

        assertTrue(result.contains("<h1>Title</h1>"))
        assertTrue(result.contains("<p>Some <b>bold</b> text.</p>"))
        assertTrue(result.contains("href=\"https://example.com/docs?section=intro&amp;lang=en\""))
        assertTrue(result.contains("Docs"))
    }

    @Test
    fun `sanitize is idempotent`() {
        val html =
            "<html><head><script>alert(1)</script>" +
                "<link rel=\"stylesheet\" href=\"app.css?v=123456\"></head>" +
                "<body><div onclick=\"x()\" nonce=\"n1\" data-csrf=\"c1\">Hi</div>" +
                "<a href=\"https://example.com/?ts=1727262000000&amp;id=7\">L</a></body></html>"

        val once = HtmlSanitizer.sanitize(html)
        val twice = HtmlSanitizer.sanitize(once)

        assertEquals(once, twice)
    }
}
