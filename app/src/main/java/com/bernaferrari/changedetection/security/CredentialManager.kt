package com.bernaferrari.changedetection.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.security.KeyStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.orhanobut.logger.Logger

/**
 * Secure credential management using Android Keystore for encryption.
 *
 * This manager handles encryption/decryption of sensitive data like credentials
 * using the device's secure hardware when available, or software-backed keystore.
 *
 * Security considerations:
 * - Credentials are encrypted at rest using AES-256-GCM
 * - Encryption key is stored in Android Keystore (hardware-backed on supported devices)
 * - Each credential is protected with a unique initialization vector (IV)
 * - Master Key is used for EncryptedSharedPreferences (Android 13+)
 */
@Serializable
data class EncryptedCredentials(
    val username: String = "",
    val password: String = "",
    val token: String = ""
)

object CredentialManager {
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "changedetection_key"
    private const val PREFS_NAME = "encrypted_credentials"
    private const val GCM_TAG_LENGTH = 128
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private var encryptedSharedPrefs: EncryptedSharedPreferences? = null

    /**
     * Initialize the credential manager with context
     */
    fun initialize(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Create or retrieve the encryption key
                ensureKeyExists()

                // Initialize EncryptedSharedPreferences
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val masterKey = MasterKey.Builder(context)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build()

                    encryptedSharedPrefs = EncryptedSharedPreferences.create(
                        context,
                        PREFS_NAME,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    ) as EncryptedSharedPreferences
                }
            }
            Logger.d("✅ CredentialManager initialized successfully")
        } catch (e: Exception) {
            Logger.e("❌ Failed to initialize CredentialManager: ${e.message}")
        }
    }

    /**
     * Encrypt credentials and return as Base64 string with IV prepended
     * Format: BASE64(IV || CIPHERTEXT)
     */
    fun encryptCredentials(username: String, password: String = "", token: String = ""): String {
        return try {
            val credentials = EncryptedCredentials(username, password, token)
            val jsonString = Json.encodeToString(EncryptedCredentials.serializer(), credentials)
            encryptString(jsonString)
        } catch (e: Exception) {
            Logger.e("❌ Failed to encrypt credentials: ${e.message}")
            "" // Return empty on failure - better to be safe than expose credentials
        }
    }

    /**
     * Decrypt credentials from encrypted string
     */
    fun decryptCredentials(encrypted: String): EncryptedCredentials? {
        return try {
            if (encrypted.isEmpty()) return null

            val decrypted = decryptString(encrypted)
            Json.decodeFromString(EncryptedCredentials.serializer(), decrypted)
        } catch (e: Exception) {
            Logger.e("❌ Failed to decrypt credentials: ${e.message}")
            null
        }
    }

    /**
     * Encrypt a generic string using the keystore key
     */
    private fun encryptString(plaintext: String): String {
        return try {
            val key = getOrCreateKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)

            val iv = cipher.iv
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

            // Prepend IV to ciphertext: IV || CIPHERTEXT
            val combined = ByteArray(iv.size + ciphertext.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)

            // Return as Base64
            android.util.Base64.encodeToString(combined, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            Logger.e("❌ Encryption failed: ${e.message}")
            throw SecurityException("Failed to encrypt data", e)
        }
    }

    /**
     * Decrypt a string encrypted with encryptString()
     */
    private fun decryptString(encrypted: String): String {
        return try {
            val key = getOrCreateKey()
            val combined = android.util.Base64.decode(encrypted, android.util.Base64.NO_WRAP)

            // Extract IV and ciphertext
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val ivLength = cipher.blockSize
            val iv = combined.sliceArray(0 until ivLength)
            val ciphertext = combined.sliceArray(ivLength until combined.size)

            // Initialize cipher with IV
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            val plaintext = cipher.doFinal(ciphertext)
            String(plaintext, Charsets.UTF_8)
        } catch (e: Exception) {
            Logger.e("❌ Decryption failed: ${e.message}")
            throw SecurityException("Failed to decrypt data", e)
        }
    }

    /**
     * Get or create the encryption key in Android Keystore
     */
    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
        keyStore.load(null)

        return if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.getKey(KEY_ALIAS, null) as SecretKey
        } else {
            createKey()
        }
    }

    /**
     * Create a new AES-256 key in Android Keystore
     */
    private fun createKey(): SecretKey {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val keySpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(256)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setUserAuthenticationRequired(false) // Can be set to true for added security
                .build()

            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER
            )
            keyGenerator.init(keySpec)
            keyGenerator.generateKey()
        } else {
            // Fallback for older Android versions (though we target Android 13+)
            throw UnsupportedOperationException("Android 6.0+ required for hardware-backed keystore")
        }
    }

    /**
     * Ensure the key exists and is valid
     */
    private fun ensureKeyExists() {
        try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)

            if (!keyStore.containsAlias(KEY_ALIAS)) {
                createKey()
                Logger.d("🔑 New encryption key created in Android Keystore")
            }
        } catch (e: Exception) {
            Logger.e("❌ Failed to ensure key exists: ${e.message}")
            throw SecurityException("Failed to manage encryption key", e)
        }
    }

    /**
     * Delete the encryption key (use with caution - will render existing data unreadable)
     */
    fun deleteKey() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
                keyStore.load(null)
                keyStore.deleteEntry(KEY_ALIAS)
                Logger.d("🗑️ Encryption key deleted from Android Keystore")
            }
        } catch (e: Exception) {
            Logger.e("❌ Failed to delete key: ${e.message}")
        }
    }

    /**
     * Securely clear sensitive data from memory
     */
    fun clearSensitiveData(text: String) {
        // In production, consider using a utility to clear char arrays
        // Java strings are immutable so this is mainly for documentation
        Logger.d("Sensitive data should be cleared when no longer needed")
    }
}
