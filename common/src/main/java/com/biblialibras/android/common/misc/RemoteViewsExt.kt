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

import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.RemoteViews
import androidx.annotation.IdRes

/**
 * A utility to call [RemoteViews.setViewVisibility] which normally accepts [VISIBLE] or [GONE]
 * with a boolean that chooses one or the other.
 */
fun RemoteViews.setViewVisibility(@IdRes viewId: Int, visible: Boolean) {
    setViewVisibility(viewId, if (visible) VISIBLE else GONE)
}
