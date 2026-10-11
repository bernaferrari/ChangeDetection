/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.background

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** Bounds immediate tap feedback; queued/backing-off requests must never look like execution. */
internal suspend fun awaitSiteRequest(status: Flow<List<SyncJobInfo>>, siteId: String) {
    withTimeoutOrNull(5_000) {
        status.first { jobs -> jobs.any { siteId in it.tags } }
    }
}
