package com.example.changedetection.groupie

import android.content.Context
import android.graphics.drawable.Animatable
import android.support.v4.content.ContextCompat
import com.example.changedetection.R
import com.example.changedetection.data.Diff
import com.example.changedetection.data.Site
import com.github.marlonlom.utilities.timeago.TimeAgo
import com.github.marlonlom.utilities.timeago.TimeAgoMessages
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.bottomsheet_item_card_list.*
import java.util.*
import android.graphics.drawable.GradientDrawable
import com.example.changedetection.MainFragment
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit
import android.support.graphics.drawable.Animatable2Compat
import android.support.graphics.drawable.AnimatedVectorDrawableCompat
import android.view.View
import android.widget.ImageView
import com.example.changedetection.onAnimationEnd
import com.orhanobut.logger.Logger
import java.text.DecimalFormat

enum class SYNC {
    LOADING, OK, ERROR
}

class BottomSheetCardItem(var tas: Site, var lastDiff: Diff?, val listener: MainFragment.bottom) : Item() {
    var status: SYNC = SYNC.OK
    private var isReloading = false
    private var taskDisposable: Disposable ? = null
    private var diffDisposable: Disposable ? = null

    fun updateTask(tas: Site){
        this.tas = tas
        changeStatus()
        notifyChanged()
    }

    fun updateTaskDiff(tas: Site, lastDiff: Diff?){
        this.tas = tas
        this.lastDiff = lastDiff
        changeStatus()
        notifyChanged()
    }

    fun updateDiff(lastDiff: Diff?){
        this.lastDiff = lastDiff
        notifyChanged()
    }

    fun startSyncing(){
        status = SYNC.LOADING
        notifyChanged()
    }

    fun updateStatus(newStatus: SYNC){
        status = newStatus
    }

    private fun changeStatus(){
        status = when (tas.successful){
            true -> SYNC.OK
            false -> SYNC.ERROR
        }
    }

    override fun getSpanSize(spanCount: Int, position: Int) = 1

    override fun getLayout() = R.layout.bottomsheet_item_card_list

    override fun bind(holder: ViewHolder, position: Int, payloads: List<Any>) {
        if (status != SYNC.LOADING){
            changeStatus()
        }

        val context = holder.containerView.context
        // imageview:src on xml sometimes doesn't cast as AnimatedVectorDrawableCompat, so this is necessary.
        holder.syncimage.takeIf { it.drawable == null }?.setImageDrawable(AnimatedVectorDrawableCompat.create(context, R.drawable.vector_anim_sync))
        holder.reload.takeIf { it.drawable == null }?.setImageDrawable(AnimatedVectorDrawableCompat.create(context, R.drawable.vector_anim_reload))

        bind(holder, position)
        bindMutable(holder)
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {
        val title = if (tas.title.isNullOrBlank()) {
            tas.url.replaceFirst("^(http[s]?://www\\.|http[s]?://|www\\.)".toRegex(), "")
        } else {
            tas.title ?: ""
        }

        viewHolder.titleTextView.text = title
        viewHolder.subtitleTextView.text = tas.url
    }

    private fun bindMutable(holder: ViewHolder) {
        val context = holder.containerView.context

        setLastDiff(holder)

        (holder.reload.background as GradientDrawable).setColor(ContextCompat.getColor(context, R.color.white))

        holder.reload.setOnClickListener {
            listener.onRefreshListener(this)
        }

        holder.containerView.setOnClickListener {
            listener.onClickListener(this)
        }

        when (status) {
            SYNC.LOADING -> {
                startProgress(holder)
                holder.lastsync.text = "Syncing..."
            }
            SYNC.OK -> {
                holder.cardView.setCardBackgroundColor(0xff356bf8.toInt())
                stopProgress(holder)
                setLastSync(holder)
                setColor(holder, context)
            }
            SYNC.ERROR -> {
                holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.md_red_A700))
                stopProgress(holder)
                setLastSync(holder)
                setRedColor(holder, context)
            }
        }
    }

    override fun unbind(holder: ViewHolder) {
        isReloading = false
        taskDisposable?.dispose()
        diffDisposable?.dispose()
        super.unbind(holder)
    }

    private fun setLastSync(holder: ViewHolder){
        holder.lastsync.text = if (status == SYNC.ERROR){
            getTimeAgo(tas.timestamp) + " – ERROR"
        } else {
            getTimeAgo(tas.timestamp) + " – ${readableFileSize(lastDiff?.value?.count() ?: 0)}"
        }

        taskDisposable?.dispose()
        taskDisposable = generateDisposable(tas.timestamp).subscribe {
            setLastSync(holder)
        }
    }

    private fun setLastDiff(holder: ViewHolder){
        if (lastDiff == null) {
            holder.lastradar.text = "nothing yet"
        } else {
            Logger.d("id: ${lastDiff?.fileId} ts: ${lastDiff?.timestamp}")
            holder.lastradar.text = getTimeAgo(lastDiff?.timestamp)
            diffDisposable?.dispose()
            diffDisposable = generateDisposable(lastDiff?.timestamp ?: 0).subscribe {
                setLastDiff(holder)
            }
        }
    }

    private fun setRedColor(holder: ViewHolder, context: Context){
        (holder.syncimage.background as GradientDrawable).setColor(ContextCompat.getColor(context, R.color.md_red_400))
        (holder.radarimage.background as GradientDrawable).setColor(ContextCompat.getColor(context, R.color.md_red_400))
    }

    private fun setColor(holder: ViewHolder, context: Context){
        (holder.syncimage.background as GradientDrawable).setColor(ContextCompat.getColor(context, R.color.md_blue_400))
        (holder.radarimage.background as GradientDrawable).setColor(ContextCompat.getColor(context, R.color.md_blue_400))
    }

    private fun getTimeAgo(timestamp: Long?): String {
        if (timestamp == null){
            return ""
        }
        val messages = TimeAgoMessages.Builder().withLocale(Locale.getDefault()).build()
        return TimeAgo.using(timestamp, messages)
    }

    private fun startProgress(holder: ViewHolder){
        isReloading = true

        holder.reload.apply {
            startAndReloadAnim(this)
            Completable.mergeArray(this.fadeOut(500), this.shrinkIn(500)).subscribe()
        }

        startAndReloadAnim(holder.syncimage)
    }

    private fun startAndReloadAnim(imageView: ImageView){
        (imageView.drawable as Animatable).start()
        (imageView.drawable as Animatable2Compat).onAnimationEnd {
            if (isReloading) (imageView.drawable as Animatable).start()
        }
    }

    private fun stopProgress(holder: ViewHolder){
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

        val seconds = delay/1000
        val min = seconds/60
        val hours = min/60

        val timeToWait = when {
            hours/24 > 0 -> 3600*24
            hours > 0 -> 3600
            min > 0 -> 60
//            seconds > 0 -> 1
            else -> 30
        }

        val ttl = (timeToWait * 1000).toFloat() // in Milli
        val finalTimeToWait = ((1 - ((delay/ttl) % 1))*ttl).toLong()

        // If timeAgo is 3.9 days ago, we want to refresh when it is 4 days ago, so we wait 0.1 day.
        // If timeAgo is 7 years ago, we want to refresh when it is 8 years ago.
        // If timeAgo is 1 min ago, we want to refresh when it is 2 min ago.

        return Completable.timer(finalTimeToWait, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
    }
}
