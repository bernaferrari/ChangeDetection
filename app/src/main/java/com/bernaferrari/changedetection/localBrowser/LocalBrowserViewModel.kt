package com.bernaferrari.changedetection.localBrowser

import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.bernaferrari.base.mvrx.MvRxViewModel
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import io.reactivex.Observable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import java.nio.charset.Charset

data class BrowserState(
    val content: String = "",
    val isLoading: Boolean = true
) : MvRxState

/**
 * initialState *must* be implemented as a constructor parameter.
 */
class LocalBrowserViewModel @AssistedInject constructor(
    @Assisted initialState: BrowserState,
    private val mSnapsRepository: SnapsRepository
) : MvRxViewModel<BrowserState>(initialState), CoroutineScope by MainScope() {

    fun fetchData(snapId: String) {

        Observable.just(mSnapsRepository.getSnapContent(snapId).toString(Charset.defaultCharset()))
            .execute {
                copy(
                    content = it() ?: "",
                    isLoading = it() == null
                )
            }
    }

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: BrowserState): LocalBrowserViewModel
    }

    companion object : MvRxViewModelFactory<LocalBrowserViewModel, BrowserState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: BrowserState
        ): LocalBrowserViewModel? {
            val fragment: LocalBrowserFragment =
                (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.textTopViewModelFactoryLocal.create(state)
        }
    }
}
