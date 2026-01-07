package com.bernaferrari.changedetection.util

actual fun getMemoryUsage(): Long {
    val runtime = Runtime.getRuntime()
    return runtime.totalMemory() - runtime.freeMemory()
}
