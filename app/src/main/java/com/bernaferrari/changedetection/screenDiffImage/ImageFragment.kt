package com.bernaferrari.changedetection.screenDiffImage

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.graphics.Point
import android.os.Bundle
import android.support.transition.AutoTransition
import android.support.transition.TransitionManager
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.Navigation
import com.afollestad.materialdialogs.MaterialDialog
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.ViewModelFactory
import com.bernaferrari.changedetection.extensions.*
import com.bernaferrari.changedetection.groupie.RowItem
import com.bernaferrari.changedetection.screenDiffText.TextFragment
import com.bernaferrari.changedetection.util.GlideApp
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Section
import com.yarolegovich.discretescrollview.DiscreteScrollView
import com.yarolegovich.discretescrollview.transform.ScaleTransformer
import kotlinx.android.synthetic.main.control_bar.*
import kotlinx.android.synthetic.main.control_bar_update_page.*
import kotlinx.android.synthetic.main.diff_image_fragment.*
import kotlinx.android.synthetic.main.diff_image_fragment.view.*
import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty

//
// This is adapted from Bíblia em Libras app, which has a GIF dictionary in a carousel powered by ExoMedia.
// If you are interested, download it and scroll the initial list of items for a large button named "Dicionário":
// https://play.google.com/store/apps/details?id=com.biblialibras.android
//
class ImageFragment : Fragment(),
    DiscreteScrollView.OnItemChangedListener<RecyclerView.ViewHolder> {

    lateinit var model: ImageViewModel
    lateinit var adapter: ImageAdapter

    private var previousAdapterPosition = 0
    private val items = mutableListOf<RowItem>()
    private val section = Section()

    private val transition = AutoTransition().apply { duration = 175 }
    private val uiState = UiState { updateUiFromState() }

    private val groupAdapter = GroupAdapter<com.xwray.groupie.ViewHolder>()

    override fun onCurrentItemChanged(viewHolder: RecyclerView.ViewHolder?, adapterPosition: Int) {

        if (items.isEmpty()) {
            Navigation.findNavController(view!!).navigateUp()
            return
        }

        // deslect the previous item on the drawer. This might trigger an exception if item was added/removed
        try {
            items[previousAdapterPosition].isSelected = false
            groupAdapter.notifyItemChanged(previousAdapterPosition)
        } catch (e: IndexOutOfBoundsException) {

        }

        drawerRecycler.scrollToIndexWithMargins(
            previousAdapterPosition,
            adapterPosition,
            items.size
        )

        previousAdapterPosition = adapterPosition
        println("adapter position: $adapterPosition")

        // select the new item on the drawer
        items[adapterPosition].isSelected = true
        groupAdapter.notifyItemChanged(adapterPosition)
        section.notifyChanged()

        val item = adapter.getItemFromAdapter(adapterPosition) ?: return
        titlecontent.text = item.timestamp.convertTimestampToDate()

        // set the photo_view with current file
        GlideApp.with(requireContext())
            .load(item.content)
            .into(photo_view)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.diff_image_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        model = obtainViewModel(requireActivity())

        closecontent.setOnClickListener {
            view.let { Navigation.findNavController(it).navigateUp() }
        }

        next_previous_bar.isVisible = false
        // highQualityToggle.isVisible = false this will be uncommented when share is implemented
        controlBar.isVisible = false // this will be commented when share is implemented

        view.menucontent.setOnClickListener { view.drawer.openDrawer(GravityCompat.END) }

        // this is needed. If visibility is off and the fragment is reopened,
        // drawable will keep the drawable from                                                                                                                                                                                                                                                      last state (off) even thought it should be on.
        visibility.setImageDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.visibility_on
            )
        )

        visibility.setOnClickListener {
            uiState.visibility++

            uiState.carousel = uiState.visibility
            uiState.controlBar = false

            updateUiFromState()

            // set and run the correct animation
            if (uiState.visibility) {
                visibility.setAndStartAnimation(R.drawable.visibility_off_to_on, requireContext())
            } else {
                visibility.setAndStartAnimation(R.drawable.visibility_on_to_off, requireContext())
            }
        }

        val recyclerListener = object :
            TextFragment.Companion.RecyclerViewItemListener {
            override fun onClickListener(item: RecyclerView.ViewHolder) {
                carouselRecycler.smoothScrollToPosition((item as ImageViewHolder).itemPosition)
            }

            override fun onLongClickListener(item: RecyclerView.ViewHolder) {
                removeItemDialog((item as ImageViewHolder).currentSnap?.snapId ?: "")
            }
        }

        menucontent.setOnClickListener {
            view.drawer.openDrawer(GravityCompat.END)
        }

        val windowDimensions = Point()
        requireActivity().windowManager.defaultDisplay.getSize(windowDimensions)
        val itemWidth = Math.round(Math.min(windowDimensions.y, windowDimensions.x) * 0.5f)
        val itemHeight = (120 * 0.7).toInt().toDp(view.resources)

        adapter = ImageAdapter(
            recyclerListener,
            itemHeight,
            itemWidth,
            GlideApp.with(this)
        )

        val siteId = arguments?.getString("SITEID") ?: ""
        model.getAllSnapsPagedForId(siteId).observe(this, Observer(adapter::submitList))
        model.getAllMinimalSnapsForId(siteId).observe(this, Observer {
            if (it != null) {
                items.clear()
                it.forEach { items.add(RowItem(it)) }
                section.update(items)
                // Since isSelected is being set at onCurrentItemChanged, there is a small bug where
                // when the list is refreshed and position doesn't change (which happens when it is in the first),
                // onCurrentItemChanged is not called again and thus the current item is not selected.
            }
        })

        carouselRecycler.also {
            it.adapter = adapter

            it.setSlideOnFling(true)
            it.setSlideOnFlingThreshold(4000)
            it.setItemTransitionTimeMillis(150)
            it.setItemTransformer(
                ScaleTransformer.Builder()
                    .setMinScale(0.8f)
                    .build()
            )

            it.addOnItemChangedListener(this@ImageFragment)
        }

        drawerRecycler.also {
            it.layoutManager = LinearLayoutManager(this.context)
            it.adapter = groupAdapter
            it.addItemDecoration(
                DividerItemDecoration(
                    this.context,
                    DividerItemDecoration.VERTICAL
                )
            )
        }

        groupAdapter.add(section)
        groupAdapter.setOnItemClickListener { item, _ ->
            if (item is RowItem) {
                val nextPosition =
                    items.indexOfFirst { it.minimalSnap.snapId == item.minimalSnap.snapId }
                drawer.closeDrawer(GravityCompat.END)
                carouselRecycler.smoothScrollToPosition(nextPosition) // position becomes selected with animated scroll
            }
        }
        groupAdapter.setOnItemLongClickListener { item, _ ->
            if (item is RowItem) {
                removeItemDialog(item.minimalSnap.snapId)
            }
            true
        }
    }

    private fun removeItemDialog(snapId: String) {
        MaterialDialog.Builder(requireContext())
            .title(R.string.remove)
            .content(R.string.remove_content)
            .positiveText(R.string.yes)
            .negativeText(R.string.no)
            .onPositive { _, _ ->
                model.removeSnap(snapId)
            }
            .show()
    }

    private fun obtainViewModel(activity: FragmentActivity): ImageViewModel {
        // Use a Factory to inject dependencies into the ViewModel
        val factory = ViewModelFactory.getInstance(activity.application)
        return ViewModelProviders.of(activity, factory).get(ImageViewModel::class.java)
    }

    private fun updateUiFromState() {
        beginDelayedTransition()

        carouselRecycler.isVisible = uiState.carousel
        controlBar.isVisible = uiState.controlBar
    }

    private fun beginDelayedTransition() =
        TransitionManager.beginDelayedTransition(container, transition)

    companion object {
        private class UiState(private val callback: () -> Unit) {

            private inner class BooleanProperty(initialValue: Boolean) :
                ObservableProperty<Boolean>(initialValue) {
                override fun afterChange(
                    property: KProperty<*>,
                    oldValue: Boolean,
                    newValue: Boolean
                ) {
                    callback()
                }
            }

            var visibility by BooleanProperty(true)
            var carousel by BooleanProperty(true)
            var controlBar by BooleanProperty(true)
            var highQuality by BooleanProperty(false)
        }
    }
}
