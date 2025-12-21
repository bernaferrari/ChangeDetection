package com.bernaferrari.changedetection.util

import com.bernaferrari.changedetection.extensions.cleanUpHtml
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import java.util.Collections
import java.util.LinkedHashMap

/**
 * Thread-safe utility class for efficient content hashing and comparison.
 * Provides fast content change detection using cryptographic hashes.
 *
 * Security & Performance:
 * - SHA-256 hashing for cryptographic integrity
 * - Thread-safe using ConcurrentHashMap and synchronization
 * - LRU cache to prevent unbounded memory growth
 * - Synchronized MessageDigest access (not thread-safe)
 */
@Singleton
class ContentHasher @Inject constructor() {

    private val messageDigest = MessageDigest.getInstance("SHA-256")

    // Use thread-safe LinkedHashMap with LRU eviction policy
    private val htmlCleanupCache: MutableMap<String, String> =
        Collections.synchronizedMap(object : LinkedHashMap<String, String>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean {
                return size > MAX_CACHE_SIZE
            }
        })

    private val hashCache: MutableMap<String, String> =
        Collections.synchronizedMap(object : LinkedHashMap<String, String>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean {
                return size > MAX_CACHE_SIZE
            }
        })

    companion object {
        private const val MAX_CACHE_SIZE = 1000
    }

    /**
     * Generate SHA-256 hash for raw content
     * Thread-safe with synchronized messageDigest access
     */
    fun hashContent(content: ByteArray): String {
        val contentString = content.toString(Charsets.UTF_8)

        // Check cache first (synchronized map)
        val cachedHash = hashCache[contentString]
        if (cachedHash != null) {
            return cachedHash
        }

        // Synchronize MessageDigest access only
        val hash = synchronized(messageDigest) {
            messageDigest.reset()
            val hashBytes = messageDigest.digest(content)
            hashBytes.joinToString("") { "%02x".format(it) }
        }

        // Cache the result
        hashCache[contentString] = hash
        return hash
    }

    /**
     * Generate hash for HTML content after cleanup
     * This provides more stable comparison for HTML content
     */
    fun hashHtmlContent(content: ByteArray): String {
        val contentString = content.toString(Charsets.UTF_8)

        // Get cleaned HTML (cached)
        val cleanedHtml = getCleanedHtml(contentString)

        // Hash the cleaned content
        return hashContent(cleanedHtml.toByteArray(Charsets.UTF_8))
    }

    /**
     * Compare two content pieces efficiently using hashes
     */
    fun contentChanged(
        newContent: ByteArray,
        previousContent: ByteArray,
        contentType: String
    ): Boolean {
        if (newContent.isEmpty()) return false
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
     * Returns true if content might have changed based on size
     */
    fun quickSizeCheck(newSize: Int, previousSize: Int, threshold: Double = 0.05): Boolean {
        if (previousSize == 0) return newSize > 0

        val sizeChange = kotlin.math.abs(newSize - previousSize).toDouble() / previousSize
        return sizeChange > threshold
    }

    /**
     * Get cleaned HTML with caching
     * Thread-safe through synchronized map
     */
    private fun getCleanedHtml(html: String): String {
        // Check cache first
        val cached = htmlCleanupCache[html]
        if (cached != null) {
            return cached
        }

        // Clean the HTML
        val cleaned = html.cleanUpHtml()

        // Cache the result (LinkedHashMap will auto-evict when full)
        htmlCleanupCache[html] = cleaned
        return cleaned
    }

    /**
     * Clear all caches (useful for memory management)
     * Thread-safe operation
     */
    fun clearCaches() {
        htmlCleanupCache.clear()
        hashCache.clear()
    }

    /**
     * Get cache statistics for monitoring
     * Thread-safe read of synchronized maps
     */
    fun getCacheStats(): CacheStats {
        return CacheStats(
            htmlCacheSize = htmlCleanupCache.size,
            hashCacheSize = hashCache.size,
            maxCacheSize = MAX_CACHE_SIZE
        )
    }
}

data class CacheStats(
    val htmlCacheSize: Int,
    val hashCacheSize: Int,
    val maxCacheSize: Int
) {
    val htmlCacheUsage: Float get() = htmlCacheSize.toFloat() / maxCacheSize
    val hashCacheUsage: Float get() = hashCacheSize.toFloat() / maxCacheSize
}

/**
 * Extension functions for easier content comparison
 */
fun ByteArray.hashedWith(hasher: ContentHasher): String = hasher.hashContent(this)

fun ByteArray.htmlHashedWith(hasher: ContentHasher): String = hasher.hashHtmlContent(this)

fun ByteArray.isChangedFrom(
    previous: ByteArray,
    contentType: String,
    hasher: ContentHasher
): Boolean = hasher.contentChanged(this, previous, contentType)
