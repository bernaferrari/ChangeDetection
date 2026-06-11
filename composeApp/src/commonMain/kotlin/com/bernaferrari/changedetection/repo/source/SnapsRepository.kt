/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.repo.source

import com.bernaferrari.changedetection.repo.ContentTypeInfo
import com.bernaferrari.changedetection.repo.Snap
import kotlinx.coroutines.flow.Flow

/**
 * Concrete implementation to load snaps from the data sources into a cache.
 * KMP Version.
 */
class SnapsRepository(
    private val mSnapsLocalDataSource: SnapsDataSource,
) : SnapsDataSource {
    override suspend fun getSingleSnapPair(id: String): ByteArray = mSnapsLocalDataSource.getSingleSnapPair(id)

    // override fun getSnapForPaging(siteId: String, filter: String): PagingSource<Int, Snap> {
    //    return mSnapsLocalDataSource.getSnapForPaging(siteId, filter)
    // }

    override suspend fun deleteAllSnaps(siteId: String) = mSnapsLocalDataSource.deleteAllSnaps(siteId)

    override suspend fun deleteSnapsForSiteIdAndContentType(
        siteId: String,
        contentType: String,
    ) = mSnapsLocalDataSource.deleteSnapsForSiteIdAndContentType(siteId, contentType)

    override suspend fun getContentTypeInfo(siteId: String): List<ContentTypeInfo> = mSnapsLocalDataSource.getContentTypeInfo(siteId)

    override suspend fun getMostRecentSnap(siteId: String): Snap? = mSnapsLocalDataSource.getMostRecentSnap(siteId)

    override suspend fun getAllMostRecentSnaps(): Map<String, Snap> = mSnapsLocalDataSource.getAllMostRecentSnaps()

    override suspend fun getSnapById(snapId: String): Snap? = mSnapsLocalDataSource.getSnapById(snapId)

    override fun getSnaps(siteId: String): Flow<List<Snap>> = mSnapsLocalDataSource.getSnaps(siteId)

    override fun getSnapsFiltered(
        siteId: String,
        filter: String,
    ): Flow<List<Snap>> = mSnapsLocalDataSource.getSnapsFiltered(siteId, filter)

    override suspend fun getSnapContent(snapId: String): ByteArray = mSnapsLocalDataSource.getSnapContent(snapId)

    override suspend fun getSnapPair(
        originalId: String,
        newId: String,
    ): Pair<Pair<Snap, ByteArray>, Pair<Snap, ByteArray>>? = mSnapsLocalDataSource.getSnapPair(originalId, newId)

    override suspend fun getLastNSnapsForSiteId(
        siteId: String,
        limit: Int,
    ): List<Snap>? = mSnapsLocalDataSource.getLastNSnapsForSiteId(siteId, limit)

    override suspend fun saveSnap(
        snap: Snap,
        content: ByteArray,
        threshold: Int,
    ): Result<Snap> = mSnapsLocalDataSource.saveSnap(snap, content, threshold)

    override suspend fun deleteSnap(snapId: String) {
        mSnapsLocalDataSource.deleteSnap(snapId)
    }

    override suspend fun pruneSnaps(siteId: String) {
        mSnapsLocalDataSource.pruneSnaps(siteId)
    }

    override suspend fun getAllSnapsForSite(siteId: String): List<Snap> = mSnapsLocalDataSource.getAllSnapsForSite(siteId)

    override suspend fun getLastContentHash(siteId: String): String? = mSnapsLocalDataSource.getLastContentHash(siteId)

    override suspend fun getLastProcessedContentHash(siteId: String): String? = mSnapsLocalDataSource.getLastProcessedContentHash(siteId)
}
