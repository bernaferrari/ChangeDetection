package com.bernaferrari.changedetection.util

import com.bernaferrari.changedetection.extensions.cleanUpHtml
import com.bernaferrari.changedetection.repo.CaptureMode

object ContentHasher {

    /** Versioned policy hashes make a rule change a baseline, never a false content alert. */
    fun detectionHash(content: ByteArray, contentType: String, mode: CaptureMode): String = when (mode) {
        CaptureMode.TEXT -> "text-v1:" + hashContent(
            readableSnapshotText(content.decodeToString(), contentType.contains("html", true)).encodeToByteArray(),
        )

        CaptureMode.SOURCE -> "source-v1:" + hashContent(content)

        else -> if (contentType.contains("html", true)) hashHtmlContent(content) else hashContent(content)
    }

    fun detectionPolicy(hash: String): String = when {
        hash.startsWith("image-v2:") || hash.startsWith("image-v3:") || hash.startsWith("screenshot-v1:") || hash.startsWith("text-region-v1:") || hash.startsWith("text-region-v2:") -> hash.substringBeforeLast(':')
        hash.startsWith("text-v1:") -> "text-v1"
        hash.startsWith("source-v1:") -> "source-v1"
        else -> "legacy"
    }

    /**
     * Generate SHA-256 hash for raw content
     */
    fun hashContent(content: ByteArray): String = CryptoUtils.sha256(content)

    /**
     * Generate hash for HTML content after cleanup
     */
    fun hashHtmlContent(content: ByteArray): String {
        val contentString = content.decodeToString()

        // Normalize without retaining complete page bodies in memory.
        val cleanedHtml = getCleanedHtml(contentString)

        // Hash the cleaned content
        return hashContent(cleanedHtml.encodeToByteArray())
    }

    /**
     * Compare two content pieces efficiently using hashes
     */
    fun contentChanged(
        newContent: ByteArray,
        previousContent: ByteArray,
        contentType: String,
    ): Boolean {
        if (newContent.isEmpty()) return previousContent.isNotEmpty()
        if (previousContent.isEmpty()) return true

        return if (contentType.contains("html", ignoreCase = true)) {
            // For HTML, use cleaned comparison
            val newHash = hashHtmlContent(newContent)
            val prevHash = hashHtmlContent(previousContent)
            newHash != prevHash
        } else {
            // For other content types, use direct comparison
            val newHash = hashContent(newContent)
            val prevHash = hashContent(previousContent)
            newHash != prevHash
        }
    }

    /**
     * Fast content size-based pre-filtering
     */
    fun quickSizeCheck(
        newSize: Int,
        previousSize: Int,
        threshold: Double = 0.05,
    ): Boolean {
        if (previousSize == 0) return newSize > 0

        val sizeChange = kotlin.math.abs(newSize - previousSize).toDouble() / previousSize
        return sizeChange > threshold
    }

    private fun getCleanedHtml(html: String): String = html.cleanUpHtml()

    /** Count changed bytes, including replacements that do not change the file length. */
    fun changedByteCount(newContent: ByteArray, previousContent: ByteArray, contentType: String): Int {
        val html = contentType.contains("html", ignoreCase = true)
        val current = if (html) getCleanedHtml(newContent.decodeToString()).encodeToByteArray() else newContent
        val previous = if (html) getCleanedHtml(previousContent.decodeToString()).encodeToByteArray() else previousContent
        return kotlin.math.abs(current.size - previous.size) +
            (0 until minOf(current.size, previous.size)).count { current[it] != previous[it] }
    }

    // Kept for callers; no document bodies are retained between checks.
    fun clearCaches() = Unit
}

/**
 * Extension functions for easier content comparison
 */
fun ByteArray.hashedWith(): String = ContentHasher.hashContent(this)

fun ByteArray.isChangedFrom(
    previous: ByteArray,
    contentType: String,
): Boolean = ContentHasher.contentChanged(this, previous, contentType)
