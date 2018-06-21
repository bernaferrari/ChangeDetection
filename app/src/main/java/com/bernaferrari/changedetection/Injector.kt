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

import android.arch.persistence.room.Room
import android.content.Context
import android.content.SharedPreferences
import com.bernaferrari.changedetection.data.source.SitesDataSource
import com.bernaferrari.changedetection.data.source.SitesRepository
import com.bernaferrari.changedetection.data.source.SnapsDataSource
import com.bernaferrari.changedetection.data.source.SnapsRepository
import com.bernaferrari.changedetection.data.source.local.*
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
}

@Module
class RepositoriesModule {

    @Singleton
    @Provides
    internal fun sitesRepository(sitesDataSource: SitesDataSource): SitesRepository =
        SitesRepository(sitesDataSource)

    @Singleton
    @Provides
    internal fun snapsRepository(snapsDataSource: SnapsDataSource): SnapsRepository =
        SnapsRepository(snapsDataSource)

    @Singleton
    @Provides
    internal fun provideSitesLocalDataSource(
        dao: SitesDao,
        executors: AppExecutors
    ): SitesDataSource = SitesLocalDataSource(executors, dao)

    @Singleton
    @Provides
    internal fun provideSnapsLocalDataSource(
        dao: SnapsDao,
        executors: AppExecutors,
        context: Context
    ): SnapsDataSource = SnapsLocalDataSource(executors, dao, context)

    @Singleton
    @Provides
    internal fun provideSitesDao(db: ChangeDatabase): SitesDao = db.siteDao()

    @Singleton
    @Provides
    internal fun provideSnapsDao(db: ChangeDatabase): SnapsDao = db.snapsDao()

}

@Module
class RepositoriesMutualDependenciesModule {

    @Singleton
    @Provides
    internal fun provideDb(context: Context): ChangeDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            ChangeDatabase::class.java,
            "Changes.db"
        )
            .build()
    }

    @Singleton
    @Provides
    internal fun provideAppExecutors(): AppExecutors = AppExecutors()
}

@Component(modules = [ContextModule::class, AppModule::class, RepositoriesModule::class, RepositoriesMutualDependenciesModule::class])
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
