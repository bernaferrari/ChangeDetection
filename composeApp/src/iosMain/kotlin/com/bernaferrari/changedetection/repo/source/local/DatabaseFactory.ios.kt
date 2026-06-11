/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.repo.source.local

import androidx.room.Room
import androidx.room.RoomDatabase
import platform.Foundation.*
import kotlinx.cinterop.ExperimentalForeignApi

fun getDatabaseBuilder(): RoomDatabase.Builder<ChangeDatabase> {
    val dbFilePath = documentDirectory() + "/change_detection.db"
    return Room.databaseBuilder<ChangeDatabase>(
        name = dbFilePath,
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(documentDirectory?.path)
}
