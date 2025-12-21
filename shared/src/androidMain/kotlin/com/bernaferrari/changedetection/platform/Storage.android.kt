/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */

package com.bernaferrari.changedetection.platform

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

private lateinit var context: Context

/**
 * Initialize Android platform storage with context
 */
fun initAndroidStorage(appContext: Context) {
    context = appContext
}

/**
 * Android implementation using EncryptedSharedPreferences (secure) and SharedPreferences (regular)
 */
actual fun getSecureStorage(): SecureStorage {
    return AndroidSecureStorage(context)
}

actual fun getPreferences(): Preferences {
    return AndroidPreferences(context)
}

/**
 * Android implementation of SecureStorage using EncryptedSharedPreferences
 */
private class AndroidSecureStorage(private val context: Context) : SecureStorage {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedSharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secret_shared_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override suspend fun save(key: String, value: String) {
        encryptedSharedPreferences.edit().putString(key, value).apply()
    }

    override suspend fun retrieve(key: String): String? {
        return encryptedSharedPreferences.getString(key, null)
    }

    override suspend fun delete(key: String) {
        encryptedSharedPreferences.edit().remove(key).apply()
    }

    override suspend fun contains(key: String): Boolean {
        return encryptedSharedPreferences.contains(key)
    }

    override suspend fun clear() {
        encryptedSharedPreferences.edit().clear().apply()
    }
}

/**
 * Android implementation of Preferences using regular SharedPreferences
 */
private class AndroidPreferences(private val context: Context) : Preferences {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

    override suspend fun save(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    override suspend fun retrieve(key: String, default: String): String {
        return sharedPreferences.getString(key, default) ?: default
    }

    override suspend fun saveInt(key: String, value: Int) {
        sharedPreferences.edit().putInt(key, value).apply()
    }

    override suspend fun retrieveInt(key: String, default: Int): Int {
        return sharedPreferences.getInt(key, default)
    }

    override suspend fun saveBoolean(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    override suspend fun retrieveBoolean(key: String, default: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, default)
    }

    override suspend fun delete(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }

    override suspend fun clear() {
        sharedPreferences.edit().clear().apply()
    }
}
