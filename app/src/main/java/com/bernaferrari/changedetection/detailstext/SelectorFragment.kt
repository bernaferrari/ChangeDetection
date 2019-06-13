package com.bernaferrari.changedetection.detailsText

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.EpoxyController
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.fragmentViewModel
import com.bernaferrari.base.mvrx.simpleController
import com.bernaferrari.changedetection.extensions.convertTimestampToDate
import com.bernaferrari.changedetection.extensions.readableFileSize
import com.bernaferrari.changedetection.loadingRow
import com.bernaferrari.ui.dagger.DaggerBaseRecyclerFragment
import javax.inject.Inject


class SelectorFragment : DaggerBaseRecyclerFragment() {

    private val viewModel: SelectorViewModel by fragmentViewModel()
    @Inject
    lateinit var logsViewModelFactory: SelectorViewModel.Factory

    private val globalModel: TextGlobalViewModel by activityViewModel()

    override fun layoutManager(): RecyclerView.LayoutManager =
        LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

    override fun epoxyController(): EpoxyController = simpleController(viewModel) { state ->

        if (state.isLoading) {
            loadingRow { id("loading") }
        }

        val selectedList = globalModel.selectedItemsList.value ?: emptyList()

        state.listOfItems.forEach { item ->

            val stringFromTimeAgo = item.timestamp.convertTimestampToDate()
            val readableFileSize = item.contentSize.readableFileSize()

            val color = when (selectedList.size) {
                0 -> ItemSelected.NONE
                1 -> if (item in selectedList) ItemSelected.ORIGINAL else ItemSelected.NONE
                else -> when (item) {
                    in selectedList -> when (item) {
                        selectedList.maxBy { it.timestamp } -> ItemSelected.REVISED
                        else -> ItemSelected.ORIGINAL
                    }
                    else -> ItemSelected.NONE
                }
            }

            SelectorItem_()
                .id(item.snapId)
                .title(stringFromTimeAgo)
                .subtitle(readableFileSize)
                .colorSelected(color)
                .onClick { _ ->
                    // this need to be set again, because value may have changed.
                    val selectedValue = globalModel.selectedItemsList

                    if (color == ItemSelected.NONE) {
                        when (selectedValue.value?.size ?: 0) {
                            0 -> globalModel.selectedItemsList.accept(listOf(item))
                            1 -> {
                                val newList = mutableListOf(item)
                                selectedValue.value?.also { newList += it }
                                selectedValue.accept(newList)
                            }
                            2 -> {
                                val newList = mutableListOf(item)
                                selectedValue.value?.also { newList += it[1] }
                                selectedValue.accept(newList)
                            }
                            else -> {

                            }
                        }
                    } else {
                        selectedValue.value?.toMutableList()?.apply {
                            remove(item)
                            selectedValue.accept(this)
                        }
                    }

                    this.requestModelBuild()
                }
                .addTo(this)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        globalModel.selectedId.observe(this, Observer {
            if (it != null) {
                viewModel.fetchData(it, globalModel.selectedItemsList, "")
            }
        })

//        val site = arguments?.getParcelable<Site>("site")
//        viewModel.fetchData(site?.id ?: "51b73de0-7dc0-4f26-98f4-000889665817", globalModel.selectedItemsList)
    }

    override fun onDestroyView() {
        globalModel.selectedId.value = null
        super.onDestroyView()
    }
}
