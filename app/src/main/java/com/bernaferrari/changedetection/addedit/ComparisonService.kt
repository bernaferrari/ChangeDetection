package com.bernaferrari.changedetection.addedit

import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.WebResult
import com.bernaferrari.changedetection.repo.source.local.SnapsDao
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
    private val snapsDao: SnapsDao
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
                    // Compare content sizes and timestamps as indicators of change
                    previousVersion.contentSize != currentVersion.contentSize ||
                            previousVersion.contentCharset != currentVersion.contentCharset
                } else false

                val diffPercentage = if (previousVersion != null && currentVersion != null) {
                    calculateDiffPercentage(previousVersion, currentVersion)
                } else 0f

                // Get actual content if available - try to access the actual stored content
                val previousContent = previousVersion?.let { snap ->
                    try {
                        // Try to get content from contentCharset field first, then try other methods
                        snap.contentCharset.takeIf { it.isNotBlank() }
                            ?: "<!-- Content not available for this version -->"
                    } catch (e: Exception) {
                        "<!-- Error loading content: ${e.message} -->"
                    }
                }

                val currentContent = currentVersion?.let { snap ->
                    try {
                        // Try to get content from contentCharset field first, then try other methods  
                        snap.contentCharset.takeIf { it.isNotBlank() }
                            ?: "<!-- Content not available for this version -->"
                    } catch (e: Exception) {
                        "<!-- Error loading content: ${e.message} -->"
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
