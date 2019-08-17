package com.bernaferrari.changedetection

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.work.*
import com.bernaferrari.changedetection.mainnew.OneTimeSync
import com.bernaferrari.changedetection.repo.Site
import com.orhanobut.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit


/**
 * Helps to deal with Work Manager.
 */
object WorkerHelper {

    const val UNIQUEWORK = "work"
    const val WIFI = "wifi"
    const val CHARGING = "charging"
    const val BATTERYNOTLOW = "batteryNotLow"
    const val IDLE = "idle"
    const val DELAY = "delay"

    class ConstraintsRequired(
        val wifi: Boolean,
        val charging: Boolean,
        val batteryNotLow: Boolean,
        val deviceIdle: Boolean
    ) {
        constructor(list: List<String>) : this(
            list.getOrNull(0)?.toBoolean() ?: false,
            list.getOrNull(1)?.toBoolean() ?: false,
            list.getOrNull(2)?.toBoolean() ?: false,
            list.getOrNull(3)?.toBoolean() ?: false
        )
    }

    fun reloadSite(site: Site, context: Context) {
        val work = OneTimeWorkRequestBuilder<OneTimeSync>()
            .setInputData(workDataOf("id" to site.id, "isSingle" to true))
            .addTag("singleEvent")
            .addTag(site.id)
            .build()

        WorkManager.getInstance(context)
            .beginUniqueWork(site.id, ExistingWorkPolicy.REPLACE, work)
            .enqueue()
    }

    suspend fun fetchFromServer(url: String): Pair<String, ByteArray> =
        withContext(Dispatchers.IO) {
        val client = OkHttpClient()

        try {
            val request = Request.Builder()
                .url(url)
                .build()

            val response = client.newCall(request).execute()
            Logger.d("isSuccessful -> ${response.isSuccessful}")
            Logger.d("header -> ${response.headers}")

            val contentTypeAndCharset = response.headers["content-type"] ?: ""

            val bytes = if (contentTypeAndCharset.contains("text")) {
                response.body?.string()?.toByteArray() ?: throw NullPointerException()
                // VERY inefficient solution for this problem:
                // https://stackoverflow.com/questions/50788229/how-to-convert-response-body-from-bytearray-to-string-without-using-okhttp-owns
            } else {
                response.body?.bytes() ?: throw NullPointerException()
            }

            Pair(contentTypeAndCharset, bytes)
        } catch (e: UnknownHostException) {
            // When internet connection is not available OR website doesn't exist
            Logger.e("UnknownHostException for $url")
            Pair("UnknownHostException for $url", byteArrayOf())
        } catch (e: IllegalArgumentException) {
            // When input is "http://"
            Logger.e("IllegalArgumentException for $url")
            Pair("IllegalArgumentException for $url", byteArrayOf())
        } catch (e: SocketTimeoutException) {
            // When site is not available
            Logger.e("SocketTimeoutException for $url")
            Pair("SocketTimeoutException for $url", byteArrayOf())
        } catch (e: Exception) {
            Logger.e("${e.localizedMessage} for $url")
            Pair("${e.localizedMessage} for $url", byteArrayOf())
        }
    }

    fun updateWorkerWithConstraints(
        sharedPrefs: SharedPreferences,
        context: Context,
        cancelCurrentWork: Boolean = true
    ) {
        val constraints = ConstraintsRequired(
            sharedPrefs.getBoolean(WIFI, false),
            sharedPrefs.getBoolean(CHARGING, false),
            sharedPrefs.getBoolean(BATTERYNOTLOW, false),
            sharedPrefs.getBoolean(IDLE, false)
        )

        if (cancelCurrentWork) {
            cancelWork(context)
        }

        if (sharedPrefs.getBoolean("backgroundSync", false)) {
            reloadWorkManager(sharedPrefs.getLong(DELAY, 30), constraints, context)
        }
    }

    private fun reloadWorkManager(
        delay: Long = 15,
        constraints: ConstraintsRequired,
        context: Context
    ) {

        val workerConstraints = Constraints.Builder().apply {
            this.setRequiredNetworkType(NetworkType.CONNECTED)
            if (constraints.batteryNotLow) this.setRequiresBatteryNotLow(true)
            if (constraints.charging) this.setRequiresCharging(true)
            if (Build.VERSION.SDK_INT >= 23 && constraints.deviceIdle) setRequiresDeviceIdle(true)
        }

        val inputData = Data.Builder()
            .putBoolean(WIFI, constraints.wifi)
            .build()

        val syncWork = OneTimeWorkRequest.Builder(OneTimeSync::class.java)
            .addTag(UNIQUEWORK)
            .setInitialDelay(delay, TimeUnit.MINUTES)
            .setConstraints(workerConstraints.build())
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueue(syncWork)
    }

    fun cancelWork(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(UNIQUEWORK)
    }
}
