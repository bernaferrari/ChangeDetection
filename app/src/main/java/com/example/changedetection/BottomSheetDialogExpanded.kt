package com.example.changedetection

import android.content.Context
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.BottomSheetDialog
import android.view.View
import android.widget.FrameLayout

class BottomSheetDialogExpanded(context: Context) : BottomSheetDialog(context) {

    private lateinit var mBehavior: BottomSheetBehavior<FrameLayout>

    override fun setContentView(view: View) {
        super.setContentView(view)
        val bottomSheet =
            window.decorView.findViewById<View>(android.support.design.R.id.design_bottom_sheet) as FrameLayout
        mBehavior = BottomSheetBehavior.from(bottomSheet)
        mBehavior.skipCollapsed = true
//        mBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onStart() {
        super.onStart()
        mBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }
}