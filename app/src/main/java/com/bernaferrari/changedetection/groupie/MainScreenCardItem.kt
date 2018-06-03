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
import com.bernaferrari.changedetection.data.Diff
import com.bernaferrari.changedetection.data.MinimalDiff
import com.bernaferrari.changedetection.data.Site
import com.bernaferrari.changedetection.extensions.onAnimationEnd
import com.github.marlonlom.utilities.timeago.TimeAgo
import com.github.marlonlom.utilities.timeago.TimeAgoMessages
import com.orhanobut.logger.Logger
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.bottomsheet_item_card_list.*
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.TimeUnit

enum class SYNC {
    LOADING, OK, ERROR
}

class MainScreenCardItem(
    var site: Site,
    var lastMinimalDiff: MinimalDiff?,
    private val reloadCallback: ((MainScreenCardItem) -> Unit)
) : Item() {
    var status: SYNC = SYNC.OK
    private var isReloading = false
    private var currentColor = "" // This variable is used to track the current color, since
    // getColor is API 24+. It will be used when Item is created and there is no color defined for
    // holder.syncimage and holder.radarimage
    private var siteDisposable: Disposable? = null
    private var diffDisposable: Disposable? = null

    fun updateSite(tas: Site) {
        this.site = tas
        changeStatus()
        notifyChanged()
    }

    fun enableOrDisable(tas: Site) {
        this.site = tas
        notifyChanged()
    }

    fun updateSiteDiff(tas: Site, lastMinimalDiff: MinimalDiff?) {
        this.site = tas
        this.lastMinimalDiff = lastMinimalDiff
        changeStatus()
        notifyChanged()
    }

    fun updateDiff(lastMinimalDiff: MinimalDiff?) {
        this.lastMinimalDiff = lastMinimalDiff
        notifyChanged()
    }

    fun updateDiff(lastDiff: Diff) {
        this.lastMinimalDiff = MinimalDiff(
            lastDiff.diffId,
            lastDiff.siteId,
            lastDiff.timestamp,
            lastDiff.size
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

    override fun getSpanSize(spanCount: Int, position: Int) = 1

    override fun getLayout() = R.layout.bottomsheet_item_card_list

    override fun bind(holder: ViewHolder, position: Int, payloads: List<Any>) {
        if (status != SYNC.LOADING) {
            changeStatus()
        }

        val context = holder.containerView.context
        // imageview:src on xml sometimes doesn't cast as AnimatedVectorDrawableCompat, so this is necessary.
        holder.syncimage.takeIf { it.drawable == null }?.setImageDrawable(
            AnimatedVectorDrawableCompat.create(
                context,
                R.drawable.vector_anim_sync
            )
        )
        holder.reload.takeIf { it.drawable == null }?.setImageDrawable(
            AnimatedVectorDrawableCompat.create(
                context,
                R.drawable.vector_anim_reload
            )
        )

        bind(holder, position)
        bindMutable(holder)
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
                        changeCardToBlue(holder, context)
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
                    changeCardToBlue(holder, context)
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

    private fun changeCardToBlue(holder: ViewHolder, context: Context) {
        currentColor = "blue"

        setImageBackgroundToColor(
            color = ContextCompat.getColor(
                context,
                R.color.md_blue_400
            ),
            holder = holder,
            context = context
        )

        holder.cardView.setCardBackgroundColor(0xff356bf8.toInt())
        holder.reload.drawable.setTint(0xff356bf8.toInt())
    }

    private fun changeCardToGrey(holder: ViewHolder, context: Context) {
        currentColor = "grey"

        setImageBackgroundToColor(
            color = ContextCompat.getColor(
                context,
                R.color.md_grey_500
            ),
            holder = holder,
            context = context
        )

        holder.cardView.setCardBackgroundColor(
            ContextCompat.getColor(
                context,
                R.color.md_grey_700
            )
        )

        holder.reload.drawable.setTint(
            ContextCompat.getColor(
                context,
                R.color.md_grey_700
            )
        )
    }

    private fun changeCardToRed(holder: ViewHolder, context: Context) {
        currentColor = "red"

        setImageBackgroundToColor(
            color = ContextCompat.getColor(
                context,
                R.color.md_red_400
            ),
            holder = holder,
            context = context
        )

        holder.reload.drawable.setTint(0xfff04a43.toInt())
        holder.cardView.setCardBackgroundColor(0xfff04a43.toInt())
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
            getTimeAgo(site.timestamp) + " – ${readableFileSize(lastMinimalDiff?.size ?: 0)}"
        }

        siteDisposable?.dispose()
        siteDisposable = generateDisposable(site.timestamp).subscribe {
            setLastSync(holder)
        }
    }

    private fun setLastDiff(holder: ViewHolder) {
        if (lastMinimalDiff == null) {
            holder.lastradar.text = holder.lastradar.context.getString(R.string.nothing_yet)
        } else {
            Logger.d("id: ${lastMinimalDiff?.diffId} ts: ${lastMinimalDiff?.timestamp}")
            holder.lastradar.text = getTimeAgo(lastMinimalDiff?.timestamp)
            diffDisposable?.dispose()
            diffDisposable = generateDisposable(lastMinimalDiff?.timestamp ?: 0).subscribe {
                setLastDiff(holder)
            }
        }
    }

    private fun setImageBackgroundToColor(color: Int, holder: ViewHolder, context: Context) {
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

    private fun generateDisposable(timestamp: Long): Completable {
        // Get the abs value just in case our user is the terminator.
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
