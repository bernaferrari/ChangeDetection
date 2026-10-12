package com.bernaferrari.changedetection.ui.main

import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.repo.source.SitesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen

/** Only complete reads reach the UI. Failed reads never impersonate an empty history. */
internal fun watchlistReader(
    sites: SitesRepository,
    onFailure: (Throwable) -> Unit,
    onLoaded: () -> Unit,
): Flow<List<SiteAndLastSnap>> = flow {
    var failures = 0
    emitAll(
        sites.observeWatchlist()
            .onEach {
                onLoaded()
                failures = 0
            }
            .retryWhen { error, _ ->
                if (error is CancellationException) throw error
                if (failures == 0) onFailure(error)
                val waitMillis = (1_000L shl failures.coerceAtMost(5)).coerceAtMost(30_000)
                failures++
                delay(waitMillis)
                true
            },
    )
}
