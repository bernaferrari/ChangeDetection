package com.bernaferrari.changedetection

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.settings.SettingsViewModel
import com.orhanobut.logger.Logger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val okHttpClient: OkHttpClient
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val siteId = inputData.getString("id")
            val isSingle = inputData.getBoolean("isSingle", false)
            val wifiRequired = inputData.getBoolean(WorkerHelper.WIFI, false)

            Logger.d("Starting sync work for site: $siteId, isSingle: $isSingle, wifi: $wifiRequired")

            // Your existing sync logic would go here
            Result.success()
        } catch (e: Exception) {
            Logger.e("Sync work failed: ${e.localizedMessage}")
            Result.retry()
        }
    }
}

@Singleton
open class WorkerHelper @Inject constructor(
    private val workManager: WorkManager,
    private val okHttpClient: OkHttpClient
) {

    companion object {
        const val UNIQUEWORK = "work"
        const val WIFI = "wifi"
        const val CHARGING = "charging"
        const val BATTERYNOTLOW = "batteryNotLow"
        const val IDLE = "idle"
        const val DELAY = "delay"
        const val BACKGROUND_SYNC = "backgroundSync"
    }

    // Add instance property for easier access
    val UNIQUEWORK = Companion.UNIQUEWORK

    suspend fun updateWorkerWithConstraints(
        dataStore: DataStore<Preferences>,
        cancelCurrentWork: Boolean = true
    ) {
        val preferences = dataStore.data.first()
        val constraints = WorkConstraints.fromDataStore(dataStore)

        if (cancelCurrentWork) {
            cancelWork()
        }

        if (preferences[SettingsViewModel.BACKGROUND_SYNC_KEY] == true) {
            val delay = preferences[SettingsViewModel.SYNC_INTERVAL_KEY] ?: 30L
            schedulePeriodicWork(delay, constraints)
        }
    }

    open fun cancelWork() {
        workManager.cancelAllWorkByTag(UNIQUEWORK)
    }

    private fun schedulePeriodicWork(
        intervalMinutes: Long,
        constraints: WorkConstraints
    ) {
        val workerConstraints = Constraints.Builder().apply {
            setRequiredNetworkType(if (constraints.wifi) NetworkType.UNMETERED else NetworkType.CONNECTED)
            if (constraints.batteryNotLow) setRequiresBatteryNotLow(true)
            if (constraints.charging) setRequiresCharging(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && constraints.deviceIdle) {
                setRequiresDeviceIdle(true)
            }
        }.build()

        val inputData = workDataOf(
            WIFI to constraints.wifi,
            CHARGING to constraints.charging,
            BATTERYNOTLOW to constraints.batteryNotLow,
            IDLE to constraints.deviceIdle
        )

        val periodicWork = PeriodicWorkRequestBuilder<SyncWorker>(intervalMinutes, TimeUnit.MINUTES)
            .addTag(UNIQUEWORK)
            .setConstraints(workerConstraints)
            .setInputData(inputData)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUEWORK,
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicWork
        )
    }

    fun reloadSite(site: Site) {
        val work = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(workDataOf("id" to site.id, "isSingle" to true))
            .addTag("singleEvent")
            .addTag(site.id)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        workManager
            .beginUniqueWork(site.id, ExistingWorkPolicy.REPLACE, work)
            .enqueue()
    }

    suspend fun fetchFromServer(url: String): FetchResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "ChangeDetection/1.0")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                Logger.d("Response successful: ${response.isSuccessful} for $url")
                Logger.d("Response headers: ${response.headers}")

                val contentType = response.header("content-type", "") ?: ""
                val bytes = response.body?.use { body ->
                    if (contentType.contains("text", ignoreCase = true)) {
                        body.string().toByteArray()
                    } else {
                        body.bytes()
                    }
                } ?: throw IllegalStateException("Response body is null")

                FetchResult.Success(contentType, bytes)
            }
        } catch (e: UnknownHostException) {
            Logger.e("Network error for $url: ${e.localizedMessage}")
            FetchResult.Error("Network not available: $url")
        } catch (e: IllegalArgumentException) {
            Logger.e("Invalid URL: $url")
            FetchResult.Error("Invalid URL: $url")
        } catch (e: SocketTimeoutException) {
            Logger.e("Timeout for $url")
            FetchResult.Error("Request timeout: $url")
        } catch (e: Exception) {
            Logger.e("Unexpected error for $url: ${e.localizedMessage}")
            FetchResult.Error("${e.localizedMessage ?: "Unknown error"}: $url")
        }
    }

    fun getWorkInfo() = workManager.getWorkInfosByTagLiveData(UNIQUEWORK)
}

data class WorkConstraints(
    val wifi: Boolean = false,
    val charging: Boolean = false,
    val batteryNotLow: Boolean = false,
    val deviceIdle: Boolean = false
) {
    companion object {
        suspend fun fromDataStore(dataStore: DataStore<Preferences>): WorkConstraints {
            val preferences = dataStore.data.first()
            return WorkConstraints(
                wifi = preferences[SettingsViewModel.REQUIRE_WIFI_KEY] ?: false,
                charging = preferences[SettingsViewModel.REQUIRE_CHARGING_KEY] ?: false,
                batteryNotLow = preferences[SettingsViewModel.REQUIRE_BATTERY_NOT_LOW_KEY] ?: false,
                deviceIdle = preferences[SettingsViewModel.REQUIRE_DEVICE_IDLE_KEY] ?: false
            )
        }
    }
}

sealed class FetchResult {
    data class Success(val contentType: String, val data: ByteArray) : FetchResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Success
            if (contentType != other.contentType) return false
            if (!data.contentEquals(other.data)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = contentType.hashCode()
            result = 31 * result + data.contentHashCode()
            return result
        }
    }

    data class Error(val message: String) : FetchResult()
}
