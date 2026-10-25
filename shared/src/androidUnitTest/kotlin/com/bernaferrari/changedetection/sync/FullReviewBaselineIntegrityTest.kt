package com.bernaferrari.changedetection.sync

import com.bernaferrari.changedetection.error.ErrorHandler
import com.bernaferrari.changedetection.repo.CaptureMode
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.local.ChangeDatabase
import com.bernaferrari.changedetection.repo.source.local.SitesDao
import com.bernaferrari.changedetection.repo.source.local.SnapsDao
import com.bernaferrari.changedetection.util.ContentHasher
import com.bernaferrari.changedetection.util.FileStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*

/** Healthy status requires actual saved bytes; failed checks never rewrite or replace history. */
class FullReviewBaselineIntegrityTest {
    @Test fun missingLatestFileMustNotBeReportedAsHealthy() = verify()
    @Test fun corruptLatestFileMustNotBeReportedAsHealthy() = verify(corrupt = true)
    @Test fun sameSizeCorruptionIsCheckedAgainstLegacySourceHash() = verify(corrupt = true, sameSize = true)
    @Test fun newPayloadChecksumRejectsSameSizeEditsHiddenByReadableTextHash() = verify(corrupt = true, sameSize = true, payloadChecksum = true)
    @Test fun healthyBaselineIsVerifiedWithoutRewritingOrCreatingHistory() = verify(healthy = true)
    @Test fun concurrentHistoryResetIsSkippedWithoutOverwritingResetStatus() = verify(resetDuringRead = true)

    @Test fun healthyUnhashedPublicLegacyBaselineIsReadOnceAndPreserved() = verify(healthy = true, legacyNoHash = true)
    @Test fun truncatedUnhashedPublicLegacyBaselineCannotBecomeAFalseChange() = verify(corrupt = true, legacyNoHash = true)
    @Test fun emptyUnhashedPublicLegacyBaselineCannotBecomeAFalseChange() = verify(corrupt = true, legacyNoHash = true, empty = true)

    private fun verify(corrupt: Boolean = false, sameSize: Boolean = false, payloadChecksum: Boolean = false, healthy: Boolean = false, resetDuringRead: Boolean = false, legacyNoHash: Boolean = false, empty: Boolean = false) = runTest {
        val sites = mock<SitesDao>();val snaps = mock<SnapsDao>();val files = mock<FileStorage>();val db = mock<ChangeDatabase>()
        val mode = if (legacyNoHash) CaptureMode.DEFAULT else if (payloadChecksum) CaptureMode.TEXT else CaptureMode.SOURCE
        val site = Site("Integrity", "https://example.test", 1L, "", "integrity", 0 to 0).copy(captureMode = mode)
        val body = "<html><body>Stable content</body></html>".encodeToByteArray()
        val hash = if (legacyNoHash) "" else ContentHasher.detectionHash(body, "text/html", mode)
        val snap = Snap("saved-version", site.id, 1L, "text/html", "UTF-8", body.size, hash, hash, payloadHash = if (payloadChecksum) ContentHasher.hashContent(body) else "")
        var latest: Snap? = snap
        whenever(sites.getSiteById(site.id)).thenReturn(site);whenever(snaps.getLastSnapForSiteId(site.id)).thenAnswer { latest }
        var reads = 0
        whenever(files.readBytes(snap.snapId)).thenAnswer {
            reads++
            if (resetDuringRead) latest = null
            when {
                healthy -> body
                empty -> ByteArray(0)
                !corrupt -> throw java.io.FileNotFoundException("Missing baseline fixture")
                sameSize && payloadChecksum -> body.decodeToString().replace("<html>", "<HTML>").encodeToByteArray()
                sameSize -> body.decodeToString().replace("Stable", "Broken").encodeToByteArray()
                else -> "corrupted bytes".encodeToByteArray()
            }
        }
        val client = HttpClient(MockEngine { respondOk("unused") })
        try {
            val engine = object : SyncEngine(client, db, sites, snaps, files, ErrorHandler()) {
                override suspend fun <T> inWriteTransaction(block: suspend () -> T): T = block()
            }
            val result = engine.processFetchResult(site, SyncEngine.FetchResult.Success("text/html", body))
            when {
                resetDuringRead -> {
                    assertTrue(result.toString(), result is SyncResult.Skipped)
                    verify(sites, never()).updateSyncMetadata(any(), any(), any(), any(), anyOrNull(), anyOrNull())
                }
                healthy -> assertTrue(result.toString(), result is SyncResult.Success && !result.hadChanges)
                else -> {
                    assertTrue("Unreadable/corrupt history must not appear healthy: $result", result is SyncResult.Failed)
                    assertTrue((result as SyncResult.Failed).exception is com.bernaferrari.changedetection.error.BaselineIntegrityError)
                }
            }
            assertEquals(1, reads)
            verify(snaps, never()).insertSnap(any());verify(snaps, never()).deleteSnapById(any());verify(files, never()).writeBytes(any(), any());verify(files, never()).deleteFile(any())
        } finally { client.close() }
    }
}
