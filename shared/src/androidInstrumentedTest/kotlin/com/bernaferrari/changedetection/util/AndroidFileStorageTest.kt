package com.bernaferrari.changedetection.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidFileStorageTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun lowSpaceDoesNotOverwriteAnExistingVersion() {
        val name = UUID.randomUUID().toString()
        val original = "saved version".toByteArray()
        context.openFileOutput(name, Context.MODE_PRIVATE).use { it.write(original) }
        try {
            val storage = AndroidFileStorage(context, availableBytes = { 0 })
            val error = runCatching { storage.writeBytes(name, "replacement".toByteArray()) }.exceptionOrNull()
            assertNotNull("Low space must fail before touching existing content", error)
            assertArrayEquals(original, storage.readBytes(name))
        } finally {
            context.deleteFile(name)
            context.deleteFile("$name.partial")
        }
    }

    @Test fun successfulWritePublishesCompleteBinaryContentWithoutTemporaryFiles() {
        val name = UUID.randomUUID().toString()
        val storage = AndroidFileStorage(context)
        val bytes = ByteArray(65_537) { (it % 251).toByte() }
        try {
            storage.writeBytes(name, bytes)
            assertArrayEquals(bytes, storage.readBytes(name))
            assertFalse(context.getFileStreamPath("$name.partial").exists())
        } finally {
            storage.deleteFile(name)
        }
    }

    @Test fun interruptedWriteIsNotExposedAndBecomesAnOrphanCandidate() {
        val name = UUID.randomUUID().toString()
        val temporary = context.getFileStreamPath("$name.partial")
        temporary.writeText("incomplete")
        temporary.setLastModified(1_000)
        try {
            val storage = AndroidFileStorage(context)
            assertFalse(storage.fileExists(name))
            assertTrue(storage.orphanCandidates(2_000).contains("$name.partial"))
        } finally {
            temporary.delete()
        }
    }
}
