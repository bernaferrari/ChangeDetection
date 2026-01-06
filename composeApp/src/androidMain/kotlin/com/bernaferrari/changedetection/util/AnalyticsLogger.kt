package com.bernaferrari.changedetection.util

import com.orhanobut.logger.Logger
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Advanced analytics and performance logging utility.
 *
 * Tracks:
 * - Operation timing and performance
 * - Memory usage
 * - Database query performance
 * - Network latency
 * - Feature usage
 * - Error rates
 *
 * Provides:
 * - Structured logging
 * - Performance metrics
 * - Memory profiling
 * - Network diagnostics
 *
 * Security: Does not log sensitive data (credentials, tokens, etc.)
 */
object AnalyticsLogger {

    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val performanceMetrics = mutableMapOf<String, PerformanceMetric>()

    /**
     * Log an operation with automatic timing
     */
    internal fun <T> logOperation(
        operationName: String,
        tag: String = "OPERATION",
        block: () -> T
    ): T {
        val startTime = System.nanoTime()
        val startMemory = getMemoryUsage()

        return try {
            val result = block()
            val duration = (System.nanoTime() - startTime) / 1_000_000 // Convert to ms
            val memoryDelta = getMemoryUsage() - startMemory

            Logger.d("✅ $tag: $operationName completed in ${duration}ms (${formatMemory(memoryDelta)})")

            recordMetric(operationName, duration, memoryDelta)
            result
        } catch (e: Exception) {
            val duration = (System.nanoTime() - startTime) / 1_000_000
            Logger.e("❌ $tag: $operationName failed after ${duration}ms: ${e.message}")
            throw e
        }
    }

    /**
     * Log database query performance
     */
    fun logDatabaseQuery(
        queryName: String,
        durationMs: Long,
        resultCount: Int = 0
    ) {
        val performanceLevel = when {
            durationMs < 10 -> "⚡ FAST"
            durationMs < 50 -> "✅ NORMAL"
            durationMs < 200 -> "⚠️  SLOW"
            else -> "❌ VERY SLOW"
        }

        Logger.d("$performanceLevel DB: $queryName took ${durationMs}ms (${resultCount} results)")
        recordMetric("db_$queryName", durationMs, 0)
    }

    /**
     * Log network request performance
     */
    fun logNetworkRequest(
        url: String,
        statusCode: Int,
        durationMs: Long,
        sizeBytes: Long = 0
    ) {
        val statusEmoji = when {
            statusCode < 300 -> "✅"
            statusCode < 400 -> "ℹ️"
            statusCode < 500 -> "⚠️"
            else -> "❌"
        }

        val sizeText = if (sizeBytes > 0) " (${formatBytes(sizeBytes)})" else ""
        Logger.d("$statusEmoji HTTP $statusCode: ${durationMs}ms$sizeText")
        recordMetric("network_$statusCode", durationMs, sizeBytes)
    }

    /**
     * Log memory usage snapshot
     */
    fun logMemorySnapshot(label: String = "Memory") {
        val runtime = Runtime.getRuntime()
        val used = (runtime.totalMemory() - runtime.freeMemory()) / 1_048_576 // MB
        val max = runtime.maxMemory() / 1_048_576 // MB
        val percent = (used * 100) / max

        val emoji = when {
            percent < 50 -> "🟢"
            percent < 75 -> "🟡"
            percent < 90 -> "🟠"
            else -> "🔴"
        }

        Logger.d("$emoji $label: ${used}MB / ${max}MB (${percent}%)")
    }

    /**
     * Log feature usage for analytics
     */
    fun logFeatureUsage(
        featureName: String,
        metadata: Map<String, Any>? = null
    ) {
        val metaStr = metadata?.entries?.joinToString(", ") { "${it.key}=${it.value}" } ?: ""
        Logger.d("📊 Feature: $featureName" + (if (metaStr.isNotEmpty()) " [$metaStr]" else ""))
    }

    /**
     * Log cache statistics
     */
    fun logCacheStats(
        cacheName: String,
        hitCount: Int,
        missCount: Int,
        evictionCount: Int = 0
    ) {
        val total = hitCount + missCount
        val hitRate = if (total > 0) (hitCount * 100) / total else 0

        Logger.d("💾 Cache $cacheName: ${hitRate}% hit rate ($hitCount hits, $missCount misses)")

        if (evictionCount > 0) {
            Logger.d("   Evicted $evictionCount entries")
        }
    }

    /**
     * Log sync operation details
     */
    fun logSyncOperation(
        siteId: String,
        siteName: String,
        durationMs: Long,
        changeDetected: Boolean,
        contentSize: Int = 0
    ) {
        val changeEmoji = if (changeDetected) "📝" else "✅"
        val sizeText = if (contentSize > 0) " (${formatBytes(contentSize.toLong())})" else ""

        Logger.d("$changeEmoji Sync $siteName: ${durationMs}ms$sizeText")
        recordMetric("sync_$siteId", durationMs, contentSize.toLong())
    }

    /**
     * Log error with context
     */
    fun logErrorWithContext(
        errorName: String,
        errorMessage: String,
        context: String = "",
        additionalInfo: Map<String, Any>? = null
    ) {
        val contextStr = if (context.isNotEmpty()) " in $context" else ""
        val infoStr = additionalInfo?.entries?.joinToString(", ") { "${it.key}=${it.value}" } ?: ""
        val infoText = if (infoStr.isNotEmpty()) " [$infoStr]" else ""

        Logger.e("❌ $errorName$contextStr: $errorMessage$infoText")
    }

    /**
     * Get performance summary report
     */
    fun getPerformanceSummary(): String {
        if (performanceMetrics.isEmpty()) {
            return "No metrics recorded yet"
        }

        val report = StringBuilder()
        report.append("Performance Metrics Summary:\n")

        performanceMetrics.forEach { (name, metric) ->
            report.append("  $name: avg=${metric.avgDuration}ms, count=${metric.count}\n")
        }

        return report.toString()
    }

    /**
     * Clear all recorded metrics
     */
    fun clearMetrics() {
        performanceMetrics.clear()
        Logger.d("📊 Performance metrics cleared")
    }

    /**
     * Log ANR (Application Not Responding) warning
     */
    fun logANRWarning(operationName: String, durationMs: Long) {
        Logger.e("🚨 ANR WARNING: $operationName took ${durationMs}ms (>5000ms threshold)")
    }

    /**
     * Log jank detection (frame drops)
     */
    fun logJankDetected(frameCount: Int, droppedFrames: Int) {
        val jankPercent = (droppedFrames * 100) / frameCount
        if (jankPercent > 10) {
            Logger.w("⚠️  JANK DETECTED: $droppedFrames frames dropped out of $frameCount ($jankPercent%)")
        }
    }

    // Private helpers

    internal fun recordMetric(name: String, durationMs: Long, memoryDelta: Long) {
        val metric = performanceMetrics[name] ?: PerformanceMetric(name)
        metric.totalDuration += durationMs
        metric.count++
        metric.maxDuration = maxOf(metric.maxDuration, durationMs)
        metric.minDuration = minOf(metric.minDuration, durationMs)
        performanceMetrics[name] = metric
    }

    internal fun getMemoryUsage(): Long {
        return Runtime.getRuntime().run {
            totalMemory() - freeMemory()
        }
    }

    internal fun formatMemory(bytes: Long): String {
        return when {
            bytes >= 1_048_576 -> "${bytes / 1_048_576}MB"
            bytes >= 1_024 -> "${bytes / 1_024}KB"
            else -> "${bytes}B"
        }
    }

    internal fun formatBytes(bytes: Long): String = formatMemory(bytes)

    /**
     * Data class for tracking performance metrics
     */
    private data class PerformanceMetric(
        val name: String,
        var totalDuration: Long = 0,
        var count: Int = 0,
        var maxDuration: Long = 0,
        var minDuration: Long = Long.MAX_VALUE
    ) {
        val avgDuration: Long get() = if (count > 0) totalDuration / count else 0
    }
}

/**
 * Extension functions for easier logging
 */
internal fun <T> logTiming(label: String, block: () -> T): T {
    val startTime = System.nanoTime()
    return try {
        block().also {
            val duration = (System.nanoTime() - startTime) / 1_000_000
            Logger.d("⏱️  $label: ${duration}ms")
        }
    } catch (e: Exception) {
        val duration = (System.nanoTime() - startTime) / 1_000_000
        Logger.e("❌ $label failed after ${duration}ms")
        throw e
    }
}

/**
 * Measure memory allocation
 */
fun <T> measureMemory(label: String, block: () -> T): T {
    val runtime = Runtime.getRuntime()
    val memBefore = runtime.totalMemory() - runtime.freeMemory()

    return block().also {
        val memAfter = runtime.totalMemory() - runtime.freeMemory()
        val delta = memAfter - memBefore
        Logger.d("💾 $label: allocated ${delta / 1_024}KB")
    }
}
