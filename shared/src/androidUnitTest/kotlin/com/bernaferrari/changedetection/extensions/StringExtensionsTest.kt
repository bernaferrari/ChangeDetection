/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.extensions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StringExtensionsTest {
    @Test
    fun `canonical key ignores host case default port fragment and trailing slash`() {
        val first = "HTTPS://Example.COM:443/products/#details".canonicalUrlKey()
        val second = "https://example.com/products".canonicalUrlKey()

        assertEquals(second, first)
    }

    @Test
    fun `canonical key preserves path and query casing`() {
        assertNotEquals(
            "https://example.com/Product?SKU=A".canonicalUrlKey(),
            "https://example.com/product?sku=a".canonicalUrlKey(),
        )
    }

    @Test
    fun `canonical key treats bare host as https root`() {
        assertEquals(
            "https://example.com/",
            "example.com".canonicalUrlKey(),
        )
    }

    @Test
    fun `canonical key rejects non-web addresses`() {
        assertNull("javascript:alert(1)".canonicalUrlKey())
    }
}
