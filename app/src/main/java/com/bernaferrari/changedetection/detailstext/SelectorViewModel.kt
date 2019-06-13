package com.bernaferrari.changedetection.detailsText

import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.bernaferrari.base.mvrx.MvRxViewModel
import com.bernaferrari.changedetection.repo.Snap
import com.jakewharton.rxrelay2.BehaviorRelay
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import io.reactivex.schedulers.Schedulers


data class SelectorState(
    val listOfItems: List<Snap> = emptyList(),
    val isLoading: Boolean = true
) : MvRxState

/**
 * initialState *must* be implemented as a constructor parameter.
 */
class SelectorViewModel @AssistedInject constructor(
    @Assisted initialState: SelectorState,
    private val mTextBottomDataSource: TextBottomDataSource
) : MvRxViewModel<SelectorState>(initialState) {

    fun fetchData(id: String, selectedList: BehaviorRelay<List<Snap>>, selectedId: String = "") {

        var hasSetInitialColor = true

        mTextBottomDataSource.getItems(id)
            .doOnNext { fullList ->
                // sanity check
                // un-select if any value is missing
                val filteredList = selectedList.value?.filter { it in fullList }
                if (selectedList.value?.size != filteredList?.size) {
                    selectedList.accept(filteredList)
                }
            }
            .doOnNext { fullList ->

                println("rawr Selector Thread is: ${Thread.currentThread().name}")

                if (hasSetInitialColor) {
                    if (selectedId == "") {
                        // reversed so when user selects another one, the first position keeps.
                        selectedList.accept(fullList.take(2).reversed())
                    } else {
                        val index = fullList.indexOfFirst { it.snapId == selectedId }
                        val newList = fullList.drop(index).take(1).toMutableList()
                        newList += fullList[index]
                        selectedList.accept(newList)
                    }
                    hasSetInitialColor = false
                }
            }
            .subscribeOn(Schedulers.computation())
            .observeOn(Schedulers.computation())
            .execute {
                copy(
                    listOfItems = it() ?: emptyList(),
                    isLoading = false
                )
            }
    }

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: SelectorState): SelectorViewModel
    }

    companion object : MvRxViewModelFactory<SelectorViewModel, SelectorState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: SelectorState
        ): SelectorViewModel? {
            val fragment: SelectorFragment =
                (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.logsViewModelFactory.create(state)
        }
    }
}
