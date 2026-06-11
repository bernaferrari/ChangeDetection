/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.repo.source.local

import com.bernaferrari.changedetection.repo.AppExecutors
import com.bernaferrari.changedetection.repo.ContentTypeInfo
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.Result
import com.bernaferrari.changedetection.repo.source.SnapsDataSource
import com.bernaferrari.changedetection.util.ContentHasher
import com.bernaferrari.changedetection.util.FileStorage
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Concrete implementation of a data source as a db.
 * KMP Compatible version.
 */
class SnapsLocalDataSource constructor(
    private val mAppExecutors: AppExecutors,
    private val mSnapsDao: SnapsDao,
    private val fileStorage: FileStorage,
) : SnapsDataSource {
    override suspend fun pruneSnaps(siteId: String) = withContext(mAppExecutors.ioContext) {
        // Use optimized database-level pruning instead of loading all data into memory
        pruneSnapsOptimized(siteId)
    }

    private suspend fun pruneSnapsOptimized(siteId: String) {
        // First, get content duplication stats to determine pruning strategy
        val duplicationStats = mSnapsDao.getContentDuplicationStats(siteId)

        if (duplicationStats.duplicationRate > 0.3) {
            // High duplication rate - remove duplicates first using content hashes
            pruneByContentHash(siteId)
        }

        // Then keep only the most recent 10 unique snapshots
        pruneOldSnaps(siteId, keepCount = 10)
    }

    private suspend fun pruneByContentHash(siteId: String) {
        val uniqueSnaps = mSnapsDao.getUniqueContentSnaps(siteId)
        val allSnapIds = mSnapsDao.getAllSnapsForSiteId(siteId).map { it.snapId }.toSet()
        val uniqueSnapIds = uniqueSnaps.map { it.snapId }.toSet()

        // Delete duplicates (snaps not in unique list)
        val duplicateIds = allSnapIds - uniqueSnapIds
        duplicateIds.forEach { snapId ->
            fileStorage.deleteFile(snapId)
            mSnapsDao.deleteSnapById(snapId)
            Napier.d("Removed duplicate snap: $snapId")
        }
    }

    private suspend fun pruneOldSnaps(
        siteId: String,
        keepCount: Int,
    ) {
        val oldSnaps = mSnapsDao.getAllSnapsForSiteId(siteId).drop(keepCount)
        oldSnaps.forEach { snap ->
            fileStorage.deleteFile(snap.snapId)
            mSnapsDao.deleteSnapById(snap.snapId)
            Napier.d("Removed old snap: ${snap.snapId}")
        }
    }

    override suspend fun deleteAllSnaps(siteId: String) {
        mSnapsDao.getAllSnapsForSiteId(siteId).forEach {
            fileStorage.deleteFile(it.snapId)
            mSnapsDao.deleteSnapById(it.snapId)
        }
    }

    override suspend fun deleteSnap(snapId: String) = withContext(mAppExecutors.ioContext) {
        fileStorage.deleteFile(snapId)
        mSnapsDao.deleteSnapById(snapId)
    }

    override fun getSnaps(siteId: String): Flow<List<Snap>> = mSnapsDao.getAllSnapsForSiteIdWithFlow(siteId)

    override fun getSnapsFiltered(
        siteId: String,
        filter: String,
    ): Flow<List<Snap>> = mSnapsDao.getSnapsWithChange(siteId, filter)

    override suspend fun getAllSnapsForSite(siteId: String): List<Snap> = withContext(mAppExecutors.ioContext) {
        mSnapsDao.getAllSnapsForSiteId(siteId)
    }

    override suspend fun getSnapById(snapId: String): Snap? = withContext(mAppExecutors.ioContext) {
        mSnapsDao.getSnapById(snapId)
    }

    override suspend fun getSnapContent(snapId: String): ByteArray = withContext(mAppExecutors.ioContext) {
        fileStorage.readBytes(snapId)
    }

    override suspend fun saveSnap(
        snap: Snap,
        content: ByteArray,
        threshold: Int,
    ): Result<Snap> = withContext(mAppExecutors.ioContext) {
        val lastSnapValue =
            mSnapsDao.getLastSnapForSiteId(snap.siteId).let { previousValue ->
                if (previousValue != null && fileStorage.fileExists(previousValue.snapId)) {
                    fileStorage.readBytes(previousValue.snapId)
                } else {
                    ByteArray(0)
                }
            }

        val belowThresh = threshold <= abs(lastSnapValue.size - content.size)

        if (belowThresh &&
            ContentHasher.contentChanged(
                content,
                lastSnapValue,
                snap.contentType,
            )
        ) {
            Napier.d("Difference detected! Size went from ${lastSnapValue.size} to ${content.size}")
            mSnapsDao.insertSnap(snap)
            fileStorage.writeBytes(snap.snapId, content)
            Result.Success(snap)
        } else {
            Napier.d("No difference detected!")
            Result.Error()
        }
    }

    override suspend fun deleteSnapsForSiteIdAndContentType(
        siteId: String,
        contentType: String,
    ) = withContext(mAppExecutors.ioContext) {
        mSnapsDao.getAllSnapsForSiteIdAndContentType(siteId, contentType).forEach {
            fileStorage.deleteFile(it.snapId)
            mSnapsDao.deleteSnapById(it.snapId)
        }
    }

    override suspend fun getContentTypeInfo(siteId: String): List<ContentTypeInfo> = withContext(mAppExecutors.ioContext) {
        mSnapsDao.getContentTypesWithCount(siteId).map {
            ContentTypeInfo(it.contentType, it.count)
        }
    }

    override suspend fun getMostRecentSnap(siteId: String): Snap? = withContext(mAppExecutors.ioContext) {
        mSnapsDao.getLastSnapForSiteId(siteId)
    }

    override suspend fun getAllMostRecentSnaps(): Map<String, Snap> = withContext(mAppExecutors.ioContext) {
        val allSnapshots = mSnapsDao.getAllMostRecentSnaps()
        allSnapshots.associateBy { it.siteId }
    }

    override suspend fun getLastNSnapsForSiteId(
        siteId: String,
        limit: Int,
    ): List<Snap> = withContext(mAppExecutors.ioContext) {
        mSnapsDao.getLastNSnapsForSiteId(siteId, limit)
    }

    override suspend fun getSnapPair(
        originalId: String,
        newId: String,
    ): Pair<Pair<Snap, ByteArray>, Pair<Snap, ByteArray>>? = withContext(mAppExecutors.ioContext) {
        val originalSnap = mSnapsDao.getSnapById(originalId)
        val newSnap = mSnapsDao.getSnapById(newId)

        if (originalSnap == null || newSnap == null) {
            Napier.w("⚠️ Snap not found: original=$originalSnap, new=$newSnap")
            return@withContext null
        }

        try {
            val originalContent = fileStorage.readBytes(originalId)
            val newContent = fileStorage.readBytes(newId)

            (originalSnap to originalContent) to (newSnap to newContent)
        } catch (e: Exception) {
            Napier.e("❌ Error reading snap content: ${e.message}")
            null
        }
    }

    override suspend fun getSingleSnapPair(id: String): ByteArray = withContext(mAppExecutors.ioContext) {
        fileStorage.readBytes(id)
    }

    override suspend fun getLastContentHash(siteId: String): String? = withContext(mAppExecutors.ioContext) {
        mSnapsDao.getLastContentHash(siteId)
    }

    override suspend fun getLastProcessedContentHash(siteId: String): String? = withContext(mAppExecutors.ioContext) {
        mSnapsDao.getLastProcessedContentHash(siteId)
    }
}
