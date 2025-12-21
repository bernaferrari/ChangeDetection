/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */

package com.bernaferrari.changedetection.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents a website being monitored for changes.
 *
 * Core properties for change detection:
 * - Basic info: title, URL, tags
 * - Monitoring: enabled/disabled, sync frequency
 * - Detection: CSS selector, XPath, element monitoring
 * - Notifications: enabled/disabled, threshold
 * - Browser mode: JavaScript rendering required
 * - Authentication: credentials, cookies, OAuth
 * - Filters: custom HTML/CSS normalization, noise filtering
 *
 * @param id Unique identifier for this site
 * @param title Human-readable title
 * @param url The website URL to monitor
 * @param timestamp When this site was added (milliseconds since epoch)
 * @param tags Optional tags for organization
 * @param colors Pair of primary and secondary colors for UI display
 * @param isSyncEnabled Whether automatic sync is enabled
 * @param isNotificationEnabled Whether notifications are enabled
 * @param threshold Pixel difference threshold for change detection
 * @param isBrowser Whether to render JavaScript (browser mode)
 */
@Serializable
data class Site(
    val id: String,
    val title: String?,
    val url: String,
    val timestamp: Long,
    val tags: String = "",
    val colors: Pair<Int, Int> = (0xFF2196F3.toInt() to 0xFF1976D2.toInt()),
    val isSyncEnabled: Boolean = true,
    val isNotificationEnabled: Boolean = true,
    val threshold: Int = 0,
    val isBrowser: Boolean = false,
    val cssSelector: String = "",
    val elementXPath: String = "",
    val elementMonitoring: Boolean = false,
    val userAgent: String = "",
    val followRedirects: Boolean = true,
    val requestTimeoutMs: Long = 30000,
    val notes: String = ""
) {
    /**
     * Get display title (falls back to URL if title is empty)
     */
    fun getDisplayTitle(): String = title.takeIf { !it.isNullOrEmpty() } ?: url

    /**
     * Check if any filters are active
     */
    fun hasFilters(): Boolean = cssSelector.isNotEmpty() || elementXPath.isNotEmpty() || elementMonitoring

    /**
     * Get active monitoring status text
     */
    fun getStatusText(): String = when {
        !isSyncEnabled -> "Paused"
        else -> "Monitoring"
    }

    companion object {
        /**
         * Create a test site for development/testing
         */
        fun createTest(
            id: String = "test-site",
            title: String = "Test Site",
            url: String = "https://example.com"
        ) = Site(
            id = id,
            title = title,
            url = url,
            timestamp = System.currentTimeMillis(),
            isSyncEnabled = true
        )
    }
}
