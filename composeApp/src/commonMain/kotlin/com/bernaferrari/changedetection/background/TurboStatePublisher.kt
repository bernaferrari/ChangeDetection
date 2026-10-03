/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.background

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen

/** Retry durable state/widget publication without replaying an obsolete running state. */
internal suspend fun publishTurboState(
    session: TurboSession,
    read: suspend () -> TurboState,
    publish: suspend (TurboState) -> Unit,
    onFailure: (Throwable) -> Unit,
) {
    var failures = 0
    flow {
        // restore refuses to overwrite any Start/Stop performed while storage was unavailable.
        session.restore(read())
        emitAll(session.state)
    }.map { it.copy(successfulChecks = 0, failedChecks = 0, updatesSaved = 0, lastCheckTime = 0, remainingTimeSource = null) }
        .distinctUntilChanged()
        .onEach {
            publish(it)
            failures = 0
        }
        .retryWhen { error, _ ->
            if (error is CancellationException) throw error
            if (failures == 0) onFailure(error)
            val waitMillis = (1_000L shl failures.coerceAtMost(5)).coerceAtMost(30_000)
            failures++
            delay(waitMillis)
            true
        }
        .collect()
}
