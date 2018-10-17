package com.bernaferrari.changedetection.ui

import android.app.Dialog
import android.os.Bundle
import com.bernaferrari.changedetection.Injector
import com.bernaferrari.changedetection.MainActivity
import com.bernaferrari.changedetection.R

/**
 * BottomSheetDialog fragment that uses a custom
 * theme which sets a rounded background to the dialog
 * and doesn't dim the navigation bar
 */
open class RoundedBottomSheetDialogFragment :
    com.google.android.material.bottomsheet.BottomSheetDialogFragment() {

    private val isDarkMode = Injector.get().sharedPrefs().getBoolean(MainActivity.DARKMODE, false)

    override fun getTheme(): Int = if (isDarkMode) {
        R.style.BottomSheetDialogThemeDark
    } else {
        R.style.BottomSheetDialogThemeLight
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        com.google.android.material.bottomsheet.BottomSheetDialog(requireContext(), theme)

}