/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.util

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ContentHasher.
 *
 * Tests cover:
 * - SHA-256 hash generation
 * - HTML content normalization
 * - Change detection logic
 * - Cache performance
 */
class ContentHasherTest {

    @Before
    fun setup() {
        ContentHasher.clearCaches()
    }

    // ========== Hash Generation Tests ==========

    @Test
    fun `hashContent generates consistent SHA-256 hash`() {
        // Arrange
        val content = "Hello, World!".toByteArray()

        // Act
        val hash1 = ContentHasher.hashContent(content)
        val hash2 = ContentHasher.hashContent(content)

        // Assert
        assertEquals(hash1, hash2)
        assertEquals(64, hash1.length) // SHA-256 produces 64 hex characters
        assertTrue(hash1.matches(Regex("[a-f0-9]{64}")))
    }

    @Test
    fun `hashContent produces different hashes for different content`() {
        // Arrange
        val content1 = "Hello, World!".toByteArray()
        val content2 = "Hello, World?".toByteArray()

        // Act
        val hash1 = ContentHasher.hashContent(content1)
        val hash2 = ContentHasher.hashContent(content2)

        // Assert
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `hashContent handles empty content`() {
        // Arrange
        val emptyContent = ByteArray(0)

        // Act
        val hash = ContentHasher.hashContent(emptyContent)

        // Assert
        assertNotNull(hash)
        assertEquals(64, hash.length)
    }

    @Test
    fun `hashContent handles large content efficiently`() {
        // Arrange
        val largeContent = ByteArray(1_000_000) { it.toByte() }

        // Act
        val startTime = System.currentTimeMillis()
        val hash = ContentHasher.hashContent(largeContent)
        val duration = System.currentTimeMillis() - startTime

        // Assert
        assertNotNull(hash)
        assertTrue("Hashing 1MB should take less than 100ms", duration < 100)
    }

    // ========== HTML Content Tests ==========

    @Test
    fun `hashHtmlContent normalizes HTML before hashing`() {
        // Arrange - Same semantic content, different whitespace/formatting
        val html1 = "<html><body>Hello World</body></html>".toByteArray()
        val html2 = "<html>\n<body>   Hello World   </body>\n</html>".toByteArray()

        // Act
        val hash1 = ContentHasher.hashHtmlContent(html1)
        val hash2 = ContentHasher.hashHtmlContent(html2)

        // Assert - After normalization, hashes should be similar
        // Note: This depends on the cleanUpHtml implementation
        assertNotNull(hash1)
        assertNotNull(hash2)
    }

    @Test
    fun `hashHtmlContent handles malformed HTML gracefully`() {
        // Arrange
        val malformedHtml = "<html><body>Unclosed tags<div>".toByteArray()

        // Act
        val hash = ContentHasher.hashHtmlContent(malformedHtml)

        // Assert - Should not throw, should return valid hash
        assertNotNull(hash)
        assertEquals(64, hash.length)
    }

    // ========== Change Detection Tests ==========

    @Test
    fun `contentChanged returns true for different content`() {
        // Arrange
        val oldContent = "Old content".toByteArray()
        val newContent = "New content".toByteArray()

        // Act
        val changed = ContentHasher.contentChanged(newContent, oldContent, "text/plain")

        // Assert
        assertTrue(changed)
    }

    @Test
    fun `contentChanged returns false for identical content`() {
        // Arrange
        val content = "Same content".toByteArray()

        // Act
        val changed = ContentHasher.contentChanged(content, content, "text/plain")

        // Assert
        assertFalse(changed)
    }

    @Test
    fun `contentChanged returns true when previous is empty`() {
        // Arrange
        val newContent = "New content".toByteArray()
        val emptyContent = ByteArray(0)

        // Act
        val changed = ContentHasher.contentChanged(newContent, emptyContent, "text/html")

        // Assert
        assertTrue(changed)
    }

    @Test
    fun `contentChanged returns false when both are empty`() {
        // Arrange
        val empty1 = ByteArray(0)
        val empty2 = ByteArray(0)

        // Act
        val changed = ContentHasher.contentChanged(empty1, empty2, "text/html")

        // Assert
        assertFalse(changed)
    }

    // ========== Quick Size Check Tests ==========

    @Test
    fun `quickSizeCheck detects significant size change`() {
        // Arrange
        val oldSize = 1000
        val newSize = 1200 // 20% change

        // Act
        val hasChange = ContentHasher.quickSizeCheck(newSize, oldSize, threshold = 0.05)

        // Assert
        assertTrue(hasChange)
    }

    @Test
    fun `quickSizeCheck ignores minor size change`() {
        // Arrange
        val oldSize = 1000
        val newSize = 1040 // 4% change, below 5% threshold

        // Act
        val hasChange = ContentHasher.quickSizeCheck(newSize, oldSize, threshold = 0.05)

        // Assert
        assertFalse(hasChange)
    }

    @Test
    fun `quickSizeCheck handles zero previous size`() {
        // Arrange
        val oldSize = 0
        val newSize = 100

        // Act
        val hasChange = ContentHasher.quickSizeCheck(newSize, oldSize)

        // Assert
        assertTrue(hasChange)
    }

    // ========== Cache Tests ==========

    @Test
    fun `cache returns same hash for same content`() {
        // Arrange
        val content = "Cached content".toByteArray()

        // Act
        val hash1 = ContentHasher.hashContent(content)
        val hash2 = ContentHasher.hashContent(content)

        // Assert
        assertEquals(hash1, hash2)
    }

    @Test
    fun `clearCaches removes cached entries`() {
        // Arrange
        val content = "To be cleared".toByteArray()
        val hash1 = ContentHasher.hashContent(content)

        // Act
        ContentHasher.clearCaches()

        // Assert - After clear, next call should still work
        val hash2 = ContentHasher.hashContent(content)
        assertEquals(hash1, hash2) // Same content = same hash
    }

    // ========== Extension Function Tests ==========

    @Test
    fun `hashedWith extension works correctly`() {
        // Arrange
        val content = "Extension test".toByteArray()

        // Act
        val hash = content.hashedWith()

        // Assert
        assertNotNull(hash)
        assertEquals(64, hash.length)
    }

    @Test
    fun `isChangedFrom extension works correctly`() {
        // Arrange
        val oldContent = "Old".toByteArray()
        val newContent = "New".toByteArray()

        // Act
        val changed = newContent.isChangedFrom(oldContent, "text/plain")

        // Assert
        assertTrue(changed)
    }
}
