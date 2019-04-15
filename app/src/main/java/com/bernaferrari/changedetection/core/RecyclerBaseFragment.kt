package com.bernaferrari.changedetection.core

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.epoxy.EpoxyController
import com.airbnb.mvrx.BaseMvRxFragment
import com.bernaferrari.changedetection.R
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.recyclerview.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext

abstract class RecyclerBaseFragment : BaseMvRxFragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main + Job()

    val disposableManager = CompositeDisposable()

    val epoxyController by lazy { epoxyController() }

    abstract fun epoxyController(): EpoxyController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        epoxyController.onRestoreInstanceState(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.recyclerview, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recycler.setController(epoxyController)
    }

    override fun invalidate() {
        recycler.requestModelBuild()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        epoxyController.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        epoxyController.cancelPendingModelBuild()
        disposableManager.clear()
        super.onDestroyView()
    }

    override fun onDestroy() {
        coroutineContext.cancel()
        super.onDestroy()
    }
}
