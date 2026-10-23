package com.bernaferrari.changedetection.util

import com.fleeksoft.ksoup.Ksoup
import kotlinx.serialization.json.Json

/**
 * Comprehensive input validation and data integrity checking utility.
 */
object ValidationHelper {
    /**
     * Validate URL format and protocol
     */
    fun isValidUrl(
        url: String,
        allowHttp: Boolean = true,
    ): Pair<Boolean, String?> {
        return try {
            if (url.isBlank()) {
                return Pair(false, "URL cannot be empty")
            }

            val trimmedUrl = url.trim()

            // Check for valid protocols
            val hasValidProtocol =
                trimmedUrl.startsWith("http://") ||
                    trimmedUrl.startsWith("https://") ||
                    trimmedUrl.startsWith("ftp://")

            if (!hasValidProtocol) {
                return Pair(false, "URL must start with http://, https://, or ftp://")
            }

            if (!allowHttp && trimmedUrl.startsWith("http://")) {
                return Pair(false, "Only HTTPS URLs are allowed")
            }

            // Basic regex validation for URL structure to replace java.net.URL
            val regex = "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$".toRegex()
            if (!regex.matches(trimmedUrl)) {
                return Pair(false, "Invalid URL format")
            }

            Pair(true, null)
        } catch (e: Exception) {
            Pair(false, "Invalid URL: ${e.message}")
        }
    }

    /**
     * Validate email address
     */
    fun isValidEmail(email: String): Pair<Boolean, String?> {
        return try {
            if (email.isBlank()) {
                return Pair(false, "Email cannot be empty")
            }

            val emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}$".toRegex()

            if (!emailPattern.matches(email)) {
                return Pair(false, "Invalid email format")
            }

            Pair(true, null)
        } catch (e: Exception) {
            Pair(false, "Email validation failed: ${e.message}")
        }
    }

    /**
     * Validate CSS selector
     */
    fun isValidCssSelector(selector: String): Pair<Boolean, String?> {
        return try {
            if (selector.isBlank()) {
                return Pair(true, null) // Empty selector is valid (select all)
            }

            // Basic CSS selector validation
            if (selector.contains(";;") || selector.contains("}}")) {
                return Pair(false, "Invalid characters in selector")
            }

            // Try parsing with Ksoup
            Ksoup.parse("<div></div>").select(selector)

            Pair(true, null)
        } catch (e: Exception) {
            Pair(false, "Invalid CSS selector: ${e.message}")
        }
    }

    /**
     * Validate XPath expression
     */
    fun isValidXPath(xpath: String): Pair<Boolean, String?> {
        return try {
            if (xpath.isBlank()) {
                return Pair(false, "XPath cannot be empty")
            }

            // Basic XPath validation
            when {
                xpath.count { it == '(' } != xpath.count { it == ')' } ->
                    return Pair(false, "Mismatched parentheses in XPath")

                xpath.count { it == '[' } != xpath.count { it == ']' } ->
                    return Pair(false, "Mismatched brackets in XPath")

                xpath.count { it == '"' } % 2 != 0 ->
                    return Pair(false, "Mismatched quotes in XPath")

                xpath.contains("..") && !xpath.contains("//") ->
                    return Pair(false, "Invalid XPath syntax")
            }

            Pair(true, null)
        } catch (e: Exception) {
            Pair(false, "XPath validation failed: ${e.message}")
        }
    }

    /**
     * Validate JSON string
     */
    fun isValidJson(jsonString: String): Pair<Boolean, String?> {
        return try {
            if (jsonString.isBlank()) {
                return Pair(false, "JSON cannot be empty")
            }

            Json.parseToJsonElement(jsonString)
            Pair(true, null)
        } catch (e: Exception) {
            Pair(false, "Invalid JSON: ${e.message}")
        }
    }

    /**
     * Validate regular expression pattern
     */
    fun isValidRegex(pattern: String): Pair<Boolean, String?> {
        return try {
            if (pattern.isBlank()) {
                return Pair(true, null) // Empty pattern is valid
            }

            pattern.toRegex()
            Pair(true, null)
        } catch (e: Exception) {
            Pair(false, "Invalid regex pattern: ${e.message}")
        }
    }

    /**
     * Validate credentials format
     */
    fun isValidCredentials(
        username: String,
        password: String,
    ): Pair<Boolean, String?> = when {
        username.isBlank() -> Pair(false, "Username cannot be empty")
        username.length < 1 -> Pair(false, "Username too short")
        username.length > 255 -> Pair(false, "Username too long")
        password.isBlank() -> Pair(false, "Password cannot be empty")
        password.length < 1 -> Pair(false, "Password too short")
        password.length > 1000 -> Pair(false, "Password too long")
        else -> Pair(true, null)
    }

    /**
     * Validate content size
     */
    fun isValidContentSize(
        sizeBytes: Long,
        maxMB: Int = 100,
    ): Pair<Boolean, String?> {
        val maxBytes = maxMB * 1_048_576L

        return when {
            sizeBytes < 0 -> Pair(false, "Content size cannot be negative")
            sizeBytes == 0L -> Pair(false, "Content is empty")
            sizeBytes > maxBytes -> Pair(false, "Content exceeds ${maxMB}MB limit")
            else -> Pair(true, null)
        }
    }

    /**
     * Validate site name/title
     */
    fun isValidSiteName(name: String): Pair<Boolean, String?> = when {
        name.isBlank() -> Pair(false, "Site name cannot be empty")
        name.length < 3 -> Pair(false, "Site name too short (minimum 3 characters)")
        name.length > 255 -> Pair(false, "Site name too long (maximum 255 characters)")
        name.contains(Regex("[<>\"'/]")) -> Pair(false, "Site name contains invalid characters")
        else -> Pair(true, null)
    }

    /**
     * Validate sync interval
     */
    fun isValidSyncInterval(intervalMinutes: Int): Pair<Boolean, String?> = when {
        intervalMinutes < 5 -> Pair(false, "Sync interval must be at least 5 minutes")
        intervalMinutes > 10080 -> Pair(false, "Sync interval must be at most 7 days (10080 minutes)")
        else -> Pair(true, null)
    }

    /**
     * Validate user agent string
     */
    fun isValidUserAgent(userAgent: String): Pair<Boolean, String?> {
        return try {
            if (userAgent.isBlank()) {
                return Pair(true, null) // Empty user agent is valid (will use default)
            }

            if (userAgent.length > 500) {
                return Pair(false, "User agent too long")
            }

            if (userAgent.contains(Regex("[<>\"']"))) {
                return Pair(false, "User agent contains invalid characters")
            }

            Pair(true, null)
        } catch (e: Exception) {
            Pair(false, "User agent validation failed: ${e.message}")
        }
    }

    /**
     * Validate HTTP header
     */
    fun isValidHttpHeader(
        headerName: String,
        headerValue: String,
    ): Pair<Boolean, String?> = when {
        headerName.isBlank() -> Pair(false, "Header name cannot be empty")

        headerValue.isBlank() -> Pair(false, "Header value cannot be empty")

        headerName.contains(":") -> Pair(false, "Header name cannot contain colon")

        headerValue.contains("\n") || headerValue.contains("\r") ->
            Pair(false, "Header value cannot contain line breaks")

        else -> Pair(true, null)
    }

    /**
     * Validate all inputs for a new site
     */
    fun validateNewSite(
        url: String,
        title: String,
        selector: String = "",
        xpath: String = "",
    ): List<String> {
        val errors = mutableListOf<String>()

        // Validate URL
        val (urlValid, urlError) = isValidUrl(url)
        if (!urlValid) errors.add(urlError ?: "Invalid URL")

        // Validate title
        val (titleValid, titleError) = isValidSiteName(title)
        if (!titleValid) errors.add(titleError ?: "Invalid site name")

        // Validate selectors if provided
        if (selector.isNotBlank()) {
            val (selectorValid, selectorError) = isValidCssSelector(selector)
            if (!selectorValid) errors.add("CSS Selector: ${selectorError ?: "Invalid"}")
        }

        if (xpath.isNotBlank()) {
            val (xpathValid, xpathError) = isValidXPath(xpath)
            if (!xpathValid) errors.add("XPath: ${xpathError ?: "Invalid"}")
        }

        return errors
    }

    /**
     * Log validation result
     */
    fun logValidationResult(
        fieldName: String,
        isValid: Boolean,
        errorMessage: String? = null,
    ) {
        if (isValid) {
            appLog.d("✅ Validation: $fieldName is valid")
        } else {
            appLog.w("⚠️  Validation: $fieldName failed - $errorMessage")
        }
    }
}

/**
 * Extension function for easy validation with logging
 */
inline fun <T> validateAndLog(
    label: String,
    value: T,
    validator: (T) -> Pair<Boolean, String?>,
): Boolean {
    val (isValid, errorMessage) = validator(value)
    ValidationHelper.logValidationResult(label, isValid, errorMessage)
    return isValid
}

private val appLog = com.bernaferrari.changedetection.logging.AppLog.tagged("ValidationHelper")
