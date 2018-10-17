package com.bernaferrari.changedetection.detailsvisual

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Point
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import com.afollestad.materialdialogs.MaterialDialog
import com.bernaferrari.changedetection.*
import com.bernaferrari.changedetection.data.Snap
import com.bernaferrari.changedetection.extensions.*
import com.bernaferrari.changedetection.groupie.RowItem
import com.bernaferrari.changedetection.ui.ElasticDragDismissFrameLayout
import com.bernaferrari.changedetection.ui.RecyclerViewItemListener
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
import kotlinx.android.synthetic.main.diff_visual_fragment.*
import kotlinx.android.synthetic.main.diff_visual_fragment.view.*
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
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
class VisualFragment : ScopedFragment(),
    DiscreteScrollView.OnItemChangedListener<RecyclerView.ViewHolder> {

    private lateinit var model: VisualViewModel
    private lateinit var carouselAdapter: VisualAdapter
    private lateinit var fileKind: FORMAT

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

    // this variable is necessary since onCurrentItemChanged might be triggered when RecyclerView is shown/hidden
    // (visibility button). This way, the app never has to refresh the file
    private var currentFileId = ""

    private enum class FORMAT {
        PDF, IMAGE
    }

    private val groupAdapter = GroupAdapter<com.xwray.groupie.ViewHolder>()

    private val recyclerListener = object :
        RecyclerViewItemListener {
        override fun onClickListener(item: RecyclerView.ViewHolder) {
            carouselRecycler.smoothScrollToPosition((item as VisualViewHolder).itemPosition)
        }

        override fun onLongClickListener(item: RecyclerView.ViewHolder) {
            removeItemDialog((item as VisualViewHolder).currentSnap?.snapId ?: "")
        }
    }

    private fun beginDelayedTransition() =
        TransitionManager.beginDelayedTransition(container, transition)

    override fun onCurrentItemChanged(
        viewHolder: RecyclerView.ViewHolder?,
        adapterPosition: Int
    ) {

        // set the photo_view with current file
        val item = carouselAdapter.getItemFromAdapter(adapterPosition)
            ?.also { titlecontent?.text = it.timestamp.convertTimestampToDate() }
            ?.takeIf { it.snapId != currentFileId }
            ?.also { currentFileId = it.snapId } ?: return

        when (fileKind) {
            FORMAT.PDF -> {
                if (updateFileDescriptor(item.snapId)) {
                    showPage(0)
                }
            }
            FORMAT.IMAGE -> {
                val contentUri =
                    FileProvider.getUriForFile(
                        requireContext(),
                        "com.bernaferrari.changedetection.files",
                        File(requireContext().filesDir, item.snapId)
                    )

                photo_view?.setImage(ImageSource.uri(contentUri))
            }
        }

        if (items.isNotEmpty()) {
            selectItem(adapterPosition)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = ChangeBounds().apply {
            duration = MainActivity.TRANSITION
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.diff_visual_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        model = viewModelProvider(ViewModelFactory.getInstance(requireActivity().application))

        closecontent.setOnClickListener { view.findNavController().navigateUp() }

        elastic.addListener(object : ElasticDragDismissFrameLayout.ElasticDragDismissCallback() {
            override fun onDragDismissed() {
                view.findNavController().navigateUp()
            }
        })

        titlecontent.text = getStringFromArguments(MainActivity.LASTCHANGE)
        next_previous_bar.isVisible = false
        showOriginalAndChanges.isVisible = false
        sourceView.isVisible = false

        fileKind = if (arguments?.getString(MainActivity.TYPE) == "application/pdf") {
            FORMAT.PDF
        } else {
            highQualityToggle.isVisible = false
            FORMAT.IMAGE
        }

        menucontent.setOnClickListener { view.drawer.openDrawer(GravityCompat.END) }

        highQualityToggle.setOnClickListener { uiState.highQuality++ }

        mButtonPrevious.setOnClickListener { showPage(currentIndex - 1) }

        mButtonNext.setOnClickListener { showPage(currentIndex + 1) }

        shareToggle.setOnClickListener { _ ->
            carouselAdapter.getItemFromAdapter(previousAdapterPosition)?.also { shareItem(it) }
        }

        openBrowserToggle.setOnClickListener {
            requireContext().openInBrowser(getStringFromArguments(MainActivity.URL))
        }

        visibility.setImageDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                VisibilityHelper.getStaticIcon(uiState.visibility)
            )
        )

        // this is needed. If visibility is off and the fragment is reopened,
        // drawable will keep the drawable from last state (off) even thought it should be on.
        visibility.setOnClickListener {
            uiState.visibility++
            updateVisibility()
        }

        val windowDimensions = Point()
        requireActivity().windowManager.defaultDisplay.getSize(windowDimensions)
        val itemWidth = Math.round(Math.min(windowDimensions.y, windowDimensions.x) * 0.5f)
        val itemHeight = (120 * 0.7).toInt().toDp(view.resources)

        carouselAdapter = VisualAdapter(
            recyclerListener,
            itemHeight,
            itemWidth,
            requireContext(),
            fileKind == FORMAT.PDF,
            GlideApp.with(this)
        )

        carouselRecycler.addOnItemChangedListener(this)

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
        }

        drawerRecycler.apply {
            adapter = groupAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        fetchData()

        groupAdapter.add(section)
        groupAdapter.setOnItemClickListener { item, _ ->
            if (item is RowItem) {
                val nextPosition = items.indexOfFirst { it.snap.snapId == item.snap.snapId }
                drawer.closeDrawer(GravityCompat.END)
                carouselRecycler.smoothScrollToPosition(nextPosition) // position becomes selected with animated scroll
            }
        }
        groupAdapter.setOnItemLongClickListener { item, _ ->
            consume { if (item is RowItem) removeItemDialog(item.snap.snapId) }
        }
    }

    private fun updateVisibility() {
        uiState.carousel = uiState.visibility
        uiState.controlBar = uiState.visibility

        // set and run the correct animation
        visibility.setAndStartAnimation(VisibilityHelper.getAnimatedIcon(uiState.visibility))
    }

    // this is needed since getSnapsFiltered retrieves a liveData from Room to be observed
    private fun fetchData() = launch(Dispatchers.Main) {
        val siteId = getStringFromArguments(MainActivity.SITEID)

        val filtering = when (fileKind) {
            FORMAT.PDF -> "%pdf"
            FORMAT.IMAGE -> "image%"
        }

        val liveData = model.getSnapsFiltered(siteId, filtering)

        // add a delay corresponding to the transition time to avoid anim lag
        delay(MainActivity.TRANSITION + 25 + 15)

        model.getAllSnapsPagedForId(siteId, filtering)
            .observe(requireActivity(), Observer(carouselAdapter::submitList))

            progressBar?.isVisible = false

            liveData.observe(requireActivity(), Observer { filtered ->
                val isItemsEmpty = items.isEmpty()
                items.clear()

                if (filtered != null) {
                    filtered.mapTo(items) { RowItem(it) }
                    section.update(items)

                    // Since selectItem is being set at onCurrentItemChanged, and this code is ran async,
                    // if it happens after onCurrentItemChanged is called, which usually happens, the
                    // first item won't be selected.
                    if (isItemsEmpty) selectItem(0)

                    // If all items were removed, close this fragment
                    if (items.isEmpty()) view?.findNavController()?.navigateUp()
                }
            })
    }

    private fun updateUiFromState() {
        beginDelayedTransition()

        carouselRecycler.isVisible = uiState.carousel
        controlBar.isVisible = uiState.controlBar

        if (uiState.highQuality != highQualityToggle.isActivated) {
            highQualityToggle.isActivated = uiState.highQuality

            if (carouselAdapter.itemCount > 0) {
                val item = carouselAdapter.getItemFromAdapter(previousAdapterPosition) ?: return
                updateFileDescriptor(item.snapId)
                showPage(currentIndex)
            }
        }
    }

    private fun shareItem(item: Snap) {

        val fileExtension = item.contentType.split("/").getOrNull(1) ?: ""

        val file = when (fileKind) {
            FORMAT.PDF -> File(requireContext().cacheDir, "share.pdf")
            FORMAT.IMAGE -> File(requireContext().cacheDir, "share.$fileExtension")
        }

        file.createNewFile()
        file.writeBytes(requireContext().openFileInput(item.snapId).readBytes())

        val contentUri = FileProvider.getUriForFile(
            requireContext(),
            "com.bernaferrari.changedetection.files",
            file
        )

        val emailIntent = Intent(Intent.ACTION_SEND)
        emailIntent.type = item.contentType
        emailIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(emailIntent, getString(R.string.share)))
    }

    private fun removeItemDialog(snapId: String) {
        MaterialDialog(requireContext())
            .title(R.string.remove)
            .message(R.string.remove_content)
            .positiveButton(R.string.yes) { model.removeSnap(snapId) }
            .negativeButton(R.string.cancel)
            .show()
    }

    private fun updateFileDescriptor(fileName: String, forceUpdate: Boolean = false): Boolean {
        if (currentFileName == fileName && !forceUpdate) return false
        currentFileName = fileName

        val file = File("${Injector.get().appContext().filesDir.absolutePath}/$fileName")

        val mFileDescriptor =
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)

        mPdfRenderer = PdfRenderer(mFileDescriptor)
        return true
    }

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
        trySilently { mCurrentPage?.close() }

        // Use `openPage` to open a specific page in PDF.
        mCurrentPage = mPdfRenderer!!.openPage(index).also { mCurrentPage ->

            val qualityMultiplier = if (uiState.highQuality) 6 else 4

            // Important: the destination bitmap must be ARGB (not RGB).
            val bitmap = Bitmap.createBitmap(
                mCurrentPage.width * qualityMultiplier, mCurrentPage.height * qualityMultiplier,
                Bitmap.Config.ARGB_8888
            )

            Canvas(bitmap).apply {
                drawColor(Color.WHITE)
                drawBitmap(bitmap, 0f, 0f, null)
            }

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

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)

        if (newConfig?.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            uiState.visibility = false
            updateVisibility()
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

        drawerRecycler?.scrollToIndexWithMargins(
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

    override fun onStart() {
        super.onStart()

        // this is needed to avoid java.lang.IllegalStateException: Already closed when app
        // goes to background (onStop is called) and return. This won't be called on the first
        // run, since itemCount will be 0.
        if (fileKind == FORMAT.PDF && carouselAdapter.itemCount > 0) {
            val item = carouselAdapter.getItemFromAdapter(previousAdapterPosition) ?: return
            if (updateFileDescriptor(item.snapId, true)) {
                showPage(currentIndex)
            }
        }
    }

    override fun onStop() {

        if (fileKind == FORMAT.PDF) {
            // this is necessary to avoid java.lang.IllegalStateException: Already closed
            trySilently { mCurrentPage?.close() }
            trySilently { mPdfRenderer?.close() }
        }

        super.onStop()
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
