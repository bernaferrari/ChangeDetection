package com.bernaferrari.changedetection.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual val fileIoDispatcher: CoroutineDispatcher get() = Dispatchers.Default
