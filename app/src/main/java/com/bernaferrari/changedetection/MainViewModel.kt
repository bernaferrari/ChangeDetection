/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bernaferrari.changedetection

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import androidx.work.WorkManager
import androidx.work.WorkStatus
import com.bernaferrari.changedetection.data.Diff
import com.bernaferrari.changedetection.data.Site
import com.bernaferrari.changedetection.data.source.DiffsDataSource
import com.bernaferrari.changedetection.data.source.DiffsRepository
import com.bernaferrari.changedetection.data.source.SitesRepository
import com.bernaferrari.changedetection.data.source.local.SiteAndLastDiff
import com.bernaferrari.changedetection.util.SingleLiveEvent

/**
 * Exposes the data to be used in the site list screen.
 *
 */
class MainViewModel(
    context: Application,
    private val mDiffsRepository: DiffsRepository,
    private val mSitesRepository: SitesRepository
) : AndroidViewModel(context) {

    private val mSiteUpdated = SingleLiveEvent<Void>()

    fun getOutputStatus(): LiveData<List<WorkStatus>> {
        return WorkManager.getInstance().getStatusesForUniqueWork(WorkerHelper.UNIQUEWORK)
    }

    fun currentTime(): Long = System.currentTimeMillis()

    fun removeSite(site: Site) {
        mDiffsRepository.deleteAllDiffsForSite(site.id)
        mSitesRepository.deleteSite(site.id)
    }

    // Called when clicking on fab.
    internal fun saveSite(title: String, url: String, timestamp: Long): Site {
        val site = Site(title, url, timestamp)

        mSitesRepository.saveSite(site)
        mSiteUpdated.call()
        return site
    }

    // Called when clicking on fab.
    internal fun saveWebsite(
        diff: Diff
    ): MutableLiveData<Boolean> {

        val didItWork = MutableLiveData<Boolean>()
        mDiffsRepository.saveDiff(diff, object : DiffsDataSource.GetDiffCallback {
            override fun onDiffLoaded(diff: Diff) {
                didItWork.value = true
            }

            override fun onDataNotAvailable() {
                didItWork.value = false
            }
        })
        mSiteUpdated.call()
        return didItWork
    }

    internal fun updateSite(site: Site) {
        mSitesRepository.saveSite(site)
        mSiteUpdated.call()
    }

    val items = MutableLiveData<MutableList<SiteAndLastDiff>>()

    fun loadSites(): MutableLiveData<MutableList<SiteAndLastDiff>> {
        items.value = null
        updateItems()
        return items
    }

    fun updateItems() {
        mSitesRepository.getSiteAndLastDiff {
            items.value = it
        }
    }
}
