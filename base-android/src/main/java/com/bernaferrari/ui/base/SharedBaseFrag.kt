package com.bernaferrari.ui.base

import android.os.Bundle
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext

/**
 * Fragment base that contains CoroutineScope, disposableManager and optionsMenu,
 * so that you can reduce boilerplate.
 */
abstract class SharedBaseFrag : TiviMvRxFragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main + Job()

    val disposableManager = CompositeDisposable()

    /** Define the close icon, usually back or close */
    open val closeIconRes: Int? = 0

    /** Should toolbar menu be shown? */
    open val showMenu = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(showMenu)
    }

    /** Function callback when back button from toolbar is pressed */
    open fun dismiss() {
        activity?.onBackPressed()
    }

    override fun onDestroy() {
        coroutineContext.cancel()
        disposableManager.clear()
        super.onDestroy()
    }
}
