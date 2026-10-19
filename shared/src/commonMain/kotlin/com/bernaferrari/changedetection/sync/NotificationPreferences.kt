package com.bernaferrari.changedetection.sync

import androidx.datastore.preferences.core.booleanPreferencesKey

/** Saved page images can contain private information. Absent means disabled on every install. */
object NotificationPreferences {
    val IMAGE_PREVIEWS = booleanPreferencesKey("notification_image_previews")
}
