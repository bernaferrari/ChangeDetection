package com.bernaferrari.changedetection

import android.app.Application
import com.facebook.stetho.Stetho
import com.jakewharton.threetenabp.AndroidThreeTen
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger

class App : Application() {

    lateinit var component: SingletonComponent

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this

        component = DaggerSingletonComponent.builder()
            .contextModule(ContextModule(this))
            .appModule(AppModule(this))
            .build()

        AndroidThreeTen.init(this)
        if (BuildConfig.DEBUG) {
            Stetho.initializeWithDefaults(this)
        }

        Logger.addLogAdapter(object : AndroidLogAdapter() {
            override fun isLoggable(priority: Int, tag: String?): Boolean {
                return true
            }
        })
    }

    companion object {
        private var INSTANCE: App? = null

        @JvmStatic
        fun get(): App =
            INSTANCE ?: throw NullPointerException("App INSTANCE must not be null")
    }
}
