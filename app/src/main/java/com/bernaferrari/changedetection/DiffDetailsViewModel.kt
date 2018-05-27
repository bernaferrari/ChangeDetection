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
import android.arch.paging.LivePagedListBuilder
import android.arch.paging.PagedList
import com.bernaferrari.changedetection.data.Diff
import com.bernaferrari.changedetection.data.DiffWithoutValue
import com.bernaferrari.changedetection.data.Site
import com.bernaferrari.changedetection.data.source.DiffsDataSource
import com.bernaferrari.changedetection.data.source.DiffsRepository
import com.bernaferrari.changedetection.data.source.SitesRepository
import com.bernaferrari.changedetection.diffs.text.DiffRowGenerator
import com.bernaferrari.changedetection.extensions.cleanUpHtml
import com.bernaferrari.changedetection.groupie.TextRecycler
import com.bernaferrari.changedetection.util.SingleLiveEvent
import com.orhanobut.logger.Logger
import com.xwray.groupie.Section
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * Exposes the data to be used in the site diff screen.
 */
class DiffDetailsViewModel(
    context: Application,
    private val mDiffsRepository: DiffsRepository,
    private val mSitesRepository: SitesRepository
) : AndroidViewModel(context) {

    val showDiffError = SingleLiveEvent<Boolean>()
    val showNoChangesDetectedError = SingleLiveEvent<Void>()
    val showProgress = SingleLiveEvent<Void>()

    private val mSiteUpdated = SingleLiveEvent<Void>()

    fun removeDiff(id: String) {
        mDiffsRepository.deleteDiff(id)
    }

    private fun updateCanShowDiff(adapter: DiffAdapter, section: Section) {

        if (adapter.colorSelected
                .count { it.value > 0 } < 2
        ) {
            // Empty when there is not enough selection
            section.update(mutableListOf())
        }

        showDiffError.value = adapter.colorSelected.count { it.value > 0 } != 2
    }

    // Called when clicking on fab.
    internal fun saveSite(title: String, url: String, timestamp: Long): Site {
        val site = Site(title, url, timestamp)

        mSitesRepository.saveSite(site)
        mSiteUpdated.call()
        return site
    }

    var currentJob: Job? = null
    var withAllDiff: Boolean = false

    fun generateDiff(section: Section, originalId: String, newId: String) {
        currentJob?.cancel()
        if (originalId.isBlank() || newId.isBlank()) {
            return
        }

        currentJob = launch {

            val (original, new) = getFromDb(originalId, newId)
            val (onlyDiff, nonDiff) = generateDiffRows(original, new)

            mutableListOf<TextRecycler>().also { mutableList ->
                if (withAllDiff) {
                    mutableList.addAll(nonDiff)
                }
                mutableList.addAll(onlyDiff)
                mutableList.sortBy { it.index }

                launch(UI) {
                    if (showDiffError.value == false && mutableList.isEmpty()) {
                        showNoChangesDetectedError.call()
                    }

                    // There is a bug on Groupie 2.1.0 which is rendering the diff operation on UI thread.
                    // remove + insert is cheaper than checking if there was a change on thousands of lines.
                    section.update(mutableListOf())
                    section.update(mutableList)
                }
            }
        }
    }

    private suspend fun getFromDb(originalId: String, newId: String): Pair<Diff, Diff> =
        suspendCoroutine { cont ->
            mDiffsRepository.getDiffStorage(
                originalId,
                newId,
                object : DiffsDataSource.GetPairCallback {
                    override fun onDiffLoaded(pair: Pair<Diff, Diff>) {
                        cont.resume(pair)
                    }

                    override fun onDataNotAvailable() =
                        cont.resumeWithException(NullPointerException())
                })
        }

    fun fsmSelectWithCorrectColor(item: DiffViewHolder, section: Section) {
        // This is a simple Finite State Machine
        when (item.colorSelected) {
            2 -> {
                // ORANGE -> GREY
                item.setColor(0)
            }
            1 -> {
                // AMBER -> GREY
                item.setColor(0)
            }
            else -> {
                when (item.adapter.colorSelected.count { it.value > 0 }) {
                    0 -> {
                        // NOTHING IS SELECTED -> AMBER
                        item.setColor(1)
                    }
                    1 -> {
                        // ONE THING IS SELECTED AND IT IS AMBER -> ORANGE
                        // ONE THING IS SELECTED AND IT IS ORANGE -> AMBER
                        for ((_, value) in item.adapter.colorSelected) {
                            if (value > 0) {
                                if (value == 2) {
                                    item.setColor(1)

                                    for ((position, _) in item.adapter.colorSelected.filter { it.value == 2 }) {
                                        generateDiff(
                                            section,
                                            item.diff?.diffId!!,
                                            item.adapter.getItemFromAdapter(position)?.diffId!!
                                        )
                                        break
                                    }
                                } else {
                                    item.setColor(2)

                                    for ((position, _) in item.adapter.colorSelected.filter { it.value == 1 }) {
                                        generateDiff(
                                            section,
                                            item.diff?.diffId!!,
                                            item.adapter.getItemFromAdapter(position)?.diffId!!
                                        )
                                        break
                                    }
                                }
                                break
                            }
                        }
                    }
                    else -> {
                        // TWO ARE SELECTED. UNSELECT THE ORANGE, SELECT ANOTHER THING.
                        for ((position, _) in item.adapter.colorSelected.filter { it.value >= 2 }) {
                            item.adapter.setColor(0, position)
                        }

                        for ((position, _) in item.adapter.colorSelected.filter { it.value == 1 }) {
                            generateDiff(
                                section,
                                item.adapter.getItemFromAdapter(position)?.diffId!!,
                                item.diff?.diffId!!
                            )
                            break
                        }

                        item.setColor(2)
                    }
                }
            }
        }

        updateCanShowDiff(item.adapter, section)
    }

    private fun generateDiffRows(
        original: Diff?,
        it: Diff?
    ): Pair<MutableList<TextRecycler>, MutableList<TextRecycler>> {
        if (original == null || it == null) {
            Logger.d("original or it are null")
            return Pair(mutableListOf(), mutableListOf())
        }

        val generator = DiffRowGenerator.create()
            .showInlineDiffs(true)
            .inlineDiffByWord(true)
            .oldTag { f -> "TEXTREMOVED" }
            .newTag { f -> "TEXTADDED" }
            .build()

        //compute the differences for two test texts.
        val rows = generator.generateDiffRows(
            it.value.cleanUpHtml().split("\n"),
            original.value.cleanUpHtml().split("\n")
        )

        val updatingNonDiff = mutableListOf<TextRecycler>()
        val updatingOnlyDiff = mutableListOf<TextRecycler>()

        rows.forEachIndexed { index, row ->
            if (row.oldLine == row.newLine) {
                updatingNonDiff.add(TextRecycler(row.oldLine, index))
                println("$index none: " + row.oldLine)
            } else {
                when {
                    row.newLine.isBlank() -> {
                        updatingOnlyDiff.add(TextRecycler("-" + row.oldLine, index))
                        println("$index old: " + row.oldLine)
                    }
                    row.oldLine.isBlank() -> {
                        updatingOnlyDiff.add(TextRecycler("+" + row.newLine, index))
                        println("$index new: " + row.newLine)
                    }
                    else -> {
                        updatingOnlyDiff.add(TextRecycler("-" + row.oldLine, index))
                        updatingOnlyDiff.add(TextRecycler("+" + row.newLine, index))

                        println("$index old: " + row.oldLine)
                        println("$index new: " + row.newLine)
                    }
                }
            }
        }

        return Pair(updatingOnlyDiff, updatingNonDiff)
    }

    fun getWebHistoryForId(id: String): LiveData<PagedList<DiffWithoutValue>> {
        return LivePagedListBuilder(
            mDiffsRepository.getCheese(id), PagedList.Config.Builder()
                .setPageSize(PAGE_SIZE)
                .setEnablePlaceholders(ENABLE_PLACEHOLDERS)
                .build()
        ).build()
    }

    companion object {
        /**
         * A good page size is a value that fills at least a screen worth of content on a large
         * device so the User is unlikely to see a null item.
         * You can play with this constant to observe the paging behavior.
         * <p>
         * It's possible to vary this with list device size, but often unnecessary, unless a user
         * scrolling on a large device is expected to scroll through items more quickly than a small
         * device, such as when the large device uses a grid layout of items.
         */
        private const val PAGE_SIZE = 8

        /**
         * If placeholders are enabled, PagedList will report the full size but some items might
         * be null in onBind method (PagedListAdapter triggers a rebind when data is loaded).
         * <p>
         * If placeholders are disabled, onBind will never receive null but as more pages are
         * loaded, the scrollbars will jitter as new pages are loaded. You should probably disable
         * scrollbars if you disable placeholders.
         */
        private const val ENABLE_PLACEHOLDERS = true
    }
}
