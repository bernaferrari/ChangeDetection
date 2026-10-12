package com.bernaferrari.changedetection.background

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Only notification content and terminal lifecycle state cross into NotificationManager. */
internal data class TurboNotificationState(
    val isActive: Boolean,
    val pageCount: Int,
    val intervalSeconds: Int,
    val endReason: TurboEndReason? = null,
)

internal fun Flow<TurboState>.notificationStates(): Flow<TurboNotificationState> = map {
    TurboNotificationState(it.isActive, it.siteIds.size, it.intervalSeconds, it.endReason)
}.distinctUntilChanged()
