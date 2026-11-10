package com.bernaferrari.changedetection.backup

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class BackupSettingsTest {
    @Test fun onlyUserSettingsAreExported() {
        val exported = BackupSettings.read(
            preferencesOf(
                booleanPreferencesKey("background_sync") to false,
                longPreferencesKey("sync_interval") to 120L,
                stringPreferencesKey("theme_mode") to "DARK",
                booleanPreferencesKey("quick_monitor_active") to true,
                stringPreferencesKey("monitoring_batch_cursor") to "abc",
            ),
        )
        assertEquals(mapOf("background_sync" to "false", "sync_interval" to "120", "theme_mode" to "DARK"), exported)
    }

    @Test fun importedSettingsAreWrittenWithTheirRealTypes() {
        val target = mutablePreferencesOf()
        BackupSettings.apply(mapOf("background_sync" to "false", "sync_interval" to "120", "theme_mode" to "DARK", "browser_delay_ms" to "8000"), target)
        assertEquals(false, target[booleanPreferencesKey("background_sync")])
        assertEquals(120L, target[longPreferencesKey("sync_interval")])
        assertEquals("DARK", target[stringPreferencesKey("theme_mode")])
        assertEquals(8000L, target[longPreferencesKey("browser_delay_ms")])
    }

    @Test fun unknownNamesAndBadValuesAreIgnored() {
        val target = mutablePreferencesOf(longPreferencesKey("sync_interval") to 60L)
        BackupSettings.apply(
            mapOf(
                "quick_monitor_active" to "true",
                "made_up" to "1",
                "background_sync" to "yes",
                "sync_interval" to "soon",
                "browser_delay_ms" to "99999999",
                "theme_mode" to "x".repeat(200),
            ),
            target,
        )
        assertNull(target[booleanPreferencesKey("quick_monitor_active")])
        assertNull(target[booleanPreferencesKey("background_sync")])
        assertEquals("a bad value must leave the current one alone", 60L, target[longPreferencesKey("sync_interval")])
        assertNull(target[longPreferencesKey("browser_delay_ms")])
        assertNull(target[stringPreferencesKey("theme_mode")])
        assertFalse(target.asMap().keys.any { it.name == "made_up" })
    }
}
