package com.bernaferrari.ui.base

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.EpoxyController
import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.EpoxyRecyclerView
import com.airbnb.mvrx.BaseMvRxFragment

/**
 * Really basic fragment, inspired from TiVi, with a lazy RecyclerView and most MvRx methods,
 * to reduce overall boilerplate.
 */
abstract class TiviMvRxFragment : BaseMvRxFragment() {

    lateinit var recyclerView: EpoxyRecyclerView

    private val epoxyController by lazy { epoxyController() }

    abstract fun epoxyController(): EpoxyController

    /** Define the layoutManager to be used, by default Linear */
    open fun layoutManager(): RecyclerView.LayoutManager = LinearLayoutManager(context)

    private lateinit var mvrxPersistedViewId: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView.layoutManager = layoutManager().apply {
            (this as? LinearLayoutManager)?.recycleChildrenOnDetach = true
        }
        recyclerView.setController(epoxyController)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        epoxyController.onRestoreInstanceState(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        epoxyController.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {

        epoxyController.cancelPendingModelBuild()
        // this kills animations, but was the only way I found to avoid crashes when
        // switching and coming back to the same fragment (via back button) fast.
//        recyclerView.adapter = null

//        recyclerView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
//            override fun onViewAttachedToWindow(v: View) = Unit // no-op
//
//            override fun onViewDetachedFromWindow(v: View) {
//                recyclerView.adapter = null
//            }
//        })

        super.onDestroyView()
    }

    override fun invalidate() {
        recyclerView.requestModelBuild()
    }

    fun getModelAtPos(pos: Int): EpoxyModel<*>? {
        return try {
            epoxyController.adapter.getModelAtPosition(pos)
        } catch (e: IllegalStateException) {
            null
        }
    }
}

const val PERSISTED_VIEW_ID_KEY = "mvrx:persisted_view_id"
