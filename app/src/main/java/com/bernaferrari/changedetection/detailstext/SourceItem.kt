package com.bernaferrari.changedetection.detailsText


import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.core.KotlinEpoxyHolder
import com.bernaferrari.changedetection.extensions.getColorFromAttr

@EpoxyModelClass(layout = R.layout.diff_text_item_text)
abstract class SourceItem : EpoxyModelWithHolder<SourceItem.Holder>() {

    @EpoxyAttribute
    var itemTitle: String = ""

    @EpoxyAttribute
    var index: Int = 0

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var onClick: View.OnClickListener? = null

    override fun bind(viewHolder: Holder) {
        viewHolder.bindAux()
    }

    private fun Holder.bindAux() {
        subtitle.text = index.toString()
        title.text = itemTitle

        containerView.setOnClickListener(onClick)

        val context = containerView.context

        // Sample string: +TEXTADDED this text was added TEXTADDED
        // [+, this text was added, ] -> highlight "this text was added"
        val token = itemTitle.split("TEXTADDED|TEXTREMOVED".toRegex())
        val firstChar = itemTitle.getOrNull(0) ?: ' '

        val codeColorHighlight = when (firstChar) {
            '+' -> context.getColorFromAttr(R.attr.code_addition_diff)
            '-' -> context.getColorFromAttr(R.attr.code_deletion_diff)
            else -> Color.TRANSPARENT
        }

        if (codeColorHighlight != Color.TRANSPARENT) {
            setSpannable(token, codeColorHighlight, title)
        }

        val codeColor = when (firstChar) {
            '+' -> context.getColorFromAttr(R.attr.code_addition)
            '-' -> context.getColorFromAttr(R.attr.code_deletion)
            else -> Color.TRANSPARENT
        }

        title.setBackgroundColor(codeColor)

        subtitle.apply {
            when (firstChar) {
                '+' -> {
                    gravity = Gravity.CENTER or Gravity.END
                    setBackgroundColor(context.getColorFromAttr(R.attr.num_addition))
                }
                '-' -> {
                    gravity = Gravity.CENTER or Gravity.START
                    setBackgroundColor(context.getColorFromAttr(R.attr.num_deletion))
                }
                else -> {
                    gravity = Gravity.CENTER
                    setBackgroundColor(Color.TRANSPARENT)
                }
            }
        }
    }

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
            if (previousIndex != currentIndex && index % 2 != 0) {
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

    //
    // Holder
    //

    class Holder : KotlinEpoxyHolder() {
        val title by bind<TextView>(R.id.title)
        val subtitle by bind<TextView>(R.id.subtitle)
        val containerView by bind<ConstraintLayout>(R.id.container)
    }

}
