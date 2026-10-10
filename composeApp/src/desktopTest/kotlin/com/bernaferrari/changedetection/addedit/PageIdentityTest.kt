/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.addedit

import org.junit.Assert.assertEquals
import org.junit.Test

class PageIdentityTest {
    @Test fun appleTouchIconWinsOverSmallFavicons() {
        val html = """
            <head>
            <link rel="icon" href="/favicon-16.png" sizes="16x16">
            <link rel="icon" type="image/svg+xml" href="/icon.svg">
            <link rel="mask-icon" href="/mask.svg">
            <link rel="apple-touch-icon" href="/apple.png">
            </head>
        """.trimIndent()
        assertEquals("https://g1.globo.com/apple.png", htmlIcon(html, "https://g1.globo.com/news/today"))
    }

    @Test fun largestRasterIconAndRelativeAddresses() {
        val html = """<link href="img/i32.png" rel="shortcut icon" sizes="32x32"><link rel="icon" sizes="192x192" href="//cdn.example.com/i192.png?v=1&amp;x=2">"""
        assertEquals("https://cdn.example.com/i192.png?v=1&x=2", htmlIcon(html, "https://example.com/a/b"))
        assertEquals("https://example.com/a/img/i32.png", htmlIcon("""<link rel="icon" href="img/i32.png">""", "https://example.com/a/b"))
    }

    @Test fun pagesWithoutIconsFallBackToFaviconIco() {
        assertEquals("https://example.com/favicon.ico", htmlIcon("<html><head><title>x</title></head></html>", "https://example.com/page"))
    }

    @Test fun longTitlesKeepTheirBrand() {
        assertEquals("g1", cleanTitle("g1 - O portal de notícias da Globo"))
        assertEquals("Amazon.com: Online Shopping", cleanTitle("Amazon.com: Online Shopping"))
    }
}
