package com.bernaferrari.changedetection.ui.comparison

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecureSnapshotDocumentTest {
    @Test
    fun `stored snapshot strips executable navigable and remote content`() {
        val result = secureSnapshotDocument(
            """
            <script>alert('xss')</script>
            <a href="https://example.com/escape" onclick="alert(1)">Open</a>
            <img src="https://example.com/tracker.png" onerror="alert(2)">
            <form action="https://example.com/post"><input name="secret"></form>
            <iframe src="file:///data/data/private"></iframe>
            <p style="background: url(https://example.com/pixel)">Snapshot text</p>
            """.trimIndent(),
        ).lowercase()

        assertTrue(result.contains("content-security-policy"))
        assertTrue(result.contains("default-src 'none'"))
        assertTrue(result.contains("snapshot text"))
        assertFalse(result.contains("<script"))
        assertFalse(result.contains("<iframe"))
        assertFalse(result.contains("<form"))
        assertFalse(result.contains("onclick="))
        assertFalse(result.contains("onerror="))
        assertFalse(result.contains("https://"))
        assertFalse(result.contains("file://"))
    }

    @Test
    fun `stored snapshot preserves self contained layout and body identity`() {
        val result = secureSnapshotDocument(
            """<html lang="pt" dir="rtl" class="theme"><head><style>
            .schedule > h1 { color: #31442b; font-family: serif; }
            .schedule { background: #f4f1e6; padding: 28px; }
            </style><style media="print">.print { display: none; }</style></head><body class="schedule" style="margin: 12px" onclick="alert(1)">
            <h1>Conference program</h1><p>Opening session</p></body></html>""",
        )
        val document = com.fleeksoft.ksoup.Ksoup.parse(result)
        assertTrue(document.body().hasClass("schedule"))
        assertTrue(document.body().attr("style") == "margin: 12px")
        assertTrue(document.selectFirst("html")?.attr("dir") == "rtl")
        assertTrue(document.selectFirst("html")?.attr("lang") == "pt")
        assertTrue(document.select("style").any { ".schedule > h1" in it.data() })
        assertFalse(result.contains("onclick="))
        assertTrue(document.selectFirst("style[media=print]")?.data()?.contains(".print") == true)
        assertTrue(result.contains("default-src 'none'"))
    }

    @Test
    fun `remote escaped and oversized styles are omitted without discarding content`() {
        val result = secureSnapshotDocument(
            """<style>@import 'https://example.test/remote.css';</style>
            <style>.remote { background: url(https://example.test/pixel); }</style>
            <style>.escaped { color: r\65 d; }</style>
            <style>${" ".repeat(256_001)}.oversized { color: red; }</style>
            <style>.local { color: green; }</style><p class="local">Still readable</p>""",
        )
        assertFalse(result.contains("https://"))
        assertFalse(result.contains(".escaped"))
        assertFalse(result.contains(".oversized"))
        assertTrue(result.contains(".local { color: green; }"))
        assertTrue(result.contains("Still readable"))
    }

    @Test
    fun `stored snapshot keeps inert inline raster images`() {
        val result = secureSnapshotDocument(
            """<img alt="Chart" src="data:image/png;base64,aGVsbG8=">""",
        )

        assertTrue(result.contains("alt=\"Chart\""))
        assertTrue(result.contains("src=\"data:image/png;base64,aGVsbG8=\""))
    }
}
