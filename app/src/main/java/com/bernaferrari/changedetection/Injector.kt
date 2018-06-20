/*
 * Copyright 2018 Vandolf Estrellado
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bernaferrari.changedetection

import android.content.Context
import android.content.SharedPreferences
import com.bernaferrari.changedetection.data.source.SitesRepository
import com.bernaferrari.changedetection.data.source.SnapsRepository
import com.bernaferrari.changedetection.data.source.local.ChangeDatabase
import com.bernaferrari.changedetection.data.source.local.SitesLocalDataSource
import com.bernaferrari.changedetection.data.source.local.SnapsLocalDataSource
import com.bernaferrari.changedetection.util.AppExecutors
import dagger.Component
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class ContextModule(private val appContext: Context) {
    @Provides
    fun appContext(): Context = appContext
}

@Module
class AppModule(private val appContext: Context) {

    @Provides
    @Singleton
    fun sharedPrefs(): SharedPreferences {
        return appContext.getSharedPreferences("workerPreferences", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun sitesRepository(): SitesRepository {
        val database = ChangeDatabase.getInstance(appContext)
        return SitesRepository.getInstance(
            SitesLocalDataSource.getInstance(
                AppExecutors(),
                database.siteDao()
            )
        )
    }

    @Provides
    @Singleton
    fun snapsRepository(): SnapsRepository {
        val database = ChangeDatabase.getInstance(appContext)
        return SnapsRepository.getInstance(
            SnapsLocalDataSource.getInstance(
                AppExecutors(),
                database.snapsDao()
            )
        )
    }
}


@Component(modules = [ContextModule::class, AppModule::class])
@Singleton
interface SingletonComponent {
    fun appContext(): Context
    fun sharedPrefs(): SharedPreferences
    fun sitesRepository(): SitesRepository
    fun snapsRepository(): SnapsRepository
}

class Injector private constructor() {
    companion object {
        fun get(): SingletonComponent = Application.get().component
    }
}
