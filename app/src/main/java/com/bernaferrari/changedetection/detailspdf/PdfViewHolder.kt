package com.bernaferrari.changedetection.detailspdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.support.v7.widget.RecyclerView
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.data.Snap
import com.bernaferrari.changedetection.detailstext.TextFragment
import com.bernaferrari.changedetection.extensions.convertTimestampToDate
import kotlinx.android.synthetic.main.diff_image_item_paging.view.*
import java.io.File

class PdfViewHolder(
    parent: ViewGroup,
    private val itemHeight: Int,
    private val itemWidth: Int,
    val callback: TextFragment.Companion.RecyclerViewItemListener
) : RecyclerView.ViewHolder(
    LayoutInflater.from(parent.context).inflate(
        R.layout.diff_image_item_paging,
        parent,
        false
    ).apply {
        val params = FrameLayout.LayoutParams(
            itemWidth,
            itemHeight
        )
        params.gravity = Gravity.CENTER
        this.layoutParams = params
    }
) {

    init {
        itemView.setOnClickListener {
            callback.onClickListener(this)
        }

        itemView.setOnLongClickListener {
            callback.onLongClickListener(this)
            true
        }
    }

    var currentSnap: Snap? = null
    var itemPosition: Int = 0

    fun bindTo(
        snap: Snap?,
        position: Int,
        context: Context
    ) {
        currentSnap = snap
        itemPosition = position

        if (snap != null) {
            val file = File("${context.filesDir.absolutePath}/${snap.snapId}")

            val mFileDescriptor =
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)

            val mPdfRenderer = PdfRenderer(mFileDescriptor)

            // Use `openPage` to open a specific page in PDF.
            val mCurrentPage = mPdfRenderer.openPage(0)
            // Important: the destination bitmap must be ARGB (not RGB).
            val bitmap = Bitmap.createBitmap(
                mCurrentPage!!.width, mCurrentPage.height,
                Bitmap.Config.ARGB_8888
            )
            // Here, we render the page onto the Bitmap.
            // To render a portion of the page, use the second and third parameter. Pass nulls to get
            // the default result.
            // Pass either RENDER_MODE_FOR_DISPLAY or RENDER_MODE_FOR_PRINT for the last parameter.
            mCurrentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            // We are ready to show the Bitmap to user.
            this.itemView.imageView.setImageBitmap(bitmap)
            this.itemView.subtitle.text = snap.timestamp.convertTimestampToDate()
            mCurrentPage.close()
            mPdfRenderer.close()
        }
    }
}