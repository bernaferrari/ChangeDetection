/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.sync

import com.bernaferrari.changedetection.repo.Site
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** One bounded preparation and one post. An older preparation cannot replace a newer alert. */
internal class ChangeAlertDelivery(
    private val scope: CoroutineScope,
    private val prepare: suspend (Site, String) -> String?,
    private val publish: (Site, String, String?) -> Unit,
) {
    private val lock = Any()
    private val pending = mutableMapOf<String, Job>()

    fun deliver(site: Site, snapId: String) = synchronized(lock) {
        pending.remove(site.id)?.cancel()
        lateinit var job: Job
        job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                val summary = withTimeoutOrNull(1_500) { runCatching { prepare(site, snapId) }.getOrNull() }
                ensureActive()
                synchronized(lock) {
                    if (pending[site.id] === job) publish(site, snapId, summary)
                }
            } finally {
                synchronized(lock) {
                    if (pending[site.id] === job) pending.remove(site.id)
                }
            }
        }
        pending[site.id] = job
        job.start()
        Unit
    }
}
