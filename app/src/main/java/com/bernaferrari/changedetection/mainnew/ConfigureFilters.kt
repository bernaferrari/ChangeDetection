package com.bernaferrari.changedetection.mainnew

import androidx.fragment.app.Fragment
import com.airbnb.epoxy.EpoxyRecyclerView
import com.bernaferrari.base.mvrx.simpleController
import com.bernaferrari.changedetection.epoxy.ColorPickerItemEpoxy_
import com.bernaferrari.changedetection.epoxy.TagPickerItemEpoxy_
import com.bernaferrari.changedetection.repo.ColorGroup

internal fun Fragment.configureColorAdapter(
    colorSelector: EpoxyRecyclerView,
    colorsList: List<ColorGroup>,
    mViewModel: MainViewModelNEW
) {

    val listOfSelectedItems = (mViewModel.selectedColors.value ?: emptyList()).toMutableList()

    val controller = simpleController {

        // Create each color picker item, checking for the first (because it needs extra margin)
        // and checking for the one which is selected (so it becomes selected)
        colorsList.forEachIndexed { index, color ->

            ColorPickerItemEpoxy_()
                .id("picker $index")
                .allowDeselection(true)
                .switchIsOn(color in listOfSelectedItems)
                .gradientColor(color)
                .onClick { _ ->
                    updateSelection(listOfSelectedItems, color)
                    mViewModel.selectedColors.accept(listOfSelectedItems)
                    this.requestModelBuild()
                }
                .addTo(this)
        }
    }

    colorSelector.setControllerAndBuildModels(controller)
}

internal fun Fragment.configureTagAdapter(
    tagSelector: EpoxyRecyclerView,
    tagsList: List<String>,
    mViewModel: MainViewModelNEW
) {
    val listOfSelectedTags = (mViewModel.selectedTags.value ?: emptyList()).toMutableList()

    val controller = simpleController {

        // Create each color picker item, checking for the first (because it needs extra margin)
        // and checking for the one which is selected (so it becomes selected)
        tagsList.forEachIndexed { index, tag ->

            TagPickerItemEpoxy_()
                .id("tag $index")
                .checked(tag in listOfSelectedTags)
                .name(tag)
                .onClick { _ ->
                    updateSelection(listOfSelectedTags, tag)
                    mViewModel.selectedTags.accept(listOfSelectedTags)
                    this.requestModelBuild()
                }
                .addTo(this)
        }
    }

    tagSelector.setControllerAndBuildModels(controller)
}

private fun <T> updateSelection(listOfItems: MutableList<T>, item: T) {
    val elementIndex = listOfItems.indexOf(item)
    if (elementIndex != -1) {
        listOfItems.removeAt(elementIndex)
    } else {
        listOfItems += item
    }
}
