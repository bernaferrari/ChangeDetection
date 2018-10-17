/*
 *  Copyright 2017 Google Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.bernaferrari.changedetection

import android.annotation.SuppressLint
import android.app.Application
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bernaferrari.changedetection.data.source.SitesRepository
import com.bernaferrari.changedetection.data.source.SnapsRepository
import com.bernaferrari.changedetection.detailstext.TextViewModel
import com.bernaferrari.changedetection.detailsvisual.VisualViewModel

/**
 * A creator is used to inject the product ID into the ViewModel
 *
 *
 * This creator is to showcase how to inject dependencies into ViewModels. It's not
 * actually necessary in this case, as the product ID can be passed in a public method.
 */
class ViewModelFactory private constructor(
    private val mApplication: Application,
    private val mSnapsRepository: SnapsRepository,
    private val mSitesRepository: SitesRepository
) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(MainViewModel::class.java) -> MainViewModel(
                mApplication,
                mSnapsRepository,
                mSitesRepository
            ) as T
            modelClass.isAssignableFrom(TextViewModel::class.java) -> TextViewModel(
                mApplication,
                mSnapsRepository
            ) as T
            modelClass.isAssignableFrom(VisualViewModel::class.java) -> VisualViewModel(
                mApplication,
                mSnapsRepository
            ) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
        }
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: ViewModelFactory? = null

        fun getInstance(application: Application): ViewModelFactory {
            if (INSTANCE == null) {
                synchronized(ViewModelFactory::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = ViewModelFactory(
                            application,
                            Injector.get().snapsRepository(),
                            Injector.get().sitesRepository()
                        )
                    }
                }
            }
            return INSTANCE ?: throw NullPointerException("Expression 'INSTANCE' must not be null")
        }

        @VisibleForTesting
        fun destroyInstance() {
            INSTANCE = null
        }
    }
}
