package com.bernaferrari.changedetection.sync

import com.bernaferrari.changedetection.repo.Site
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Delivery belongs to its caller. Preparation may be superseded; publication is serialized and awaited. */
internal class ChangeAlertDelivery(
    private val prepare: suspend (Site, String) -> String?,
    private val publish: suspend (Site, String, String?) -> Unit,
) {
    private val mutex = Mutex()
    private val pending = mutableMapOf<String, Deferred<Unit>>()

    suspend fun deliver(site: Site, snapId: String) = coroutineScope {
        lateinit var job: Deferred<Unit>
        job = async(start = CoroutineStart.LAZY) {
            val summary = withTimeoutOrNull(1_500) {
                try {
                    prepare(site, snapId)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
            }
            ensureActive()
            mutex.withLock {
                if (pending[site.id] === job) {
                    // Do not release publication ownership before a native asynchronous post acknowledges submission.
                    withContext(NonCancellable) { withTimeout(5_000) { publish(site, snapId, summary) } }
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
