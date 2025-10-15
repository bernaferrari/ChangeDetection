package com.bernaferrari.changedetection.util

import com.bernaferrari.changedetection.extensions.cleanUpHtml
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for efficient content hashing and comparison
 * Provides fast content change detection using cryptographic hashes
 */
@Singleton
class ContentHasher @Inject constructor() {
    
    private val messageDigest = MessageDigest.getInstance("SHA-256")
    private val htmlCleanupCache = mutableMapOf<String, String>()
    private val hashCache = mutableMapOf<String, String>()
    
    companion object {
        private const val MAX_CACHE_SIZE = 1000
        private const val CACHE_CLEANUP_THRESHOLD = 1200
    }
    
    /**
     * Generate SHA-256 hash for raw content
     */
    fun hashContent(content: ByteArray): String {
        val contentString = content.toString(Charsets.UTF_8)
        
        // Check cache first
        val cachedHash = hashCache[contentString]
        if (cachedHash != null) {
            return cachedHash
        }
        
        synchronized(messageDigest) {
            messageDigest.reset()
            val hashBytes = messageDigest.digest(content)
            val hash = hashBytes.joinToString("") { "%02x".format(it) }
            
            // Cache the result
            cacheHash(contentString, hash)
            return hash
        }
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
        cacheCleanedHtml(html, cleaned)
        return cleaned
    }
    
    /**
     * Cache hash with size management
     */
    private fun cacheHash(content: String, hash: String) {
        if (hashCache.size > CACHE_CLEANUP_THRESHOLD) {
            // Remove oldest entries (simple LRU simulation)
            val toRemove = hashCache.keys.take(hashCache.size - MAX_CACHE_SIZE)
            toRemove.forEach { hashCache.remove(it) }
        }
        
        hashCache[content] = hash
    }
    
    /**
     * Cache cleaned HTML with size management
     */
    private fun cacheCleanedHtml(original: String, cleaned: String) {
        if (htmlCleanupCache.size > CACHE_CLEANUP_THRESHOLD) {
            // Remove oldest entries (simple LRU simulation)
            val toRemove = htmlCleanupCache.keys.take(htmlCleanupCache.size - MAX_CACHE_SIZE)
            toRemove.forEach { htmlCleanupCache.remove(it) }
        }
        
        htmlCleanupCache[original] = cleaned
    }
    
    /**
     * Clear all caches (useful for memory management)
     */
    fun clearCaches() {
        htmlCleanupCache.clear()
        hashCache.clear()
    }
    
    /**
     * Get cache statistics for monitoring
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