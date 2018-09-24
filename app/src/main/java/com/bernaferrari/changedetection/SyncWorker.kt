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
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.IO
import kotlinx.coroutines.experimental.launch
import org.threeten.bp.LocalTime
import com.bernaferrari.changedetection.data.source.Result as DataResult

class SyncWorker(
    context: Context,
    workerParameters: WorkerParameters
) : Worker(context, workerParameters) {

    override fun doWork(): Worker.Result {

        if (inputData.getBoolean(WorkerHelper.WIFI, false) && !isWifiConnected()) {
            Logger.d("SyncWorker: wifi is not connected. Try again next time..")
            WorkerHelper.updateWorkerWithConstraints(Injector.get().sharedPrefs())
        } else {
            heavyWork()
        }

        return Result.SUCCESS
    }

    private fun heavyWork() = GlobalScope.launch(Dispatchers.IO) {
        val now = LocalTime.now()
        Logger.d("Doing background work! " + now.hour + ":" + now.minute)

        Injector.get().sitesRepository().getSites()
            .also { sites -> sites.forEach { it -> reload(it) } }
            .takeIf { sites -> sites.count { it.isSyncEnabled } > 0 }
            // if there is nothing to sync, auto-sync will be automatically disabled
            ?.also { WorkerHelper.updateWorkerWithConstraints(Injector.get().sharedPrefs()) }
    }

    private suspend fun reload(item: Site) {
        if (!item.isSyncEnabled) {
            return
        }

        val serverResult = WorkerHelper.fetchFromServer(item)
        processServerResult(serverResult.first, serverResult.second, item)
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
                .with(applicationContext)
                .header {
                    this.icon = R.drawable.vector_sync
                    this.color = item.colors.first
                }
                .meta {
                    this.clickIntent = PendingIntent.getActivity(
                        applicationContext, 0,
                        Intent(applicationContext, MainActivity::class.java), 0
                    )
                }
                .content {
                    title = if (newSite.title.isNullOrBlank()) {
                        applicationContext.getString(
                            R.string.change_detected_notification_without_title,
                            snap.contentSize.readableFileSize()
                        )
                    } else {
                        applicationContext.getString(
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
            applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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
