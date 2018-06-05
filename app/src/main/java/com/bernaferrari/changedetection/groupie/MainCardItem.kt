package com.bernaferrari.changedetection.groupie

import android.content.Context
import android.graphics.drawable.Animatable
import android.graphics.drawable.GradientDrawable
import android.support.graphics.drawable.Animatable2Compat
import android.support.graphics.drawable.AnimatedVectorDrawableCompat
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.ImageView
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.data.MinimalSnap
import com.bernaferrari.changedetection.data.Site
import com.bernaferrari.changedetection.data.Snap
import com.bernaferrari.changedetection.extensions.onAnimationEnd
import com.github.marlonlom.utilities.timeago.TimeAgo
import com.github.marlonlom.utilities.timeago.TimeAgoMessages
import com.orhanobut.logger.Logger
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.main_item_card.*
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Main screen card item
 *
 * @param site               site item
 * @param lastMinimalSnap    for item subtitle
 * @param reloadCallback     when reload button is selected
 */
class MainCardItem(
    var site: Site,
    var lastMinimalSnap: MinimalSnap?,
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

    // update the site and the minimalSnap
    fun update(tas: Site, lastMinimalSnap: MinimalSnap?) {
        this.site = tas
        this.lastMinimalSnap = lastMinimalSnap
        changeStatus()
        notifyChanged()
    }

    // update the minimalSnap.
    fun update(lastMinimalSnap: MinimalSnap?) {
        this.lastMinimalSnap = lastMinimalSnap
        notifyChanged()
    }

    // update the minimalSnap with a snap that gets converted to a minimalSnap
    fun update(lastSnap: Snap) {
        this.lastMinimalSnap = MinimalSnap(
            lastSnap.snapId,
            lastSnap.siteId,
            lastSnap.timestamp,
            lastSnap.size
        )
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

        val context = holder.containerView.context
        // imageview:src on xml sometimes doesn't cast as AnimatedVectorDrawableCompat, so this is necessary.
        holder.syncimage.setImageDrawableIfNull(
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

        bind(holder, position)
        bindMutable(holder)
    }

    private fun ImageView.setImageDrawableIfNull(drawable: AnimatedVectorDrawableCompat?) {
        if (this.drawable == null) {
            this.setImageDrawable(drawable)
        }
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {
        val title = if (site.title.isNullOrBlank()) {
            site.url.replaceFirst("^(http[s]?://www\\.|http[s]?://|www\\.)".toRegex(), "")
        } else {
            site.title ?: ""
        }

        viewHolder.titleTextView.text = title
        viewHolder.subtitleTextView.text = site.url
    }

    private fun bindMutable(holder: ViewHolder) {
        val context = holder.containerView.context

        setLastDiff(holder)

        (holder.reload.background as GradientDrawable).setColor(
            ContextCompat.getColor(
                context,
                R.color.white
            )
        )

        holder.reload.setOnClickListener {
            reloadCallback.invoke(this)
        }

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
                holder.lastsync.text = holder.lastsync.context.getString(R.string.syncing)
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

        val shape = GradientDrawable(
            GradientDrawable.Orientation.TR_BL, intArrayOf(
                site.colors.first, site.colors.second
            )
        )

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
        holder.lastsync.text = if (status == SYNC.ERROR) {
            getTimeAgo(site.timestamp) + " – " + holder.lastsync.context.getString(R.string.error)
        } else {
            getTimeAgo(site.timestamp) + " – ${readableFileSize(lastMinimalSnap?.size ?: 0)}"
        }

        siteDisposable?.dispose()
        siteDisposable = generateTimerDisposable(site.timestamp).subscribe {
            setLastSync(holder)
        }
    }

    private fun setLastDiff(holder: ViewHolder) {
        if (lastMinimalSnap == null) {
            // if "last" snapshot was null, there is not a single snapshot for this site
            holder.lastradar.text = holder.lastradar.context.getString(R.string.nothing_yet)
        } else {
            Logger.d("id: ${lastMinimalSnap?.snapId} ts: ${lastMinimalSnap?.timestamp}")
            holder.lastradar.text = getTimeAgo(lastMinimalSnap?.timestamp)
            diffDisposable?.dispose()
            diffDisposable = generateTimerDisposable(lastMinimalSnap?.timestamp ?: 0).subscribe {
                setLastDiff(holder)
            }
        }
    }

    private fun updateRoundBackgrounds(color: Int, holder: ViewHolder) {
        (holder.syncimage.background as GradientDrawable).setColor(color)
        (holder.radarimage.background as GradientDrawable).setColor(color)
    }

    private fun getTimeAgo(timestamp: Long?): String {
        if (timestamp == null) {
            return ""
        }
        val messages = TimeAgoMessages.Builder().withLocale(Locale.getDefault()).build()
        return TimeAgo.using(timestamp, messages)
    }

    private fun startProgress(holder: ViewHolder) {
        isReloading = true

        holder.reload.apply {
            startAndReloadAnim(this)
            Completable.mergeArray(this.fadeOut(500), this.shrinkIn(500)).subscribe()
        }

        startAndReloadAnim(holder.syncimage)
    }

    private fun startAndReloadAnim(imageView: ImageView) {
        (imageView.drawable as Animatable).start()
        (imageView.drawable as Animatable2Compat).onAnimationEnd {
            if (isReloading) (imageView.drawable as Animatable).start()
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

    // converts a size in bytes into a readable format
    private fun readableFileSize(size: Int): String {
        if (size <= 0) return "EMPTY"
        val units = arrayOf("B", "kB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(
            size / Math.pow(
                1024.0,
                digitGroups.toDouble()
            )
        ) + " " + units[digitGroups]
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
