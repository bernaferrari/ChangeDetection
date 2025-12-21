/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */

package com.bernaferrari.changedetection.ui.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext

/**
 * Base ViewModel class for multiplatform use.
 *
 * Provides a coroutine scope that survives configuration changes on Android
 * while remaining platform-independent.
 *
 * Usage:
 * ```kotlin
 * class MyViewModel : BaseViewModel() {
 *     fun loadData() = viewModelScope.launch {
 *         // Your async code here
 *     }
 * }
 * ```
 *
 * Note: On Android, wrap with AndroidViewModel in the app module if you need
 * access to Application context.
 */
open class BaseViewModel : CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext = Dispatchers.Default + job

    /**
     * Clear the scope when ViewModel is cleared.
     * Call this from onCleared() in Android ViewModels.
     */
    open fun onCleared() {
        job.cancel()
    }
}
