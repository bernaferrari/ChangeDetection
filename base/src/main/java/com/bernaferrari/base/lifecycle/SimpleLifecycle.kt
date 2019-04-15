/**
 * Designed and developed by Aidan Follestad (@afollestad)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bernaferrari.base.lifecycle

import androidx.lifecycle.Lifecycle.Event.*
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

/**
 * A wrapper to easily provide a arch Lifecycle in places where they don't already exist, such as
 * a View or Service.
 *
 * @author Aidan Follestad (@afollestad)
 */
class SimpleLifecycle(provider: LifecycleOwner) : LifecycleRegistry(provider) {

    /** Handles the create, start, and resume lifecycle events. */
    fun onCreate() {
        handleLifecycleEvent(ON_CREATE)
        handleLifecycleEvent(ON_START)
        handleLifecycleEvent(ON_RESUME)
    }

    /** Handles the pause, stop, and destroy lifecycle events. */
    fun onDestroy() {
        handleLifecycleEvent(ON_PAUSE)
        handleLifecycleEvent(ON_STOP)
        handleLifecycleEvent(ON_DESTROY)
    }
}
