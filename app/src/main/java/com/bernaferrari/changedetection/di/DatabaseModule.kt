package com.bernaferrari.changedetection.di

import android.content.Context
import androidx.room.Room
import com.bernaferrari.changedetection.repo.AppExecutors
import com.bernaferrari.changedetection.repo.source.local.ChangeDatabase
import com.bernaferrari.changedetection.repo.source.local.SitesDao
import com.bernaferrari.changedetection.repo.source.local.SnapsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideChangeDatabase(@ApplicationContext context: Context): ChangeDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            ChangeDatabase::class.java,
            "Changes.db"
        )
            .addMigrations(ChangeDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideSitesDao(database: ChangeDatabase): SitesDao = database.siteDao()

    @Provides
    fun provideSnapsDao(database: ChangeDatabase): SnapsDao = database.snapsDao()

    @Provides
    @Singleton
    fun provideAppExecutors(): AppExecutors = AppExecutors()
}
