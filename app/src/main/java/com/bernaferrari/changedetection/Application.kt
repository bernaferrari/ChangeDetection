package com.bernaferrari.changedetection

import android.content.Context
import android.content.SharedPreferences
import android.support.multidex.MultiDexApplication
import com.facebook.stetho.Stetho
import com.jakewharton.threetenabp.AndroidThreeTen
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger

class Application : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        instance = this

        AndroidThreeTen.init(this)
        if (BuildConfig.DEBUG) {
            Stetho.initializeWithDefaults(this)
        }

        Logger.addLogAdapter(object : AndroidLogAdapter() {
            override fun isLoggable(priority: Int, tag: String?): Boolean {
                return BuildConfig.DEBUG
            }
        })
    }

    companion object {
        lateinit var instance: Application
            private set
    }

    fun sharedPrefs(name: String): SharedPreferences {
        return this.getSharedPreferences(name, Context.MODE_PRIVATE)
    }
}