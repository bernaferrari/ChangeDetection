package com.bernaferrari.changedetection.repo

import androidx.paging.PagingSource
import androidx.room3.Room
import androidx.room3.withWriteTransaction
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.bernaferrari.changedetection.repo.source.local.*
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class HistoryPagingDatabaseTest {
    @Test fun tiedTimestampsKeepTheSameOrderAcrossPagedAndFilteredHistory() = runBlocking {
        val directory = Files.createTempDirectory("history-paging").toFile()
        val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = directory.resolve("test.db").path), BundledSQLiteDriver())
        try {
            val archive = (0 until 3000).map { index ->
                Snap("capture-${index.toString().padStart(5, '0')}", "page-${index % 50}", (index % 20).toLong(), "text/html", "UTF-8", 100)
            }
            db.withWriteTransaction {
                repeat(50) { index ->
                    db.siteDao().insertSite(Site(if (index % 2 == 0) "Tracked 100% page" else "Other page", if (index % 4 == 1) "https://example.test/archive_!/$index" else "https://example.test/$index", 0L, "", "page-$index", 0 to 0))
                }
                archive.shuffled(Random(41)).forEach { db.snapsDao().insertSnap(it) }
            }
            val ordered = archive.sortedWith(compareByDescending<Snap> { it.timestamp }.thenByDescending { it.snapId })
            // Previews resolve the exact chronological predecessor, including tied timestamps.
            val pageVersions = ordered.filter { it.siteId == "page-0" }
            for ((index, version) in pageVersions.withIndex()) {
                assertEquals(pageVersions.getOrNull(index + 1), db.snapsDao().getPrecedingSnap(version.siteId, version.timestamp, version.snapId))
            }
            val all = drain(db.snapsDao().getSnapsPagedSource())
            assertEquals(ordered.map { it.snapId }, all)
            assertEquals(3000, all.toSet().size)
            assertEquals(all, drain(db.snapsDao().getSnapsPagedSource("")))
            val filtered = ordered.filter { it.siteId.removePrefix("page-").toInt() % 2 == 0 }.map { it.snapId }
            assertEquals(filtered, drain(db.snapsDao().getSnapsPagedSource("Tracked")))
            assertEquals(filtered, drain(db.snapsDao().getSnapsPagedSource("%")))
            val punctuationUrl = ordered.filter { it.siteId.removePrefix("page-").toInt() % 4 == 1 }.map { it.snapId }
            for (query in listOf("_", "!", "archive_!")) {
                assertEquals("Literal search: $query", punctuationUrl, drain(db.snapsDao().getSnapsPagedSource(query)))
            }
            assertTrue(drain(db.snapsDao().getSnapsPagedSource("%_!")).isEmpty())
            assertEquals(ordered.map { it.snapId }, db.snapsDao().getSnapsWithChange2().map { it.snapId })
        } finally {
            db.close()
            directory.deleteRecursively()
        }
    }

    private suspend fun drain(source: PagingSource<Int, Snap>): List<String> {
        val result = mutableListOf<String>()
        var key: Int? = null
        do {
            val params: PagingSource.LoadParams<Int> = key?.let { PagingSource.LoadParams.Append(it, 37, false) }
                ?: PagingSource.LoadParams.Refresh(null, 37, false)
            val page = source.load(params)
            check(page is PagingSource.LoadResult.Page) { "History load failed: $page" }
            result += page.data.map { it.snapId }
            key = page.nextKey
        } while (key != null)
        source.invalidate()
        return result
    }
}
