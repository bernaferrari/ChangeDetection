package com.bernaferrari.changedetection.background

import kotlinx.coroutines.launch
import kotlinx.cinterop.*
import platform.Foundation.NSError
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate
import platform.Foundation.dateByAddingTimeInterval

/**
 * iOS implementation using BGTaskScheduler.
 *
 * IMPORTANT: iOS app must:
 * 1. Enable "Background fetch" and "Background processing" capabilities in Xcode
 * 2. Add task identifiers to Info.plist under BGTaskSchedulerPermittedIdentifiers
 * 3. Call registerBackgroundTasks() in AppDelegate.applicationDidFinishLaunching
 */
internal class IosBackgroundScheduler {
    companion object {
        const val PERIODIC_SYNC_TASK_ID = "com.bernaferrari.changedetection.periodicSync"

        // Minimum interval iOS allows is 15 minutes (in practice, much longer)
        const val MIN_INTERVAL_SECONDS = 15.0 * 60.0
    }

    private var periodicIntervalMinutes: Long = 30

    @OptIn(ExperimentalForeignApi::class)
    fun schedulePeriodicSync(intervalMinutes: Long) {
        this.periodicIntervalMinutes = intervalMinutes

        // Create a background app refresh task request
        val request = BGAppRefreshTaskRequest(identifier = PERIODIC_SYNC_TASK_ID)

        // Set earliest begin date (iOS decides actual timing)
        val intervalSeconds = (intervalMinutes * 60).coerceAtLeast(MIN_INTERVAL_SECONDS.toLong()).toDouble()
        request.earliestBeginDate = NSDate().dateByAddingTimeInterval(intervalSeconds)

        appLog.i("Scheduling iOS periodic sync: every $intervalMinutes min", tag = "BackgroundScheduler")
        // Submit the request
        try {
            submitBackgroundRequest(request)
        } catch (e: Exception) {
            appLog.e("Failed to schedule periodic sync: ${e.message}", e, tag = "BackgroundScheduler")
        }
    }

    fun cancelPeriodicSync() {
        appLog.i("Canceling iOS periodic sync", tag = "BackgroundScheduler")
        BGTaskScheduler.sharedScheduler.cancelTaskRequestWithIdentifier(PERIODIC_SYNC_TASK_ID)
    }

    /** Swift entry point: background refresh runs every active page through the shared sync engine. */
    fun registerDefaultBackgroundWork() {
        registerBackgroundWork(
            work = { org.koin.mp.KoinPlatformTools.defaultContext().get().get<com.bernaferrari.changedetection.sync.IosCheckRunner>().checkAll() },
            reschedule = { org.koin.mp.KoinPlatformTools.defaultContext().get().get<MonitoringManager>().updateSchedule() },
        )
    }

    /**
     * Registers handlers that keep iOS informed: the task completes only after the checks finish,
     * and an expiring task cancels the work instead of being killed mid-write.
     */
    fun registerBackgroundWork(
        work: suspend () -> Unit,
        reschedule: suspend () -> Unit = { schedulePeriodicSync(periodicIntervalMinutes) },
    ) {
        appLog.i("Registering iOS background task handlers", tag = "BackgroundScheduler")
        BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(identifier = PERIODIC_SYNC_TASK_ID, usingQueue = null) { task ->
            if (task == null) return@registerForTaskWithIdentifier
            val job = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
                try {
                    reschedule()
                    work()
                    task.setTaskCompletedWithSuccess(true)
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    task.setTaskCompletedWithSuccess(false)
                    throw cancelled
                } catch (error: Exception) {
                    appLog.w("iOS background checks failed", error)
                    task.setTaskCompletedWithSuccess(false)
                }
            }
            task.expirationHandler = { job.cancel() }
        }
    }
}

/** Apple reports scheduling failures through BOOL and NSError, rather than throwing. */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal fun submitBackgroundRequest(
    request: BGAppRefreshTaskRequest,
    submit: (BGAppRefreshTaskRequest, CPointer<ObjCObjectVar<NSError?>>) -> Boolean = { task, error -> BGTaskScheduler.sharedScheduler.submitTaskRequest(task, error) },
) = memScoped {
    val error = alloc<ObjCObjectVar<NSError?>>()
    error.value = null
    check(submit(request, error.ptr)) {
        val reason = error.value
        "Could not schedule ${request.identifier}: ${reason?.domain ?: "unknown error"} (${reason?.code ?: 0}): ${reason?.localizedDescription ?: "request rejected"}"
    }
}

internal val iosBackgroundScheduler = IosBackgroundScheduler()

fun registerIosBackgroundTasks() = iosBackgroundScheduler.registerDefaultBackgroundWork()

private val appLog = com.bernaferrari.changedetection.logging.AppLog.tagged("IosBackgroundScheduler")
