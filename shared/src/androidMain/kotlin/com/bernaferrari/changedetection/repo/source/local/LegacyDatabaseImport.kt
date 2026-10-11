/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.repo.source.local

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** The shipped v1 database used a capital C. Preserve its files even after a successful import. */
@Synchronized
internal fun importLegacyDatabase(
    legacy: File,
    current: File,
    files: LegacyDatabaseFileIo = LocalLegacyDatabaseFileIo,
) {
    if ((current.exists() && current.length() > 0) || !legacy.exists()) return
    if (!legacy.isFile || legacy.length() == 0L) throw IOException("The legacy database is empty or unreadable")

    val directory = requireNotNull(current.parentFile)
    val staging = Files.createTempDirectory(directory.toPath(), "${current.name}.legacy-import-").toFile()
    val stagedMain = staging.resolve("database")
    val previousSidecars = mutableMapOf<File, File?>()
    val changedSidecars = mutableListOf<File>()
    var activatingMain = false
    var keepStaging = false
    try {
        files.copy(legacy, stagedMain)
        val sidecars = listOf("-wal", "-shm", "-journal").map { suffix ->
            val source = File(legacy.path + suffix)
            val staged = staging.resolve("database$suffix")
            if (source.exists()) files.copy(source, staged)
            File(current.path + suffix) to staged
        }
        // An empty destination may have unrelated sidecars. Preserve them for rollback.
        sidecars.forEachIndexed { index, (destination, _) ->
            previousSidecars[destination] = if (destination.exists()) {
                staging.resolve("previous-$index").also { files.copy(destination, it) }
            } else {
                null
            }
        }
        sidecars.forEach { (destination, staged) ->
            changedSidecars += destination
            if (staged.exists()) files.move(staged, destination) else Files.deleteIfExists(destination.toPath())
        }
        // Atomic activation is last: Room sees the main copy with all its recovery files.
        activatingMain = true
        files.move(stagedMain, current)
    } catch (error: Throwable) {
        // If an atomic move completed before reporting an error, the full group is already active.
        if (!activatingMain || stagedMain.exists()) {
            changedSidecars.asReversed().forEach { destination ->
                try {
                    val previous = previousSidecars[destination]
                    if (previous != null) files.move(previous, destination) else Files.deleteIfExists(destination.toPath())
                } catch (rollbackError: Throwable) {
                    keepStaging = true // Retain original destination bytes if rollback itself fails.
                    error.addSuppressed(rollbackError)
                }
            }
        }
        throw error
    } finally {
        if (!keepStaging) staging.deleteRecursively()
    }
}

/** Only copying and atomic activation are injectable; the importer owns rollback and file identity. */
internal interface LegacyDatabaseFileIo {
    fun copy(source: File, destination: File)
    fun move(source: File, destination: File)
}

internal object LocalLegacyDatabaseFileIo : LegacyDatabaseFileIo {
    override fun copy(source: File, destination: File) {
        source.inputStream().use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
                output.fd.sync()
            }
        }
    }

    override fun move(source: File, destination: File) {
        Files.move(source.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }
}
