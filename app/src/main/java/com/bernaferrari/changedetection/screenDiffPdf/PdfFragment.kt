package com.bernaferrari.changedetection.screenDiffPdf

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.view.GravityCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.navigation.Navigation
import com.afollestad.materialdialogs.MaterialDialog
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.ViewModelFactory
import com.bernaferrari.changedetection.extensions.convertTimestampToDate
import com.bernaferrari.changedetection.groupie.RowItem
import com.bernaferrari.changedetection.screenDiffText.TextFragment
import com.orhanobut.logger.Logger
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Section
import com.yarolegovich.discretescrollview.DiscreteScrollView
import com.yarolegovich.discretescrollview.transform.ScaleTransformer
import kotlinx.android.synthetic.main.diff_image_fragment.*
import kotlinx.android.synthetic.main.diff_image_fragment.view.*
import java.io.File

//
// This is adapted from Bíblia em Libras app, which has a GIF dictionary in a carousel powered by ExoMedia.
// If you are interested, download it and scroll the initial list of items for a large button named "Dicionário":
// https://play.google.com/store/apps/details?id=com.biblialibras.android
//
class PdfFragment : Fragment(),
    DiscreteScrollView.OnItemChangedListener<RecyclerView.ViewHolder> {

    lateinit var model: PdfViewModel
    lateinit var adapter: PdfAdapter

    private var previousAdapterPosition = 0
    private val items = mutableListOf<RowItem>()
    private val section = Section()

    override fun onCurrentItemChanged(viewHolder: RecyclerView.ViewHolder?, adapterPosition: Int) {
        titlecontent.text =
                adapter.getItemFromAdapter(adapterPosition)?.timestamp?.convertTimestampToDate()

        items[previousAdapterPosition].isSelected = false
        groupAdapter.notifyItemChanged(previousAdapterPosition)

        scrollToIndex(previousAdapterPosition, adapterPosition, items.size, drawerRecycler)
        previousAdapterPosition = adapterPosition

        items[adapterPosition].isSelected = true
        groupAdapter.notifyItemChanged(adapterPosition)
        section.notifyChanged()
    }

    private val groupAdapter = GroupAdapter<com.xwray.groupie.ViewHolder>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.diff_image_fragment, container, false)

        model = obtainViewModel(requireActivity())

        view.closecontent.setOnClickListener {
            view.let { Navigation.findNavController(it).navigateUp() }
        }

        view.menucontent.setOnClickListener {
            view.drawer.openDrawer(GravityCompat.END)
        }

        val recyclerListener = object :
            TextFragment.Companion.RecyclerViewItemListener {
            override fun onClickListener(item: RecyclerView.ViewHolder) {

                val materialDialog = MaterialDialog.Builder(requireContext())
                    .customView(R.layout.diff_image_photo_view, false)
                // TODO Add option to share the image

                val materialdialog = materialDialog.build()

                materialdialog.customView?.findViewById<ImageView>(R.id.photo_view)?.run {

                    val filename = "temporary.pdf"
                    val file = File(context.cacheDir, filename)
                    file.createNewFile()
                    file.writeBytes((item as PdfViewHolder).currentSnap!!.content)

                    val mFileDescriptor =
                        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    val mPdfRenderer = PdfRenderer(mFileDescriptor)

                    // Use `openPage` to open a specific page in PDF.
                    val mCurrentPage = mPdfRenderer.openPage(0)
                    // Important: the destination bitmap must be ARGB (not RGB).
                    val bitmap = Bitmap.createBitmap(
                        mCurrentPage!!.width * 2, mCurrentPage.height * 2,
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
                    this.setImageBitmap(bitmap)

                    mCurrentPage.close()
                    mPdfRenderer.close()
                }

                materialdialog.show()
            }

            override fun onLongClickListener(item: RecyclerView.ViewHolder) {
                removeItemDialog((item as PdfViewHolder).currentSnap?.snapId ?: "")
            }
        }

        val windowDimensions = Point()
        requireActivity().windowManager.defaultDisplay.getSize(windowDimensions)
        val itemHeight = Math.round(Math.min(windowDimensions.y, windowDimensions.x) * 0.7f)

        adapter = PdfAdapter(
            recyclerListener,
            itemHeight,
            requireContext()
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

        view.carouselRecycler.also {
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

        view.drawerRecycler.also {
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

        return view
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

    private fun obtainViewModel(activity: FragmentActivity): PdfViewModel {
        // Use a Factory to inject dependencies into the ViewModel
        val factory = ViewModelFactory.getInstance(activity.application)
        return ViewModelProviders.of(activity, factory).get(PdfViewModel::class.java)
    }

    private fun scrollToIndex(
        previousIndex: Int,
        index: Int,
        size: Int,
        recyclerView: RecyclerView
    ) {
        if (previousIndex == -1) {
            // This will run on first iteration
            Logger.d("index value: $index")
            when (index) {
                in 1 until (size - 1) -> recyclerView.scrollToPosition(index - 1)
                (size - 1) -> recyclerView.scrollToPosition(index)
                else -> recyclerView.scrollToPosition(0)
            }
        } else {
            Logger.d("index: " + index + " previousIndex = " + previousIndex + " /// " + (index in 1 until (previousIndex - 1)))
            when (index) {
                in 1 until previousIndex -> recyclerView.scrollToPosition(index - 1)
                in previousIndex until (size - 1) -> recyclerView.scrollToPosition(index + 1)
                else -> recyclerView.scrollToPosition(index)
            }
        }
    }
}
