package com.bernaferrari.changedetection

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
import com.bernaferrari.changedetection.data.source.DiffsRepository
import com.bernaferrari.changedetection.data.source.SitesRepository
import com.bernaferrari.changedetection.data.source.local.ChangeDatabase
import com.bernaferrari.changedetection.data.source.local.DiffsLocalDataSource
import com.bernaferrari.changedetection.data.source.local.SitesLocalDataSource
import com.bernaferrari.changedetection.util.AppExecutors
import kotlin.jvm.internal.Intrinsics.checkNotNull

/**
 * Enables injection of mock implementations for
 * [TasksDataSource] at compile time. This is useful for testing, since it allows us to use
 * a fake instance of the class to isolate the dependencies and run a test hermetically.
 */
object Injection {

    fun provideDiffsRepository(context: Context): DiffsRepository {
        checkNotNull(context)
        val database = ChangeDatabase.getInstance(context)
        return DiffsRepository.getInstance(
            DiffsLocalDataSource.getInstance(
                AppExecutors(),
                database.diffsDao()
            )
        )
    }

    fun provideSitesRepository(context: Context): SitesRepository {
        checkNotNull(context)
        val database = ChangeDatabase.getInstance(context)
        return SitesRepository.getInstance(
            SitesLocalDataSource.getInstance(
                AppExecutors(),
                database.siteDao(),
                database.diffsDao()
            )
        )
    }
}
