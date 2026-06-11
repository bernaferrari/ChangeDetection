/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.repo.source

import com.bernaferrari.changedetection.repo.ContentTypeInfo
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.Result
import kotlinx.coroutines.flow.Flow

/**
 * Main entry point for accessing snaps data.
 * Inspired from Architecture Components MVVM sample app
 */
interface SnapsDataSource {
    suspend fun getContentTypeInfo(siteId: String): List<ContentTypeInfo>

    suspend fun getMostRecentSnap(siteId: String): Snap?

    suspend fun getAllMostRecentSnaps(): Map<String, Snap>

    fun getSnaps(siteId: String): Flow<List<Snap>>

    fun getSnapsFiltered(
        siteId: String,
        filter: String,
    ): Flow<List<Snap>>

    suspend fun getAllSnapsForSite(siteId: String): List<Snap>

    // fun getSnapForPaging(siteId: String, filter: String): PagingSource<Int, Snap>

    suspend fun getSnapById(snapId: String): Snap?

    suspend fun getSnapContent(snapId: String): ByteArray

    suspend fun getLastNSnapsForSiteId(
        siteId: String,
        limit: Int,
    ): List<Snap>?

    suspend fun getSnapPair(
        originalId: String,
        newId: String,
    ): Pair<Pair<Snap, ByteArray>, Pair<Snap, ByteArray>>?

    suspend fun saveSnap(
        snap: Snap,
        content: ByteArray,
        threshold: Int,
    ): Result<Snap>

    suspend fun deleteAllSnaps(siteId: String)

    suspend fun deleteSnap(snapId: String)

    suspend fun deleteSnapsForSiteIdAndContentType(
        siteId: String,
        contentType: String,
    )

    suspend fun pruneSnaps(siteId: String)

    suspend fun getSingleSnapPair(id: String): ByteArray

    // Fast content hash comparison methods
    suspend fun getLastContentHash(siteId: String): String?

    suspend fun getLastProcessedContentHash(siteId: String): String?
}
