package com.bernaferrari.changedetection.mainnew

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bernaferrari.changedetection.Injector
import com.bernaferrari.changedetection.MainActivity
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.WorkerHelper
import com.bernaferrari.changedetection.addedit.fetchFromWebView
import com.bernaferrari.changedetection.extensions.findCharset
import com.bernaferrari.changedetection.extensions.readableFileSize
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.WebResult
import com.bernaferrari.changedetection.ui.CustomWebView
import com.orhanobut.logger.Logger
import io.karn.notify.Notify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalTime
import com.bernaferrari.changedetection.repo.source.Result as DataResult

class OneTimeSync(
    val context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    private val debugLog = StringBuilder()
    private val isDebugEnabled = false //Injector.get().sharedPrefs().getBoolean("debug", true)

    override suspend fun doWork(): Result {

        val now = LocalTime.now()
        Logger.d("Doing background work! " + now.hour + ":" + now.minute)

        debugLog.setLength(0)

        val item = inputData.getString("id") ?: ""
        val site = Injector.get().sitesRepository().getSiteById(item)
        reload(site!!)
        debugMode()

        return Result.success()
    }

    private suspend fun reload(item: Site) {

        if (item.isBrowser) {
            withContext(Dispatchers.Main) {

                val webView = CustomWebView(context)
                val result: WebResult<ByteArray> = fetchFromWebView(
                    item.url,
                    context,
                    webView
                )

                webView.destroy()

                when (result) {
                    is WebResult.Success -> {
                        processServerResult("text/html;charset=UTF-8", result.data, item)
                    }
                }
            }
        } else {
            val (contentTypeAndCharset, bytes) = WorkerHelper.fetchFromServer(item.url)

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

        // This will be used to verify if there is a previous snap.
        // We don't want to show notification if user just added a website to be tracked.
        val lastSnap = Injector.get().snapsRepository().getMostRecentSnap(item.id)

        val snapSavedResult = Injector.get().snapsRepository().saveSnap(snap, content)

        if (snapSavedResult is DataResult.Success && lastSnap != null) {
            notifyUser(item, snap, newSite)
        }
    }

    private fun notifyUser(item: Site, snap: Snap, newSite: Site) {
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

    fun debugMode() {
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
}
