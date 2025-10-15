package com.bernaferrari.changedetection.repo

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.parcelize.Parcelize
import java.util.UUID
import kotlinx.serialization.Serializable

/**
 * Immutable model class for a Site with enhanced features.
 * 
 * Supports:
 * - Basic site monitoring (URL, title, thresholds)
 * - Authentication (credentials, cookies, custom headers)
 * - Element monitoring (CSS selectors, XPath, smart filtering)
 * - Advanced features (zap rules, content filters, focus modes)
 * 
 * Security considerations:
 * - Sensitive data (passwords, tokens) should be encrypted before storage
 * - Cookie data is stored as JSON and should be validated
 * - Custom headers may contain sensitive information
 * 
 * Inspired from Architecture Components MVVM sample app
 */
@Parcelize
@Entity(
    tableName = "sites",
    indices = [(Index(value = ["siteId"], unique = true))]
)
@TypeConverters(
    Site.ConstraintsConverter::class, 
    Site.PairConverter::class,
    Site.ContentFocusModeConverter::class,
    Site.AuthenticationMethodConverter::class,
    Site.SyncPriorityConverter::class
)
data class Site(
    val title: String?,
    val url: String,
    val timestamp: Long,
    @field:PrimaryKey
    @field:ColumnInfo(name = "siteId")
    val id: String,
    val isSuccessful: Boolean,
    val isBrowser: Boolean,
    val threshold: Int,
    val isRead: Boolean, // reserved for future use
    val isSyncEnabled: Boolean, // enable/disable sync
    val isNotificationEnabled: Boolean,
    val notes: String,
    val colors: ColorGroup,
    val constraints: ConstraintsRequired, // Work manager constraints
    
    // Browser and Request Configuration
    val browserMode: Boolean = false,
    val userAgent: String = "",
    val followRedirects: Boolean = true,
    val requestTimeoutMs: Long = 30000L, // 30 seconds default
    val customHeadersJson: String = "", // JSON string of custom HTTP headers
    
    // Authentication Support
    val requiresAuthentication: Boolean = false,
    val authenticationMethod: AuthenticationMethod = AuthenticationMethod.NONE,
    val encryptedCredentials: String = "", // Encrypted username/password JSON  
    val cookieData: String = "", // JSON string of cookies
    val oauthTokenData: String = "", // Encrypted OAuth tokens
    val sessionPersistence: Boolean = true, // Whether to persist session cookies
    
    // Element Monitoring
    val cssSelector: String = "", // CSS selector for element monitoring
    val elementXPath: String = "", // XPath for precise element selection
    val elementMonitoring: Boolean = false, // Enable element-specific monitoring
    val multipleElementMonitoring: Boolean = false, // Monitor multiple elements
    val elementHighlighting: Boolean = false, // Visual highlighting of monitored elements
    
    // Smart Content Processing
    val smartMonitoringEnabled: Boolean = false, // Enable smart content processing
    val zapRulesJson: String = "", // JSON string containing ZAP rules for element removal
    val customFiltersJson: String = "", // JSON string containing custom content filters
    val contentFocusMode: ContentFocusMode = ContentFocusMode.AUTO,
    val noiseFiltering: Boolean = true, // Enable noise filtering
    val contentNormalization: Boolean = true, // Enable content normalization
    
    // Sync and Priority Settings
    val syncPriority: SyncPriority = SyncPriority.NORMAL,
    val backgroundSyncEnabled: Boolean = true, // Enable background sync
    val syncFrequencyMinutes: Int = 60, // Sync frequency in minutes
    
    // Proxy and Network Settings
    val useProxy: Boolean = false,
    val proxyConfigJson: String = "", // JSON string for proxy configuration
    val retryAttempts: Int = 3, // Number of retry attempts on failure
    val retryDelayMs: Long = 5000L // Delay between retries in milliseconds
) : Parcelable {

    @Ignore
    constructor(
        title: String?,
        url: String,
        timestamp: Long,
        tags: String,
        id: String,
        colors: Pair<Int, Int>,
        threshold: Int = 0
    ) : this(
        title,
        url,
        timestamp,
        id,
        true,
        false,
        threshold,
        false,
        true,
        true,
        tags,
        colors,
        ConstraintsRequired(false, false, false, false),
        false,
        "",
        true,
        30000L,
        "",
        false,
        AuthenticationMethod.NONE,
        "",
        "",
        "",
        true,
        "",
        "",
        false,
        false,
        false,
        false,
        "",
        "",
        ContentFocusMode.AUTO,
        true,
        true,
        SyncPriority.NORMAL,
        true,
        60,
        false,
        "",
        3,
        5000L
    )

    @Ignore
    constructor(
        title: String?,
        url: String,
        timestamp: Long,
        id: String,
        colors: ColorGroup,
        isSyncEnabled: Boolean,
        isNotificationEnabled: Boolean,
        threshold: Int = 0
    ) : this(
        title,
        url,
        timestamp,
        id,
        true,
        false,
        threshold,
        false,
        isSyncEnabled,
        isNotificationEnabled,
        "",
        colors,
        ConstraintsRequired(false, false, false, false),
        false,
        "",
        true,
        30000L,
        "",
        false,
        AuthenticationMethod.NONE,
        "",
        "",
        "",
        true,
        "",
        "",
        false,
        false,
        false,
        false,
        "",
        "",
        ContentFocusMode.AUTO,
        true,
        true,
        SyncPriority.NORMAL,
        true,
        60,
        false,
        "",
        3,
        5000L
    )

    @Ignore
    constructor(
        title: String?,
        url: String,
        timestamp: Long,
        tags: String,
        colors: ColorGroup,
        isBrowser: Boolean,
        threshold: Int = 0
    ) : this(
        title,
        url,
        timestamp,
        UUID.randomUUID().toString(),
        true,
        isBrowser,
        threshold,
        false,
        true,
        true,
        tags,
        colors,
        ConstraintsRequired(false, false, false, false),
        false,
        "",
        true,
        30000L,
        "",
        false,
        AuthenticationMethod.NONE,
        "",
        "",
        "",
        true,
        "",
        "",
        false,
        false,
        false,
        false,
        "",
        "",
        ContentFocusMode.AUTO,
        true,
        true,
        SyncPriority.NORMAL,
        true,
        60,
        false,
        "",
        3,
        5000L
    )

    override fun toString(): String {
        return "Site: $title | url: $url | tags: $notes | timestamp: $timestamp"
    }

    internal class ConstraintsConverter {
        @TypeConverter
        fun storedStringToConstraintsRequired(value: String): ConstraintsRequired {
            val langs = value.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toList()

            return ConstraintsRequired(langs)
        }

        @TypeConverter
        fun constraintsRequiredToStoredString(cl: ConstraintsRequired): String {
            var value = ""
            value += "${cl.batteryNotLow}, "
            value += "${cl.wifi}, "
            value += "${cl.charging}, "
            value += "${cl.deviceIdle}"

            return value
        }
    }

    internal class PairConverter {
        @TypeConverter
        fun storedStringToPair(value: String): Pair<Int, Int> {
            val langs = value.split(",".toRegex())
            return Pair(langs.getOrNull(0)?.toInt() ?: 0, langs.getOrNull(1)?.toInt() ?: 0)
        }

        @TypeConverter
        fun pairToStoredString(pair: Pair<Int, Int>): String {
            return "${pair.first},${pair.second}"
        }
    }

    internal class ContentFocusModeConverter {
        @TypeConverter
        fun fromContentFocusMode(value: ContentFocusMode): String {
            return value.name
        }

        @TypeConverter
        fun toContentFocusMode(value: String): ContentFocusMode {
            return try {
                ContentFocusMode.valueOf(value)
            } catch (e: IllegalArgumentException) {
                ContentFocusMode.AUTO
            }
        }
    }

    internal class AuthenticationMethodConverter {
        @TypeConverter
        fun fromAuthenticationMethod(value: AuthenticationMethod): String {
            return value.name
        }

        @TypeConverter
        fun toAuthenticationMethod(value: String): AuthenticationMethod {
            return try {
                AuthenticationMethod.valueOf(value)
            } catch (e: IllegalArgumentException) {
                AuthenticationMethod.NONE
            }
        }
    }

    internal class SyncPriorityConverter {
        @TypeConverter
        fun fromSyncPriority(value: SyncPriority): String {
            return value.name
        }

        @TypeConverter
        fun toSyncPriority(value: String): SyncPriority {
            return try {
                SyncPriority.valueOf(value)
            } catch (e: IllegalArgumentException) {
                SyncPriority.NORMAL
            }
        }
    }
}

/**
 * Authentication methods supported by the Site entity
 */
enum class AuthenticationMethod {
    NONE,           // No authentication required
    BASIC,          // Basic HTTP authentication
    BEARER_TOKEN,   // Bearer token authentication
    OAUTH2,         // OAuth 2.0 authentication
    CUSTOM_HEADER,  // Custom header authentication
    FORM_LOGIN,     // Form-based login
    COOKIE_AUTH     // Cookie-based authentication
}

/**
 * Content focus modes for smart monitoring
 */
enum class ContentFocusMode {
    AUTO,           // Auto-detect based on URL and content
    ECOMMERCE,      // E-commerce focused monitoring
    NEWS,           // News/blog focused monitoring
    GENERAL,        // General content monitoring
    CUSTOM          // Custom focus mode with manual configuration
}

/**
 * Sync priority levels for background processing
 */
enum class SyncPriority {
    LOW,            // Low priority, sync when convenient
    NORMAL,         // Normal priority, regular sync schedule
    HIGH,           // High priority, more frequent sync
    CRITICAL        // Critical priority, immediate sync
}
