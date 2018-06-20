package com.bernaferrari.changedetection

import android.support.multidex.MultiDexApplication
import com.facebook.stetho.Stetho
import com.jakewharton.threetenabp.AndroidThreeTen
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger

class Application : MultiDexApplication() {

    lateinit var component: SingletonComponent

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this

        component = DaggerSingletonComponent.builder()
            .contextModule(ContextModule(this))
            .build()

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
        lateinit var INSTANCE: Application
            private set
    }
}