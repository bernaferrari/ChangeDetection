package com.example.changedetection

import android.support.multidex.MultiDexApplication
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.stetho.Stetho
import com.jakewharton.threetenabp.AndroidThreeTen
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import com.squareup.leakcanary.LeakCanary

public class Application : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        Fresco.initialize(this)

        DebugVisibleLogs.isDebug = BuildConfig.DEBUG
        AndroidThreeTen.init(this)
        Stetho.initializeWithDefaults(this);

        Logger.addLogAdapter(object : AndroidLogAdapter() {
            override fun isLoggable(priority: Int, tag: String?): Boolean {
                return BuildConfig.DEBUG
            }
        })

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return
        }
        LeakCanary.install(this)
    }

    companion object {
        var instance: Application? = null
            private set
    }

}