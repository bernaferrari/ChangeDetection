package com.bernaferrari.changedetection.addedit

import androidx.lifecycle.ViewModel
import com.bernaferrari.changedetection.util.SingleLiveEvent

class SharedViewModel : ViewModel() {

    val selected = SingleLiveEvent<String?>()

    fun select(item: String?) {
        selected.value = item
    }
}