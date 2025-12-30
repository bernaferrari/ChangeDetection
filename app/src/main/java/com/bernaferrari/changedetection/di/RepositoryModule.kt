package com.bernaferrari.changedetection.di

import android.content.Context
import com.bernaferrari.changedetection.repo.AppExecutors
import com.bernaferrari.changedetection.repo.source.SitesDataSource
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.repo.source.SnapsDataSource
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import com.bernaferrari.changedetection.repo.source.local.SitesDao
import com.bernaferrari.changedetection.repo.source.local.SitesLocalDataSource
import com.bernaferrari.changedetection.repo.source.local.SnapsDao
import com.bernaferrari.changedetection.repo.source.local.SnapsLocalDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for app-level repository dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    // ============= App-level Data Sources =============

    @Provides
    @Singleton
    fun provideSitesDataSource(
        dao: SitesDao,
        executors: AppExecutors
    ): SitesDataSource = SitesLocalDataSource(executors, dao)

    @Provides
    @Singleton
    fun provideSnapsDataSource(
        dao: SnapsDao,
        executors: AppExecutors,
        @ApplicationContext context: Context
    ): SnapsDataSource = SnapsLocalDataSource(executors, dao, context)

    // ============= App-level Repositories =============

    @Provides
    @Singleton
    fun provideSitesRepository(sitesDataSource: SitesDataSource): SitesRepository =
        SitesRepository(sitesDataSource)

    @Provides
    @Singleton
    fun provideSnapsRepository(snapsDataSource: SnapsDataSource): SnapsRepository =
        SnapsRepository(snapsDataSource)

}
