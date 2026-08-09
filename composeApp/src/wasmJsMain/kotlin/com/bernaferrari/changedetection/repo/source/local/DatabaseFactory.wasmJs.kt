/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.repo.source.local

import androidx.room3.Room
import com.bernaferrari.changedetection.worker.createSQLiteWasmWorker

fun createWasmDatabase(): ChangeDatabase = getRoomDatabase(
    Room.databaseBuilder<ChangeDatabase>(name = "change-detection-demo.db")
        .fallbackToDestructiveMigration(dropAllTables = true),
    createSQLiteWasmWorker(),
)
