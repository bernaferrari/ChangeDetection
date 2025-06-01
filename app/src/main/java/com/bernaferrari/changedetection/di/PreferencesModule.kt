package com.bernaferrari.changedetection.di

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("workerPreferences", Context.MODE_PRIVATE)
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
