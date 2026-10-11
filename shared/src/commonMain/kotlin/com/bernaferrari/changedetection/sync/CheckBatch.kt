/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal class CheckBatchFailure(val failed: Int, val checked: Int) : IllegalStateException("$failed of $checked page checks failed")

/** Individual failures do not starve later pages; batch completion still reports them to its owner. */
internal suspend fun checkBatch(ids: List<String>, check: suspend (String) -> SyncResult): Int {
    var failed = 0
    for (id in ids) {
        currentCoroutineContext().ensureActive()
        try {
            if (check(id) is SyncResult.Failed) failed++
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            failed++
        }
    }
    if (failed > 0) throw CheckBatchFailure(failed, ids.size)
    return ids.size
}
