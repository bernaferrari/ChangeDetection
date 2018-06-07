package com.bernaferrari.changedetection

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.paging.LivePagedListBuilder
import android.arch.paging.PagedList
import com.bernaferrari.changedetection.data.MinimalSnap
import com.bernaferrari.changedetection.data.Snap
import com.bernaferrari.changedetection.data.source.SnapsDataSource
import com.bernaferrari.changedetection.data.source.SnapsRepository
import com.bernaferrari.changedetection.diffs.text.DiffRowGenerator
import com.bernaferrari.changedetection.extensions.getPositionForAdapter
import com.bernaferrari.changedetection.extensions.removeClutterAndBeautifyHtml
import com.bernaferrari.changedetection.extensions.unescapeHtml
import com.bernaferrari.changedetection.groupie.TextRecycler
import com.bernaferrari.changedetection.util.SingleLiveEvent
import com.orhanobut.logger.Logger
import com.xwray.groupie.Section
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import java.nio.charset.Charset
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * Exposes the data to be used in the site diff screen.
 */
class DetailsViewModel(
    context: Application,
    private val mDiffsRepository: SnapsRepository
) : AndroidViewModel(context) {

    val showNotEnoughtInfoError = SingleLiveEvent<Boolean>()
    val showNoChangesDetectedError = SingleLiveEvent<Void>()
    val showProgress = SingleLiveEvent<Void>()

    /**
     * Called to remove a diff
     *
     * @param id The diff id to be removed.
     */
    fun removeSnap(id: String) {
        mDiffsRepository.deleteSnap(id)
    }

    private var currentJob: Job? = null
    var changePlusOriginal: Boolean = false

    /**
     * Called when we want to generate a diff between two items. Takes as input two ids and the top
     * section and outputs the result to this top section.
     *
     * @param topSection    The section corresponding to the top recyclerview, which will
     * be updated with the result from the diff.
     * @param originalId    The snapId from the original item (which will be in red)
     * @param newId         The snapId from the new item (which will be in green)
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
                if (changePlusOriginal) {
                    mutableList.addAll(nonDiff)
                }
                mutableList.addAll(onlyDiff)
                mutableList.sortBy { it.index }

                launch(UI) {
                    // this way it captures if showNotEnoughtInfoError is null or false
                    if (showNotEnoughtInfoError.value != true && mutableList.isEmpty()) {
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

    /**
     * This will asynchronously fetch a pair of snaps from the database based on their ids
     *
     * @param originalId The original diff id to be fetched.
     * @param newId The newest id to be fetched.
     * @return a pair of diffs
     */
    private suspend fun getFromDb(originalId: String, newId: String): Pair<Snap, Snap> =
        suspendCoroutine { cont ->
            mDiffsRepository.getSnapPair(
                originalId,
                newId,
                object : SnapsDataSource.GetPairCallback {
                    override fun onSnapsLoaded(pair: Pair<Snap, Snap>) {
                        cont.resume(pair)
                    }

                    override fun onDataNotAvailable() =
                        cont.resumeWithException(NullPointerException())
                })
        }

    /**
     * This will asynchronously fetch a pair of diffs from the database based on their ids
     *
     * @param originalId The original diff id to be fetched.
     * @param newId The newest id to be fetched.
     * @return a pair of diffs
     */
    suspend fun getSnapValue(snapId: String): String = suspendCoroutine { cont ->
        mDiffsRepository.getSnap(
            snapId,
            object : SnapsDataSource.GetSnapsCallback {
                override fun onSnapsLoaded(snap: Snap) {
                    cont.resume(snap.content.toString(Charset.defaultCharset()))
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
    fun fsmSelectWithCorrectColor(item: DetailsViewHolder, topSection: Section) {
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
                                    item.adapter.colorSelected.getPositionForAdapter(2)
                                        ?.let { position ->
                                            item.setColor(1)

                                            generateDiff(
                                                topSection,
                                                item.minimalSnap?.snapId!!,
                                                item.adapter.getItemFromAdapter(position)?.snapId!!
                                            )
                                        }

                                } else {
                                    item.adapter.colorSelected.getPositionForAdapter(1)
                                        ?.let { position ->
                                            item.setColor(2)

                                            generateDiff(
                                                topSection,
                                                item.minimalSnap?.snapId!!,
                                                item.adapter.getItemFromAdapter(position)?.snapId!!
                                            )
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

                        item.adapter.colorSelected.getPositionForAdapter(1)?.let { position ->
                            generateDiff(
                                topSection,
                                item.adapter.getItemFromAdapter(position)?.snapId!!,
                                item.minimalSnap?.snapId!!
                            )

                            item.setColor(2)
                        }
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
     * @param adapter    The adapter with a map of selected gradientColor
     * @param topSection The top section, which will be cleared if there are
     * not enought gradientColor selected
     */
    private fun updateCanShowDiff(adapter: DetailsAdapter, topSection: Section) {

        if (adapter.colorSelected.count { it.value > 0 } < 2) {
            // Empty when there is not enough selection
            topSection.update(mutableListOf())
        }

        showNotEnoughtInfoError.value = adapter.colorSelected.count { it.value > 0 } != 2
    }

    private fun generateDiffRows(
        original: Snap?,
        it: Snap?
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

        // compute the differences for two test texts.
        // generateDiffRows will split the lines anyway, so there is no need for splitting again here.
        val rows = generator.generateDiffRows(
            mutableListOf(it.content.toString(Charset.defaultCharset()).removeClutterAndBeautifyHtml()),
            mutableListOf(original.content.toString(Charset.defaultCharset()).removeClutterAndBeautifyHtml())
        )

        val updatingNonDiff = mutableListOf<TextRecycler>()
        val updatingOnlyDiff = mutableListOf<TextRecycler>()

        // for some reason, unescapeHtml isn't working when put after removeClutterAndBeautifyHtml
        // but it is working fine when put here.
        rows.forEachIndexed { index, row ->
            if (row.oldLine == row.newLine) {
                updatingNonDiff.add(TextRecycler(row.oldLine.unescapeHtml(), index))
            } else {
                when {
                    row.newLine.isBlank() -> {
                        updatingOnlyDiff.add(TextRecycler("-" + row.oldLine.unescapeHtml(), index))
                    }
                    row.oldLine.isBlank() -> {
                        updatingOnlyDiff.add(TextRecycler("+" + row.newLine.unescapeHtml(), index))
                    }
                    else -> {
                        updatingOnlyDiff.add(TextRecycler("-" + row.oldLine.unescapeHtml(), index))
                        updatingOnlyDiff.add(TextRecycler("+" + row.newLine.unescapeHtml(), index))
                    }
                }
            }
        }

        return Pair(updatingOnlyDiff, updatingNonDiff)
    }

    fun getWebHistoryForId(id: String): LiveData<PagedList<MinimalSnap>> {
        return LivePagedListBuilder(
            mDiffsRepository.getSnapForPaging(id), PagedList.Config.Builder()
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
