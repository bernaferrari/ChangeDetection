/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */

package com.bernaferrari.changedetection.platform

import java.io.File
import java.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Desktop implementation using JSON files for both secure and regular storage
 */
actual fun getSecureStorage(): SecureStorage {
    return DesktopSecureStorage()
}

actual fun getPreferences(): Preferences {
    return DesktopPreferences()
}

/**
 * Desktop implementation of SecureStorage using encrypted JSON files
 * In production, consider using a proper encryption library like Tink
 */
private class DesktopSecureStorage : SecureStorage {
    private val storageDir = File(System.getProperty("user.home"), ".changedetection/secure")
    private val data = mutableMapOf<String, String>()

    init {
        storageDir.mkdirs()
    }

    override suspend fun save(key: String, value: String) {
        // Simple Base64 encoding (not real encryption - use Tink in production)
        val encoded = Base64.getEncoder().encodeToString(value.toByteArray())
        data[key] = encoded
        persist()
    }

    override suspend fun retrieve(key: String): String? {
        return try {
            val encoded = data[key] ?: return null
            String(Base64.getDecoder().decode(encoded))
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun delete(key: String) {
        data.remove(key)
        persist()
    }

    override suspend fun contains(key: String): Boolean {
        return data.containsKey(key)
    }

    override suspend fun clear() {
        data.clear()
        persist()
    }

    private fun persist() {
        val json = Json.encodeToString(data)
        val file = File(storageDir, "secure.json")
        file.writeText(json)
    }
}

/**
 * Desktop implementation of Preferences using JSON file
 */
private class DesktopPreferences : Preferences {
    private val storageDir = File(System.getProperty("user.home"), ".changedetection")
    private val data = mutableMapOf<String, String>()

    init {
        storageDir.mkdirs()
        load()
    }

    override suspend fun save(key: String, value: String) {
        data[key] = value
        persist()
    }

    override suspend fun retrieve(key: String, default: String): String {
        return data[key] ?: default
    }

    override suspend fun saveInt(key: String, value: Int) {
        data[key] = value.toString()
        persist()
    }

    override suspend fun retrieveInt(key: String, default: Int): Int {
        return try {
            data[key]?.toInt() ?: default
        } catch (e: NumberFormatException) {
            default
        }
    }

    override suspend fun saveBoolean(key: String, value: Boolean) {
        data[key] = value.toString()
        persist()
    }

    override suspend fun retrieveBoolean(key: String, default: Boolean): Boolean {
        return when (data[key]?.lowercase()) {
            "true" -> true
            "false" -> false
            else -> default
        }
    }

    override suspend fun delete(key: String) {
        data.remove(key)
        persist()
    }

    override suspend fun clear() {
        data.clear()
        persist()
    }

    private fun load() {
        try {
            val file = File(storageDir, "preferences.json")
            if (file.exists()) {
                val json = file.readText()
                val loaded: Map<String, String> = Json.decodeFromString(json)
                data.putAll(loaded)
            }
        } catch (e: Exception) {
            // Ignore load errors
        }
    }

    private fun persist() {
        try {
            val json = Json.encodeToString(data)
            val file = File(storageDir, "preferences.json")
            file.writeText(json)
        } catch (e: Exception) {
            // Log but don't crash
        }
    }
}
