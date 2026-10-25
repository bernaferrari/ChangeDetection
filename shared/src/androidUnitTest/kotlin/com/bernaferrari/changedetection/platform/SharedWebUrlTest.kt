package com.bernaferrari.changedetection.platform

import org.junit.Assert.*
import org.junit.Test

class SharedWebUrlTest {
    @Test fun extractsWebLinkFromSharedProseWithoutTrailingPunctuation() {
        assertEquals("https://example.test/page", extractSharedWebUrl("Look at this:\nhttps://example.test/page)."))
    }
    @Test fun ignoresNonWebSchemesAndBoundsUntrustedText() {
        assertNull(extractSharedWebUrl("javascript:alert(1) file:///private/account"))
        assertNull(extractSharedWebUrl("x".repeat(16_384) + " https://example.test"))
        assertNull(extractSharedWebUrl("https://example.test/" + "x".repeat(8_192)))
    }
}
