package com.bernaferrari.changedetection.settings

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.NumberPicker
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.bernaferrari.changedetection.Injector
import com.bernaferrari.changedetection.R
import kotlinx.android.synthetic.main.dialog_sync.*
import java.util.*

// inspired from mnml
class DialogBackgroundSync : DialogFragment() {

    companion object {
        private const val TAG = "[ABOUT_DIALOG]"

        /** Shows the about dialog inside of [activity]. */
        fun show(activity: FragmentActivity) {
            val dialog = DialogBackgroundSync()
            dialog.show(activity.supportFragmentManager, TAG)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = activity ?: throw IllegalStateException("Oh no!")

        return MaterialDialog(context)
            .customView(R.layout.dialog_sync, noVerticalPadding = true)
            .also { it.getCustomView().setUpViews() }
    }

    private val singular by lazy { resources.getStringArray(R.array.singularTime) }
    private val plural by lazy { resources.getStringArray(R.array.pluralTime) }

    private fun View.setUpViews() {

        // kindPicker can be minute(1)/hour(2)/day(3)
        kindPicker.minValue = 1
        kindPicker.maxValue = 3

        item_switch.isChecked = Injector.get().backgroundSync().get()

        Injector.get().syncInterval().get().also { prefs ->
            // retrieve kindPicker from sharedPrefs and initialize numberPicker
            kindPicker.value = prefs.substring(0, 1).toInt()
            numberPicker.updateMinMax(kindPicker.value)
            numberPicker.value = prefs.substring(1, 3).toInt()
        }

        // add formatter, so numbers are always 01, 02, 03, etc.
        numberPicker.setFormatter(twoDigit)

        // update all descriptions and visibilities.
        fixSingularPlural()
        updateDescription()
        updatePickerVisibility()

        // add listeners
        numberPicker.setOnValueChangedListener { _, _, _ ->
            fixSingularPlural()
            updateDescription()
            updateSyncInterval()
        }

        kindPicker.setOnValueChangedListener { _, _, newVal ->
            numberPicker.updateMinMax(newVal)
            fixSingularPlural()
            updateDescription()
            updateSyncInterval()
        }

        title_bar.setOnClickListener {
            item_switch.toggle()
            updatePickerVisibility()
            // set shared value for backgroundSync
            Injector.get().backgroundSync().set(item_switch.isChecked)
        }
    }

    private fun View.fixSingularPlural() {
        val newVal = numberPicker.value
        kindPicker.displayedValues = if (newVal == 1) singular else plural
    }

    private fun View.updateSyncInterval() {
        // set shared value for syncInterval
        val prefs = Injector.get().syncInterval()
        val firstDigit = kindPicker.value
        val secondThirdDigits = "%02d".format(numberPicker.value)
        prefs.set("$firstDigit$secondThirdDigits")

        // update workManager
//        WorkerHelper.updateBackgroundWorker(true)
    }

    private fun View.updateDescription() {
        // update text string
        if (!item_switch.isChecked) {
            nextSync.text = getString(R.string.sync_disabled)
        } else {
            // retrieve correct word pronunciation before writing on screen
            val arr = if (numberPicker.value == 1) singular else plural
            val word = arr[kindPicker.value - 1]
            nextSync.text = getString(R.string.sync_every, numberPicker.value, word)
        }
    }

    private fun View.updatePickerVisibility() {
        kindPicker.isVisible = item_switch.isChecked
        numberPicker.isVisible = item_switch.isChecked
    }

    private fun NumberPicker.updateMinMax(newVal: Int) {
        when (newVal) {
            1 -> {
                this.minValue = 15
                this.maxValue = 59
            }
            2 -> {
                this.minValue = 1
                this.maxValue = 24
            }
            3 -> {
                this.minValue = 1
                this.maxValue = 30
            }
        }
    }

    private val twoDigit = TwoDigitFormatter()

    /**
     * Use a custom NumberPicker formatting callback to use two-digit minutes
     * strings like "01". Keeping a static formatter etc. is the most efficient
     * way to do this; it avoids creating temporary objects on every call to
     * format().
     */
    private class TwoDigitFormatter internal constructor() : NumberPicker.Formatter {
        internal val mBuilder = StringBuilder()
        val locale = Locale.getDefault()

        internal var mZeroDigit: Char = ' '
        internal var mFmt: java.util.Formatter = createFormatter(locale)

        internal val mArgs = arrayOfNulls<Any>(1)

        init {
            init(locale)
        }

        private fun init(locale: Locale) {
            mFmt = createFormatter(locale)
            mZeroDigit = getZeroDigit()
        }

        override fun format(value: Int): String {
            val currentLocale = Locale.getDefault()
            if (mZeroDigit != getZeroDigit()) {
                init(currentLocale)
            }
            mArgs[0] = value
            mBuilder.delete(0, mBuilder.length)
            mFmt.format("%02d", *mArgs)
            return mFmt.toString()
        }

        private fun getZeroDigit(): Char {
            return '0'
        }

        private fun createFormatter(locale: Locale): java.util.Formatter {
            return java.util.Formatter(mBuilder, locale)
        }
    }
}
