package com.bernaferrari.changedetection.detailstext

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.bernaferrari.changedetection.data.Snap
import com.bernaferrari.changedetection.data.source.SnapsRepository
import com.bernaferrari.changedetection.extensions.getPositionForAdapter
import com.bernaferrari.changedetection.extensions.removeClutterAndBeautifyHtmlIfNecessary
import com.bernaferrari.changedetection.extensions.unescapeHtml
import com.bernaferrari.changedetection.groupie.TextRecycler
import com.bernaferrari.changedetection.util.SingleLiveEvent
import com.bernaferrari.diffutils.diffs.text.DiffRowGenerator
import com.xwray.groupie.Section
import kotlinx.coroutines.experimental.*
import java.nio.charset.Charset
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Exposes the data to be used in the site diff screen.
 */
class TextViewModel(
    context: Application,
    private val mSnapsRepository: SnapsRepository
) : AndroidViewModel(context), CoroutineScope {

    val showNotEnoughInfoError = SingleLiveEvent<Boolean>()
    val showNoChangesDetectedError = SingleLiveEvent<Void>()
    val showProgress = SingleLiveEvent<Void>()

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCleared() {
        job.cancel()
        super.onCleared()
    }

    /**
     * Called to remove a diff
     *
     * @param id The diff url to be removed.
     */
    fun removeSnap(id: String?) = GlobalScope.launch {
        if (id != null) mSnapsRepository.deleteSnap(id)
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
     * @param revisedId     The snapId from the revised item (which will be in green)
     * which will be updated by [generateDiff] with corresponding diff
     */
    fun generateDiff(topSection: Section, originalId: String?, revisedId: String?) {
        currentJob?.cancel()
        if (originalId.isNullOrBlank() || revisedId.isNullOrBlank()) {

            mutableListOf<TextRecycler>().also { mutableList ->
                mutableList.addAll(mutableListOf())
                // this way it captures if showNotEnoughInfoError is null or false
                if (showNotEnoughInfoError.value != true && mutableList.isEmpty()) {
                    showNoChangesDetectedError.call()
                }
                topSection.update(mutableListOf())
            }

            return
        }

        currentJob = launch(Dispatchers.Main) {

            val (original, new) = getFromDb(originalId!!, revisedId!!)
            val (onlyDiff, nonDiff) = generateDiffRows(original, new)

            val mutableList = mutableListOf<TextRecycler>()
            withContext(Dispatchers.IO) {
                if (changePlusOriginal) mutableList.addAll(nonDiff)
                mutableList.addAll(onlyDiff)
                mutableList.sortBy { item -> item.index }
            }

            // this way it captures if showNotEnoughInfoError is null or false
            if (showNotEnoughInfoError.value != true && mutableList.isEmpty()) {
                showNoChangesDetectedError.call()
            }

            // There is a bug on Groupie 2.1.0 which is rendering the diff operation on UI thread.
            // remove + insert is cheaper than checking if there was a change on thousands of lines.
            topSection.update(mutableListOf())
            topSection.update(mutableList)
        }
    }


    /**
     * Called when we want to generate a diff between two items. Takes as input two ids and the top
     * section and outputs the result to this top section.
     *
     * @param topSection    The section corresponding to the top recyclerview, which will
     * be updated with the result from the diff.
     * @param originalId    The snapId from the original item (which will be in red)
     * @param revisedId     The snapId from the revised item (which will be in green)
     * which will be updated by [generateDiff] with corresponding diff
     */
    suspend fun generateDiffVisual(originalId: String?, revisedId: String?): String {
        if (originalId.isNullOrBlank() || revisedId.isNullOrBlank()) return ""
        val (original, new) = getFromDb(originalId!!, revisedId!!)
        return generateVisualDiffRows(original, new)
    }

    /**
     * This will asynchronously fetch a pair of snaps from the database based on their ids
     *
     * @param originalId The original diff url to be fetched.
     * @param newId The newest url to be fetched.
     * @return a pair of diffs
     */
    private suspend fun getFromDb(
        originalId: String,
        newId: String
    ): Pair<Pair<Snap, ByteArray>, Pair<Snap, ByteArray>> =
        mSnapsRepository.getSnapPair(originalId, newId)

    /**
     * This will asynchronously fetch a pair of diffs from the database based on their ids
     *
     * @param originalId The original diff url to be fetched.
     * @param newId The newest url to be fetched.
     * @return a pair of diffs
     */
    suspend fun getSnapValue(snapId: String): String =
        mSnapsRepository.getSnapContent(snapId).toString(Charset.defaultCharset())

    /**
     * Called when there is a selection. This is a simple Finite State Machine, where
     * it decides the color to select based on previous selection.
     *
     * @param item    Pass the item to be selected
     * @param topSection Pass the top part of the screen,
     * which will be updated by [generateDiff] with corresponding diff
     */
    inline fun fsmSelectWithCorrectColor(item: TextViewHolder, f: (String?, String?) -> Unit) {
        when (item.colorSelected) {
            ItemSelected.NONE -> {
                when (item.adapter.colorSelected.count { it.value != ItemSelected.NONE }) {
                    0 -> {
                        // NOTHING IS SELECTED -> SELECT REVISED
                        item.setColor(ItemSelected.REVISED)
                    }
                    1 -> {
                        // ONE THING IS SELECTED AND IT IS REVISED -> ORIGINAL
                        // ONE THING IS SELECTED AND IT IS ORIGINAL -> REVISED
                        for ((_, value) in item.adapter.colorSelected) {
                            if (value == ItemSelected.NONE) continue

                            if (value == ItemSelected.ORIGINAL) {
                                item.adapter.colorSelected.getPositionForAdapter(ItemSelected.ORIGINAL)
                                    ?.let { position ->
                                        item.setColor(ItemSelected.REVISED)

                                        f(
                                            item.adapter.getItemFromAdapter(position)?.snapId,
                                            item.snap?.snapId
                                        )
                                    }

                            } else {
                                item.adapter.colorSelected.getPositionForAdapter(ItemSelected.REVISED)
                                    ?.let { position ->
                                        item.setColor(ItemSelected.ORIGINAL)

                                        f(
                                            item.snap?.snapId,
                                            item.adapter.getItemFromAdapter(position)?.snapId
                                        )
                                    }
                            }

                            break
                        }
                    }
                    else -> {
                        // TWO ARE SELECTED. UNSELECT THE ORIGINAL, SELECT ANOTHER THING.
                        for ((position, _) in item.adapter.colorSelected.filter { it.value == ItemSelected.ORIGINAL }) {
                            item.adapter.setColor(ItemSelected.NONE, position)
                        }

                        item.setColor(ItemSelected.ORIGINAL)

                        item.adapter.colorSelected.getPositionForAdapter(ItemSelected.REVISED)
                            ?.let { position ->
                                f(
                                    item.snap?.snapId,
                                    item.adapter.getItemFromAdapter(position)?.snapId
                                )
                            }
                    }
                }
            }
            else -> {
                item.setColor(ItemSelected.NONE)
            }
        }
    }

    /**
     * When there is only one item selected, we want to show an error message
     * and clear the RecyclerView
     *
     * @param adapter    The adapter with a map of selected gradientColor
     * @param topSection The top section, which will be cleared if there are
     * not enough gradientColor selected
     */
    inline fun updateCanShowDiff(adapter: TextAdapter, callback: () -> Unit) {

        adapter.colorSelected.count { it.value != ItemSelected.NONE }.let { numOfItemsNotNone ->
            if (numOfItemsNotNone < 2) {
                // Empty when there is not enough selection
                callback()
            }

            showNotEnoughInfoError.value = numOfItemsNotNone != 2
        }
    }

    private suspend fun generateVisualDiffRows(
        original: Pair<Snap, ByteArray>,
        revised: Pair<Snap, ByteArray>
    ): String = withContext(Dispatchers.IO) {

        val oldTag =
            { f: Boolean -> if (f) "<span class=\"editOldInline\" style=\"background-color:#acf2bd\">" else "</span>" }
        val newTag =
            { f: Boolean -> if (f) "<span class=\"editNewInline\" style=\"background-color:#ffdce0\">" else "</span>" }

        val generator = DiffRowGenerator.create()
            .showInlineDiffs(true)
            .mergeOriginalRevised(true)
            .inlineDiffByWord(true)
            .oldTag(oldTag)
            .newTag(newTag)
            .build()

        fun findCorrectCharset(content: Snap): Charset =
            content.contentCharset.takeUnless { it.isBlank() }
                ?.let { Charset.forName(it) }
                    ?: Charset.defaultCharset()

        // find the correct charset
        val newCharset = findCorrectCharset(revised.first)
        val originalCharset = findCorrectCharset(original.first)

        // compute the differences for two test texts.
        // generateDiffRows will split the lines anyway, so there is no need for splitting again here.

        val rows = generator.generateDiffRows(
            original = mutableListOf(
                original.second.toString(
                    OkHttpCharset.bomAwareCharset(
                        source = original.second,
                        charset = originalCharset
                    )
                )
            ),
            revised = mutableListOf(
                revised.second.toString(
                    OkHttpCharset.bomAwareCharset(
                        source = revised.second,
                        charset = newCharset
                    )
                )
            )
        )

        val mapping = StringBuilder()
        rows.forEach { mapping.append(it.oldLine.unescapeHtml() + "\n") }
        println("mapping: $mapping")
        mapping.toString()
    }

    private suspend fun generateDiffRows(
        original: Pair<Snap, ByteArray>,
        revised: Pair<Snap, ByteArray>
    ): Pair<MutableList<TextRecycler>, MutableList<TextRecycler>> = withContext(Dispatchers.IO) {

        val generator = DiffRowGenerator.create()
            .showInlineDiffs(true)
            .inlineDiffByWord(true)
            .oldTag { _ -> "TEXTREMOVED" }
            .newTag { _ -> "TEXTADDED" }
            .build()

        fun findCorrectCharset(content: Snap): Charset =
            content.contentCharset.takeUnless { it.isBlank() }
                ?.let { Charset.forName(it) }
                    ?: Charset.defaultCharset()

        // find the correct charset
        val newCharset = findCorrectCharset(revised.first)
        val originalCharset = findCorrectCharset(original.first)

        // compute the differences for two test texts.
        // generateDiffRows will split the lines anyway, so there is no need for splitting again here.

        val rows = generator.generateDiffRows(
            original = mutableListOf(
                original.second.toString(
                    OkHttpCharset.bomAwareCharset(
                        source = original.second,
                        charset = originalCharset
                    )
                ).removeClutterAndBeautifyHtmlIfNecessary(original.first.contentType)
            ),
            revised = mutableListOf(
                revised.second.toString(
                    OkHttpCharset.bomAwareCharset(
                        source = revised.second,
                        charset = newCharset
                    )
                ).removeClutterAndBeautifyHtmlIfNecessary(revised.first.contentType)
            )
        )

        val updatingNonDiff = mutableListOf<TextRecycler>()
        val updatingOnlyDiff = mutableListOf<TextRecycler>()

        // for some reason, unescapeHtml isn't working when put after removeClutterAndBeautifyHtml
        // but it is working fine when put here.
        rows.forEachIndexed { index, row ->
            if (row.oldLine == row.newLine) {
                updatingNonDiff.add(TextRecycler(row.oldLine.unescapeHtml(), index))
                println(row.oldLine.unescapeHtml())
            } else when {
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

        Pair(updatingOnlyDiff, updatingNonDiff)
    }

    suspend fun getAllSnapsPagedForId(id: String, filter: String): LiveData<PagedList<Snap>> {
        return LivePagedListBuilder(
            mSnapsRepository.getSnapForPaging(id, filter), PagedList.Config.Builder()
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
