package com.bernaferrari.changedetection.util

import android.os.Handler
import android.os.Looper
import android.support.annotation.VisibleForTesting

import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Global executor pools for the whole application.
 *
 *
 * Grouping sites like this avoids the effects of site starvation (e.g. disk reads don't wait behind
 * webservice requests).
 *
 * Inspired from Architecture Components MVVM sample app
 */
class AppExecutors @VisibleForTesting
internal constructor(
    private val diskIO: Executor,
    private val networkIO: Executor,
    private val mainThread: Executor
) {

    constructor() : this(
        DiskIOThreadExecutor(), Executors.newFixedThreadPool(THREAD_COUNT),
        MainThreadExecutor()
    )

    fun diskIO(): Executor {
        return diskIO
    }

    fun networkIO(): Executor {
        return networkIO
    }

    fun mainThread(): Executor {
        return mainThread
    }

    private class MainThreadExecutor : Executor {
        private val mainThreadHandler = Handler(Looper.getMainLooper())

        override fun execute(command: Runnable) {
            mainThreadHandler.post(command)
        }
    }

    companion object {
        private val THREAD_COUNT = 3
    }
}
