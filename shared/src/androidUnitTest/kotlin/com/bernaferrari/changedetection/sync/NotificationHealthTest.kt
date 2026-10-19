package com.bernaferrari.changedetection.sync

import org.junit.Assert.*
import org.junit.Test

class NotificationHealthTest {
    @Test fun permissionAppAndChannelFailuresAreDistinct() {
        assertEquals(NotificationHealth.PermissionDenied, notificationHealth(false, false, true))
        assertEquals(NotificationHealth.AppDisabled, notificationHealth(true, false, true))
        assertEquals(NotificationHealth.ChannelDisabled, notificationHealth(true, true, false))
        assertEquals(NotificationHealth.Enabled, notificationHealth(true, true, true))
    }

    @Test fun imagePreviewsAreAnExplicitSeparatePreference() {
        val empty = androidx.datastore.preferences.core.emptyPreferences()
        assertFalse(empty[NotificationPreferences.IMAGE_PREVIEWS] ?: false)
    }
}
