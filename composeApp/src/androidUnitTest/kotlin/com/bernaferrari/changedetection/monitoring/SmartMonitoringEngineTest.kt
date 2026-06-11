/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.monitoring

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SmartMonitoringEngine.
 *
 * Tests cover:
 * - Content filtering and processing
 * - Element zapping (ad removal)
 * - Change detection and classification
 * - Importance scoring
 * - Content type classification
 */
class SmartMonitoringEngineTest {

    private lateinit var engine: SmartMonitoringEngine

    @Before
    fun setup() {
        engine = SmartMonitoringEngine()
    }

    // ========== Content Processing Tests ==========

    @Test
    fun `processContent extracts meaningful text from HTML`() = runTest {
        // Arrange
        val html = """
            <html>
                <head><script>var x = 1;</script></head>
                <body>
                    <h1>Important Content</h1>
                    <p>This is the main text.</p>
                </body>
            </html>
        """.trimIndent()

        // Act
        val result = engine.processContent(html, emptyFilters())

        // Assert
        assertTrue(result.extractedContent.contains("Important Content"))
        assertTrue(result.extractedContent.contains("main text"))
        assertFalse(result.extractedContent.contains("script"))
    }

    @Test
    fun `processContent removes timestamps and UUIDs`() = runTest {
        // Arrange
        val content = "Data: 1704067200000 and uuid: 123e4567-e89b-12d3-a456-426614174000"

        // Act
        val result = engine.processContent(content, emptyFilters())

        // Assert
        assertTrue(result.extractedContent.contains("[TIMESTAMP]"))
        assertTrue(result.extractedContent.contains("[UUID]"))
    }

    // ========== Element Zapping Tests ==========

    @Test
    fun `zapElements removes elements by CSS selector`() = runTest {
        // Arrange
        val html = """
            <html>
                <body>
                    <div class="ad">Advertisement</div>
                    <div class="content">Real content</div>
                </body>
            </html>
        """.trimIndent()
        val rules = listOf(ZapRule.CssSelector(".ad"))

        // Act
        val result = engine.zapElements(html, rules)

        // Assert
        assertFalse(result.contains("Advertisement"))
        assertTrue(result.contains("Real content"))
    }

    @Test
    fun `zapElements removes multiple element types`() = runTest {
        // Arrange
        val html = """
            <html>
                <body>
                    <script>console.log('ad')</script>
                    <style>.ad { color: red; }</style>
                    <nav>Navigation</nav>
                    <article>Article content</article>
                </body>
            </html>
        """.trimIndent()
        val rules = listOf(
            ZapRule.ElementType("script"),
            ZapRule.ElementType("style"),
            ZapRule.ElementType("nav"),
        )

        // Act
        val result = engine.zapElements(html, rules)

        // Assert
        assertFalse(result.contains("console.log"))
        assertFalse(result.contains("Navigation"))
        assertTrue(result.contains("Article content"))
    }

    @Test
    fun `zapElements handles malformed selectors gracefully`() = runTest {
        // Arrange
        val html = "<html><body>Content</body></html>"
        val rules = listOf(
            ZapRule.CssSelector("[invalid[selector"), // Malformed
            ZapRule.CssSelector("body"), // Valid
        )

        // Act - Should not throw
        val result = engine.zapElements(html, rules)

        // Assert - At least content processing should work
        assertNotNull(result)
    }

    // ========== Change Classification Tests ==========

    @Test
    fun `detectSmartChanges identifies price changes`() = runTest {
        // Arrange
        val oldContent = "Price: $100"
        val newContent = "Price: $150"

        // Act
        val result = engine.processContent(newContent, emptyFilters(), oldContent)

        // Assert
        assertTrue(result.changes.isNotEmpty())
        val priceChanges = result.changes.filter { it.category == ChangeCategory.PRICE }
        assertTrue(priceChanges.isNotEmpty())
    }

    @Test
    fun `detectSmartChanges calculates correct significance`() = runTest {
        // Arrange
        val oldContent = "Short"
        val newContent = "This is a much longer piece of content with more than one hundred characters for testing significance calculation"

        // Act
        val result = engine.processContent(newContent, emptyFilters(), oldContent)

        // Assert
        assertTrue(result.changes.isNotEmpty())
        assertTrue(
            result.changes.any {
                it.significance == ChangeSignificance.CRITICAL ||
                    it.significance == ChangeSignificance.IMPORTANT
            },
        )
    }

    // ========== Content Type Classification Tests ==========

    @Test
    fun `classifyContentType detects ECOMMERCE for price content`() = runTest {
        // Arrange
        val content = "Buy now for $99.99! Limited offer."

        // Act
        val result = engine.processContent(content, emptyFilters())

        // Assert
        assertEquals(ContentType.ECOMMERCE, result.contentType)
    }

    @Test
    fun `classifyContentType detects INVENTORY for stock content`() = runTest {
        // Arrange
        val content = "Available in stock. Check availability."

        // Act
        val result = engine.processContent(content, emptyFilters())

        // Assert
        assertEquals(ContentType.INVENTORY, result.contentType)
    }

    @Test
    fun `classifyContentType detects SCHEDULE for date content`() = runTest {
        // Arrange
        val content = "Scheduled for 2024-01-15. Check the schedule."

        // Act
        val result = engine.processContent(content, emptyFilters())

        // Assert
        assertEquals(ContentType.SCHEDULE, result.contentType)
    }

    // ========== Importance Score Tests ==========

    @Test
    fun `calculateImportanceScore returns zero for no changes`() = runTest {
        // Arrange
        val content = "Same content"

        // Act
        val result = engine.processContent(content, emptyFilters())

        // Assert
        assertEquals(0f, result.importanceScore, 0.01f)
    }

    @Test
    fun `calculateImportanceScore increases with price changes`() = runTest {
        // Arrange
        val oldContent = "Price: $100"
        val newContent = "Price: $200"

        // Act
        val result = engine.processContent(newContent, emptyFilters(), oldContent)

        // Assert
        assertTrue(result.importanceScore > 0f)
    }

    // ========== Metadata Extraction Tests ==========

    @Test
    fun `extractMetadata detects prices`() = runTest {
        // Arrange
        val content = "Item costs $50 and €30"

        // Act
        val result = engine.processContent(content, emptyFilters())

        // Assert
        assertTrue(result.metadata.hasPrices)
    }

    @Test
    fun `extractMetadata detects URLs`() = runTest {
        // Arrange
        val content = "Visit https://example.com for more"

        // Act
        val result = engine.processContent(content, emptyFilters())

        // Assert
        assertTrue(result.metadata.hasUrls)
    }

    @Test
    fun `extractMetadata detects emails`() = runTest {
        // Arrange
        val content = "Contact us at test@example.com"

        // Act
        val result = engine.processContent(content, emptyFilters())

        // Assert
        assertTrue(result.metadata.hasEmails)
    }

    @Test
    fun `extractMetadata detects dates`() = runTest {
        // Arrange
        val content = "Event on 2024-01-15 or 01/20/2024"

        // Act
        val result = engine.processContent(content, emptyFilters())

        // Assert
        assertTrue(result.metadata.hasDates)
    }

    @Test
    fun `extractMetadata calculates word count`() = runTest {
        // Arrange
        val content = "One two three four five"

        // Act
        val result = engine.processContent(content, emptyFilters())

        // Assert
        assertEquals(5, result.metadata.wordCount)
    }

    // ========== Smart Filter Creation Tests ==========

    @Test
    fun `createNoiseReductionFilters includes ad removers`() = runTest {
        // Arrange
        val content = "Sample"

        // Act
        val filters = engine.createSmartFilters(content, SmartFilterType.NOISE_REDUCTION)

        // Assert
        assertTrue(filters.any { it is ContentFilter.CssElementFilter && it.selector.contains("ad") })
    }

    @Test
    fun `createTrackingRemovalFilters includes analytics removers`() = runTest {
        // Arrange
        val content = "Sample"

        // Act
        val filters = engine.createSmartFilters(content, SmartFilterType.TRACKING_REMOVAL)

        // Assert
        assertTrue(
            filters.any {
                it is ContentFilter.CssElementFilter &&
                    it.selector.contains("analytics")
            },
        )
    }

    @Test
    fun `createDynamicContentFilters removes timestamps and UUIDs`() = runTest {
        // Arrange
        val content = "Sample"

        // Act
        val filters = engine.createSmartFilters(content, SmartFilterType.DYNAMIC_CONTENT)

        // Assert
        assertTrue(
            filters.any {
                it is ContentFilter.RegexFilter &&
                    it.pattern.contains("\\d")
            },
        )
    }

    // ========== Reading Level Tests ==========

    @Test
    fun `readingLevel is calculated for content`() = runTest {
        // Arrange
        val content = "This is a simple sentence for testing reading level."

        // Act
        val result = engine.processContent(content, emptyFilters())

        // Assert
        assertNotNull(result.metadata.readingLevel)
    }

    // ========== Helper Methods ==========

    private fun emptyFilters(): List<ContentFilter> = emptyList()
}
