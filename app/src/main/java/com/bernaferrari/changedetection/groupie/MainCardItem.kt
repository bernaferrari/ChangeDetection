package com.bernaferrari.changedetection.groupie

import android.content.Context
import android.graphics.drawable.Animatable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.data.Site
import com.bernaferrari.changedetection.data.Snap
import com.bernaferrari.changedetection.extensions.convertTimestampToDate
import com.bernaferrari.changedetection.extensions.onAnimationEnd
import com.bernaferrari.changedetection.extensions.readableFileSize
import com.bernaferrari.changedetection.util.GradientColors
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.main_item_card.*
import java.util.concurrent.TimeUnit

/**
 * Main screen card item
 *
 * @param site               site item
 * @param lastSnap           for item subtitle
 * @param reloadCallback     when reload button is selected
 */
class MainCardItem(
    var site: Site,
    var lastSnap: Snap?,
    private val reloadCallback: ((MainCardItem) -> Unit)
) : Item() {

    // this will be used to track the current item status
    enum class SYNC {
        LOADING, OK, ERROR
    }

    var status: SYNC = SYNC.OK
    private var isReloading = false
    private var currentColor = "" // This variable is used to track the current color, since
    // getColor is API 24+. It will be used when Item is created and there is no color defined for
    // holder.syncimage and holder.radarimage
    private var siteDisposable: Disposable? = null
    private var diffDisposable: Disposable? = null

    // update the current site, change the status and notifyChanged
    fun update(site: Site) {
        this.site = site
        changeStatus()
        notifyChanged()
    }

    // update the site and the snap
    fun update(tas: Site, lastSnap: Snap?) {
        this.site = tas
        this.lastSnap = lastSnap
        changeStatus()
        notifyChanged()
    }

    // update the snap.
    fun update(lastSnap: Snap?) {
        this.lastSnap = lastSnap
        notifyChanged()
    }

    fun startSyncing() {
        status = SYNC.LOADING
        notifyChanged()
    }

    private fun changeStatus() {
        status = when (site.isSuccessful) {
            true -> SYNC.OK
            false -> SYNC.ERROR
        }
    }

    // when used with a GridLayoutManager items only take one span
    override fun getSpanSize(spanCount: Int, position: Int) = 1

    override fun getLayout() = R.layout.main_item_card

    override fun bind(holder: ViewHolder, position: Int, payloads: List<Any>) {
        if (status != SYNC.LOADING) {
            changeStatus()
        }

        val tag = site.id
        holder.containerView.tag = tag
        ViewCompat.setTransitionName(holder.containerView, tag)

        val context = holder.containerView.context
        // imageview:src on xml sometimes doesn't cast as AnimatedVectorDrawableCompat, so this is necessary.
        holder.lastSyncImage.setImageDrawableIfNull(
            AnimatedVectorDrawableCompat.create(
                context,
                R.drawable.vector_anim_sync
            )
        )

        holder.reload.setImageDrawableIfNull(
            AnimatedVectorDrawableCompat.create(
                context,
                R.drawable.vector_anim_reload
            )
        )

        holder.subtitleTextView.text = site.url

        bind(holder, position)
        bindMutable(holder, context)
    }

    private fun ImageView.setImageDrawableIfNull(drawable: AnimatedVectorDrawableCompat?) {
        if (this.drawable == null) {
            this.setImageDrawable(drawable)
        }
    }

    override fun bind(holder: ViewHolder, position: Int) {
        val title = if (site.title.isNullOrBlank()) {
            site.url.replaceFirst("^(http[s]?://www\\.|http[s]?://|www\\.)".toRegex(), "")
        } else {
            site.title ?: ""
        }

        holder.titleTextView.text = title

        setLastDiff(holder)

        (holder.reload.background as? GradientDrawable)?.setColor(
            ContextCompat.getColor(
                holder.containerView.context,
                R.color.white
            )
        )

        holder.reload.setOnClickListener {
            reloadCallback.invoke(this)
        }
    }

    private fun bindMutable(holder: ViewHolder, context: Context) {
        when (status) {
            SYNC.LOADING -> {
                if (currentColor.isBlank()) {
                    if (site.isSuccessful) {
                        changeCardToStandardColor(holder, context)
                    } else {
                        changeCardToRed(holder, context)
                    }
                }
                startProgress(holder)
                holder.lastSyncText.text = holder.lastSyncText.context.getString(R.string.syncing)
            }
            SYNC.OK -> {
                stopSyncing(holder)
                if (site.isSyncEnabled) {
                    changeCardToStandardColor(holder, context)
                } else {
                    changeCardToGrey(holder, context)
                }
            }
            SYNC.ERROR -> {
                stopSyncing(holder)
                if (site.isSyncEnabled) {
                    changeCardToRed(holder, context)
                } else {
                    changeCardToGrey(holder, context)
                }
            }
        }
    }

    private fun stopSyncing(holder: ViewHolder) {
        stopProgress(holder)
        setLastSync(holder)
    }

    private fun changeCardToStandardColor(holder: ViewHolder, context: Context) {
        currentColor = "standard"

        updateRoundBackgrounds(
            color = site.colors.second,
            holder = holder
        )

        val shape = GradientColors.getGradientDrawable(site.colors.first, site.colors.second)

        // radius should be 8dp
        shape.cornerRadius = 8 * context.resources.displayMetrics.density
        holder.cardView.background = shape

        holder.reload.drawable.setTint(site.colors.first)
    }

    private fun changeCardToGrey(holder: ViewHolder, context: Context) {
        currentColor = "grey"

        updateRoundBackgrounds(
            color = ContextCompat.getColor(
                context,
                R.color.md_grey_500
            ),
            holder = holder
        )

        // This needed since setCardBackgroundColor stops working when we change the background
        // drawable, and we need to change it for gradients to work. Also if we just edit the
        // drawable, it will be changed elsewhere, so we need to allow mutation first.
        holder.cardView.background =
                ContextCompat.getDrawable(holder.cardView.context, R.drawable.full_round_corner)
                    ?.mutate()
                    ?.also {
                        it.setTint(
                            ContextCompat.getColor(
                                context,
                                R.color.md_grey_700
                            )
                        )
                    }

        holder.reload.drawable.setTint(
            ContextCompat.getColor(
                context,
                R.color.md_grey_700
            )
        )
    }

    private fun changeCardToRed(holder: ViewHolder, context: Context) {
        currentColor = "red"

        updateRoundBackgrounds(
            color = ContextCompat.getColor(
                context,
                R.color.md_red_400
            ),
            holder = holder
        )

        holder.reload.drawable.setTint(0xfff04a43.toInt())

        // This needed since setCardBackgroundColor stops working when we change the background
        // drawable, and we need to change it for gradients to work. Also if we just edit the
        // drawable, it will be changed elsewhere, so we need to allow mutation first.
        holder.cardView.background =
                ContextCompat.getDrawable(holder.cardView.context, R.drawable.full_round_corner)
                    ?.mutate()
                    ?.also {
                        it.setTint(0xfff04a43.toInt())
                    }
    }

    override fun unbind(holder: ViewHolder) {
        isReloading = false
        siteDisposable?.dispose()
        diffDisposable?.dispose()
        super.unbind(holder)
    }

    private fun setLastSync(holder: ViewHolder) {
        holder.lastSyncText.text = if (status == SYNC.ERROR) {
            site.timestamp.convertTimestampToDate() + " – " + holder.lastSyncText.context.getString(
                R.string.error
            )
        } else {
            site.timestamp.convertTimestampToDate() + " – ${lastSnap?.contentSize?.readableFileSize()}"
        }

        siteDisposable?.dispose()
        siteDisposable = generateTimerDisposable(site.timestamp).subscribe {
            setLastSync(holder)
        }
    }

    private fun setLastDiff(holder: ViewHolder) {
        if (lastSnap == null) {
            // if "last" snapshot was null, there is not a single snapshot for this site
            holder.lastChangeText.text =
                    holder.lastChangeText.context.getString(R.string.nothing_yet)
        } else {
            holder.lastChangeText.text = lastSnap?.timestamp?.convertTimestampToDate()
            diffDisposable?.dispose()
            diffDisposable = generateTimerDisposable(lastSnap?.timestamp ?: 0).subscribe {
                setLastDiff(holder)
            }
        }
    }

    private fun updateRoundBackgrounds(color: Int, holder: ViewHolder) {
        (holder.lastSyncImage.background as? GradientDrawable)?.setColor(color)
        (holder.lastChangeImage.background as? GradientDrawable)?.setColor(color)
    }

    private fun startProgress(holder: ViewHolder) {
        isReloading = true

        holder.reload.apply {
            startAndReloadAnim(this)
            Completable.mergeArray(this.fadeOut(500), this.shrinkIn(500)).subscribe()
        }

        startAndReloadAnim(holder.lastSyncImage)
    }

    private fun startAndReloadAnim(imageView: ImageView) {
        (imageView.drawable as? Animatable)?.start()
        (imageView.drawable as? Animatable2Compat)?.onAnimationEnd {
            if (isReloading) (imageView.drawable as? Animatable)?.start()
        }
    }

    private fun stopProgress(holder: ViewHolder) {
        isReloading = false

        holder.reload
            .animate()
            .setDuration(150)
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setListener(null)

        holder.reload.visibility = View.VISIBLE
    }

    // creates a completable animation for fading the reload button when pressed
    private fun View.fadeOut(duration: Long = 30): Completable {
        return Completable.create {
            animate().setDuration(duration)
                .alpha(0f)
                .withEndAction {
                    visibility = View.GONE
                    it.onComplete()
                }
        }
    }

    // creates a completable animation for shrinking the reload button when pressed
    private fun View.shrinkIn(duration: Long = 30): Completable {
        return Completable.create {
            animate().setDuration(duration)
                .scaleX(0.25f)
                .scaleY(0.25f)
                .withEndAction {
                    it.onComplete()
                }
        }
    }

    /**
     * This calculates how long it will take until updating the "last sync" text again.
     *
     * @param timestamp in milliseconds
     */
    private fun generateTimerDisposable(timestamp: Long): Completable {
        // Get the abs value just in case our user is the terminator and came from the future.
        val delay = Math.abs(System.currentTimeMillis() - timestamp)

        val seconds = delay / 1000
        val min = seconds / 60
        val hours = min / 60

        val timeToWait = when {
            hours / 24 > 0 -> 3600 * 24
            hours > 0 -> 3600
            min > 0 -> 60
            else -> 30
        }

        val ttl = (timeToWait * 1000).toFloat() // in Milli
        val finalTimeToWait = ((1 - ((delay / ttl) % 1)) * ttl).toLong()

        // If timeAgo is 3.9 days ago, we want to refresh when it is 4 days ago, so we wait 0.1 day.
        // If timeAgo is 7 years ago, we want to refresh when it is 8 years ago, so we wait 1 year.
        // If timeAgo is 1 min ago, we want to refresh when it is 2 min ago.

        return Completable.timer(
            finalTimeToWait,
            TimeUnit.MILLISECONDS,
            AndroidSchedulers.mainThread()
        )
    }
}
