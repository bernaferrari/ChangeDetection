/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.main

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Owns the Undo decision separately from screen lifetime and the irreversible commit. */
internal class UndoableDeletion<T>(
    private val scope: CoroutineScope,
    private val delete: suspend (T) -> Unit,
    private val onHidden: (T) -> Unit,
    private val onRestored: (T) -> Unit,
    private val onDeleted: (T) -> Unit,
    private val onFailure: (T, Throwable) -> Unit,
    private val abandonedWindowMillis: Long = 6_000,
) {
    class Ticket<T> internal constructor(internal val value: T) {
        internal val mutex = Mutex()
        internal var pending = true
        internal var deciding = false
        internal var fallback: Job? = null
    }

    fun stage(value: T): Ticket<T> = Ticket(value).also {
        onHidden(value)
        armFallback(it)
    }

    /** The visible snackbar owns its accessible timeout; only abandonment uses the fallback. */
    suspend fun resolve(ticket: Ticket<T>, chooseUndo: suspend () -> Boolean) {
        val accepted = ticket.mutex.withLock {
            if (!ticket.pending || ticket.deciding) {
                false
            } else {
                ticket.deciding = true
                ticket.fallback?.cancel()
                true
            }
        }
        if (!accepted) return
        try {
            if (chooseUndo()) {
                ticket.mutex.withLock {
                    if (ticket.pending) {
                        ticket.pending = false
                        onRestored(ticket.value)
                    }
                }
            } else {
                commit(ticket)
            }
        } finally {
            // Launch in the owner scope even when the screen's coroutine was cancelled.
            scope.launch {
                ticket.mutex.withLock {
                    ticket.deciding = false
                    if (ticket.pending) armFallback(ticket)
                }
            }
        }
    }

    /** Claim all tickets before opening or queuing the single shared snackbar. */
    suspend fun resolveAll(tickets: List<Ticket<T>>, chooseUndo: suspend () -> Boolean) = coroutineScope {
        if (tickets.isEmpty()) return@coroutineScope
        val decision = async(start = CoroutineStart.LAZY) { chooseUndo() }
        try {
            tickets.map { ticket -> async(start = CoroutineStart.UNDISPATCHED) { resolve(ticket) { decision.await() } } }.awaitAll()
        } finally {
            decision.cancel()
        }
    }

    fun retry(value: T) {
        val ticket = Ticket(value)
        onHidden(value)
        scope.launch { commit(ticket) }
    }

    private fun armFallback(ticket: Ticket<T>) {
        ticket.fallback = scope.launch {
            delay(abandonedWindowMillis)
            commit(ticket, onlyIfAbandoned = true)
        }
    }

    private suspend fun commit(ticket: Ticket<T>, onlyIfAbandoned: Boolean = false) {
        val accepted = ticket.mutex.withLock {
            if (!ticket.pending || (onlyIfAbandoned && ticket.deciding)) {
                false
            } else {
                ticket.pending = false
                ticket.fallback?.cancel()
                true
            }
        }
        if (!accepted) return
        // Once accepted, Undo cannot restore a row whose delete is already in flight.
        scope.launch {
            try {
                delete(ticket.value)
                onDeleted(ticket.value)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                onRestored(ticket.value)
                onFailure(ticket.value, error)
            }
        }
    }
}
