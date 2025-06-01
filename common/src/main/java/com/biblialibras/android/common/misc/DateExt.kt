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

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Converts a Date to a timestamp string that can be used for file names, etc. */
fun Date.timestampString(): String {
    if (time <= 0) {
        return "Invalid"
    }
    val df = SimpleDateFormat("MMMMd-hhmmssa", Locale.US)
    return df.format(this)
}

/** Converts milliseconds into a human readable date string. */
fun Long.friendlyDate(): String {
    if (this <= 0) {
        return "Invalid"
    }
    val df = SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault())
    return df.format(this)
}
