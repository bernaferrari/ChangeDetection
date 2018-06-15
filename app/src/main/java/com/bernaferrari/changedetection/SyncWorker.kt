package com.bernaferrari.changedetection

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.Worker
import com.bernaferrari.changedetection.data.Site
import com.bernaferrari.changedetection.data.Snap
import com.bernaferrari.changedetection.data.source.SnapsDataSource
import com.orhanobut.logger.Logger
import io.karn.notify.Notify
import kotlinx.coroutines.experimental.launch
import org.threeten.bp.LocalTime

class SyncWorker : Worker() {

    override fun doWork(): Worker.WorkerResult {

        if (inputData.getBoolean(WorkerHelper.WIFI, false)) {
            if (isWifiConnected()) {
                heavyWork()
            } else {
                Logger.d("SyncWorker: wifi is not connected. Try again next time..")
                WorkerHelper.updateWorkerWithConstraints(Application.instance.sharedPrefs("workerPreferences"))
            }
        } else {
            heavyWork()
        }

        return WorkerResult.SUCCESS
    }

    private fun heavyWork() {
        val now = LocalTime.now()
        Logger.d("Doing background work! " + now.hour + ":" + now.minute)
        Injection.provideSitesRepository(this@SyncWorker.applicationContext)
            .getSites { sites ->
                sites.forEach(::reload)
                WorkerHelper.updateWorkerWithConstraints(Application.instance.sharedPrefs("workerPreferences"))
            }
    }

    private fun reload(item: Site) {
        if (!item.isSyncEnabled) {
            return
        }

        launch {
            val serverResult = WorkerHelper.fetchFromServer(item)
            if (serverResult != null) {
                processServerResult(serverResult.first, serverResult.second, item)
            }
        }
    }

    private fun processServerResult(
        contentTypeCharset: String,
        content: ByteArray,
        item: Site
    ) {
        Logger.d("count size -> ${content.size}")

        val newSite = item.copy(
            timestamp = System.currentTimeMillis(),
            isSuccessful = content.isNotEmpty()
        )

        Injection.provideSitesRepository(this@SyncWorker.applicationContext).updateSite(newSite)

        // text/html;charset=UTF-8 needs to become text/html and UTF-8
        val snap = Snap(
            siteId = item.id,
            timestamp = newSite.timestamp,
            contentType = contentTypeCharset.split(";").first(),
            contentCharset = contentTypeCharset.split(";").getOrNull(1)?.split("=")?.getOrNull(1)
                    ?: "",
            contentSize = content.size
        )

        Injection.provideSnapsRepository(this@SyncWorker.applicationContext)
            .saveSnap(snap, content, callback = object : SnapsDataSource.GetSnapsCallback {
                override fun onSnapsLoaded(snap: Snap) {
                    if (!item.isNotificationEnabled) {
                        return
                    }

                    Notify
                        .with(applicationContext)
                        .header {
                            this.icon = R.drawable.vector_sync
//                            this.color = item.colors.second Notify can't handle this for now
                        }
                        .meta {
                            this.clickIntent = PendingIntent.getActivity(
                                applicationContext, 0,
                                Intent(applicationContext, MainActivity::class.java), 0
                            )
                        }
                        .content {
                            title = if (newSite.title.isNullOrBlank()) {
                                "Change detected!"
                            } else {
                                "Change detected on ${newSite.title}!"
                            }
                            text = newSite.url
                        }
                        .show()
                }

                override fun onDataNotAvailable() {

                }
            })
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