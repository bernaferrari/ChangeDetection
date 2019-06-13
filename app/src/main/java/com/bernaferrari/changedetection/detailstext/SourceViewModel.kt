package com.bernaferrari.changedetection.detailsText

import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.bernaferrari.base.mvrx.MvRxViewModel
import com.bernaferrari.changedetection.extensions.removeClutterAndBeautifyHtmlIfNecessary
import com.bernaferrari.changedetection.extensions.unescapeHtml
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import com.bernaferrari.diffutils.diffs.text.DiffRowGenerator
import com.jakewharton.rxrelay2.BehaviorRelay
import com.pacoworks.komprehensions.rx2.doSwitchMap
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import java.nio.charset.Charset
import kotlin.coroutines.CoroutineContext


data class SourceState(
    val listOfItems: List<DiffVisualItem> = emptyList(),
    val isLoading: Boolean = true
) : MvRxState


class DiffVisualItem(val content: String, val index: Int)

/**
 * initialState *must* be implemented as a constructor parameter.
 */
class SourceViewModel @AssistedInject constructor(
    @Assisted initialState: SourceState,
    private val mSnapsRepository: SnapsRepository
) : MvRxViewModel<SourceState>(initialState), CoroutineScope {

    private var job: Job = Job()

    var changePlusOriginal: Boolean = false

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

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
    fun generateDiff(original: Snap, revised: Snap): List<DiffVisualItem> {

        val originalB = mSnapsRepository.getSingleSnapPair(original.snapId)
        val revisedB = mSnapsRepository.getSingleSnapPair(revised.snapId)

        val originalD = Pair(original, originalB)
        val revisedD = Pair(revised, revisedB)

        val (onlyDiff, nonDiff) = generateDiffRows(originalD, revisedD)

        return mutableListOf<DiffVisualItem>().apply {
            if (changePlusOriginal) this.addAll(nonDiff)
            this.addAll(onlyDiff)
            this.sortBy { item -> item.index }
        }
    }

    private fun generateDiffRows(
        original: Pair<Snap, ByteArray>,
        revised: Pair<Snap, ByteArray>
    ): Pair<MutableList<DiffVisualItem>, MutableList<DiffVisualItem>> {

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

        val updatingNonDiff = mutableListOf<DiffVisualItem>()
        val updatingOnlyDiff = mutableListOf<DiffVisualItem>()

        // for some reason, unescapeHtml isn't working when put after removeClutterAndBeautifyHtml
        // but it is working fine when put here.
        rows.forEachIndexed { index, row ->
            if (row.oldLine == row.newLine) {
                updatingNonDiff.add(DiffVisualItem(row.oldLine.unescapeHtml(), index))
                println(row.oldLine.unescapeHtml())
            } else when {
                row.newLine.isBlank() -> {
                    updatingOnlyDiff.add(DiffVisualItem("-" + row.oldLine.unescapeHtml(), index))
                }
                row.oldLine.isBlank() -> {
                    updatingOnlyDiff.add(DiffVisualItem("+" + row.newLine.unescapeHtml(), index))
                }
                else -> {
                    updatingOnlyDiff.add(DiffVisualItem("-" + row.oldLine.unescapeHtml(), index))
                    updatingOnlyDiff.add(DiffVisualItem("+" + row.newLine.unescapeHtml(), index))
                }
            }
        }

        return Pair(updatingOnlyDiff, updatingNonDiff)
    }

    fun fetchData(selectedList: BehaviorRelay<List<Snap>>, visibility: BehaviorRelay<Boolean>) {

//        setErrorHandler { e -> }

        doSwitchMap(
            { visibility },
            { selectedList }
        ) { isVisible, selected ->

            if (isVisible && selected.size >= 2) {
                Observable.fromCallable {
                    println("rawr Source thread is: ${Thread.currentThread().name}")
                    generateDiff(selected[0], selected[1])
                }
                    .subscribeOn(Schedulers.computation())
            } else {
                Observable.just(emptyList())
            }
        }.execute {
            copy(
                isLoading = it() == null,
                listOfItems = it() ?: emptyList()
            )
        }
    }

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: SourceState): SourceViewModel
    }

    companion object : MvRxViewModelFactory<SourceViewModel, SourceState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: SourceState
        ): SourceViewModel? {
            val fragment: SourceViewFragment =
                (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.textTopViewModelFactory.create(state)
        }
    }
}
