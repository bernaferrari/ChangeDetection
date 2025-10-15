package com.bernaferrari.changedetection.monitoring

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Smart Monitoring Engine
 * 
 * Provides intelligent content filtering, element zapping, and change detection
 * with advanced features like noise filtering, content classification, and
 * smart diffing algorithms.
 */

@Singleton
class SmartMonitoringEngine @Inject constructor() {
    
    suspend fun processContent(
        content: String,
        filters: List<ContentFilter>,
        previousContent: String? = null
    ): ProcessedContent = withContext(Dispatchers.Default) {
        
        // Step 1: Apply content filters
        val filteredContent = applyContentFilters(content, filters)
        
        // Step 2: Extract meaningful content
        val extractedContent = extractMeaningfulContent(filteredContent)
        
        // Step 3: Detect and classify changes
        val changes = if (previousContent != null) {
            detectSmartChanges(extractedContent, previousContent)
        } else {
            emptyList()
        }
        
        // Step 4: Calculate importance score
        val importanceScore = calculateImportanceScore(changes, extractedContent)
        
        ProcessedContent(
            originalContent = content,
            filteredContent = filteredContent,
            extractedContent = extractedContent,
            changes = changes,
            importanceScore = importanceScore,
            contentType = classifyContentType(extractedContent),
            metadata = extractMetadata(extractedContent)
        )
    }
    
    suspend fun zapElements(
        htmlContent: String,
        zapRules: List<ZapRule>
    ): String = withContext(Dispatchers.Default) {
        if (zapRules.isEmpty()) return@withContext htmlContent

        val document = Jsoup.parse(htmlContent)

        zapRules.forEach { rule ->
            try {
                when (rule) {
                    is ZapRule.CssSelector -> {
                        document.select(rule.selector).forEach { it.remove() }
                    }

                    is ZapRule.ElementType -> {
                        document.select(rule.tagName).forEach { it.remove() }
                    }

                    is ZapRule.AttributePattern -> {
                        val pattern = runCatching { Pattern.compile(rule.pattern, Pattern.CASE_INSENSITIVE) }.getOrNull()
                        val candidates = document.getElementsByAttribute(rule.attribute)
                        candidates.filter { element ->
                            val value = element.attr(rule.attribute)
                            when {
                                pattern != null -> pattern.matcher(value).find()
                                else -> value.contains(rule.pattern, ignoreCase = true)
                            }
                        }.forEach { it.remove() }
                    }

                    is ZapRule.TextPattern -> {
                        val pattern = runCatching { Pattern.compile(rule.pattern, Pattern.CASE_INSENSITIVE) }.getOrNull()
                        if (pattern != null) {
                            document.getElementsMatchingText(pattern).forEach { it.remove() }
                        } else {
                            document.getElementsMatchingText(rule.pattern).forEach { it.remove() }
                        }
                    }

                    is ZapRule.XPath -> {
                        // TODO: Provide XPath support using W3C DOM bridge if needed
                    }
                }
            } catch (_: Exception) {
                // Skip malformed selectors without interrupting processing
            }
        }

        document.outerHtml()
    }
    
    suspend fun createSmartFilters(
        sampleContent: String,
        filterType: SmartFilterType
    ): List<ContentFilter> = withContext(Dispatchers.Default) {
        when (filterType) {
            SmartFilterType.NOISE_REDUCTION -> createNoiseReductionFilters(sampleContent)
            SmartFilterType.CONTENT_FOCUS -> createContentFocusFilters(sampleContent)
            SmartFilterType.TRACKING_REMOVAL -> createTrackingRemovalFilters(sampleContent)
            SmartFilterType.DYNAMIC_CONTENT -> createDynamicContentFilters(sampleContent)
        }
    }
    
    private fun applyContentFilters(content: String, filters: List<ContentFilter>): String {
        if (filters.isEmpty()) return content

        var workingContent = content

        filters.forEach { filter ->
            workingContent = when (filter) {
                is ContentFilter.RegexFilter -> {
                    val regex = runCatching { Regex(filter.pattern, RegexOption.IGNORE_CASE) }.getOrNull()
                    if (regex == null) {
                        workingContent
                    } else if (filter.isRemoval) {
                        workingContent.replace(regex, "")
                    } else {
                        regex.findAll(workingContent).joinToString(separator = "\n") { it.value }
                    }
                }

                is ContentFilter.CssElementFilter -> {
                    if (filter.isRemoval) {
                        removeElementsBySelector(workingContent, filter.selector)
                    } else {
                        extractElementsBySelector(workingContent, filter.selector)
                    }
                }

                is ContentFilter.TextNormalization -> normalizeText(workingContent, filter.options)

                is ContentFilter.ContentTypeFilter -> extractContentByType(workingContent, filter.contentType)
            }
        }

        return workingContent.trim()
    }
    
    private fun extractMeaningfulContent(content: String): String {
        // Remove noise and extract meaningful text
        return content
            .replace(Regex("<script[^>]*>.*?</script>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<style[^>]*>.*?</style>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<[^>]+>"), " ") // Remove HTML tags
            .replace(Regex("\\s+"), " ") // Normalize whitespace
            .replace(Regex("\\b\\d{13,}\\b"), "[TIMESTAMP]") // Replace timestamps
            .replace(Regex("\\b[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}\\b"), "[UUID]") // Replace UUIDs
            .trim()
    }
    
    private fun detectSmartChanges(newContent: String, oldContent: String): List<SmartChange> {
        val changes = mutableListOf<SmartChange>()
        
        // Word-level diff for better granularity
        val oldWords = oldContent.split(Regex("\\s+"))
        val newWords = newContent.split(Regex("\\s+"))
        
        val diffResult = computeLCS(oldWords, newWords)
        
        diffResult.forEach { change ->
            val smartChange = SmartChange(
                type = change.type,
                content = change.content,
                position = change.position,
                context = extractContext(newContent, change.position),
                significance = calculateSignificance(change),
                category = categorizeChange(change.content)
            )
            changes.add(smartChange)
        }
        
        return changes
    }
    
    private fun calculateImportanceScore(changes: List<SmartChange>, content: String): Float {
        if (changes.isEmpty()) return 0f
        
        var score = 0f
        
        changes.forEach { change ->
            score += when (change.significance) {
                ChangeSignificance.CRITICAL -> 1.0f
                ChangeSignificance.IMPORTANT -> 0.7f
                ChangeSignificance.MODERATE -> 0.4f
                ChangeSignificance.MINOR -> 0.1f
            }
            
            // Boost score for certain categories
            score += when (change.category) {
                ChangeCategory.PRICE -> 0.8f
                ChangeCategory.AVAILABILITY -> 0.6f
                ChangeCategory.DATE -> 0.4f
                ChangeCategory.TEXT_CONTENT -> 0.3f
                ChangeCategory.NAVIGATION -> 0.1f
                ChangeCategory.METADATA -> 0.05f
                ChangeCategory.UNKNOWN -> 0.0f
            }
        }
        
        // Normalize by content length
        val contentLength = content.length.coerceAtLeast(1)
        return (score / contentLength * 1000).coerceAtMost(1.0f)
    }
    
    private fun classifyContentType(content: String): ContentType {
        val lowerContent = content.lowercase()
        
        return when {
            lowerContent.contains("price") || lowerContent.contains("$") || lowerContent.contains("€") -> ContentType.ECOMMERCE
            lowerContent.contains("available") || lowerContent.contains("stock") -> ContentType.INVENTORY
            lowerContent.contains("date") || lowerContent.contains("schedule") -> ContentType.SCHEDULE
            lowerContent.contains("article") || lowerContent.contains("news") -> ContentType.NEWS
            lowerContent.contains("status") || lowerContent.contains("state") -> ContentType.STATUS
            else -> ContentType.GENERAL
        }
    }
    
    private fun extractMetadata(content: String): ContentMetadata {
        val wordCount = content.split(Regex("\\s+")).size
        val hasNumbers = Regex("\\d+").containsMatchIn(content)
        val hasPrices = Regex("[\\$€£¥]\\d+|\\d+[\\$€£¥]").containsMatchIn(content)
        val hasDates = Regex("\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\d{4}-\\d{2}-\\d{2}").containsMatchIn(content)
        val hasUrls = Regex("https?://\\S+").containsMatchIn(content)
        val hasEmails = Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b").containsMatchIn(content)
        
        return ContentMetadata(
            wordCount = wordCount,
            hasNumbers = hasNumbers,
            hasPrices = hasPrices,
            hasDates = hasDates,
            hasUrls = hasUrls,
            hasEmails = hasEmails,
            language = detectLanguage(content),
            readingLevel = calculateReadingLevel(content)
        )
    }
    
    private fun createNoiseReductionFilters(@Suppress("UNUSED_PARAMETER") content: String): List<ContentFilter> {
        return listOf(
            ContentFilter.CssElementFilter("script[src*='analytics']", isRemoval = true),
            ContentFilter.CssElementFilter("script[src*='tracking']", isRemoval = true),
            ContentFilter.CssElementFilter("script[src*='gtag']", isRemoval = true),
            ContentFilter.CssElementFilter(".ad", isRemoval = true),
            ContentFilter.CssElementFilter(".advertisement", isRemoval = true),
            ContentFilter.CssElementFilter("[id*='ad-']", isRemoval = true),
            ContentFilter.CssElementFilter(".social-share", isRemoval = true),
            ContentFilter.CssElementFilter(".twitter-tweet", isRemoval = true),
            ContentFilter.CssElementFilter(".fb-like", isRemoval = true),
            ContentFilter.RegexFilter("data-timestamp=\"[^\"]*\"", isRemoval = true),
            ContentFilter.RegexFilter("id=\"[a-f0-9-]{36}\"", isRemoval = true)
        )
    }

    private fun createContentFocusFilters(content: String): List<ContentFilter> {
        val filters = mutableListOf<ContentFilter>()

        filters += ContentFilter.CssElementFilter("main", isRemoval = false)
        filters += ContentFilter.CssElementFilter("article", isRemoval = false)
        filters += ContentFilter.CssElementFilter(".content", isRemoval = false)
        filters += ContentFilter.CssElementFilter("#content", isRemoval = false)

        if (content.contains("price", ignoreCase = true) || content.contains("$")) {
            filters += ContentFilter.CssElementFilter(".price", isRemoval = false)
            filters += ContentFilter.CssElementFilter("[class*='price']", isRemoval = false)
        }

        if (content.contains("stock", ignoreCase = true) || content.contains("available", ignoreCase = true)) {
            filters += ContentFilter.CssElementFilter(".availability", isRemoval = false)
            filters += ContentFilter.CssElementFilter(".stock", isRemoval = false)
        }

        return filters
    }

    private fun createTrackingRemovalFilters(@Suppress("UNUSED_PARAMETER") content: String): List<ContentFilter> {
        return listOf(
            ContentFilter.RegexFilter("utm_[a-z]+=[^&]*", isRemoval = true),
            ContentFilter.RegexFilter("gclid=[^&]*", isRemoval = true),
            ContentFilter.RegexFilter("fbclid=[^&]*", isRemoval = true),
            ContentFilter.CssElementFilter("script[src*='google-analytics']", isRemoval = true),
            ContentFilter.CssElementFilter("script[src*='googletagmanager']", isRemoval = true),
            ContentFilter.CssElementFilter("iframe[src*='facebook']", isRemoval = true)
        )
    }

    private fun createDynamicContentFilters(@Suppress("UNUSED_PARAMETER") content: String): List<ContentFilter> {
        return listOf(
            ContentFilter.RegexFilter("\\d{13,}", isRemoval = true),
            ContentFilter.RegexFilter("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}", isRemoval = true),
            ContentFilter.RegexFilter("csrfToken=[^&\"']*", isRemoval = true),
            ContentFilter.RegexFilter("sessionId=[^&\"']*", isRemoval = true)
        )
    }

    private fun removeElementsBySelector(html: String, selector: String): String {
        return try {
            val document = Jsoup.parse(html)
            document.select(selector).forEach { it.remove() }
            document.outerHtml()
        } catch (_: Exception) {
            html
        }
    }

    private fun extractElementsBySelector(html: String, selector: String): String {
        return try {
            val document = Jsoup.parse(html)
            document.select(selector)
                .joinToString(separator = "\n") { it.outerHtml() }
        } catch (_: Exception) {
            html
        }
    }
    
    private fun normalizeText(text: String, options: TextNormalizationOptions): String {
        var normalized = text
        
        if (options.removeExtraWhitespace) {
            normalized = normalized.replace(Regex("\\s+"), " ")
        }
        
        if (options.toLowerCase) {
            normalized = normalized.lowercase()
        }
        
        if (options.removePunctuation) {
            normalized = normalized.replace(Regex("[^\\w\\s]"), "")
        }
        
        if (options.removeNumbers) {
            normalized = normalized.replace(Regex("\\d+"), "")
        }
        
        return normalized.trim()
    }
    
    private fun extractContentByType(content: String, contentType: String): String {
        // Extract specific content types
        return when (contentType.lowercase()) {
            "text" -> content.replace(Regex("<[^>]+>"), "")
            "links" -> Regex("<a[^>]*href\\s*=\\s*[\"']([^\"']*)[\"'][^>]*>([^<]*)</a>")
                .findAll(content)
                .map { "${it.groupValues[2]} (${it.groupValues[1]})" }
                .joinToString("\n")
            "images" -> Regex("<img[^>]*src\\s*=\\s*[\"']([^\"']*)[\"'][^>]*>")
                .findAll(content)
                .map { it.groupValues[1] }
                .joinToString("\n")
            else -> content
        }
    }
    
    private fun computeLCS(oldWords: List<String>, newWords: List<String>): List<DiffChange> {
        val sanitizedOld = oldWords.filter { it.isNotBlank() }
        val sanitizedNew = newWords.filter { it.isNotBlank() }

        val n = sanitizedOld.size
        val m = sanitizedNew.size
        if (n == 0 && m == 0) return emptyList()

        val dp = Array(n + 1) { IntArray(m + 1) }
        for (i in n - 1 downTo 0) {
            for (j in m - 1 downTo 0) {
                dp[i][j] = if (sanitizedOld[i] == sanitizedNew[j]) {
                    1 + dp[i + 1][j + 1]
                } else {
                    max(dp[i + 1][j], dp[i][j + 1])
                }
            }
        }

        val changes = mutableListOf<DiffChange>()
        var i = 0
        var j = 0
        while (i < n && j < m) {
            val oldWord = sanitizedOld[i]
            val newWord = sanitizedNew[j]
            when {
                oldWord == newWord -> {
                    i++
                    j++
                }

                dp[i + 1][j] == dp[i][j + 1] -> {
                    val position = calculateWordPosition(sanitizedNew, j)
                    changes += DiffChange(
                        type = ChangeType.MODIFICATION,
                        content = "$oldWord → $newWord",
                        position = position
                    )
                    i++
                    j++
                }

                dp[i + 1][j] >= dp[i][j + 1] -> {
                    val position = calculateWordPosition(sanitizedNew, j)
                    changes += DiffChange(
                        type = ChangeType.DELETION,
                        content = oldWord,
                        position = position
                    )
                    i++
                }

                else -> {
                    val position = calculateWordPosition(sanitizedNew, j)
                    changes += DiffChange(
                        type = ChangeType.ADDITION,
                        content = newWord,
                        position = position
                    )
                    j++
                }
            }
        }

        while (i < n) {
            val position = calculateWordPosition(sanitizedNew, m)
            changes += DiffChange(
                type = ChangeType.DELETION,
                content = sanitizedOld[i],
                position = position
            )
            i++
        }

        while (j < m) {
            val position = calculateWordPosition(sanitizedNew, j)
            changes += DiffChange(
                type = ChangeType.ADDITION,
                content = sanitizedNew[j],
                position = position
            )
            j++
        }

        return changes
    }

    private fun calculateWordPosition(words: List<String>, index: Int): Int {
        if (words.isEmpty() || index <= 0) return 0
        var position = 0
        val upperBound = index.coerceAtMost(words.size)
        for (k in 0 until upperBound) {
            position += words[k].length
            if (k < upperBound - 1) {
                position += 1
            }
        }
        return position
    }
    
    private fun extractContext(content: String, position: Int): String {
        val start = (position - 50).coerceAtLeast(0)
        val end = (position + 50).coerceAtMost(content.length)
        return content.substring(start, end)
    }
    
    private fun calculateSignificance(change: DiffChange): ChangeSignificance {
        return when {
            change.content.length > 100 -> ChangeSignificance.CRITICAL
            change.content.length > 50 -> ChangeSignificance.IMPORTANT
            change.content.length > 10 -> ChangeSignificance.MODERATE
            else -> ChangeSignificance.MINOR
        }
    }
    
    private fun categorizeChange(content: String): ChangeCategory {
        val lowerContent = content.lowercase()
        
        return when {
            Regex("[\\$€£¥]\\d+|\\d+[\\$€£¥]").containsMatchIn(content) -> ChangeCategory.PRICE
            lowerContent.contains("available") || lowerContent.contains("stock") -> ChangeCategory.AVAILABILITY
            Regex("\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}").containsMatchIn(content) -> ChangeCategory.DATE
            lowerContent.contains("nav") || lowerContent.contains("menu") -> ChangeCategory.NAVIGATION
            lowerContent.contains("meta") || lowerContent.contains("data-") -> ChangeCategory.METADATA
            content.length > 10 -> ChangeCategory.TEXT_CONTENT
            else -> ChangeCategory.UNKNOWN
        }
    }
    
    private fun detectLanguage(content: String): String {
        // Simplified language detection based on common words
        val lowerContent = content.lowercase()
        return when {
            lowerContent.contains("el") && lowerContent.contains("en") && lowerContent.contains("la") -> "es" // Spanish
            lowerContent.contains("le") && lowerContent.contains("de") && lowerContent.contains("et") -> "fr" // French
            lowerContent.contains("der") && lowerContent.contains("die") && lowerContent.contains("das") -> "de" // German
            lowerContent.contains("o") && lowerContent.contains("e") && lowerContent.contains("a") -> "pt" // Portuguese
            else -> "en" // Default to English
        }
    }
    
    private fun calculateReadingLevel(content: String): ReadingLevel {
        if (content.isBlank()) return ReadingLevel.STANDARD

        val words = content.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return ReadingLevel.STANDARD

        val sentences = content.split(Regex("[.!?]+")).map { it.trim() }.filter { it.isNotEmpty() }
        val avgWordsPerSentence = words.size.toDouble() / sentences.size.coerceAtLeast(1)
        val avgSyllablesPerWord = words.map { countSyllables(it).coerceAtLeast(1) }.average().takeIf { it.isFinite() && it > 0.0 } ?: 1.0

        val score = 206.835 - (1.015 * avgWordsPerSentence) - (84.6 * avgSyllablesPerWord)

        return when {
            score >= 90 -> ReadingLevel.VERY_EASY
            score >= 80 -> ReadingLevel.EASY
            score >= 70 -> ReadingLevel.FAIRLY_EASY
            score >= 60 -> ReadingLevel.STANDARD
            score >= 50 -> ReadingLevel.FAIRLY_DIFFICULT
            score >= 30 -> ReadingLevel.DIFFICULT
            else -> ReadingLevel.VERY_DIFFICULT
        }
    }
    
    private fun countSyllables(word: String): Int {
        // Simplified syllable counting
        return word.lowercase()
            .replace(Regex("[^aeiou]"), "")
            .replace(Regex("([aeiou])\\1+"), "$1")
            .length
            .coerceAtLeast(1)
    }
}

// Data classes and enums for smart monitoring
data class ProcessedContent(
    val originalContent: String,
    val filteredContent: String,
    val extractedContent: String,
    val changes: List<SmartChange>,
    val importanceScore: Float,
    val contentType: ContentType,
    val metadata: ContentMetadata
)

data class SmartChange(
    val type: ChangeType,
    val content: String,
    val position: Int,
    val context: String,
    val significance: ChangeSignificance,
    val category: ChangeCategory
)

data class ContentMetadata(
    val wordCount: Int,
    val hasNumbers: Boolean,
    val hasPrices: Boolean,
    val hasDates: Boolean,
    val hasUrls: Boolean,
    val hasEmails: Boolean,
    val language: String,
    val readingLevel: ReadingLevel
)

data class DiffChange(
    val type: ChangeType,
    val content: String,
    val position: Int
)

sealed class ContentFilter {
    data class RegexFilter(val pattern: String, val isRemoval: Boolean) : ContentFilter()
    data class CssElementFilter(val selector: String, val isRemoval: Boolean) : ContentFilter()
    data class TextNormalization(val options: TextNormalizationOptions) : ContentFilter()
    data class ContentTypeFilter(val contentType: String) : ContentFilter()
}

sealed class ZapRule {
    data class CssSelector(val selector: String) : ZapRule()
    data class XPath(val xpath: String) : ZapRule()
    data class TextPattern(val pattern: String) : ZapRule()
    data class ElementType(val tagName: String) : ZapRule()
    data class AttributePattern(val attribute: String, val pattern: String) : ZapRule()
}

data class TextNormalizationOptions(
    val removeExtraWhitespace: Boolean = true,
    val toLowerCase: Boolean = false,
    val removePunctuation: Boolean = false,
    val removeNumbers: Boolean = false
)

enum class SmartFilterType {
    NOISE_REDUCTION,
    CONTENT_FOCUS,
    TRACKING_REMOVAL,
    DYNAMIC_CONTENT
}

enum class ContentType {
    GENERAL,
    ECOMMERCE,
    INVENTORY,
    SCHEDULE,
    NEWS,
    STATUS
}

enum class ChangeType {
    ADDITION,
    DELETION,
    MODIFICATION
}

enum class ChangeSignificance {
    CRITICAL,
    IMPORTANT,
    MODERATE,
    MINOR
}

enum class ChangeCategory {
    PRICE,
    AVAILABILITY,
    DATE,
    TEXT_CONTENT,
    NAVIGATION,
    METADATA,
    UNKNOWN
}

enum class ReadingLevel {
    VERY_EASY,
    EASY,
    FAIRLY_EASY,
    STANDARD,
    FAIRLY_DIFFICULT,
    DIFFICULT,
    VERY_DIFFICULT
}