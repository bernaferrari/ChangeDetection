package com.bernaferrari.changedetection.mainnew

import android.content.Context
import android.graphics.drawable.Animatable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import com.bernaferrari.base.misc.getColorCompat
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.core.KotlinEpoxyHolder
import com.bernaferrari.changedetection.extensions.onAnimationEnd
import com.bernaferrari.changedetection.extensions.setImageDrawableIfNull
import com.bernaferrari.changedetection.repo.ColorGroup
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.util.GradientColors
import io.reactivex.Completable

@EpoxyModelClass(layout = R.layout.main_item_card)
abstract class MainCardItem : EpoxyModelWithHolder<MainCardItem.Holder>() {

    @EpoxyAttribute
    lateinit var site: Site

    @EpoxyAttribute
    var syncingNow: Boolean = false

    @EpoxyAttribute
    var lastSnap: Snap? = null

    @EpoxyAttribute
    lateinit var title: String

    @EpoxyAttribute
    lateinit var lastSyncStr: String

    @EpoxyAttribute
    lateinit var lastDiffStr: String

    var currentlyReloading: Boolean = false

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var onClick: View.OnClickListener? = null

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var onReload: View.OnClickListener? = null

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var onLongClick: View.OnLongClickListener? = null

    override fun bind(holder: Holder) {

        // set up transition
        val tag = site.id
        holder.containerView.tag = tag
        ViewCompat.setTransitionName(holder.containerView, tag)

        holder.auxBind(holder.containerView.context)
    }

    private fun Holder.auxBind(context: Context) {

        // set title and subtitle
        titleTextView.text = title
        subtitleTextView.text = site.url

        // set images that will be animated
        // imgView:src sometimes doesn't cast as AnimatedVectorDrawableCompat, so this is necessary.
        lastSyncImage.setImageDrawableIfNull(R.drawable.vector_anim_sync)
        reload.setImageDrawableIfNull(R.drawable.vector_anim_reload)

        (reload.background as? GradientDrawable)?.setColor(context.getColorCompat(R.color.white))

        reload.setOnClickListener(onReload)

        containerView.setOnClickListener(onClick)
        containerView.setOnLongClickListener(onLongClick)

        if (syncingNow) startProgress() else stopProgress()

        // set last sync and last change text
        lastSyncText.text = lastSyncStr
        lastChangeText.text = lastDiffStr

        // change the card color accordingly
        changeColor(context)
    }

    //
    // Methods that deal with colors
    //

    private fun Holder.changeColor(context: Context) = when {
        site.isSyncEnabled -> when {
            site.isSuccessful -> changeCardToStandardColor(context, site.colors)
            else -> changeCardColor(context.getColorCompat(R.color.md_red_400), 0xfff04a43.toInt())
        }
        else -> changeCardColor(
            context.getColorCompat(R.color.md_grey_500),
            context.getColorCompat(R.color.md_grey_700)
        )
    }

    private fun Holder.changeCardToStandardColor(context: Context, colors: ColorGroup) {
        updateSmallImagesColor(color = colors.second)

        val shape = GradientColors.getGradientDrawable(site.colors)
        shape.cornerRadius = 8 * context.resources.displayMetrics.density // 8dp
        cardView.background = shape

        reload.drawable.setTint(colors.first)
    }

    private fun Holder.changeCardBackground(color: Int) {
        // This needed since setCardBackgroundColor stops working when we change the background
        // drawable, and we need to change it for gradients to work. Also if we just edit the
        // drawable, it will be changed elsewhere, so we need to allow mutation first.

        cardView.background =
            ContextCompat.getDrawable(cardView.context, R.drawable.full_round_corner)
                ?.mutate()
                ?.also { it.setTint(color) }
    }

    private fun Holder.changeCardColor(strongerColor: Int, weakerColor: Int) {
        updateSmallImagesColor(color = strongerColor)
        changeCardBackground(weakerColor)
        reload.drawable.setTint(weakerColor)
    }

    //
    // Methods that deal directly with animation
    //

    override fun unbind(holder: Holder) {
        holder.cancelAsync()
        super.unbind(holder)
    }

    private fun Holder.cancelAsync() {
        (lastSyncImage.drawable as? Animatable2Compat)?.clearAnimationCallbacks()
        (reload.drawable as? Animatable2Compat)?.clearAnimationCallbacks()
    }

    private fun Holder.updateSmallImagesColor(color: Int) {
        (lastSyncImage.background as? GradientDrawable)?.setColor(color)
        (lastChangeImage.background as? GradientDrawable)?.setColor(color)
    }

    private fun Holder.startProgress() {

        if (!currentlyReloading) {
            reload.apply {
                this.startAndReloadAnim()
                Completable.mergeArray(this.fadeOut(500), this.shrinkIn(500)).subscribe()
            }

            lastSyncImage.startAndReloadAnim()
        }

        currentlyReloading = true
    }

    private fun ImageView.startAndReloadAnim() {
        (this.drawable as? Animatable)?.start()
        (this.drawable as? Animatable2Compat)?.onAnimationEnd {
            if (syncingNow) (this.drawable as? Animatable)?.start()
        }
    }

    private fun Holder.stopProgress() {
        currentlyReloading = false

        reload.animate()
            .setDuration(150)
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setListener(null)

        reload.visibility = View.VISIBLE

        cancelAsync()
    }

    //
    // Holder
    //

    class Holder : KotlinEpoxyHolder() {

        val cardView by bind<CardView>(R.id.cardView)
        val containerView by bind<FrameLayout>(R.id.containerView)

        val lastSyncText by bind<TextView>(R.id.lastSyncText)
        val lastSyncImage by bind<ImageView>(R.id.lastSyncImage)

        val lastChangeText by bind<TextView>(R.id.lastChangeText)
        val lastChangeImage by bind<ImageView>(R.id.lastChangeImage)

        val subtitleTextView by bind<TextView>(R.id.subtitleTextView)
        val titleTextView by bind<TextView>(R.id.titleTextView)
        val reload by bind<ImageView>(R.id.reload)
    }
}
