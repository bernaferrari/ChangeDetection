package com.bernaferrari.changedetection

import android.content.SharedPreferences
import android.os.Build
import androidx.work.*
import com.bernaferrari.changedetection.data.Site
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

    suspend fun fetchFromServer(item: Site): Pair<String, ByteArray> = withContext(Dispatchers.IO) {
        val client = OkHttpClient()

        try {
            val request = Request.Builder()
                .url(item.url)
                .build()

            val response = client.newCall(request).execute()
            Logger.d("isSuccessful -> ${response.isSuccessful}")
            Logger.d("header -> ${response.headers()}")

            val contentTypeAndCharset = response.headers().get("content-type") ?: ""

            val bytes = if (contentTypeAndCharset.contains("text")) {
                response.body()?.string()?.toByteArray() ?: throw NullPointerException()
                // VERY inefficient solution for this problem:
                // https://stackoverflow.com/questions/50788229/how-to-convert-response-body-from-bytearray-to-string-without-using-okhttp-owns
            } else {
                response.body()?.bytes() ?: throw NullPointerException()
            }

            Pair(contentTypeAndCharset, bytes)
        } catch (e: UnknownHostException) {
            // When internet connection is not available OR website doesn't exist
            Logger.e("UnknownHostException for ${item.url}")
            Pair("UnknownHostException for ${item.url}", byteArrayOf())
        } catch (e: IllegalArgumentException) {
            // When input is "http://"
            Logger.e("IllegalArgumentException for ${item.url}")
            Pair("IllegalArgumentException for ${item.url}", byteArrayOf())
        } catch (e: SocketTimeoutException) {
            // When site is not available
            Logger.e("SocketTimeoutException for ${item.url}")
            Pair("SocketTimeoutException for ${item.url}", byteArrayOf())
        } catch (e: Exception) {
            Logger.e("${e.localizedMessage} for ${item.url}")
            Pair("${e.localizedMessage} for ${item.url}", byteArrayOf())
        }
    }

    fun updateWorkerWithConstraints(
        sharedPrefs: SharedPreferences,
        cancelCurrentWork: Boolean = true
    ) {
        val constraints = ConstraintsRequired(
            sharedPrefs.getBoolean(WorkerHelper.WIFI, false),
            sharedPrefs.getBoolean(WorkerHelper.CHARGING, false),
            sharedPrefs.getBoolean(WorkerHelper.BATTERYNOTLOW, false),
            sharedPrefs.getBoolean(WorkerHelper.IDLE, false)
        )

        if (cancelCurrentWork) {
            cancelWork()
        }
        WorkManager.getInstance().pruneWork()

        if (sharedPrefs.getBoolean("backgroundSync", false)) {
            reloadWorkManager(sharedPrefs.getLong(WorkerHelper.DELAY, 30), constraints)
        }
    }

    private fun reloadWorkManager(delay: Long = 15, constraints: ConstraintsRequired) {

        val workerConstraints = Constraints.Builder().apply {
            this.setRequiredNetworkType(NetworkType.CONNECTED)
            if (constraints.batteryNotLow) this.setRequiresBatteryNotLow(true)
            if (constraints.charging) this.setRequiresCharging(true)
            if (Build.VERSION.SDK_INT >= 23 && constraints.deviceIdle) setRequiresDeviceIdle(true)
        }

        val inputData = Data.Builder()
            .putBoolean(WIFI, constraints.wifi)
            .build()

        val syncWork = OneTimeWorkRequest.Builder(SyncWorker::class.java)
            .addTag(UNIQUEWORK)
            .setInitialDelay(delay, TimeUnit.MINUTES)
            .setConstraints(workerConstraints.build())
            .setInputData(inputData)
            .build()

        WorkManager.getInstance().enqueue(syncWork)
    }

    fun cancelWork() {
        WorkManager.getInstance().cancelAllWorkByTag(WorkerHelper.UNIQUEWORK)
    }
}
