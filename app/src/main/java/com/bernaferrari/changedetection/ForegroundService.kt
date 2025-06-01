package com.bernaferrari.changedetection

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class ForegroundService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "sync_service_channel"
        private const val CHANNEL_NAME = "Sync Service"
    }

    @Inject
    lateinit var workerHelper: WorkerHelper

    private val syncWork = OneTimeWorkRequest.Builder(SyncWorker::class.java)
        .setInitialDelay(500, TimeUnit.MILLISECONDS)
        .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Toast.makeText(this, "Service starting", Toast.LENGTH_SHORT).show()

        startForeground(NOTIFICATION_ID, createNotification())
        scheduleWork()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background sync service notifications"
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Turbo mode is enabled")
        .setContentText("Checking every X seconds")
        .setSmallIcon(R.drawable.ic_sync)
        .setColor(0xffff0000.toInt())
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        .setOngoing(true)
        .build()

    private fun scheduleWork() {
        WorkManager.getInstance(this)
            .beginUniqueWork(WorkerHelper.UNIQUEWORK, ExistingWorkPolicy.REPLACE, syncWork)
            .enqueue()
    }

    override fun onDestroy() {
        super.onDestroy()
        workerHelper.cancelWork()
        stopForeground(true)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        workerHelper.cancelWork()
        stopSelf()
    }
}
