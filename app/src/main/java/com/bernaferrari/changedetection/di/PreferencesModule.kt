package com.bernaferrari.changedetection.di

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Extension property for DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.dataStore

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("workerPreferences", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }

    object PreferenceKeys {
        val LIGHT_MODE = booleanPreferencesKey("lightMode")
        val COLOR_BY_SDK = booleanPreferencesKey("colorBySdk")
        val SHOW_SYSTEM_APPS = booleanPreferencesKey("showSystemApps")
        val BACKGROUND_SYNC = booleanPreferencesKey("backgroundSync")
        val SYNC_INTERVAL = stringPreferencesKey("syncInterval")
        val ORDER_BY_SDK = booleanPreferencesKey("orderBySdk")
    }
}
