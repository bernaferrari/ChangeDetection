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
package com.bernaferrari.base.view

import android.view.View
import java.lang.System.currentTimeMillis

private const val DEFAULT_DEBOUNCE_INTERVAL = 750L

/** @author Aidan Follestad (@afollestad) */
abstract class DebouncedOnClickListener(
    private val delayBetweenClicks: Long = DEFAULT_DEBOUNCE_INTERVAL
) : View.OnClickListener {

    private var lastClickTimestamp = -1L

    @Deprecated(
        message = "onDebouncedClick should be overridden instead.",
        replaceWith = ReplaceWith("onDebouncedClick(v)")
    )
    override fun onClick(v: View) {
        val now = currentTimeMillis()
        if (lastClickTimestamp == -1L || now >= (lastClickTimestamp + delayBetweenClicks)) {
            onDebouncedClick(v)
        }
        lastClickTimestamp = now
    }

    abstract fun onDebouncedClick(v: View)
}

/**
 * Sets a click listener that prevents quick repeated clicks.
 *
 * @author Aidan Follestad (@afollestad)
 */
fun View.onDebouncedClick(
    delayBetweenClicks: Long = DEFAULT_DEBOUNCE_INTERVAL,
    click: (view: View) -> Unit
) {
    setOnClickListener(object : DebouncedOnClickListener(delayBetweenClicks) {
        override fun onDebouncedClick(v: View) {
            click(v)
        }
    })
}
