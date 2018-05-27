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
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.support.annotation.VisibleForTesting
import com.bernaferrari.changedetection.data.source.DiffsRepository
import com.bernaferrari.changedetection.data.source.SitesRepository

/**
 * A creator is used to inject the product ID into the ViewModel
 *
 *
 * This creator is to showcase how to inject dependencies into ViewModels. It's not
 * actually necessary in this case, as the product ID can be passed in a public method.
 */
class ViewModelFactory private constructor(
    private val mApplication: Application,
    private val mDiffsRepository: DiffsRepository,
    private val mSitesRepository: SitesRepository
) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FragmentsViewModel::class.java)) {
            return FragmentsViewModel(mApplication, mDiffsRepository, mSitesRepository) as T
        }
        //        } else if (modelClass.isAssignableFrom(TaskDetailViewModel.class)) {
        //            //noinspection unchecked
        //            return (T) new TaskDetailViewModel(mApplication, mSitesRepository);
        //        } else if (modelClass.isAssignableFrom(AddEditTaskViewModel.class)) {
        //            //noinspection unchecked
        //            return (T) new AddEditTaskViewModel(mApplication, mSitesRepository);
        //        } else if (modelClass.isAssignableFrom(FragmentsViewModel.class)) {
        //            //noinspection unchecked
        //            return (T) new FragmentsViewModel(mApplication, mSitesRepository);
        //        }
        throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
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
                            Injection.provideDiffsRepository(application.applicationContext),
                            Injection.provideSitesRepository(application.applicationContext)
                        )
                    }
                }
            }
            return INSTANCE!!
        }

        @VisibleForTesting
        fun destroyInstance() {
            INSTANCE = null
        }
    }
}
