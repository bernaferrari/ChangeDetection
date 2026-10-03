/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.background

import com.bernaferrari.changedetection.sync.CheckRunner
import com.bernaferrari.changedetection.sync.SyncResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Clock
import kotlin.time.TimeSource

const val TURBO_DURATION_MS = 3 * 60 * 60 * 1000L
val TURBO_INTERVAL_SECONDS = setOf(15, 30, 60, 120)
val TURBO_DURATION_MINUTES = setOf(15, 30, 60, 180)

enum class TurboEndReason { Stopped, Expired, Interrupted, SystemLimit, StartFailed, NoSites }

data class TurboState(
    val isActive: Boolean = false,
    val siteIds: Set<String> = emptySet(),
    val startTime: Long = 0,
    val deadlineTime: Long = 0,
    val intervalSeconds: Int = 30,
    val durationMinutes: Int = 180,
    val successfulChecks: Int = 0,
    val failedChecks: Int = 0,
    val updatesSaved: Int = 0,
    val lastCheckTime: Long = 0,
    val endReason: TurboEndReason? = null,
    /** Live process-only clock; never persisted or resumed from a saved session. */
    internal val remainingTimeSource: (() -> Long)? = null,
) {
    fun remainingTimeMillis(wallTimeMillis: Long = Clock.System.now().toEpochMilliseconds()): Long {
        if (!isActive) return 0
        return (remainingTimeSource?.invoke() ?: (deadlineTime - wallTimeMillis)).coerceIn(0L, durationMinutes * 60_000L)
    }
}

/** Injectable clocks keep wall-clock labels separate from elapsed-time scheduling. */
interface MonitorClock {
    fun wallTimeMillis(): Long
    fun elapsedMillis(): Long
}

class SystemMonitorClock : MonitorClock {
    private val origin = TimeSource.Monotonic.markNow()
    override fun wallTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
    override fun elapsedMillis(): Long = origin.elapsedNow().inWholeMilliseconds
}

/**
 * Owns the complete lifetime of a Turbo session. Called on the platform main dispatcher.
 * Workers, UI and widgets never implement their own timer or publish optimistic running state.
 * A selected page has one child loop; changing selection leaves other checks alone.
 */
class TurboSession(
    private val scope: CoroutineScope,
    private val runner: CheckRunner,
    private val clock: MonitorClock = SystemMonitorClock(),
) {
    private val mutableState = MutableStateFlow(TurboState())
    val state = mutableState.asStateFlow()
    private var sessionJob: Job? = null
    private var generation = 0L

    /** Restore configuration and outcome only; process recreation never resumes a timed session. */
    fun restore(previous: TurboState) {
        // Preference IO may finish after a user starts or stops a new session.
        if (generation != 0L) return
        ++generation
        mutableState.value = previous.copy(
            isActive = false,
            remainingTimeSource = null,
            intervalSeconds = previous.intervalSeconds.takeIf { it in TURBO_INTERVAL_SECONDS } ?: 30,
            durationMinutes = previous.durationMinutes.takeIf { it in TURBO_DURATION_MINUTES } ?: 180,
            endReason = if (previous.isActive) TurboEndReason.Interrupted else previous.endReason,
        )
    }

    fun start(siteIds: Set<String>, intervalSeconds: Int, durationMinutes: Int = 180) {
        require(intervalSeconds in TURBO_INTERVAL_SECONDS)
        require(durationMinutes in TURBO_DURATION_MINUTES)
        if (siteIds.isEmpty()) {
            stop(TurboEndReason.NoSites)
            return
        }
        if (mutableState.value.isActive) {
            mutableState.update { it.copy(siteIds = siteIds.toSet(), intervalSeconds = intervalSeconds) }
            return
        }
        val durationMs = durationMinutes * 60_000L
        val token = ++generation
        val started = clock.wallTimeMillis()
        val elapsedClock = clock
        val elapsedDeadline = elapsedClock.elapsedMillis() + durationMs
        mutableState.value = TurboState(
            isActive = true,
            siteIds = siteIds.toSet(),
            startTime = started,
            deadlineTime = started + durationMs,
            durationMinutes = durationMinutes,
            intervalSeconds = intervalSeconds,
            remainingTimeSource = { elapsedDeadline - elapsedClock.elapsedMillis() },
        )
        sessionJob = scope.launch {
            var reason = TurboEndReason.Interrupted
            try {
                // Dispatch delay is part of the bounded session, not extra lifetime.
                withTimeout((elapsedDeadline - elapsedClock.elapsedMillis()).coerceAtLeast(0)) {
                    supervisorScope {
                        val checks = mutableMapOf<String, Job>()
                        state.map { it.siteIds }.distinctUntilChanged().collect { selected ->
                            (checks.keys - selected).forEach { id -> checks.remove(id)?.cancelAndJoin() }
                            selected.filter { it !in checks }.forEach { id ->
                                checks[id] = launch {
                                    while (isActive) {
                                        val tickStart = clock.elapsedMillis()
                                        val result = try {
                                            runner.check(id)
                                        } catch (_: TimeoutCancellationException) {
                                            currentCoroutineContext().ensureActive()
                                            null
                                        } catch (cancelled: CancellationException) {
                                            throw cancelled
                                        } catch (_: Exception) {
                                            null
                                        }
                                        mutableState.update { current ->
                                            if (!current.isActive || generation != token) {
                                                current
                                            } else {
                                                current.copy(
                                                    successfulChecks = current.successfulChecks + if (result is SyncResult.Success) 1 else 0,
                                                    failedChecks = current.failedChecks + if (result == null || result is SyncResult.Failed) 1 else 0,
                                                    updatesSaved = current.updatesSaved + if (result is SyncResult.Success && result.hadChanges && result.snapId != null) 1 else 0,
                                                    lastCheckTime = clock.wallTimeMillis(),
                                                )
                                            }
                                        }
                                        // Count cadence from the start of the check, not its completion.
                                        // Slow checks skip missed ticks instead of overlapping or bursting.
                                        awaitNextTick(tickStart, clock.elapsedMillis())
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (_: TimeoutCancellationException) {
                reason = TurboEndReason.Expired
            } catch (cancelled: CancellationException) {
                throw cancelled
            } finally {
                if (generation == token && mutableState.value.isActive) {
                    mutableState.update { it.copy(isActive = false, endReason = reason, remainingTimeSource = null) }
                }
            }
        }
    }

    private suspend fun awaitNextTick(tickStart: Long, completedAt: Long) {
        var interval = state.value.intervalSeconds
        while (true) {
            val intervalMs = interval * 1_000L
            val nextTick = if (completedAt - tickStart < intervalMs) tickStart + intervalMs else completedAt + intervalMs
            val remaining = nextTick - clock.elapsedMillis()
            if (remaining <= 0) return
            // Configuration changes wake a pending wait. They never restart capture
            // or change the parent session deadline.
            interval = withTimeoutOrNull(remaining) {
                state.map { it.intervalSeconds }.first { it != interval }
            } ?: return
        }
    }

    fun stop(reason: TurboEndReason = TurboEndReason.Stopped) {
        ++generation
        sessionJob?.cancel()
        sessionJob = null
        mutableState.update { it.copy(isActive = false, endReason = reason, remainingTimeSource = null) }
    }
}
