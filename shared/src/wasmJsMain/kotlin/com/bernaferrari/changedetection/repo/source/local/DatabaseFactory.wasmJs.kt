package com.bernaferrari.changedetection.repo.source.local

import androidx.room3.Room
import com.bernaferrari.changedetection.worker.createSQLiteWasmWorker

fun createWasmDatabase(): ChangeDatabase = getRoomDatabase(
    Room.databaseBuilder<ChangeDatabase>(name = "change-detection-demo.db")
        .fallbackToDestructiveMigration(dropAllTables = true),
    createSQLiteWasmWorker(),
)
