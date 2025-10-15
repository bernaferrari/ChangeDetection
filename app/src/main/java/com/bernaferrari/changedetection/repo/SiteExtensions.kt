package com.bernaferrari.changedetection.repo

import com.bernaferrari.changedetection.monitoring.ZapRule
import com.bernaferrari.changedetection.monitoring.ContentFilter
import com.bernaferrari.changedetection.monitoring.SmartFilterType
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Serializable
import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import com.orhanobut.logger.Logger
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Extensions for Site model to support smart monitoring features, authentication, and security
 */

// Authentication data classes
@Serializable
data class LoginCredentials(
    val username: String,
    val password: String,
    val domain: String? = null, // For NTLM/Windows auth
    val additionalFields: Map<String, String> = emptyMap() // For custom form fields
)

@Serializable
data class CustomHeader(
    val name: String,
    val value: String,
    val isSecure: Boolean = false // Whether this header contains sensitive data
)

@Serializable
data class ProxyConfig(
    val host: String,
    val port: Int,
    val username: String? = null,
    val password: String? = null,
    val type: ProxyType = ProxyType.HTTP
)

enum class ProxyType {
    HTTP, HTTPS, SOCKS4, SOCKS5
}

@Serializable
data class OAuthTokens(
    val accessToken: String,
    val refreshToken: String? = null,
    val tokenType: String = "Bearer",
    val expiresAt: Long? = null,
    val scope: String? = null
)

// Serializable data classes for JSON storage
@Serializable
data class SerializableZapRule(
    val type: String, // "css", "xpath", "text", "element", "attribute"
    val selector: String? = null,
    val xpath: String? = null,
    val pattern: String? = null,
    val tagName: String? = null,
    val attribute: String? = null
)

@Serializable
data class SerializableContentFilter(
    val type: String, // "regex", "css", "text", "content_type"
    val pattern: String? = null,
    val selector: String? = null,
    val isRemoval: Boolean = true,
    val contentType: String? = null,
    val textOptions: SerializableTextOptions? = null
)

@Serializable
data class SerializableTextOptions(
    val removeExtraWhitespace: Boolean = true,
    val toLowerCase: Boolean = false,
    val removePunctuation: Boolean = false,
    val removeNumbers: Boolean = false
)

// Extension properties for Site
val Site.zapRules: List<ZapRule>
    get() = try {
        if (zapRulesJson.isBlank()) {
            emptyList()
        } else {
            val serializableRules = Json.decodeFromString<List<SerializableZapRule>>(zapRulesJson)
            serializableRules.mapNotNull { it.toZapRule() }
        }
    } catch (e: Exception) {
        emptyList()
    }

val Site.customFilters: List<ContentFilter>?
    get() = try {
        if (customFiltersJson.isBlank()) {
            null
        } else {
            val serializableFilters = Json.decodeFromString<List<SerializableContentFilter>>(customFiltersJson)
            serializableFilters.mapNotNull { it.toContentFilter() }
        }
    } catch (e: Exception) {
        null
    }

val Site.smartFilterType: SmartFilterType
    get() = when (contentFocusMode) {
        ContentFocusMode.ECOMMERCE -> SmartFilterType.CONTENT_FOCUS
        ContentFocusMode.NEWS -> SmartFilterType.TRACKING_REMOVAL
        ContentFocusMode.GENERAL -> SmartFilterType.NOISE_REDUCTION
        ContentFocusMode.AUTO -> {
            // Auto-detect based on URL
            val lowerUrl = url.lowercase()
            when {
                lowerUrl.contains("shop") || lowerUrl.contains("store") || lowerUrl.contains("buy") -> SmartFilterType.CONTENT_FOCUS
                lowerUrl.contains("news") || lowerUrl.contains("blog") -> SmartFilterType.TRACKING_REMOVAL
                else -> SmartFilterType.NOISE_REDUCTION
            }
        }
        ContentFocusMode.CUSTOM -> SmartFilterType.DYNAMIC_CONTENT
    }

// Extension functions to convert from serializable to actual types
private fun SerializableZapRule.toZapRule(): ZapRule? {
    return when (type) {
        "css" -> selector?.let { ZapRule.CssSelector(it) }
        "xpath" -> xpath?.let { ZapRule.XPath(it) }
        "text" -> pattern?.let { ZapRule.TextPattern(it) }
        "element" -> tagName?.let { ZapRule.ElementType(it) }
        "attribute" -> if (attribute != null && pattern != null) {
            ZapRule.AttributePattern(attribute, pattern)
        } else null
        else -> null
    }
}

private fun SerializableContentFilter.toContentFilter(): ContentFilter? {
    return when (type) {
        "regex" -> pattern?.let { ContentFilter.RegexFilter(it, isRemoval) }
        "css" -> selector?.let { ContentFilter.CssElementFilter(it, isRemoval) }
        "text" -> textOptions?.let { options ->
            ContentFilter.TextNormalization(
                com.bernaferrari.changedetection.monitoring.TextNormalizationOptions(
                    removeExtraWhitespace = options.removeExtraWhitespace,
                    toLowerCase = options.toLowerCase,
                    removePunctuation = options.removePunctuation,
                    removeNumbers = options.removeNumbers
                )
            )
        }
        "content_type" -> contentType?.let { ContentFilter.ContentTypeFilter(it) }
        else -> null
    }
}

// Extension functions to create updated Site with smart monitoring data
fun Site.withZapRules(zapRules: List<ZapRule>): Site {
    val serializableRules = zapRules.map { it.toSerializable() }
    val json = Json.encodeToString(serializableRules)
    return copy(zapRulesJson = json)
}

fun Site.withCustomFilters(filters: List<ContentFilter>): Site {
    val serializableFilters = filters.map { it.toSerializable() }
    val json = Json.encodeToString(serializableFilters)
    return copy(customFiltersJson = json)
}

fun Site.withSmartMonitoring(enabled: Boolean = true): Site {
    return copy(smartMonitoringEnabled = enabled)
}

fun Site.withContentFocusMode(mode: ContentFocusMode): Site {
    return copy(contentFocusMode = mode)
}

// Authentication Extension Properties
val Site.loginCredentials: LoginCredentials?
    get() = try {
        if (encryptedCredentials.isBlank()) {
            null
        } else {
            val decrypted = decrypt(encryptedCredentials)
            Json.decodeFromString<LoginCredentials>(decrypted)
        }
    } catch (e: Exception) {
        null
    }

val Site.customHeaders: List<CustomHeader>
    get() = try {
        if (customHeadersJson.isBlank()) {
            emptyList()
        } else {
            Json.decodeFromString<List<CustomHeader>>(customHeadersJson)
        }
    } catch (e: Exception) {
        emptyList()
    }

val Site.proxyConfig: ProxyConfig?
    get() = try {
        if (proxyConfigJson.isBlank()) {
            null
        } else {
            Json.decodeFromString<ProxyConfig>(proxyConfigJson)
        }
    } catch (e: Exception) {
        null
    }

val Site.oauthTokens: OAuthTokens?
    get() = try {
        if (oauthTokenData.isBlank()) {
            null
        } else {
            val decrypted = decrypt(oauthTokenData)
            Json.decodeFromString<OAuthTokens>(decrypted)
        }
    } catch (e: Exception) {
        null
    }

// Authentication Helper Functions
fun Site.withLoginCredentials(credentials: LoginCredentials): Site {
    val json = Json.encodeToString(credentials)
    val encrypted = encrypt(json)
    return copy(
        encryptedCredentials = encrypted,
        requiresAuthentication = true,
        authenticationMethod = if (authenticationMethod == AuthenticationMethod.NONE) {
            AuthenticationMethod.FORM_LOGIN
        } else authenticationMethod
    )
}

fun Site.withCustomHeaders(headers: List<CustomHeader>): Site {
    val json = Json.encodeToString(headers)
    return copy(customHeadersJson = json)
}

fun Site.withProxyConfig(config: ProxyConfig): Site {
    val json = Json.encodeToString(config)
    return copy(
        proxyConfigJson = json,
        useProxy = true
    )
}

fun Site.withOAuthTokens(tokens: OAuthTokens): Site {
    val json = Json.encodeToString(tokens)
    val encrypted = encrypt(json)
    return copy(
        oauthTokenData = encrypted,
        requiresAuthentication = true,
        authenticationMethod = AuthenticationMethod.OAUTH2
    )
}

fun Site.withAuthenticationMethod(method: AuthenticationMethod): Site {
    return copy(
        authenticationMethod = method,
        requiresAuthentication = method != AuthenticationMethod.NONE
    )
}

// Security Helper Functions
fun Site.clearAuthenticationData(): Site {
    return copy(
        requiresAuthentication = false,
        authenticationMethod = AuthenticationMethod.NONE,
        encryptedCredentials = "",
        oauthTokenData = "",
        cookieData = ""
    )
}

fun Site.isAuthenticationExpired(): Boolean {
    return when (authenticationMethod) {
        AuthenticationMethod.OAUTH2 -> {
            val tokens = oauthTokens
            tokens?.expiresAt?.let { expiresAt ->
                System.currentTimeMillis() > expiresAt
            } ?: false
        }
        else -> false
    }
}

// Network Configuration Extensions
fun Site.withTimeout(timeoutMs: Long): Site {
    return copy(requestTimeoutMs = timeoutMs)
}

fun Site.withRetryConfig(attempts: Int, delayMs: Long): Site {
    return copy(
        retryAttempts = attempts,
        retryDelayMs = delayMs
    )
}

fun Site.withSyncPriority(priority: SyncPriority): Site {
    return copy(syncPriority = priority)
}

fun Site.withSyncFrequency(minutes: Int): Site {
    return copy(syncFrequencyMinutes = minutes)
}

// Extension functions to convert from actual types to serializable
private fun ZapRule.toSerializable(): SerializableZapRule {
    return when (this) {
        is ZapRule.CssSelector -> SerializableZapRule("css", selector = selector)
        is ZapRule.XPath -> SerializableZapRule("xpath", xpath = xpath)
        is ZapRule.TextPattern -> SerializableZapRule("text", pattern = pattern)
        is ZapRule.ElementType -> SerializableZapRule("element", tagName = tagName)
        is ZapRule.AttributePattern -> SerializableZapRule("attribute", attribute = attribute, pattern = pattern)
    }
}

private fun ContentFilter.toSerializable(): SerializableContentFilter {
    return when (this) {
        is ContentFilter.RegexFilter -> SerializableContentFilter("regex", pattern = pattern, isRemoval = isRemoval)
        is ContentFilter.CssElementFilter -> SerializableContentFilter("css", selector = selector, isRemoval = isRemoval)
        is ContentFilter.TextNormalization -> SerializableContentFilter(
            "text",
            textOptions = SerializableTextOptions(
                removeExtraWhitespace = options.removeExtraWhitespace,
                toLowerCase = options.toLowerCase,
                removePunctuation = options.removePunctuation,
                removeNumbers = options.removeNumbers
            )
        )
        is ContentFilter.ContentTypeFilter -> SerializableContentFilter("content_type", contentType = contentType)
    }
}

// ContentFocusMode enum is now defined in Site.kt

// Helper function to suggest smart monitoring settings based on URL
fun Site.suggestSmartMonitoringSettings(): SmartMonitoringSettings {
    val lowerUrl = url.lowercase()
    val lowerTitle = title?.lowercase() ?: ""
    
    return when {
        // E-commerce sites
        lowerUrl.contains("shop") || lowerUrl.contains("store") || lowerUrl.contains("buy") ||
        lowerUrl.contains("amazon") || lowerUrl.contains("ebay") || lowerUrl.contains("etsy") ||
        lowerTitle.contains("shop") || lowerTitle.contains("store") -> {
            SmartMonitoringSettings(
                contentFocusMode = ContentFocusMode.ECOMMERCE,
                suggestedZapRules = listOf(
                    ZapRule.CssSelector(".ad"),
                    ZapRule.CssSelector(".advertisement"),
                    ZapRule.CssSelector("[id*='ad-']"),
                    ZapRule.CssSelector(".reviews"),
                    ZapRule.CssSelector(".related-products")
                ),
                suggestedFilters = listOf(
                    ContentFilter.CssElementFilter(".price", isRemoval = false),
                    ContentFilter.CssElementFilter(".availability", isRemoval = false),
                    ContentFilter.CssElementFilter(".stock", isRemoval = false)
                )
            )
        }
        
        // News and blog sites
        lowerUrl.contains("news") || lowerUrl.contains("blog") || lowerUrl.contains("medium") ||
        lowerUrl.contains("reddit") || lowerUrl.contains("wordpress") ||
        lowerTitle.contains("news") || lowerTitle.contains("blog") -> {
            SmartMonitoringSettings(
                contentFocusMode = ContentFocusMode.NEWS,
                suggestedZapRules = listOf(
                    ZapRule.CssSelector("script[src*='analytics']"),
                    ZapRule.CssSelector("script[src*='tracking']"),
                    ZapRule.CssSelector(".social-share"),
                    ZapRule.CssSelector(".comments"),
                    ZapRule.CssSelector(".sidebar")
                ),
                suggestedFilters = listOf(
                    ContentFilter.CssElementFilter("article", isRemoval = false),
                    ContentFilter.CssElementFilter(".content", isRemoval = false),
                    ContentFilter.CssElementFilter("main", isRemoval = false)
                )
            )
        }
        
        // General websites
        else -> {
            SmartMonitoringSettings(
                contentFocusMode = ContentFocusMode.GENERAL,
                suggestedZapRules = listOf(
                    ZapRule.CssSelector("script[src*='analytics']"),
                    ZapRule.CssSelector(".ad"),
                    ZapRule.CssSelector(".advertisement")
                ),
                suggestedFilters = listOf(
                    ContentFilter.RegexFilter("\\d{13,}", isRemoval = true), // Timestamps
                    ContentFilter.RegexFilter("utm_[a-z]+=[^&]*", isRemoval = true) // UTM parameters
                )
            )
        }
    }
}

data class SmartMonitoringSettings(
    val contentFocusMode: ContentFocusMode,
    val suggestedZapRules: List<ZapRule>,
    val suggestedFilters: List<ContentFilter>
)

// Secure Encryption/Decryption using Android Keystore
private const val KEYSTORE_ALIAS = "ChangeDetectionSecretKey"
private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val TRANSFORMATION = "AES/GCM/NoPadding"

private fun getOrCreateSecretKey(): SecretKey {
    return try {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        
        // Try to get existing key
        val existingKey = keyStore.getKey(KEYSTORE_ALIAS, null) as? SecretKey
        if (existingKey != null) {
            return existingKey
        }
        
        // Generate new key if doesn't exist
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .setKeySize(256)
            .build()
        
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    } catch (e: Exception) {
        Logger.e("Failed to get or create secret key: ${e.message}")
        throw SecurityException("Cannot create secure encryption key", e)
    }
}

private fun encrypt(plainText: String): String {
    return try {
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        
        // Combine IV and encrypted data
        val combined = iv + encryptedData
        Base64.encodeToString(combined, Base64.NO_WRAP)
    } catch (e: Exception) {
        Logger.e("Encryption failed: ${e.message}")
        throw SecurityException("Failed to encrypt sensitive data", e)
    }
}

private fun decrypt(encryptedText: String): String {
    return try {
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        
        val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
        val iv = combined.sliceArray(0..11) // GCM IV is 12 bytes
        val encryptedData = combined.sliceArray(12 until combined.size)
        
        val spec = javax.crypto.spec.GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        
        val decryptedData = cipher.doFinal(encryptedData)
        String(decryptedData, Charsets.UTF_8)
    } catch (e: Exception) {
        Logger.e("Decryption failed: ${e.message}")
        throw SecurityException("Failed to decrypt sensitive data", e)
    }
}

// Cookie Helper Functions
fun Site.withCookies(cookies: Map<String, String>): Site {
    val json = Json.encodeToString(cookies)
    return copy(cookieData = json)
}

val Site.cookies: Map<String, String>
    get() = try {
        if (cookieData.isBlank()) {
            emptyMap()
        } else {
            Json.decodeFromString<Map<String, String>>(cookieData)
        }
    } catch (e: Exception) {
        emptyMap()
    }

// Validation Helper Functions
fun Site.hasValidAuthentication(): Boolean {
    return when (authenticationMethod) {
        AuthenticationMethod.NONE -> true
        AuthenticationMethod.BASIC, AuthenticationMethod.FORM_LOGIN -> {
            loginCredentials?.let { creds ->
                creds.username.isNotBlank() && creds.password.isNotBlank()
            } ?: false
        }
        AuthenticationMethod.BEARER_TOKEN, AuthenticationMethod.CUSTOM_HEADER -> {
            customHeaders.any { it.name.isNotBlank() && it.value.isNotBlank() }
        }
        AuthenticationMethod.OAUTH2 -> {
            oauthTokens?.accessToken?.isNotBlank() ?: false
        }
        AuthenticationMethod.COOKIE_AUTH -> {
            cookies.isNotEmpty()
        }
    }
}

fun Site.isConfigurationValid(): Boolean {
    return url.isNotBlank() && 
           requestTimeoutMs > 0 && 
           retryAttempts >= 0 && 
           retryDelayMs >= 0 &&
           syncFrequencyMinutes > 0 &&
           (!requiresAuthentication || hasValidAuthentication()) &&
           (!useProxy || proxyConfig != null)
}