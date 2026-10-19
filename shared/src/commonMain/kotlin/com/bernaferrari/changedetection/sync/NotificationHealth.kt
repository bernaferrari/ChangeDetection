package com.bernaferrari.changedetection.sync

import androidx.compose.runtime.Composable

internal enum class NotificationHealth { Enabled, PermissionDenied, AppDisabled, ChannelDisabled }
internal fun notificationHealth(permission: Boolean, app: Boolean, channel: Boolean): NotificationHealth = when {
    !permission -> NotificationHealth.PermissionDenied
    !app -> NotificationHealth.AppDisabled
    !channel -> NotificationHealth.ChannelDisabled
    else -> NotificationHealth.Enabled
}

/** Read-only platform status, refreshed after returning from system settings. */
@Composable
internal expect fun notificationHealthDescription(): String?
