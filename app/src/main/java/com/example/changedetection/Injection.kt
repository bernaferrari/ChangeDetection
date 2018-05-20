package com.example.changedetection

/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context
import com.example.changedetection.data.source.DiffsRepository
import com.example.changedetection.data.source.TasksRepository
import com.example.changedetection.data.source.local.DiffsLocalDataSource
import com.example.changedetection.data.source.local.TasksLocalDataSource
import com.example.changedetection.data.source.local.ToDoDatabase
import com.example.changedetection.util.AppExecutors
import kotlin.jvm.internal.Intrinsics.checkNotNull

/**
 * Enables injection of mock implementations for
 * [TasksDataSource] at compile time. This is useful for testing, since it allows us to use
 * a fake instance of the class to isolate the dependencies and run a test hermetically.
 */
object Injection {

    fun provideDiffsRepository(context: Context): DiffsRepository {
        checkNotNull(context)
        val database = ToDoDatabase.getInstance(context)
        return DiffsRepository.getInstance(
//            FakeTasksRemoteDataSource.instance,
            DiffsLocalDataSource.getInstance(
                AppExecutors(),
                database.diffsDao()
            )
        )
    }

    fun provideTasksRepository(context: Context): TasksRepository {
        checkNotNull(context)
        val database = ToDoDatabase.getInstance(context)
        return TasksRepository.getInstance(
            FakeTasksRemoteDataSource.instance,
            TasksLocalDataSource.getInstance(
                AppExecutors(),
                database.taskDao()
            )
        )
    }
}
