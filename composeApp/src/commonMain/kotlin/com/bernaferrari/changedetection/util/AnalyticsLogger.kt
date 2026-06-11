/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.util

import io.github.aakira.napier.Napier
import kotlin.time.TimeSource

/**
 * Advanced analytics and performance logging utility.
 * Migrated to Multiplatform using Napier.
 */
object AnalyticsLogger {
    private val performanceMetrics = mutableMapOf<String, PerformanceMetric>()

    /**
     * Log an operation with automatic timing
     */
    internal fun <T> logOperation(
        operationName: String,
        tag: String = "OPERATION",
        block: () -> T,
    ): T {
        val mark = TimeSource.Monotonic.markNow()
        val startMemory = getMemoryUsage()

        return try {
            val result = block()
            val duration = mark.elapsedNow().inWholeMilliseconds
            val memoryDelta = getMemoryUsage() - startMemory

            Napier.d("✅ $tag: $operationName completed in ${duration}ms (${formatMemory(memoryDelta)})", tag = tag)

            recordMetric(operationName, duration, memoryDelta)
            result
        } catch (e: Exception) {
            val duration = mark.elapsedNow().inWholeMilliseconds
            Napier.e("❌ $tag: $operationName failed after ${duration}ms: ${e.message}", tag = tag)
            throw e
        }
    }

    /**
     * Log database query performance
     */
    fun logDatabaseQuery(
        queryName: String,
        durationMs: Long,
        resultCount: Int = 0,
    ) {
        val performanceLevel =
            when {
                durationMs < 10 -> "⚡ FAST"
                durationMs < 50 -> "✅ NORMAL"
                durationMs < 200 -> "⚠️  SLOW"
                else -> "❌ VERY SLOW"
            }

        Napier.d("$performanceLevel DB: $queryName took ${durationMs}ms ($resultCount results)", tag = "Database")
        recordMetric("db_$queryName", durationMs, 0)
    }

    /**
     * Log network request performance
     */
    fun logNetworkRequest(
        url: String,
        statusCode: Int,
        durationMs: Long,
        sizeBytes: Long = 0,
    ) {
        val statusEmoji =
            when {
                statusCode < 300 -> "✅"
                statusCode < 400 -> "ℹ️"
                statusCode < 500 -> "⚠️"
                else -> "❌"
            }

        val sizeText = if (sizeBytes > 0) " (${formatBytes(sizeBytes)})" else ""
        Napier.d("$statusEmoji HTTP $statusCode: ${durationMs}ms$sizeText", tag = "Network")
        recordMetric("network_$statusCode", durationMs, sizeBytes)
    }

    /**
     * Log memory usage snapshot
     */
    fun logMemorySnapshot(label: String = "Memory") {
        val used = getMemoryUsage() / 1_048_576 // MB
        // Max memory is tricky in common code, omitting percentage for now or need another expect

        Napier.d("💾 $label: ${used}MB used", tag = "Memory")
    }

    /**
     * Log feature usage for analytics
     */
    fun logFeatureUsage(
        featureName: String,
        metadata: Map<String, Any>? = null,
    ) {
        val metaStr = metadata?.entries?.joinToString(", ") { "${it.key}=${it.value}" } ?: ""
        Napier.d("📊 Feature: $featureName" + (if (metaStr.isNotEmpty()) " [$metaStr]" else ""), tag = "Feature")
    }

    /**
     * Log cache statistics
     */
    fun logCacheStats(
        cacheName: String,
        hitCount: Int,
        missCount: Int,
        evictionCount: Int = 0,
    ) {
        val total = hitCount + missCount
        val hitRate = if (total > 0) (hitCount * 100) / total else 0

        Napier.d("💾 Cache $cacheName: $hitRate% hit rate ($hitCount hits, $missCount misses)", tag = "Cache")

        if (evictionCount > 0) {
            Napier.d("   Evicted $evictionCount entries", tag = "Cache")
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
        contentSize: Int = 0,
    ) {
        val changeEmoji = if (changeDetected) "📝" else "✅"
        val sizeText = if (contentSize > 0) " (${formatBytes(contentSize.toLong())})" else ""

        Napier.d("$changeEmoji Sync $siteName: ${durationMs}ms$sizeText", tag = "Sync")
        recordMetric("sync_$siteId", durationMs, contentSize.toLong())
    }

    /**
     * Log error with context
     */
    fun logErrorWithContext(
        errorName: String,
        errorMessage: String,
        context: String = "",
        additionalInfo: Map<String, Any>? = null,
    ) {
        val contextStr = if (context.isNotEmpty()) " in $context" else ""
        val infoStr = additionalInfo?.entries?.joinToString(", ") { "${it.key}=${it.value}" } ?: ""
        val infoText = if (infoStr.isNotEmpty()) " [$infoStr]" else ""

        Napier.e("❌ $errorName$contextStr: $errorMessage$infoText", tag = "Error")
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
        Napier.d("📊 Performance metrics cleared", tag = "Metrics")
    }

    /**
     * Log ANR (Application Not Responding) warning
     */
    fun logANRWarning(
        operationName: String,
        durationMs: Long,
    ) {
        Napier.e("🚨 ANR WARNING: $operationName took ${durationMs}ms (>5000ms threshold)", tag = "ANR")
    }

    /**
     * Log jank detection (frame drops)
     */
    fun logJankDetected(
        frameCount: Int,
        droppedFrames: Int,
    ) {
        val jankPercent = (droppedFrames * 100) / frameCount
        if (jankPercent > 10) {
            Napier.w("⚠️  JANK DETECTED: $droppedFrames frames dropped out of $frameCount ($jankPercent%)", tag = "Performance")
        }
    }

    // Private helpers

    internal fun recordMetric(
        name: String,
        durationMs: Long,
        memoryDelta: Long,
    ) {
        val metric = performanceMetrics[name] ?: PerformanceMetric(name)
        metric.totalDuration += durationMs
        metric.count++
        metric.maxDuration = maxOf(metric.maxDuration, durationMs)
        metric.minDuration = minOf(metric.minDuration, durationMs)
        performanceMetrics[name] = metric
    }

    internal fun formatMemory(bytes: Long): String = when {
        bytes >= 1_048_576 -> "${bytes / 1_048_576}MB"
        bytes >= 1_024 -> "${bytes / 1_024}KB"
        else -> "${bytes}B"
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
        var minDuration: Long = Long.MAX_VALUE,
    ) {
        val avgDuration: Long get() = if (count > 0) totalDuration / count else 0
    }
}

/**
 * Extension functions for easier logging
 */
internal fun <T> logTiming(
    label: String,
    block: () -> T,
): T {
    val mark = TimeSource.Monotonic.markNow()
    return try {
        block().also {
            val duration = mark.elapsedNow().inWholeMilliseconds
            Napier.d("⏱️  $label: ${duration}ms", tag = "Timing")
        }
    } catch (e: Exception) {
        val duration = mark.elapsedNow().inWholeMilliseconds
        Napier.e("❌ $label failed after ${duration}ms", tag = "Timing")
        throw e
    }
}

/**
 * Measure memory allocation
 */
fun <T> measureMemory(
    label: String,
    block: () -> T,
): T {
    val memBefore = getMemoryUsage()

    return block().also {
        val memAfter = getMemoryUsage()
        val delta = memAfter - memBefore
        Napier.d("💾 $label: allocated ${delta / 1_024}KB", tag = "Memory")
    }
}
