package com.bernaferrari.changedetection

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.bernaferrari.changedetection.data.Site
import com.bernaferrari.changedetection.data.Snap
import com.bernaferrari.changedetection.extensions.findCharset
import com.bernaferrari.changedetection.extensions.readableFileSize
import com.orhanobut.logger.Logger
import io.karn.notify.Notify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.threeten.bp.LocalTime
import com.bernaferrari.changedetection.data.source.Result as DataResult

class SyncWorker(
    val context: Context,
    workerParameters: WorkerParameters
) : Worker(context, workerParameters) {

    private val debugLog = StringBuilder()
    private val isDebugEnabled = false //Injector.get().sharedPrefs().getBoolean("debug", true)

    override fun doWork(): Result {

        if (inputData.getBoolean(WorkerHelper.WIFI, false) && !isWifiConnected()) {
            Logger.d("SyncWorker: wifi is not connected. Try again next time..")
            WorkerHelper.updateWorkerWithConstraints(Injector.get().sharedPrefs(), false)
        } else {
            heavyWork()
        }

        return Result.SUCCESS
    }

    private fun heavyWork() = runBlocking(Dispatchers.IO) {
        val now = LocalTime.now()
        Logger.d("Doing background work! " + now.hour + ":" + now.minute)

        debugLog.setLength(0)

        Injector.get().sitesRepository().getSites()
            .also { sites -> sites.forEach { it -> reload(it) } }
            .takeIf { sites -> sites.count { it.isSyncEnabled } > 0 }
            // if there is nothing to sync, auto-sync will be automatically disabled
            ?.also { WorkerHelper.updateWorkerWithConstraints(Injector.get().sharedPrefs(), false) }

        if (isDebugEnabled) {
            Notify.with(context)
                .meta {
                    this.clickIntent = PendingIntent.getActivity(
                        context, 0,
                        Intent(context, MainActivity::class.java), 0
                    )
                }
                .asBigText {
                    title = "[Debug] There has been a sync"
                    text = "Expand to see the full log"
                    expandedText = "..."
                    bigText = debugLog
                }
                .show()
        }
    }

    private suspend fun reload(item: Site) {
        if (!item.isSyncEnabled) return

        val (contentTypeAndCharset, bytes) = WorkerHelper.fetchFromServer(item)

        if (isDebugEnabled) {
            if (bytes.isEmpty()) {
                debugLog.append("• $contentTypeAndCharset\n")
            } else {
                val title = item.title?.takeIf { it.isNotBlank() } ?: item.url
                debugLog.append("• ${bytes.size.readableFileSize()} from $title\n")
            }
        }

        processServerResult(contentTypeAndCharset, bytes, item)
    }

    private suspend fun processServerResult(
        contentTypeCharset: String,
        content: ByteArray,
        item: Site
    ) {
        Logger.d("count size -> ${content.size}")

        val newSite = item.copy(
            timestamp = System.currentTimeMillis(),
            isSuccessful = content.isNotEmpty()
        )

        Injector.get().sitesRepository().updateSite(newSite)

        // text/html;charset=UTF-8 needs to become text/html and UTF-8
        val snap = Snap(
            siteId = item.id,
            timestamp = newSite.timestamp,
            contentType = contentTypeCharset.split(";").first(),
            contentCharset = contentTypeCharset.findCharset(),
            contentSize = content.size
        )

        val snapSavedResult = Injector.get().snapsRepository().saveSnap(snap, content)

        if (snapSavedResult is DataResult.Success) {
            if (!item.isNotificationEnabled) {
                return
            }

            Notify
                .with(context)
                .header {
                    this.icon = R.drawable.ic_sync
                    this.color = item.colors.first
                }
                .meta {
                    this.clickIntent = PendingIntent.getActivity(
                        context, 0,
                        Intent(context, MainActivity::class.java), 0
                    )
                }
                .content {
                    title = if (newSite.title.isNullOrBlank()) {
                        context.getString(
                            R.string.change_detected_notification_without_title,
                            snap.contentSize.readableFileSize()
                        )
                    } else {
                        context.getString(
                            R.string.change_detected_notification_with_title,
                            newSite.title,
                            snap.contentSize.readableFileSize()
                        )
                    }
                    text = newSite.url
                }
                .show()
        }

    }

    private fun isWifiConnected(): Boolean {
        // From https://stackoverflow.com/a/34576776/4418073
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworks = connectivityManager.allNetworks
        for (network in activeNetworks) {
            if (connectivityManager.getNetworkInfo(network).isConnected) {
                val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    return true
                }
            }
        }
        return false
    }
}
