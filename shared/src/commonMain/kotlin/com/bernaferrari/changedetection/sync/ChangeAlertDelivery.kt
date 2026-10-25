package com.bernaferrari.changedetection.sync

import com.bernaferrari.changedetection.repo.Site
import kotlinx.coroutines.*
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Delivery belongs to its caller. Preparation may be superseded; publication is serialized and awaited.
 * Publishers that suspend before native submission must recheck the supplied eligibility predicate afterward.
 */
internal class ChangeAlertDelivery<T>(
    private val prepare: suspend (Site, String) -> T?,
    private val publish: suspend (Site, String, T?, suspend () -> Boolean) -> Unit,
) {
    private val mutex = Mutex()
    private val pending = mutableMapOf<String, Deferred<Unit>>()

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun deliver(site: Site, snapId: String, isCurrent: suspend () -> Boolean = { true }) = coroutineScope {
        lateinit var job: Deferred<Unit>
        job = async(start = CoroutineStart.LAZY) {
            val preparation = async {
                try {
                    prepare(site, snapId)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
            }
            val summary = try {
                select<T?> {
                    preparation.onAwait { it }
                    onTimeout(1_500) { null }
                }
            } finally {
                // Do not wait for a native/non-cancellable decoder before publishing the ordinary fallback.
                // It remains our child, so cleanup is still owned and can never publish a late update.
                preparation.cancel()
            }
            ensureActive()
            mutex.withLock {
                if (pending[site.id] === job) {
                    // Do not release publication ownership before a native asynchronous post acknowledges submission.
                    withContext(NonCancellable) {
                        withTimeout(5_000) {
                            if (isCurrent()) publish(site, snapId, summary, isCurrent)
                        }
                    }
                }
            }
        }
        mutex.withLock {
            pending.remove(site.id)?.cancel()
            pending[site.id] = job
        }
        job.start()
        try {
            job.await()
        } catch (cancelled: CancellationException) {
            // A newer alert cancels only preparation, never its owning worker. Caller cancellation still propagates.
            currentCoroutineContext().ensureActive()
            if (mutex.withLock { pending[site.id] === job }) throw cancelled
        } finally {
            withContext(NonCancellable) { mutex.withLock { if (pending[site.id] === job) pending.remove(site.id) } }
        }
    }
}
