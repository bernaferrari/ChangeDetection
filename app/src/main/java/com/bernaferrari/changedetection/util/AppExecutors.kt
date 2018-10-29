package com.bernaferrari.changedetection.util

import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

/**
 * Global executor pools for the whole application.
 *
 *
 * Grouping sites like this avoids the effects of site starvation (e.g. disk reads don't wait behind
 * webservice requests).
 *
 * Inspired from this: https://github.com/dmytrodanylyk/android-architecture
 */
open class AppExecutors constructor(
    val ioContext: CoroutineContext = Dispatchers.IO,
    val uiContext: CoroutineContext = Dispatchers.Main
)
