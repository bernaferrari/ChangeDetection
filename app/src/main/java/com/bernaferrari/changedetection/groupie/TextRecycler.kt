package com.bernaferrari.changedetection.groupie


import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.view.Gravity
import android.widget.TextView
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.extensions.getColorFromAttr
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.diff_text_item_text.*

class TextRecycler(
    val title: String,
    val index: Int
) : Item() {

    override fun getLayout() = R.layout.diff_text_item_text

    // This will make the spannable showing the diffs
    private fun setSpannable(
        token: List<String>,
        color: Int,
        textView: TextView
    ) {
        val spannableDiff2 = SpannableString(token.joinToString(""))

        var previousIndex = 0
        var currentIndex = 0

        token.forEachIndexed { index, s ->
            currentIndex += s.count()
//            println("token $token -- current $s" )
            if (previousIndex != currentIndex && index % 2 != 0) {
//                println("span: $previousIndex -- $currentIndex")
                spannableDiff2.setSpan(
                    BackgroundColorSpan(color),
                    previousIndex,
                    currentIndex,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            previousIndex += s.count()
        }

        textView.text = SpannableStringBuilder(spannableDiff2)
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.subtitle.text = index.toString()
        viewHolder.title.text = title

        val context = viewHolder.containerView.context

        // Sample string: +TEXTADDED this text was added TEXTADDED
        // [+, this text was added, ] -> highlight "this text was added"
        val token = title.split("TEXTADDED|TEXTREMOVED".toRegex())
        val firstChar = title.getOrNull(0) ?: ' '

        val codeColorHighlight = when (firstChar) {
            '+' -> context.getColorFromAttr(R.attr.code_addition_diff)
            '-' -> context.getColorFromAttr(R.attr.code_deletion_diff)
            else -> Color.TRANSPARENT
        }

        if (codeColorHighlight != Color.TRANSPARENT) {
            setSpannable(
                token,
                codeColorHighlight,
                viewHolder.title
            )
        }

        val codeColor = when (firstChar) {
            '+' -> context.getColorFromAttr(R.attr.code_addition)
            '-' -> context.getColorFromAttr(R.attr.code_deletion)
            else -> Color.TRANSPARENT
        }

        viewHolder.title.setBackgroundColor(codeColor)

        viewHolder.subtitle.run {
            when (firstChar) {
                '+' -> {
                    gravity = Gravity.CENTER or Gravity.RIGHT
                    setBackgroundColor(context.getColorFromAttr(R.attr.num_addition))
                }
                '-' -> {
                    gravity = Gravity.CENTER or Gravity.LEFT
                    setBackgroundColor(context.getColorFromAttr(R.attr.num_deletion))
                }
                else -> {
                    gravity = Gravity.CENTER
                    setBackgroundColor(Color.TRANSPARENT)
                }
            }
        }
    }
}
