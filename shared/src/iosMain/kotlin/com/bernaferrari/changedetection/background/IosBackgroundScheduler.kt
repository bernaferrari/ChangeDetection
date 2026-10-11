/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.background

import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import kotlinx.cinterop.ExperimentalForeignApi
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
class IosBackgroundScheduler : BackgroundScheduler {
    companion object {
        const val PERIODIC_SYNC_TASK_ID = "com.bernaferrari.changedetection.periodicSync"
        const val QUICK_MONITOR_TASK_ID = "com.bernaferrari.changedetection.quickMonitor"

        // Minimum interval iOS allows is 15 minutes (in practice, much longer)
        const val MIN_INTERVAL_SECONDS = 15.0 * 60.0
    }

    private var periodicIntervalMinutes: Long = 30
    private var periodicConstraints: SyncConstraints = SyncConstraints()

    @OptIn(ExperimentalForeignApi::class)
    override fun schedulePeriodicSync(
        intervalMinutes: Long,
        constraints: SyncConstraints,
    ) {
        this.periodicIntervalMinutes = intervalMinutes
        this.periodicConstraints = constraints

        // Create a background app refresh task request
        val request = BGAppRefreshTaskRequest(identifier = PERIODIC_SYNC_TASK_ID)

        // Set earliest begin date (iOS decides actual timing)
        val intervalSeconds = (intervalMinutes * 60).coerceAtLeast(MIN_INTERVAL_SECONDS.toLong()).toDouble()
        request.earliestBeginDate = NSDate().dateByAddingTimeInterval(intervalSeconds)

        Napier.i("Scheduling iOS periodic sync: every $intervalMinutes min", tag = "BackgroundScheduler")
        // Submit the request
        try {
            BGTaskScheduler.sharedScheduler.submitTaskRequest(request, error = null)
        } catch (e: Exception) {
            Napier.e("Failed to schedule periodic sync: ${e.message}", e, tag = "BackgroundScheduler")
        }
    }

    override fun cancelPeriodicSync() {
        Napier.i("Canceling iOS periodic sync", tag = "BackgroundScheduler")
        BGTaskScheduler.sharedScheduler.cancelTaskRequestWithIdentifier(PERIODIC_SYNC_TASK_ID)
    }

    override fun requestImmediateSync(siteId: String) {
        // On iOS, we can't really do "immediate" background work
        // The app needs to be in foreground
        Napier.d("iOS: Immediate sync requested for $siteId - handle in foreground", tag = "BackgroundScheduler")
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun startQuickMonitor(
        siteIds: List<String>,
        intervalSeconds: Int,
    ) {
        // iOS doesn't support rapid polling in background
        Napier.i("Scheduling iOS quick monitor task", tag = "BackgroundScheduler")

        val request = BGAppRefreshTaskRequest(identifier = QUICK_MONITOR_TASK_ID)
        request.earliestBeginDate = NSDate().dateByAddingTimeInterval(MIN_INTERVAL_SECONDS)

        try {
            BGTaskScheduler.sharedScheduler.submitTaskRequest(request, error = null)
        } catch (e: Exception) {
            Napier.e("Failed to schedule quick monitor: ${e.message}", e, tag = "BackgroundScheduler")
        }
    }

    override fun stopQuickMonitor() {
        BGTaskScheduler.sharedScheduler.cancelTaskRequestWithIdentifier(QUICK_MONITOR_TASK_ID)
    }

    override fun isPeriodicSyncScheduled(): Boolean {
        // iOS doesn't provide a direct way to check pending tasks
        // We'd need to track this ourselves
        return true // Assume scheduled if user enabled it
    }

    /**
     * Must be called from AppDelegate to register background task handlers.
     *
     * Usage in Swift:
     * ```swift
     * func application(_ application: UIApplication,
     *                  didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
     *     IosBackgroundScheduler.Companion.shared.registerBackgroundTasks()
     *     return true
     * }
     * ```
     */
    fun registerBackgroundTasks(onSync: () -> Unit) {
        registerBackgroundWork(work = { onSync() })
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
        reschedule: suspend () -> Unit = { schedulePeriodicSync(periodicIntervalMinutes, periodicConstraints) },
    ) {
        Napier.i("Registering iOS background task handlers", tag = "BackgroundScheduler")
        listOf(PERIODIC_SYNC_TASK_ID, QUICK_MONITOR_TASK_ID).forEach { identifier ->
            BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(identifier = identifier, usingQueue = null) { task ->
                if (task == null) return@registerForTaskWithIdentifier
                val job = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
                    try {
                        if (identifier == PERIODIC_SYNC_TASK_ID) reschedule()
                        work()
                        task.setTaskCompletedWithSuccess(true)
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        task.setTaskCompletedWithSuccess(false)
                        throw cancelled
                    } catch (error: Exception) {
                        Napier.w("iOS background checks failed", error)
                        task.setTaskCompletedWithSuccess(false)
                    }
                }
                task.expirationHandler = { job.cancel() }
            }
        }
    }
}

// Provide the factory function for iOS
private val sharedScheduler = IosBackgroundScheduler()

fun registerIosBackgroundTasks() = sharedScheduler.registerDefaultBackgroundWork()

actual fun createBackgroundScheduler(): BackgroundScheduler = sharedScheduler
