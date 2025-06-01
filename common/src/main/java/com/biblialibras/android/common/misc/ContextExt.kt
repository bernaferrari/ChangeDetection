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
package com.biblialibras.android.common.misc

import android.content.Context
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG

var toast: Toast? = null

/** Calls [Context.getSystemService] and casts the return value to [T]. */
inline fun <reified T> Context.systemService(name: String): T {
    return getSystemService(name) as T
}

/** Shows a toast in the receiving context, cancelling any previous. */
fun Context.toast(message: Int) {
    toast?.cancel()
    toast = Toast.makeText(this, message, LENGTH_LONG)
        .apply {
            show()
        }
}

/** Shows a toast in the receiving context, cancelling any previous. */
fun Context.toast(message: String) {
    toast?.cancel()
    toast = Toast.makeText(this, message, LENGTH_LONG)
        .apply {
            show()
        }
}
