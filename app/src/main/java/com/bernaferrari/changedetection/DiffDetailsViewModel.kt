package com.bernaferrari.changedetection

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.paging.LivePagedListBuilder
import android.arch.paging.PagedList
import com.bernaferrari.changedetection.data.Diff
import com.bernaferrari.changedetection.data.DiffWithoutValue
import com.bernaferrari.changedetection.data.source.DiffsDataSource
import com.bernaferrari.changedetection.data.source.DiffsRepository
import com.bernaferrari.changedetection.data.source.SitesRepository
import com.bernaferrari.changedetection.diffs.text.DiffRowGenerator
import com.bernaferrari.changedetection.extensions.cleanUpHtml
import com.bernaferrari.changedetection.extensions.getPositionForAdapter
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

    fun removeDiff(id: String) {
        mDiffsRepository.deleteDiff(id)
    }

    private var currentJob: Job? = null
    var withAllDiff: Boolean = false

    /**
     * Called when we want to generate a diff between two items. Takes as input two ids and the top
     * section and outputs the result to this top section.
     *
     * @param topSection    The section corresponding to the top recyclerview, which will
     * be updated with the result from the diff.
     * @param originalId    The diffId from the original item (which will be in red)
     * @param newId         The diffId from the new item (which will be in green)
     * which will be updated by [generateDiff] with corresponding diff
     */
    fun generateDiff(topSection: Section, originalId: String, newId: String) {
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
                    topSection.update(mutableListOf())
                    topSection.update(mutableList)
                }
            }
        }
    }

    private suspend fun getFromDb(originalId: String, newId: String): Pair<Diff, Diff> =
        suspendCoroutine { cont ->
            mDiffsRepository.getDiffPair(
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


    suspend fun getDiff(diffId: String): Diff = suspendCoroutine { cont ->
        mDiffsRepository.getDiff(diffId,
            object : DiffsDataSource.GetDiffCallback {
                override fun onDiffLoaded(diff: Diff) {
                    cont.resume(diff)
                }

                override fun onDataNotAvailable() = cont.resumeWithException(NullPointerException())
            })
    }

    /**
     * Called when there is a selection. This is a simple Finite State Machine, where
     * it decides the color to select based on previous selection.
     *
     * @param item    Pass the item to be selected
     * @param topSection Pass the top part of the screen,
     * which will be updated by [generateDiff] with corresponding diff
     */
    fun fsmSelectWithCorrectColor(item: DiffViewHolder, topSection: Section) {
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

                                    generateDiff(
                                        topSection,
                                        item.diff?.diffId!!,
                                        item.adapter.getItemFromAdapter(
                                            item.adapter.colorSelected.getPositionForAdapter(
                                                2
                                            )
                                        )?.diffId!!
                                    )
                                } else {
                                    item.setColor(2)

                                    generateDiff(
                                        topSection,
                                        item.diff?.diffId!!,
                                        item.adapter.getItemFromAdapter(
                                            item.adapter.colorSelected.getPositionForAdapter(
                                                1
                                            )
                                        )?.diffId!!
                                    )
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

                        generateDiff(
                            topSection,
                            item.adapter.getItemFromAdapter(
                                item.adapter.colorSelected.getPositionForAdapter(
                                    1
                                )
                            )?.diffId!!,
                            item.diff?.diffId!!
                        )

                        item.setColor(2)
                    }
                }
            }
        }

        updateCanShowDiff(item.adapter, topSection)
    }

    /**
     * When there is only one item selected, we want to show an error message
     * and clear the RecyclerView
     *
     * @param adapter    The adapter with a map of selected colors
     * @param topSection The top section, which will be cleared if there are
     * not enought colors selected
     */
    private fun updateCanShowDiff(adapter: DiffAdapter, topSection: Section) {

        if (adapter.colorSelected.count { it.value > 0 } < 2) {
            // Empty when there is not enough selection
            topSection.update(mutableListOf())
        }

        showDiffError.value = adapter.colorSelected.count { it.value > 0 } != 2
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
            .oldTag { _ -> "TEXTREMOVED" }
            .newTag { _ -> "TEXTADDED" }
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
            } else {
                when {
                    row.newLine.isBlank() -> {
                        updatingOnlyDiff.add(TextRecycler("-" + row.oldLine, index))
                    }
                    row.oldLine.isBlank() -> {
                        updatingOnlyDiff.add(TextRecycler("+" + row.newLine, index))
                    }
                    else -> {
                        updatingOnlyDiff.add(TextRecycler("-" + row.oldLine, index))
                        updatingOnlyDiff.add(TextRecycler("+" + row.newLine, index))
                    }
                }
            }
        }

        return Pair(updatingOnlyDiff, updatingNonDiff)
    }

    fun getWebHistoryForId(id: String): LiveData<PagedList<DiffWithoutValue>> {
        return LivePagedListBuilder(
            mDiffsRepository.getDiffForPaging(id), PagedList.Config.Builder()
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
