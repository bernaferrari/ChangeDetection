package com.bernaferrari.changedetection

import android.content.SharedPreferences
import android.os.Build
import androidx.work.*
import com.bernaferrari.changedetection.data.Site
import com.orhanobut.logger.Logger
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

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

    internal fun customFetchFromServer(item: Site): Triple<String, ByteArray, String> {
        val client = OkHttpClient()

        return try {
            val request = Request.Builder()
                .url(item.url)
                .build()

            val response = client.newCall(request).execute()
            Logger.d("isSuccessful -> ${response.isSuccessful}")
            Logger.d("header -> ${response.headers()}")

            val contentTypeAndCharset = response.headers().get("content-type") ?: ""

            val bytes = if (contentTypeAndCharset.contains("text")) {
                response.body()!!.string()
                    .toByteArray() // VERY inefficient solution for this problem:
                // https://stackoverflow.com/questions/50788229/how-to-convert-response-body-from-bytearray-to-string-without-using-okhttp-owns
            } else {
                response.body()!!.bytes()
            }

            Triple(contentTypeAndCharset, bytes, "")
        } catch (e: UnknownHostException) {
            // When internet connection is not available OR website doesn't exist
            Logger.e("UnknownHostException for ${item.url}")
            Triple("", byteArrayOf(), e.localizedMessage)
        } catch (e: IllegalArgumentException) {
            // When input is "http://"
            Logger.e("IllegalArgumentException for ${item.url}")
            Triple("", byteArrayOf(), e.localizedMessage)
        } catch (e: SocketTimeoutException) {
            // When site is not available
            Logger.e("SocketTimeoutException for ${item.url}")
            Triple("", byteArrayOf(), e.localizedMessage)
        } catch (e: Exception) {
            Logger.e("New Exception for ${item.url}")
            Triple("", byteArrayOf(), e.localizedMessage)
        }
    }

    fun fetchFromServer(item: Site): Pair<String, ByteArray> {
        val client = OkHttpClient()

        return try {
            val request = Request.Builder()
                .url(item.url)
                .build()

            val response = client.newCall(request).execute()
            Logger.d("isSuccessful -> ${response.isSuccessful}")
            Logger.d("header -> ${response.headers()}")

            val contentTypeAndCharset = response.headers().get("content-type") ?: ""

            val bytes = if (contentTypeAndCharset.contains("text")) {
                response.body()!!.string()
                    .toByteArray() // VERY inefficient solution for this problem:
                // https://stackoverflow.com/questions/50788229/how-to-convert-response-body-from-bytearray-to-string-without-using-okhttp-owns
            } else {
                response.body()!!.bytes()
            }

            Pair(contentTypeAndCharset, bytes)
        } catch (e: UnknownHostException) {
            // When internet connection is not available OR website doesn't exist
            Logger.e("UnknownHostException for ${item.url}")
            Pair("", byteArrayOf())
        } catch (e: IllegalArgumentException) {
            // When input is "http://"
            Logger.e("IllegalArgumentException for ${item.url}")
            Pair("", byteArrayOf())
        } catch (e: SocketTimeoutException) {
            // When site is not available
            Logger.e("SocketTimeoutException for ${item.url}")
            Pair("", byteArrayOf())
        } catch (e: Exception) {
            Logger.e("New Exception for ${item.url}")
            Pair("", byteArrayOf())
        }
    }

    fun updateWorkerWithConstraints(sharedPrefs: SharedPreferences) {
        val constraints = ConstraintsRequired(
            sharedPrefs.getBoolean(WorkerHelper.WIFI, false),
            sharedPrefs.getBoolean(WorkerHelper.CHARGING, false),
            sharedPrefs.getBoolean(WorkerHelper.BATTERYNOTLOW, false),
            sharedPrefs.getBoolean(WorkerHelper.IDLE, false)
        )

        cancelWork()
        if (sharedPrefs.getBoolean("backgroundSync", false)) {
            reloadWorkManager(sharedPrefs.getLong(WorkerHelper.DELAY, 30), constraints)
        }
    }

    private fun reloadWorkManager(delay: Long = 15, constraints: ConstraintsRequired) {
        cancelWork()

        val workerConstraints = Constraints.Builder()
        workerConstraints.setRequiredNetworkType(NetworkType.CONNECTED)

        val inputData = Data.Builder()
            .putBoolean(WIFI, constraints.wifi)
            .build()

        if (constraints.batteryNotLow) workerConstraints.setRequiresBatteryNotLow(true)
        if (constraints.charging) workerConstraints.setRequiresCharging(true)
        if (Build.VERSION.SDK_INT >= 23 && constraints.deviceIdle) {
            workerConstraints.setRequiresDeviceIdle(true)
        }

        val photoWork = OneTimeWorkRequest.Builder(
            SyncWorker::class.java
        )
            .setInitialDelay(delay, TimeUnit.MINUTES)
            .setConstraints(workerConstraints.build())
            .setInputData(inputData)
            .build()

        WorkManager.getInstance()
            .beginUniqueWork(WorkerHelper.UNIQUEWORK, ExistingWorkPolicy.REPLACE, photoWork)
            .enqueue()
    }

    fun cancelWork() {
        WorkManager.getInstance().cancelUniqueWork(WorkerHelper.UNIQUEWORK)
    }
}