package com.bernaferrari.changedetection.detailsText

import androidx.lifecycle.MutableLiveData
import com.bernaferrari.base.mvrx.MvRxViewModel
import com.bernaferrari.changedetection.repo.Snap
import com.jakewharton.rxrelay2.BehaviorRelay

/**
 * initialState *must* be implemented as a constructor parameter.
 */
class TextGlobalViewModel(initialState: SelectorState) :
    MvRxViewModel<SelectorState>(initialState) {

    val selectedItemsList: BehaviorRelay<List<Snap>> = BehaviorRelay.createDefault(emptyList())

    val selectedId = MutableLiveData<String?>()

    fun fetchData(id: String) {

    }


}
