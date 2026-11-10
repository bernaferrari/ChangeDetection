package com.bernaferrari.changedetection.sync

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.bernaferrari.changedetection.error.CheckFailure
import com.bernaferrari.changedetection.error.ErrorHandler
import com.bernaferrari.changedetection.error.NetworkError
import com.bernaferrari.changedetection.repo.CaptureMode
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.source.local.ChangeDatabase
import com.bernaferrari.changedetection.repo.source.local.getRoomDatabase
import com.bernaferrari.changedetection.util.DesktopFileStorage
import io.ktor.client.HttpClient
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** A run of failed checks is counted, reported once, and forgotten the moment the page works again. */
class FailureStreakTest {
    @Test fun failuresAreCountedFromTheFirstAndClearedBySuccess() = runBlocking {
        Fixture().use { fixture ->
            fixture.fail(503)
            val first = fixture.stored()
            assertEquals(1, first.failureCount)
            val since = requireNotNull(first.failingSince)
            fixture.fail(503)
            fixture.fail(503)
            val third = fixture.stored()
            assertEquals(3, third.failureCount)
            assertEquals("the run is dated from its first failure", since, third.failingSince)
            assertEquals(CheckFailure.SERVER.name, third.lastFailureCode)
            assertTrue("a few failures within minutes are not worth an alert", fixture.alerts.isEmpty())

            fixture.succeed("Hello")
            val recovered = fixture.stored()
            assertTrue(recovered.isSuccessful)
            assertEquals(0, recovered.failureCount)
            assertNull(recovered.failingSince)
            assertFalse(recovered.failureAlerted)
        }
    }

    @Test fun aSignInWallAlertsOnceOnTheSecondFailureAndIsWithdrawnOnRecovery() = runBlocking {
        Fixture().use { fixture ->
            fixture.fail(401)
            assertTrue("one failure is not yet a verdict", fixture.alerts.isEmpty())
            fixture.fail(401)
            assertEquals(listOf("page" to CheckFailure.LOGIN), fixture.alerts)
            assertTrue(fixture.stored().failureAlerted)
            repeat(3) { fixture.fail(401) }
            assertEquals("one alert for the whole run", 1, fixture.alerts.size)
            assertEquals(5, fixture.stored().failureCount)
            assertTrue(fixture.cleared.isEmpty())

            fixture.succeed("Hello")
            assertEquals(listOf("page"), fixture.cleared)
            assertFalse(fixture.stored().failureAlerted)

            // A new run of failures is new information.
            fixture.fail(401)
            fixture.fail(401)
            assertEquals(2, fixture.alerts.size)
        }
    }

    @Test fun anUnchangedPageAlsoEndsTheRun() = runBlocking {
        Fixture().use { fixture ->
            fixture.succeed("Hello")
            fixture.fail(404)
            fixture.fail(404)
            assertEquals(listOf("page" to CheckFailure.NOT_FOUND), fixture.alerts)
            fixture.succeed("Hello")
            assertEquals(listOf("page"), fixture.cleared)
            assertEquals(0, fixture.stored().failureCount)
        }
    }

    @Test fun pagesWithNotificationsOffOrPausedStayQuiet() = runBlocking {
        Fixture(notify = false).use { fixture ->
            repeat(4) { fixture.fail(401) }
            assertTrue(fixture.alerts.isEmpty())
            assertEquals("the page still shows how long it has been failing", 4, fixture.stored().failureCount)
            assertFalse(fixture.stored().failureAlerted)
        }
    }

    @Test fun aNotifierThatThrowsDoesNotTurnARecordedFailureIntoACrash() = runBlocking {
        Fixture(broken = true).use { fixture ->
            fixture.fail(401)
            assertTrue(fixture.fail(401) is SyncResult.Failed)
            assertEquals(2, fixture.stored().failureCount)
        }
    }

    private class Fixture(notify: Boolean = true, broken: Boolean = false) : AutoCloseable {
        private val directory = Files.createTempDirectory("failure-streak").toFile()
        private val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = directory.resolve("test.db").path), BundledSQLiteDriver())
        private val client = HttpClient()
        val alerts = mutableListOf<Pair<String, CheckFailure>>()
        val cleared = mutableListOf<String>()
        private val notifier = object : FailureNotifier {
            override suspend fun notifyStoppedWatching(site: Site, failure: CheckFailure) {
                if (broken) error("notifications are unavailable")
                alerts += site.id to failure
            }

            override suspend fun clear(siteId: String) {
                cleared += siteId
            }
        }
        private val engine = SyncEngine(
            client,
            db,
            db.siteDao(),
            db.snapsDao(),
            DesktopFileStorage(directory.resolve("files").also { it.mkdirs() }),
            ErrorHandler(),
            ChangeNotifier { _, _ -> },
            failureNotifier = notifier,
        )

        init {
            runBlocking {
                db.siteDao().insertSite(
                    Site("Page", "https://example.test", 0L, "", "page", 0 to 0).copy(captureMode = CaptureMode.SOURCE, isSyncEnabled = true, isNotificationEnabled = notify),
                )
            }
        }

        suspend fun stored() = requireNotNull(db.siteDao().getSiteById("page"))

        /** Each check starts from the stored page, as the real runners do. */
        suspend fun fail(status: Int) = engine.processFetchResult(stored(), SyncEngine.FetchResult.Error("HTTP $status", NetworkError(status, "HTTP $status")))

        suspend fun succeed(text: String) = engine.processFetchResult(stored(), SyncEngine.FetchResult.Success("text/plain", text.encodeToByteArray()))

        override fun close() {
            client.close()
            db.close()
            directory.deleteRecursively()
        }
    }
}
