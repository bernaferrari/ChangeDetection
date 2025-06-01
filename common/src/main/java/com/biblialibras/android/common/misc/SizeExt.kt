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
package com.afollestad.mnmlscreenrecord.common.misc

private const val KILOBYTE = 1_024L
private const val MEGABYTE = 1_048_576L
private const val GIGABYTE = 1_073_741_824L

/** Converts byte length into a human-readable unit string, like 5GB. */
fun Long.friendlySize(): String {
    return when {
        this >= GIGABYTE -> "${this / GIGABYTE}GB"
        this >= MEGABYTE -> "${this / MEGABYTE}MB"
        this >= KILOBYTE -> "${this / KILOBYTE}KB"
        else -> "${this}B"
    }
}
