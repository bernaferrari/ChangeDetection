/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.repo.source.local

import java.io.File
import java.io.IOException
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LegacyDatabaseImportTest {
    @get:Rule val directory = TemporaryFolder()
    private val legacy get() = directory.root.resolve("Changes.db")

    // Host filesystems may be case-insensitive; Android supplies the actual Changes.db/changes.db paths.
    private val current get() = directory.root.resolve("current.db")
    private val suffixes = listOf("", "-wal", "-shm", "-journal")

    @Test fun completeImportActivatesMainLastAndRetainsEveryLegacyFile() {
        writeLegacy()
        val original = legacyBytes()
        val files = object : LegacyDatabaseFileIo by LocalLegacyDatabaseFileIo {
            override fun move(source: File, destination: File) {
                if (destination == current) {
                    assertFalse(current.exists())
                    assertArrayEquals(original.getValue("-wal"), sidecar(current, "-wal").readBytes())
                    assertArrayEquals(original.getValue("-shm"), sidecar(current, "-shm").readBytes())
                    assertArrayEquals(original.getValue("-journal"), sidecar(current, "-journal").readBytes())
                }
                LocalLegacyDatabaseFileIo.move(source, destination)
            }
        }

        importLegacyDatabase(legacy, current, files)

        assertImported(original)
        assertNoStagingFiles()
    }

    @Test fun missingLegacyDoesNotTouchAnExistingEmptyDestinationOrSidecars() {
        current.writeBytes(byteArrayOf())
        sidecar(current, "-wal").writeText("Existing WAL")

        importLegacyDatabase(legacy, current, noFileIo())

        assertTrue(current.exists())
        assertEquals(0L, current.length())
        assertEquals("Existing WAL", sidecar(current, "-wal").readText())
        assertNoStagingFiles()
    }

    @Test fun existingNonemptyCurrentLeavesBothDatabasesAndSidecarsUntouched() {
        writeLegacy()
        suffixes.forEach { sidecar(current, it).writeText("Current $it") }
        val original = legacyBytes()

        importLegacyDatabase(legacy, current, noFileIo())

        suffixes.forEach {
            assertEquals("Current $it", sidecar(current, it).readText())
            assertArrayEquals(original.getValue(it), sidecar(legacy, it).readBytes())
        }
        assertNoStagingFiles()
    }

    @Test fun partialMainCopyFailureLeavesNoActiveDatabaseAndRetryImportsCompleteBytes() {
        writeLegacy()
        val original = legacyBytes()
        val files = failingCopy(legacy)

        assertImportFails(files)

        assertNoCurrentFiles()
        assertLegacyUnchanged(original)
        assertNoStagingFiles()
        importLegacyDatabase(legacy, current)
        assertImported(original)
    }

    @Test fun partialWalCopyFailurePreservesPreexistingDestinationFilesAndCanRetry() {
        writeLegacy()
        current.writeBytes(byteArrayOf())
        sidecar(current, "-wal").writeText("Existing WAL")
        sidecar(current, "-shm").writeText("Existing SHM")
        val original = legacyBytes()

        assertImportFails(failingCopy(sidecar(legacy, "-wal")))

        assertEquals(0L, current.length())
        assertEquals("Existing WAL", sidecar(current, "-wal").readText())
        assertEquals("Existing SHM", sidecar(current, "-shm").readText())
        assertLegacyUnchanged(original)
        assertNoStagingFiles()
        importLegacyDatabase(legacy, current)
        assertImported(original)
    }

    @Test fun sidecarActivationFailureRemovesOnlyImportedDestinationFilesAndCanRetry() {
        writeLegacy()
        val original = legacyBytes()

        assertImportFails(failingMove(sidecar(current, "-shm")))

        assertNoCurrentFiles()
        assertLegacyUnchanged(original)
        assertNoStagingFiles()
        importLegacyDatabase(legacy, current)
        assertImported(original)
    }

    @Test fun partialRollbackJournalCopyFailureLeavesNoActiveDatabaseAndCanRetry() {
        writeLegacy()
        val original = legacyBytes()

        assertImportFails(failingCopy(sidecar(legacy, "-journal")))

        assertNoCurrentFiles()
        assertLegacyUnchanged(original)
        assertNoStagingFiles()
        importLegacyDatabase(legacy, current)
        assertImported(original)
    }

    @Test fun mainActivationFailureRestoresExistingSidecarsAndCanRetry() {
        writeLegacy()
        current.writeBytes(byteArrayOf())
        sidecar(current, "-wal").writeText("Existing WAL")
        sidecar(current, "-shm").writeText("Existing SHM")
        sidecar(current, "-journal").writeText("Existing journal")
        val original = legacyBytes()

        assertImportFails(failingMove(current))

        assertTrue(current.exists())
        assertEquals(0L, current.length())
        assertEquals("Existing WAL", sidecar(current, "-wal").readText())
        assertEquals("Existing SHM", sidecar(current, "-shm").readText())
        assertEquals("Existing journal", sidecar(current, "-journal").readText())
        assertLegacyUnchanged(original)
        assertNoStagingFiles()
        importLegacyDatabase(legacy, current)
        assertImported(original)
    }

    @Test fun errorAfterAtomicMainActivationKeepsTheCompleteGroupAndRetryLeavesItUntouched() {
        writeLegacy()
        val original = legacyBytes()
        val files = object : LegacyDatabaseFileIo by LocalLegacyDatabaseFileIo {
            override fun move(source: File, destination: File) {
                LocalLegacyDatabaseFileIo.move(source, destination)
                if (destination == current) throw IOException("Injected error after activation")
            }
        }

        assertImportFails(files)

        assertImported(original)
        assertNoStagingFiles()
        importLegacyDatabase(legacy, current, noFileIo())
        assertImported(original)
    }

    @Test fun legacyWithoutSidecarsDoesNotActivateUnrelatedRecoveryFiles() {
        legacy.writeText("Legacy main")
        current.writeBytes(byteArrayOf())
        sidecar(current, "-wal").writeText("Unrelated WAL")
        sidecar(current, "-shm").writeText("Unrelated SHM")
        sidecar(current, "-journal").writeText("Unrelated journal")

        importLegacyDatabase(legacy, current)

        assertEquals("Legacy main", current.readText())
        assertEquals("Legacy main", legacy.readText())
        assertFalse(sidecar(current, "-wal").exists())
        assertFalse(sidecar(current, "-shm").exists())
        assertFalse(sidecar(current, "-journal").exists())
        assertNoStagingFiles()
    }

    private fun writeLegacy() = suffixes.forEach { sidecar(legacy, it).writeText("Legacy $it") }
    private fun legacyBytes() = suffixes.associateWith { sidecar(legacy, it).readBytes() }
    private fun sidecar(main: File, suffix: String) = File(main.path + suffix)
    private fun assertNoCurrentFiles() = suffixes.forEach { assertFalse(sidecar(current, it).exists()) }
    private fun assertLegacyUnchanged(original: Map<String, ByteArray>) = suffixes.forEach {
        assertArrayEquals(original.getValue(it), sidecar(legacy, it).readBytes())
    }
    private fun assertImported(original: Map<String, ByteArray>) {
        assertLegacyUnchanged(original)
        suffixes.forEach { assertArrayEquals(original.getValue(it), sidecar(current, it).readBytes()) }
    }
    private fun assertNoStagingFiles() {
        assertTrue(directory.root.listFiles().orEmpty().none { it.name.contains(".legacy-import-") })
    }
    private fun assertImportFails(files: LegacyDatabaseFileIo) {
        assertTrue(runCatching { importLegacyDatabase(legacy, current, files) }.exceptionOrNull() is IOException)
    }
    private fun noFileIo() = object : LegacyDatabaseFileIo {
        override fun copy(source: File, destination: File) = error("Unexpected copy")
        override fun move(source: File, destination: File) = error("Unexpected activation")
    }
    private fun failingCopy(failingSource: File) = object : LegacyDatabaseFileIo by LocalLegacyDatabaseFileIo {
        override fun copy(source: File, destination: File) {
            if (source == failingSource) {
                destination.writeText("Partial copy")
                throw IOException("Injected copy failure")
            }
            LocalLegacyDatabaseFileIo.copy(source, destination)
        }
    }
    private fun failingMove(failingDestination: File) = object : LegacyDatabaseFileIo by LocalLegacyDatabaseFileIo {
        override fun move(source: File, destination: File) {
            if (destination == failingDestination) throw IOException("Injected activation failure")
            LocalLegacyDatabaseFileIo.move(source, destination)
        }
    }
}
