package com.bernaferrari.changedetection

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import androidx.core.app.NotificationCompat
import com.bernaferrari.changedetection.background.TurboSession
import com.bernaferrari.changedetection.background.TurboNotificationState
import com.bernaferrari.changedetection.background.notificationStates
import com.bernaferrari.changedetection.background.TurboEndReason
import com.bernaferrari.changedetection.background.TURBO_DURATION_MINUTES
import com.bernaferrari.changedetection.background.TURBO_INTERVAL_SECONDS
import com.bernaferrari.changedetection.library.R
import io.github.aakira.napier.Napier
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Android lifetime shell. TurboSession owns cadence, selection, cancellation and deadline. */
class QuickMonitorService :
    Service(),
    KoinComponent {
    private val session: TurboSession by inject()
    private val workerHelper: WorkerHelper by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var wakeLock: PowerManager.WakeLock? = null
    private var observer: Job? = null

    companion object {
        const val CHANNEL_ID = "quick_monitor_channel_v2"
        const val NOTIFICATION_ID = 2001
        const val ACTION_STOP = "com.bernaferrari.changedetection.STOP_QUICK_MONITOR"
        const val EXTRA_SITE_IDS = "site_ids"
        const val EXTRA_INTERVAL = "interval_seconds"
        const val EXTRA_DURATION = "duration_minutes"

        fun start(context: Context, siteIds: List<String>, intervalSeconds: Int = 30, durationMinutes: Int = 180) {
            require(siteIds.isNotEmpty())
            require(intervalSeconds in TURBO_INTERVAL_SECONDS)
            require(durationMinutes in TURBO_DURATION_MINUTES)
            context.startForegroundService(
                Intent(context, QuickMonitorService::class.java).apply {
                    putStringArrayListExtra(EXTRA_SITE_IDS, ArrayList(siteIds))
                    putExtra(EXTRA_INTERVAL, intervalSeconds)
                    putExtra(EXTRA_DURATION, durationMinutes)
                },
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, QuickMonitorService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // Instantiate the application-lifetime state publisher, also used by the widget.
        workerHelper.quickMonitorState
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            finish(TurboEndReason.Stopped)
            return START_NOT_STICKY
        }
        val ids = intent?.getStringArrayListExtra(EXTRA_SITE_IDS)?.toSet().orEmpty()
        val interval = intent?.getIntExtra(EXTRA_INTERVAL, 30) ?: 30
        val duration = intent?.getIntExtra(EXTRA_DURATION, 180) ?: 180
        if (ids.isEmpty() || interval !in TURBO_INTERVAL_SECONDS || duration !in TURBO_DURATION_MINUTES) {
            finish(TurboEndReason.NoSites)
            return START_NOT_STICKY
        }
        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, createNotification(TurboNotificationState(true, ids.size, interval)), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            if (!session.state.value.isActive) {
                wakeLock = (getSystemService(POWER_SERVICE) as PowerManager)
                    .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ChangeDetection:Turbo")
                    .apply {
                        setReferenceCounted(false)
                        acquire(duration * 60_000L)
                    }
            }
            session.start(ids, interval, duration)
            if (observer == null) {
                observer = serviceScope.launch {
                    session.state.notificationStates().collect { state ->
                        if (!state.isActive) {
                            finish(state.endReason ?: TurboEndReason.Interrupted)
                        } else {
                            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                                .notify(NOTIFICATION_ID, createNotification(state))
                        }
                    }
                }
            }
        } catch (error: Exception) {
            Napier.e("Could not start Turbo", error)
            finish(TurboEndReason.StartFailed)
        }
        return START_NOT_STICKY
    }

    override fun onTimeout(startId: Int, fgsType: Int) = finish(TurboEndReason.SystemLimit)

    private fun finish(reason: TurboEndReason) {
        session.stop(reason)
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    override fun onDestroy() {
        if (session.state.value.isActive) session.stop(TurboEndReason.Interrupted)
        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(state: TurboNotificationState): Notification {
        Napier.d("Creating notification for ${state.pageCount} sites")

        val stopIntent = Intent(this, QuickMonitorService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val mainIntent = Intent(this, MainActivityProvider.getMainActivityClass())
        val mainPendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.quick_monitor_notification_title))
            .setContentText(
                resources.getQuantityString(
                    R.plurals.quick_monitor_notification_subtitle,
                    state.pageCount,
                    state.pageCount,
                    state.intervalSeconds,
                ),
            )
            .setSmallIcon(R.drawable.ic_notification_change)
            .setContentIntent(mainPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(0, getString(R.string.action_stop_monitoring), stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.quick_monitor_title),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.quick_monitor_channel_description)
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
                setSound(null, null)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Napier.d("Notification channel created: $CHANNEL_ID")
        }
    }
}
