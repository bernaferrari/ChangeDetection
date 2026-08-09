/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.repo.source.local

import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteDriver
import kotlinx.coroutines.Dispatchers

fun getRoomDatabase(
    builder: RoomDatabase.Builder<ChangeDatabase>,
    driver: SQLiteDriver,
): ChangeDatabase = builder
    .setDriver(driver)
    .setQueryCoroutineContext(Dispatchers.Default)
    .build()
