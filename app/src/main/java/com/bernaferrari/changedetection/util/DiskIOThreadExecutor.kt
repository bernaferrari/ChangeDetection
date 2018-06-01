package com.bernaferrari.changedetection.util

import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Executor that runs a site on a new background thread.
 */
class DiskIOThreadExecutor : Executor {

    private val mDiskIO: Executor

    init {
        mDiskIO = Executors.newSingleThreadExecutor()
    }

    override fun execute(command: Runnable) {
        mDiskIO.execute(command)
    }
}
