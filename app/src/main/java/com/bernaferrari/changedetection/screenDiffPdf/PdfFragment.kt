package com.bernaferrari.changedetection.screenDiffPdf

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.support.transition.AutoTransition
import android.support.transition.TransitionManager
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.ShareCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
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
import com.bernaferrari.changedetection.Application
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.ViewModelFactory
import com.bernaferrari.changedetection.data.Snap
import com.bernaferrari.changedetection.extensions.*
import com.bernaferrari.changedetection.groupie.RowItem
import com.bernaferrari.changedetection.screenDiffText.TextFragment
import com.davemorrissey.labs.subscaleview.ImageSource
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Section
import com.yarolegovich.discretescrollview.DiscreteScrollView
import com.yarolegovich.discretescrollview.transform.ScaleTransformer
import kotlinx.android.synthetic.main.control_bar.*
import kotlinx.android.synthetic.main.control_bar_update_page.*
import kotlinx.android.synthetic.main.diff_image_fragment.*
import kotlinx.android.synthetic.main.diff_image_fragment.view.*
import java.io.File
import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty

//
// This is adapted from Bíblia em Libras app, which has a GIF dictionary in a carousel powered by ExoMedia.
// If you are interested, download it and scroll the initial list of items for a large button named "Dicionário":
// https://play.google.com/store/apps/details?id=com.biblialibras.android
//
// This is also adapted from Lottie, which currently provides one of the best open source
// sample app for a library I've ever seen: https://github.com/airbnb/lottie-android
//
class PdfFragment : Fragment(),
    DiscreteScrollView.OnItemChangedListener<RecyclerView.ViewHolder> {

    lateinit var model: PdfViewModel
    lateinit var adapter: PdfAdapter

    private val transition = AutoTransition().apply { duration = 175 }
    private val uiState = UiState { updateUiFromState() }

    private var mPdfRenderer: PdfRenderer? = null
    private var mCurrentPage: PdfRenderer.Page? = null

    // this variable is necessary since the CurrentPage index changes on updateFileDescriptor, and
    // this value is needed on updateUiFromState when uiState.highQuality is toggled, so it keeps the same page opened.
    private var currentIndex = 0

    // this variable is necessary since onCurrentItemChanged might be triggered when RecyclerView is shown/hidden
    // (visibility button). This way, the app never has to refresh the file
    private var currentFileName = ""

    // this variable is used to track the last position, so the app knows the direction the user is going
    // and can provide the correct padding. Also, it is used for items selection/deselection.
    private var previousAdapterPosition = 0

    private val items = mutableListOf<RowItem>()
    private val section = Section()

    private fun updateUiFromState() {
        beginDelayedTransition()

        carouselRecycler.isVisible = uiState.carousel
        controlBar.isVisible = uiState.controlBar

        if (uiState.highQuality != highQualityToggle.isActivated) {
            highQualityToggle.isActivated = uiState.highQuality

            val item = adapter.getItemFromAdapter(previousAdapterPosition) ?: return
            updateFileDescriptor(item.snapId)
            showPage(currentIndex)
        }
    }

    override fun onStop() {
        // this is necessary to avoid java.lang.IllegalStateException: Already closed
        // if (mCurrentPage != null) isn't working when sharing
        try {
            mCurrentPage?.close()
        } catch (e: IllegalStateException) {

        }

        try {
            mPdfRenderer?.close()
        } catch (e: IllegalStateException) {

        }

        super.onStop()
    }

    private fun beginDelayedTransition() =
        TransitionManager.beginDelayedTransition(container, transition)

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

        // select the new item on the drawer
        items[adapterPosition].isSelected = true
        groupAdapter.notifyItemChanged(adapterPosition)
        section.notifyChanged()

        // set the photo_view with current file
        val item = adapter.getItemFromAdapter(adapterPosition) ?: return
        titlecontent.text = item.timestamp.convertTimestampToDate()

        if (updateFileDescriptor(item.snapId)) {
            showPage(0)
        }
    }

    private val groupAdapter = GroupAdapter<com.xwray.groupie.ViewHolder>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.diff_image_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        model = obtainViewModel(requireActivity())

        view.closecontent.setOnClickListener {
            view.let { Navigation.findNavController(it).navigateUp() }
        }

        view.menucontent.setOnClickListener { view.drawer.openDrawer(GravityCompat.END) }

        highQualityToggle.setOnClickListener { uiState.highQuality++ }

        mButtonPrevious.setOnClickListener { showPage(currentIndex - 1) }

        mButtonNext.setOnClickListener { showPage(currentIndex + 1) }

        shareToggle.setOnClickListener {
            val item =
                adapter.getItemFromAdapter(previousAdapterPosition) ?: return@setOnClickListener
            shareItem(item)
        }

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
            uiState.controlBar = uiState.visibility

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
                carouselRecycler.smoothScrollToPosition((item as PdfViewHolder).itemPosition)
            }

            override fun onLongClickListener(item: RecyclerView.ViewHolder) {
                removeItemDialog((item as PdfViewHolder).currentSnap?.snapId ?: "")
            }
        }

        val windowDimensions = Point()
        requireActivity().windowManager.defaultDisplay.getSize(windowDimensions)
        val itemWidth = Math.round(Math.min(windowDimensions.y, windowDimensions.x) * 0.5f)
        val itemHeight = (120 * 0.7).toInt().toDp(view.resources)

        adapter = PdfAdapter(
            recyclerListener,
            itemHeight,
            itemWidth,
            requireContext()
        )

        val siteId = arguments?.getString("SITEID") ?: ""
        model.getAllSnapsPagedForId(siteId).observe(this, Observer(adapter::submitList))
        model.getAllMinimalSnapsForId(siteId).observe(this, Observer {
            items.clear()
            if (it != null) {
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

            it.addOnItemChangedListener(this@PdfFragment)
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

    private fun shareItem(item: Snap) {
        val file = File(requireContext().cacheDir, "share.pdf")
        file.createNewFile()
        file.writeBytes(requireContext().openFileInput(item.snapId).readBytes())

        val contentUri = FileProvider.getUriForFile(
            requireContext(),
            "com.bernaferrari.changedetection.files",
            file
        )

        val shareIntent = ShareCompat.IntentBuilder.from(activity)
            .setStream(contentUri)
            .setType(item.contentType)
            .intent

        shareIntent.data = contentUri
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        startActivity(shareIntent)
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

    private fun updateFileDescriptor(fileName: String): Boolean {
        if (currentFileName == fileName) return false
        currentFileName = fileName

        val file = File("${Application.instance.filesDir.absolutePath}/$fileName")

        val mFileDescriptor =
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)

        mPdfRenderer = PdfRenderer(mFileDescriptor)
        return true
    }


    /**
     * Shows the specified page of PDF to the screen.
     *
     * @param index The page index.
     */
    private fun showPage(index: Int) {

        currentIndex = index

        if (mPdfRenderer!!.pageCount <= index) {
            if (mCurrentPage != null) {
                updateUi()
            }

            return
        }

        // Make sure to close the current page before opening another one.
        // The exception catches "Already closed"
        try {
            mCurrentPage?.close()
        } catch (e: IllegalStateException) {

        }

        // Use `openPage` to open a specific page in PDF.
        mCurrentPage = mPdfRenderer!!.openPage(index).also { mCurrentPage ->

            val qualityMultiplier = if (uiState.highQuality) 4 else 2

            // Important: the destination bitmap must be ARGB (not RGB).
            val bitmap = Bitmap.createBitmap(
                mCurrentPage.width * qualityMultiplier, mCurrentPage.height * qualityMultiplier,
                Bitmap.Config.ARGB_8888
            )
            // Here, we render the page onto the Bitmap.
            // To render a portion of the page, use the second and third parameter. Pass nulls to get
            // the default result.
            // Pass either RENDER_MODE_FOR_DISPLAY or RENDER_MODE_FOR_PRINT for the last parameter.
            mCurrentPage.render(
                bitmap,
                null,
                null,
                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
            )
            // We are ready to show the Bitmap to user.
            view?.photo_view?.setImage(ImageSource.bitmap(bitmap))
        }

        updateUi()
    }

    /**
     * Updates the state of 2 control buttons in response to the current page index.
     */
    private fun updateUi() {
        val index = mCurrentPage!!.index
        val pageCount = mPdfRenderer!!.pageCount
        mButtonPrevious.isEnabled = 0 != index
        mButtonNext.isEnabled = index + 1 < pageCount

        next_previous_bar.isVisible = mButtonPrevious.isEnabled || mButtonNext.isEnabled
    }

    private fun obtainViewModel(activity: FragmentActivity): PdfViewModel {
        // Use a Factory to inject dependencies into the ViewModel
        val factory = ViewModelFactory.getInstance(activity.application)
        return ViewModelProviders.of(activity, factory).get(PdfViewModel::class.java)
    }

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
