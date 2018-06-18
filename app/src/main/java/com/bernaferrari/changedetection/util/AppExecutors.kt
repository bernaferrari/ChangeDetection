package com.bernaferrari.changedetection.util

import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.android.UI
import kotlin.coroutines.experimental.CoroutineContext

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
    val ioContext: CoroutineContext = DefaultDispatcher,
    val uiContext: CoroutineContext = UI
)