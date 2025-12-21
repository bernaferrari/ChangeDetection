/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */

package com.bernaferrari.changedetection.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents a snapshot of a website at a specific point in time.
 *
 * This is the core data model for ChangeDetection that represents:
 * - When the website was captured
 * - Which website it belongs to
 * - The state of the content (hash, size, etc.)
 * - Success/failure status of the capture
 *
 * @param snapId Unique identifier for this snapshot
 * @param siteId The site this snapshot belongs to
 * @param timestamp When this snapshot was taken (milliseconds since epoch)
 * @param contentHash SHA256 hash of the captured content for comparison
 * @param contentSize Size of the captured content in bytes
 * @param isSuccessful Whether the capture was successful
 * @param errorMessage Optional error message if capture failed
 * @param downloadTime Time taken to download the content in milliseconds
 */
@Serializable
data class Snap(
    val snapId: String,
    val siteId: String,
    val timestamp: Long,
    val contentHash: String,
    val contentSize: Long = 0,
    val isSuccessful: Boolean = true,
    val errorMessage: String = "",
    val downloadTime: Long = 0
) {
    /**
     * Check if this snapshot has a newer version available
     */
    fun isOlderThan(otherTimestamp: Long): Boolean = timestamp < otherTimestamp

    /**
     * Get human-readable size
     */
    fun getSizeString(): String = formatBytes(contentSize)

    companion object {
        private fun formatBytes(bytes: Long): String = when {
            bytes >= 1_000_000 -> "${bytes / 1_000_000}MB"
            bytes >= 1_000 -> "${bytes / 1_000}KB"
            else -> "${bytes}B"
        }

        /**
         * Create a test snap for development/testing
         */
        fun createTest(
            snapId: String = "test-snap",
            siteId: String = "test-site",
            timestamp: Long = System.currentTimeMillis()
        ) = Snap(
            snapId = snapId,
            siteId = siteId,
            timestamp = timestamp,
            contentHash = "abc123def456",
            contentSize = 5000,
            isSuccessful = true
        )
    }
}
