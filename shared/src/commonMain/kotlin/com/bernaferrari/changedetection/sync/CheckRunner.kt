/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.sync

/** One awaited observation, including capture, comparison, persistence and notification. */
fun interface CheckRunner {
    suspend fun check(siteId: String): SyncResult
}
