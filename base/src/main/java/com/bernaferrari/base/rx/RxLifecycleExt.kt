@file:Suppress("unused")

package com.bernaferrari.base.rx

import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.Lifecycle.State.DESTROYED
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import io.reactivex.disposables.Disposable

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


/** @author Aidan Follestad (afollestad) */
class LifecycleAwareDisposable(
    private val disposable: Disposable
) : LifecycleObserver {

    @OnLifecycleEvent(ON_DESTROY)
    fun dispose() = disposable.dispose()
}

/**
 * Wraps [disposable] so that it is disposed of when the receiving [LifecycleOwner]
 * is destroyed.
 *
 * @author Aidan Follestad (afollestad)
 */
fun LifecycleOwner.ownRx(disposable: Disposable) {
    if (this.lifecycle.currentState == DESTROYED) {
        disposable.dispose()
        return
    }
    this.lifecycle.addObserver(LifecycleAwareDisposable(disposable))
}

/**
 * Attaches the receiving [Disposable] so that it is disposed of when [lifecycleOwner]
 * is destroyed.
 *
 * @author Aidan Follestad (afollestad)
 */
fun Disposable.attachLifecycle(lifecycleOwner: LifecycleOwner) {
    lifecycleOwner.ownRx(this)
}
