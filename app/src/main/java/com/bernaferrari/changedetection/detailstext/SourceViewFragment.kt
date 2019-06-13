package com.bernaferrari.changedetection.detailsText

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import com.airbnb.epoxy.EpoxyController
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.fragmentViewModel
import com.bernaferrari.base.mvrx.simpleController
import com.bernaferrari.changedetection.R
import com.bernaferrari.ui.dagger.DaggerBaseRecyclerFragment
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxrelay2.BehaviorRelay
import kotlinx.android.synthetic.main.diff_text_fragment.*
import javax.inject.Inject

class SourceViewFragment : DaggerBaseRecyclerFragment() {

    private val viewModel: SourceViewModel by fragmentViewModel()
    @Inject
    lateinit var textTopViewModelFactory: SourceViewModel.Factory

    private val globalModel: TextGlobalViewModel by activityViewModel()

    override fun epoxyController(): EpoxyController = simpleController(viewModel) { state ->

        println("rawr listofitems size is: ${state.listOfItems.size}")
        state.listOfItems.forEach { item ->
            SourceItem_()
                .id("${item.index} ${item.content}")
                .index(item.index)
                .itemTitle(item.content)
                .onClick { v ->
                    copyToClipboard(v.context, item.content)
                }
                .addTo(this)
        }

    }

    lateinit var model: TextViewModel
    val color: Int by lazy { ContextCompat.getColor(requireContext(), R.color.FontStrong) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val visibility: BehaviorRelay<Boolean> = BehaviorRelay.create()
        visibility.accept(true)

        viewModel.fetchData(
            globalModel.selectedItemsList,
            visibility
        )

//        revisedToggle.setOnClickListener {
//            uiState.revised = true
//            uiState.diff = false
//            uiState.original = false
//            loadIntoWebView(true)
//        }
//
//        originalToggle.setOnClickListener {
//            uiState.original = true
//            uiState.revised = false
//            uiState.diff = false
//            loadIntoWebView(false)
//        }
//
//        diff.setOnClickListener {
//            uiState.diff = true
//            uiState.original = false
//            uiState.revised = false
//            fetchAndShow()
//        }
    }

    private fun copyToClipboard(context: Context, uri: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(context.getString(R.string.app_name), uri)

        clipboard.primaryClip = clip
        Snackbar.make(elastic, getString(R.string.success_copied), Snackbar.LENGTH_SHORT).show()
    }
}
