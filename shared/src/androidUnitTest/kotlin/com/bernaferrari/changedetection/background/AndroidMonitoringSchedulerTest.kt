package com.bernaferrari.changedetection.background

import androidx.work.impl.utils.futures.SettableFuture
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.work.Operation
import androidx.work.WorkManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidMonitoringSchedulerTest {
    private val preferences = MutableStateFlow<Preferences>(preferencesOf(MonitoringSchedule.ENABLED to false))
    private val store = mock<DataStore<Preferences>> { on { data } doReturn preferences }
    private val manager = mock<WorkManager>()
    private val scheduler = AndroidMonitoringScheduler(manager, store)

    @Test fun `pause waits for Android cancellation to finish`() = runTest {
        val completion = pendingCancellation()
        val saving = async { scheduler.reconcile() }
        runCurrent()
        assertFalse(saving.isCompleted)
        completion.set(Operation.SUCCESS)
        saving.await()
        verify(manager).cancelUniqueWork(AndroidMonitoringScheduler.WORK_NAME)
    }

    @Test fun `failed cancellation reaches the caller instead of reporting success`() = runTest {
        val completion = pendingCancellation()
        val saving = async { runCatching { scheduler.reconcile() } }
        runCurrent()
        completion.setException(IllegalStateException("Database unavailable"))
        val error = saving.await().exceptionOrNull()
        assertTrue(error is IllegalStateException)
        assertEquals("Database unavailable", error?.message)
    }

    @Test fun `overlapping reconciliation waits for the previous operation`() = runTest {
        val completion = pendingCancellation()
        val first = async { scheduler.reconcile() }
        runCurrent()
        val second = async { scheduler.reconcile() }
        runCurrent()
        verify(manager, times(1)).cancelUniqueWork(any())
        assertFalse(second.isCompleted)
        completion.set(Operation.SUCCESS)
        first.await()
        second.await()
        verify(manager, times(2)).cancelUniqueWork(any())
    }

    @Test fun `queued reconciliation reads newest preferences and waits for enqueue`() = runTest {
        val cancelled = pendingCancellation()
        val enqueued = SettableFuture.create<Operation.State.SUCCESS>()
        val operation = mock<Operation> { on { result } doReturn enqueued }
        whenever(manager.enqueueUniquePeriodicWork(any(), any(), any())).thenReturn(operation)
        val first = async { scheduler.reconcile() }
        runCurrent()
        val second = async { scheduler.reconcile() }
        runCurrent()
        preferences.value = preferencesOf(
            MonitoringSchedule.ENABLED to true,
            MonitoringSchedule.INTERVAL to 15L,
            MonitoringSchedule.UNMETERED to true,
            MonitoringSchedule.CHARGING to true,
            MonitoringSchedule.BATTERY_NOT_LOW to true,
        )
        cancelled.set(Operation.SUCCESS)
        first.await()
        runCurrent()
        assertFalse(second.isCompleted)
        val request = argumentCaptor<androidx.work.PeriodicWorkRequest>()
        verify(manager).enqueueUniquePeriodicWork(eq(AndroidMonitoringScheduler.WORK_NAME), eq(androidx.work.ExistingPeriodicWorkPolicy.UPDATE), request.capture())
        assertEquals(15 * 60_000L, request.firstValue.workSpec.intervalDuration)
        assertEquals(androidx.work.NetworkType.UNMETERED, request.firstValue.workSpec.constraints.requiredNetworkType)
        assertTrue(request.firstValue.workSpec.constraints.requiresCharging())
        assertTrue(request.firstValue.workSpec.constraints.requiresBatteryNotLow())
        enqueued.set(Operation.SUCCESS)
        second.await()
    }

    private fun pendingCancellation(): SettableFuture<Operation.State.SUCCESS> {
        val completion = SettableFuture.create<Operation.State.SUCCESS>()
        val operation = mock<Operation> { on { result } doReturn completion }
        whenever(manager.cancelUniqueWork(any())).thenReturn(operation)
        return completion
    }
}
