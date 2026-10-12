package com.bernaferrari.changedetection.background

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import org.junit.Assert.*
import org.junit.Test

class MonitoringScheduleTest {
    @Test fun `fresh installation is enabled with the same hourly default shown by settings`() {
        val schedule = MonitoringSchedule.from(emptyPreferences())
        assertTrue(schedule.enabled)
        assertEquals(60L, schedule.intervalMinutes)
    }

    @Test fun `stored explicit pause is respected`() {
        assertFalse(MonitoringSchedule.from(preferencesOf(MonitoringSchedule.ENABLED to false)).enabled)
    }

    @Test fun `legacy invalid durations are normalized to supported bounds`() {
        assertEquals(15L, MonitoringSchedule.from(preferencesOf(MonitoringSchedule.INTERVAL to 1L)).intervalMinutes)
        assertEquals(10080L, MonitoringSchedule.from(preferencesOf(MonitoringSchedule.INTERVAL to Long.MAX_VALUE)).intervalMinutes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `new invalid interval cannot enter domain model`() {
        MonitoringSchedule(intervalMinutes = 10081)
    }
}
