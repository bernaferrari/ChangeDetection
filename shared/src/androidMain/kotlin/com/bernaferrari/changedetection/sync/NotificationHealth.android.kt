package com.bernaferrari.changedetection.sync

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.*
import com.bernaferrari.changedetection.library.R

internal fun readNotificationHealth(context: Context, channelId: String = AndroidChangeNotifier.CHANNEL_ID): NotificationHealth {
    val granted = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    val manager = context.getSystemService(NotificationManager::class.java)
    val channel = manager.getNotificationChannel(channelId)
    return notificationHealth(granted, NotificationManagerCompat.from(context).areNotificationsEnabled(), channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE)
}

@Composable
internal actual fun notificationHealthDescription(): String? {
    val context = LocalContext.current
    val reader = remember(context) { { readNotificationHealth(context) } }
    val status by rememberNotificationHealth(reader)
    return status?.let {
        context.getString(
            when (it) {
                NotificationHealth.Enabled -> R.string.change_notification_enabled
                NotificationHealth.PermissionDenied -> R.string.change_notification_permission
                NotificationHealth.AppDisabled -> R.string.change_notification_app_disabled
                NotificationHealth.ChannelDisabled -> R.string.change_notification_channel_disabled
            },
        )
    }
}

@Composable
internal fun rememberNotificationHealth(read: () -> NotificationHealth): State<NotificationHealth?> {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val latest by rememberUpdatedState(read)
    return produceState<NotificationHealth?>(null, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            value = withContext(Dispatchers.Default) { latest() }
            awaitCancellation()
        }
    }
}
