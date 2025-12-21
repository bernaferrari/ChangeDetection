/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */

package com.bernaferrari.changedetection.platform

import platform.Foundation.NSUserDefaults
import platform.Foundation.NSKeychainItemAttrAccessibleAfterFirstUnlock

/**
 * iOS implementation using NSUserDefaults (regular) and Keychain (secure)
 */
actual fun getSecureStorage(): SecureStorage {
    return iOSSecureStorage()
}

actual fun getPreferences(): Preferences {
    return iOSPreferences()
}

/**
 * iOS implementation of SecureStorage using Keychain
 * Note: Keychain access requires proper entitlements in the iOS app
 */
private class iOSSecureStorage : SecureStorage {
    // TODO: Implement Keychain integration for iOS
    // This requires using Foundation framework and proper error handling

    override suspend fun save(key: String, value: String) {
        // TODO: Save to Keychain
        NSUserDefaults.standardUserDefaults.setObject(value, forKey = key)
    }

    override suspend fun retrieve(key: String): String? {
        // TODO: Retrieve from Keychain
        return NSUserDefaults.standardUserDefaults.stringForKey(key)
    }

    override suspend fun delete(key: String) {
        // TODO: Delete from Keychain
        NSUserDefaults.standardUserDefaults.removeObjectForKey(key)
    }

    override suspend fun contains(key: String): Boolean {
        // TODO: Check Keychain
        return NSUserDefaults.standardUserDefaults.stringForKey(key) != null
    }

    override suspend fun clear() {
        // TODO: Clear Keychain
        // Note: This is not recommended as it clears other app data
    }
}

/**
 * iOS implementation of Preferences using NSUserDefaults
 */
private class iOSPreferences : Preferences {
    private val defaults = NSUserDefaults.standardUserDefaults

    override suspend fun save(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }

    override suspend fun retrieve(key: String, default: String): String {
        return defaults.stringForKey(key) ?: default
    }

    override suspend fun saveInt(key: String, value: Int) {
        defaults.setInteger(value.toLong(), forKey = key)
    }

    override suspend fun retrieveInt(key: String, default: Int): Int {
        val value = defaults.integerForKey(key)
        return if (value == 0L && !defaults.objectForKey(key).isNullOrEmpty()) 0 else value.toInt()
    }

    override suspend fun saveBoolean(key: String, value: Boolean) {
        defaults.setBool(value, forKey = key)
    }

    override suspend fun retrieveBoolean(key: String, default: Boolean): Boolean {
        return if (defaults.objectForKey(key) != null) {
            defaults.boolForKey(key)
        } else {
            default
        }
    }

    override suspend fun delete(key: String) {
        defaults.removeObjectForKey(key)
    }

    override suspend fun clear() {
        // Get all keys and remove them
        val keys = defaults.dictionaryRepresentation().keys
        for (key in keys) {
            defaults.removeObjectForKey(key as String)
        }
    }
}
