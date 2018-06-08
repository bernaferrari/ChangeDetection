/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bernaferrari.changedetection.screenDiffPdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import com.bernaferrari.changedetection.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * This fragment has a big {@ImageView} that shows PDF pages, and 2
 * [Button]s to move between pages. We use a
 * [PdfRenderer] to render PDF pages as
 * [Bitmap]s.
 */
class PdfRendererBasicFragment : Fragment(), View.OnClickListener {

    /**
     * File descriptor of the PDF.
     */
    private var mFileDescriptor: ParcelFileDescriptor? = null

    /**
     * [PdfRenderer] to render the PDF.
     */
    private var mPdfRenderer: PdfRenderer? = null

    /**
     * Page that is currently shown on the screen.
     */
    private var mCurrentPage: PdfRenderer.Page? = null

    /**
     * [ImageView] that shows a PDF page as a [Bitmap]
     */
    private var mImageView: ImageView? = null

    /**
     * [Button] to move to the previous page.
     */
    private var mButtonPrevious: Button? = null

    /**
     * [Button] to move to the next page.
     */
    private var mButtonNext: Button? = null

    /**
     * PDF page index
     */
    private var mPageIndex: Int = 0


    /**
     * Gets the number of pages in the PDF. This method is marked as public for testing.
     *
     * @return The number of pages.
     */
    val pageCount: Int
        get() = mPdfRenderer!!.pageCount

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pdf_renderer_basic, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Retain view references.
        mImageView = view.findViewById<View>(R.id.image) as ImageView
        mButtonPrevious = view.findViewById<View>(R.id.previous) as Button
        mButtonNext = view.findViewById<View>(R.id.next) as Button
        // Bind events.
        mButtonPrevious!!.setOnClickListener(this)
        mButtonNext!!.setOnClickListener(this)

        mPageIndex = 0
        // If there is a savedInstanceState (screen orientations, etc.), we restore the page index.
        if (null != savedInstanceState) {
            mPageIndex = savedInstanceState.getInt(STATE_CURRENT_PAGE_INDEX, 0)
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            openRenderer(activity!!)
            showPage(mPageIndex)
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(activity, "Error! " + e.message, Toast.LENGTH_SHORT).show()
        }

    }

    override fun onStop() {
        try {
            closeRenderer()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (null != mCurrentPage) {
            outState.putInt(STATE_CURRENT_PAGE_INDEX, mCurrentPage!!.index)
        }
    }

    /**
     * Sets up a [PdfRenderer] and related resources.
     */
    @Throws(IOException::class)
    private fun openRenderer(context: Context) {
        // In this sample, we read a PDF from the assets directory.
        val file = File(
            context.cacheDir,
            FILENAME
        )
        if (!file.exists()) {
            // Since PdfRenderer cannot handle the compressed asset file directly, we copy it into
            // the cache directory.
            val asset = context.assets.open(FILENAME)
            FileOutputStream(file).use {
                asset.copyTo(it)
            }
            asset.close()
        }
        mFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        // This is the PdfRenderer we use to render the PDF.
        if (mFileDescriptor != null) {
            mPdfRenderer = PdfRenderer(mFileDescriptor!!)
        }
    }

    /**
     * Closes the [PdfRenderer] and related resources.
     *
     * @throws IOException When the PDF file cannot be closed.
     */
    @Throws(IOException::class)
    private fun closeRenderer() {
        if (null != mCurrentPage) {
            mCurrentPage!!.close()
        }
        mPdfRenderer!!.close()
        mFileDescriptor!!.close()
    }

    /**
     * Shows the specified page of PDF to the screen.
     *
     * @param index The page index.
     */
    private fun showPage(index: Int) {
        if (mPdfRenderer!!.pageCount <= index) {
            return
        }
        // Make sure to close the current page before opening another one.
        if (null != mCurrentPage) {
            mCurrentPage!!.close()
        }
        // Use `openPage` to open a specific page in PDF.
        mCurrentPage = mPdfRenderer!!.openPage(index)
        // Important: the destination bitmap must be ARGB (not RGB).
        val bitmap = Bitmap.createBitmap(
            mCurrentPage!!.width, mCurrentPage!!.height,
            Bitmap.Config.ARGB_8888
        )
        // Here, we render the page onto the Bitmap.
        // To render a portion of the page, use the second and third parameter. Pass nulls to get
        // the default result.
        // Pass either RENDER_MODE_FOR_DISPLAY or RENDER_MODE_FOR_PRINT for the last parameter.
        mCurrentPage!!.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        // We are ready to show the Bitmap to user.
        mImageView!!.setImageBitmap(bitmap)
        updateUi()
    }

    /**
     * Updates the state of 2 control buttons in response to the current page index.
     */
    private fun updateUi() {
        val index = mCurrentPage!!.index
        val pageCount = mPdfRenderer!!.pageCount
        mButtonPrevious!!.isEnabled = 0 != index
        mButtonNext!!.isEnabled = index + 1 < pageCount
        //        getActivity().setTitle(getString(R.string.app_name_with_index, index + 1, pageCount));
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.previous -> {
                // Move to the previous page
                showPage(mCurrentPage!!.index - 1)
            }
            R.id.next -> {
                // Move to the next page
                showPage(mCurrentPage!!.index + 1)
            }
        }
    }

    companion object {

        /**
         * Key string for saving the state of current page index.
         */
        private val STATE_CURRENT_PAGE_INDEX = "current_page_index"

        /**
         * The filename of the PDF.
         */
        private val FILENAME = "sample.pdf"
    }

}
