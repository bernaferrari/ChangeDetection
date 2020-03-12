package com.bernaferrari.changedetection.settings

import com.airbnb.mvrx.*
import com.bernaferrari.base.mvrx.MvRxViewModel
import com.bernaferrari.changedetection.Injector
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables

data class SettingsData(
    val colorBySdk: Boolean,
    val vibration: Boolean,
    val backgroundSync: Boolean,
    val orderBySdk: Boolean
) : MvRxState

data class SettingsState(
    val data: Async<SettingsData> = Loading()
) : MvRxState

class SettingsViewModel(
    initialState: SettingsState,
    private val sources: Observable<SettingsData>
) : MvRxViewModel<SettingsState>(initialState) {

    init {
        fetchData()
    }

    private fun fetchData() = withState {
        sources.execute { copy(data = it) }
    }

    companion object : MvRxViewModelFactory<SettingsViewModel, SettingsState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: SettingsState
        ): SettingsViewModel {

            val source = Observables.combineLatest(
                    Injector.get().isColorBySdk().observe(),
                    Injector.get().showSystemApps().observe(),
                    Injector.get().backgroundSync().observe(),
                    Injector.get().orderBySdk().observe()
            ) { color, system, backgroundSync, orderBySdk ->
                SettingsData(color, system, backgroundSync, orderBySdk)
            }

            return SettingsViewModel(state, source)
        }
    }
}
