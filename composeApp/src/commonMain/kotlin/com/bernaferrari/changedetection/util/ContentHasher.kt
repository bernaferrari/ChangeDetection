package com.bernaferrari.changedetection.util

import com.bernaferrari.changedetection.extensions.cleanUpHtml

object ContentHasher {

    private val htmlCleanupCache = MessageSafeMap<String, String>(MAX_CACHE_SIZE)
    private val hashCache = MessageSafeMap<String, String>(MAX_CACHE_SIZE)

    private const val MAX_CACHE_SIZE = 1000

    /**
     * Generate SHA-256 hash for raw content
     */
    fun hashContent(content: ByteArray): String {
        val contentString = content.decodeToString()

        // Check cache first
        val cachedHash = hashCache[contentString]
        if (cachedHash != null) {
            return cachedHash
        }

        val hash = CryptoUtils.sha256(content)

        // Cache the result
        hashCache[contentString] = hash
        return hash
    }

    /**
     * Generate hash for HTML content after cleanup
     */
    fun hashHtmlContent(content: ByteArray): String {
        val contentString = content.decodeToString()

        // Get cleaned HTML (cached)
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
     */
    fun quickSizeCheck(newSize: Int, previousSize: Int, threshold: Double = 0.05): Boolean {
        if (previousSize == 0) return newSize > 0

        val sizeChange = kotlin.math.abs(newSize - previousSize).toDouble() / previousSize
        return sizeChange > threshold
    }

    /**
     * Get cleaned HTML with caching
     */
    private fun getCleanedHtml(html: String): String {
        // Check cache first
        val cached = htmlCleanupCache[html]
        if (cached != null) {
            return cached
        }

        // Clean the HTML
        val cleaned = html.cleanUpHtml()

        // Cache the result
        htmlCleanupCache[html] = cleaned
        return cleaned
    }

    /**
     * Clear all caches
     */
    fun clearCaches() {
        htmlCleanupCache.clear()
        hashCache.clear()
    }
}

// Simple thread-safe map wrapper for KMP (naive implementation)
class MessageSafeMap<K, V>(private val maxSize: Int) {
    private val map = mutableMapOf<K, V>()
    // Lock logic would be needed for true thread safety in KMP (atomic/mutex), 
    // but for now we'll keep it simple as single-threaded/main-thread optimized for UI.
    // In a real app, use AtomicReference or Mutex.

    operator fun get(key: K): V? = map[key]

    operator fun set(key: K, value: V) {
        if (map.size >= maxSize) {
            map.remove(map.keys.first())
        }
        map[key] = value
    }

    fun clear() = map.clear()
}

/**
 * Extension functions for easier content comparison
 */
fun ByteArray.hashedWith(): String = ContentHasher.hashContent(this)

fun ByteArray.htmlHashedWith(): String = ContentHasher.hashHtmlContent(this)

fun ByteArray.isChangedFrom(
    previous: ByteArray,
    contentType: String
): Boolean = ContentHasher.contentChanged(this, previous, contentType)
