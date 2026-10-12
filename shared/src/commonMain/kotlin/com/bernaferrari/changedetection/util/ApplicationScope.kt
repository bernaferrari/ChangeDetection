package com.bernaferrari.changedetection.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Application-lifetime [CoroutineScope] for work that must outlive any screen's
 * [androidx.lifecycle.viewModelScope] — e.g. site deletion continuing after the
 * user navigates back. Provided via DI as a singleton.
 */
class ApplicationScope {
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
