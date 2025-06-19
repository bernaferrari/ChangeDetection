package com.bernaferrari.changedetection.detailsWebview

import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.WebResult
import com.bernaferrari.changedetection.repo.source.local.SnapsDao
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class VersionComparison(
    val previousVersion: Snap?,
    val currentVersion: Snap?,
    val hasChanges: Boolean,
    val diffPercentage: Float,
    val allVersions: List<Snap> = emptyList(),
    val previousContent: String? = null,
    val currentContent: String? = null
)

@Singleton
class ComparisonService @Inject constructor(
    private val snapsDao: SnapsDao,
    private val snapsRepository: SnapsRepository
) {

    suspend fun getVersionsForSite(siteId: String): WebResult<VersionComparison> =
        withContext(Dispatchers.IO) {
            try {
                val allVersions = snapsDao.getAllSnapsForSiteId(siteId)
                    .sortedByDescending { snap -> snap.timestamp }

                if (allVersions.isEmpty()) {
                    return@withContext WebResult.Error("No versions found for this site")
                }

                val currentVersion = allVersions.firstOrNull()
                val previousVersion = allVersions.getOrNull(1)

                val hasChanges = if (previousVersion != null && currentVersion != null) {
                    previousVersion.contentSize != currentVersion.contentSize
                } else false

                val diffPercentage = if (previousVersion != null && currentVersion != null) {
                    calculateDiffPercentage(previousVersion, currentVersion)
                } else 0f

                // Get actual HTML content using the repository (same as LocalBrowser)
                val previousContent = previousVersion?.let { snap ->
                    try {
                        snapsRepository.getSnapContent(snap.snapId).toString(Charsets.UTF_8)
                    } catch (e: Exception) {
                        null
                    }
                }

                val currentContent = currentVersion?.let { snap ->
                    try {
                        snapsRepository.getSnapContent(snap.snapId).toString(Charsets.UTF_8)
                    } catch (e: Exception) {
                        null
                    }
                }

                WebResult.Success(
                    VersionComparison(
                        previousVersion = previousVersion,
                        currentVersion = currentVersion,
                        hasChanges = hasChanges,
                        diffPercentage = diffPercentage,
                        allVersions = allVersions,
                        previousContent = previousContent,
                        currentContent = currentContent
                    )
                )
            } catch (e: Exception) {
                WebResult.Error(e.message ?: "Failed to load versions")
            }
        }

    private fun Double.format(digits: Int) = "%.${digits}f".format(this)

    private fun calculateDiffPercentage(previous: Snap, current: Snap): Float {
        val prevSize = previous.contentSize
        val currentSize = current.contentSize

        if (prevSize == 0 && currentSize == 0) return 0f
        if (prevSize == 0 || currentSize == 0) return 100f

        val sizeDiff = kotlin.math.abs(currentSize - prevSize)
        val maxSize = maxOf(prevSize, currentSize)

        return (sizeDiff.toFloat() / maxSize) * 100f
    }
}
