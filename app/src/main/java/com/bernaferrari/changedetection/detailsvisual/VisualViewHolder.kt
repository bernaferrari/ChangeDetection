package com.bernaferrari.changedetection.detailsvisual

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import com.bernaferrari.changedetection.Injector
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.data.Snap
import com.bernaferrari.changedetection.extensions.consume
import com.bernaferrari.changedetection.extensions.convertTimestampToDate
import com.bernaferrari.changedetection.extensions.inflate
import com.bernaferrari.changedetection.ui.RecyclerViewItemListener
import com.bernaferrari.changedetection.util.GlideRequests
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import kotlinx.android.synthetic.main.diff_visual_item_carousel.view.*
import java.io.File

class VisualViewHolder(
    parent: ViewGroup,
    private val itemHeight: Int,
    private val itemWidth: Int,
    private val isPdf: Boolean,
    val callback: RecyclerViewItemListener
) : androidx.recyclerview.widget.RecyclerView.ViewHolder(
    parent.inflate(R.layout.diff_visual_item_carousel).apply {
        this.updateLayoutParams {
            width = itemWidth
            height = itemHeight
        }
    }
) {
    init {
        itemView.setOnClickListener {
            callback.onClickListener(this)
        }

        itemView.setOnLongClickListener {
            consume { callback.onLongClickListener(this) }
        }
    }

    var currentSnap: Snap? = null
    var itemPosition: Int = 0

    fun bindTo(
        snap: Snap?,
        position: Int,
        context: Context,
        glide: GlideRequests
    ) {
        currentSnap = snap
        itemPosition = position

        if (isPdf) {
            renderPdf(snap, context)
        } else {
            renderImage(snap, glide)
        }
    }

    private fun renderImage(snap: Snap?, glide: GlideRequests) {
        if (snap != null) {
            this.itemView.subtitle.text = snap.timestamp.convertTimestampToDate()

            glide.load(Injector.get().appContext().openFileInput(snap.snapId).readBytes())
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(this.itemView.imageView)
        } else {
            glide.clear(this.itemView.imageView)
        }
    }

    private fun renderPdf(snap: Snap?, context: Context) {
        if (snap == null) return

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

        Canvas(bitmap).apply {
            drawColor(Color.WHITE)
            drawBitmap(bitmap, 0f, 0f, null)
        }

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
