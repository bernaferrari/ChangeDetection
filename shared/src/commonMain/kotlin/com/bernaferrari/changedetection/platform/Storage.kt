/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */

package com.bernaferrari.changedetection.platform

/**
 * Platform-abstracted storage interface for secure/encrypted preferences
 */
interface SecureStorage {
    /**
     * Save a key-value pair securely
     */
    suspend fun save(key: String, value: String)

    /**
     * Retrieve a securely stored value
     */
    suspend fun retrieve(key: String): String?

    /**
     * Delete a stored value
     */
    suspend fun delete(key: String)

    /**
     * Check if a key exists
     */
    suspend fun contains(key: String): Boolean

    /**
     * Clear all stored values
     */
    suspend fun clear()
}

/**
 * Platform-abstracted regular (unencrypted) preferences
 */
interface Preferences {
    /**
     * Save a preference
     */
    suspend fun save(key: String, value: String)

    /**
     * Retrieve a preference
     */
    suspend fun retrieve(key: String, default: String = ""): String

    /**
     * Save an integer preference
     */
    suspend fun saveInt(key: String, value: Int)

    /**
     * Retrieve an integer preference
     */
    suspend fun retrieveInt(key: String, default: Int = 0): Int

    /**
     * Save a boolean preference
     */
    suspend fun saveBoolean(key: String, value: Boolean)

    /**
     * Retrieve a boolean preference
     */
    suspend fun retrieveBoolean(key: String, default: Boolean = false): Boolean

    /**
     * Delete a preference
     */
    suspend fun delete(key: String)

    /**
     * Clear all preferences
     */
    suspend fun clear()
}

/**
 * Get the platform-specific secure storage implementation
 */
expect fun getSecureStorage(): SecureStorage

/**
 * Get the platform-specific preferences implementation
 */
expect fun getPreferences(): Preferences
