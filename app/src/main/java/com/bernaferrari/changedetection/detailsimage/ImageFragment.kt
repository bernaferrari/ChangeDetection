package com.bernaferrari.changedetection.detailsimage

import android.arch.lifecycle.Observer
import android.content.Intent
import android.graphics.Point
import android.os.Bundle
import android.support.transition.AutoTransition
import android.support.transition.TransitionManager
import android.support.v4.app.ShareCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.support.v4.view.GravityCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.transition.ChangeBounds
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import com.afollestad.materialdialogs.MaterialDialog
import com.bernaferrari.changedetection.MainActivity
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.ScopedFragment
import com.bernaferrari.changedetection.ViewModelFactory
import com.bernaferrari.changedetection.data.Snap
import com.bernaferrari.changedetection.detailstext.TextFragment
import com.bernaferrari.changedetection.extensions.*
import com.bernaferrari.changedetection.groupie.RowItem
import com.bernaferrari.changedetection.util.GlideApp
import com.bernaferrari.changedetection.util.VisibilityHelper
import com.davemorrissey.labs.subscaleview.ImageSource
import com.orhanobut.logger.Logger
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Section
import com.yarolegovich.discretescrollview.DiscreteScrollView
import com.yarolegovich.discretescrollview.transform.ScaleTransformer
import kotlinx.android.synthetic.main.control_bar.*
import kotlinx.android.synthetic.main.control_bar_update_page.*
import kotlinx.android.synthetic.main.diff_image_fragment.*
import kotlinx.android.synthetic.main.diff_image_fragment.view.*
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.android.Main
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import java.io.File
import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty

//
// This is adapted from Bíblia em Libras app, which has a GIF dictionary in a carousel powered by ExoMedia.
// If you are interested, download it and scroll the initial list of items for a large button named "Dicionário":
// https://play.google.com/store/apps/details?id=com.biblialibras.android
// This is also open sourced here: https://github.com/bernaferrari/CarouselGifViewer
//
class ImageFragment : ScopedFragment(),
    DiscreteScrollView.OnItemChangedListener<RecyclerView.ViewHolder> {

    private lateinit var model: ImageViewModel
    private lateinit var carouselAdapter: ImageAdapter

    private var previousAdapterPosition = 0
    private val items = mutableListOf<RowItem>()
    private val section = Section()

    private val transition = AutoTransition().apply { duration = 175 }
    private val groupAdapter = GroupAdapter<com.xwray.groupie.ViewHolder>()

    // this variable is necessary since onCurrentItemChanged might be triggered when RecyclerView is shown/hidden
    // (visibility button). This way, the app never has to refresh the file
    private var currentFileId = ""
    private val uiState = UiState { updateUiFromState() }

    override fun onCurrentItemChanged(viewHolder: RecyclerView.ViewHolder?, adapterPosition: Int) {

        val item = carouselAdapter.getItemFromAdapter(adapterPosition) ?: return

        titlecontent?.text = item.timestamp.convertTimestampToDate()

        if (item.snapId == currentFileId) return
        currentFileId = item.snapId

        val newFile = File(requireContext().filesDir, item.snapId)
        val contentUri =
            FileProvider.getUriForFile(
                requireContext(),
                "com.bernaferrari.changedetection.files",
                newFile
            )

        photo_view.setImage(ImageSource.uri(contentUri))

        if (items.isEmpty()) return

        selectItem(adapterPosition)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = ChangeBounds().apply { duration = MainActivity.TRANSITION }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.diff_image_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        model = viewModelProvider(ViewModelFactory.getInstance(requireActivity().application))

        closecontent.setOnClickListener {
            view.findNavController().navigateUp()
        }

        next_previous_bar.isVisible = false
        controlBar.isVisible = false

        menucontent.setOnClickListener { view.drawer.openDrawer(GravityCompat.END) }

        shareToggle.setOnClickListener { _ ->
            carouselAdapter.getItemFromAdapter(previousAdapterPosition)?.also { shareItem(it) }
        }

        openBrowserToggle.setOnClickListener {
            requireContext().openInBrowser(getStringFromArguments(MainActivity.URL))
        }

        // this is needed. If visibility is off and the fragment is reopened,
        // drawable will keep the drawable from last state (off) even thought it should be on.
        visibility.setImageDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                VisibilityHelper.getStaticIcon(uiState.visibility)
            )
        )

        visibility.setOnClickListener {
            uiState.visibility++
            uiState.carousel = uiState.visibility

            // set and run the correct animation
            visibility.setAndStartAnimation(
                VisibilityHelper.getAnimatedIcon(uiState.visibility),
                requireContext()
            )
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

        carouselAdapter = ImageAdapter(
            recyclerListener,
            itemHeight,
            itemWidth,
            GlideApp.with(this)
        )

        val siteId = getStringFromArguments(MainActivity.SITEID)

        launch(Dispatchers.Default) {

            model.getAllSnapsPagedForId(siteId, getStringFromArguments(MainActivity.TYPE, "image%"))
                .observe(this@ImageFragment, Observer(carouselAdapter::submitList))

            val liveData = model.getSnapsFiltered(siteId, "image%")
            withContext(Dispatchers.Main) {
                liveData.observe(this@ImageFragment, Observer { listOfSnaps ->
                    val isItemsEmpty = items.isEmpty()
                    items.clear()

                    if (listOfSnaps != null) {
                        listOfSnaps.mapTo(items) { RowItem(it) }
                        section.update(items)

                        // Since selectItem is being set at onCurrentItemChanged, and this code is ran async,
                        // if it happens after onCurrentItemChanged is called, which usually happens, the
                        // first item won't be selected. The two lines below fix this.
                        if (isItemsEmpty) {
                            selectItem(0)
                        }

                        // If all items were removed, close this fragment
                        if (items.isEmpty()) {
                            view.findNavController().navigateUp()
                        }
                    }
                })
            }
        }

        carouselRecycler.apply {
            adapter = carouselAdapter

            setSlideOnFling(true)
            setSlideOnFlingThreshold(4000)
            setItemTransitionTimeMillis(150)
            setItemTransformer(
                ScaleTransformer.Builder()
                    .setMinScale(0.8f)
                    .build()
            )

            addOnItemChangedListener(this@ImageFragment)
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
                    items.indexOfFirst { it.snap.snapId == item.snap.snapId }
                drawer.closeDrawer(GravityCompat.END)
                carouselRecycler.smoothScrollToPosition(nextPosition) // position becomes selected with animated scroll
            }
        }
        groupAdapter.setOnItemLongClickListener { item, _ ->
            if (item is RowItem) {
                removeItemDialog(item.snap.snapId)
            }
            true
        }
    }

    private fun selectItem(adapterPosition: Int) {
        // deselect the previous item on the drawer. This might trigger an exception if item was added/removed
        try {
            items[previousAdapterPosition].isSelected = false
            groupAdapter.notifyItemChanged(previousAdapterPosition)
        } catch (e: IndexOutOfBoundsException) {
            Logger.e(e.localizedMessage)
        }

        drawerRecycler.scrollToIndexWithMargins(
            previousAdapterPosition,
            adapterPosition,
            items.size
        )

        previousAdapterPosition = adapterPosition

        // select the new item on the drawer
        items[adapterPosition].isSelected = true
        groupAdapter.notifyItemChanged(adapterPosition)
        section.notifyChanged()
    }

    private fun shareItem(item: Snap) {
        // images/jpeg should become jpeg

        val fileExtension = item.contentType.split("/").getOrNull(1) ?: ""

        val file = File(requireContext().cacheDir, "share.$fileExtension")
        file.createNewFile()
        file.writeBytes(requireContext().openFileInput(item.snapId).readBytes())

        val contentUri = FileProvider.getUriForFile(
            requireContext(),
            "com.bernaferrari.changedetection.files",
            file
        )

        val shareIntent = ShareCompat.IntentBuilder.from(activity)
            .setStream(contentUri)
            .intent

        shareIntent.type = item.contentType
        shareIntent.data = contentUri
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        startActivity(shareIntent)
    }

    private fun removeItemDialog(snapId: String) {
        MaterialDialog(requireContext())
            .title(R.string.remove)
            .message(R.string.remove_content)
            .positiveButton(R.string.yes) {
                model.removeSnap(snapId)
            }
            .negativeButton(R.string.no)
            .show()
    }

    private fun updateUiFromState() {
        beginDelayedTransition()

        carouselRecycler.isVisible = uiState.carousel
//        controlBar.isVisible = uiState.controlBar
    }

    private fun beginDelayedTransition() =
        TransitionManager.beginDelayedTransition(container, transition)

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
    }
}
