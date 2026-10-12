package com.bernaferrari.changedetection.util

import androidx.compose.runtime.*
import androidx.compose.ui.platform.Clipboard
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual val fileIoDispatcher: CoroutineDispatcher = Dispatchers.IO
actual fun getMemoryUsage(): Long = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
actual suspend fun Clipboard.readText(): String? = runCatching { Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as? String }.getOrNull()

/** Desktop Escape is routed through the same local override used by search and modal content. */
class DesktopBackDispatcher {
    private val handlers = mutableListOf<() -> Unit>()
    fun register(handler: () -> Unit) {
        handlers += handler
    }
    fun unregister(handler: () -> Unit) {
        handlers -= handler
    }
    fun dispatch(): Boolean = handlers.lastOrNull()?.let {
        it()
        true
    } ?: false
}
val LocalDesktopBackDispatcher = staticCompositionLocalOf { DesktopBackDispatcher() }

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    val dispatcher = LocalDesktopBackDispatcher.current
    val current by rememberUpdatedState(onBack)
    DisposableEffect(enabled, dispatcher) {
        val handler = { current() }
        if (enabled) dispatcher.register(handler)
        onDispose { if (enabled) dispatcher.unregister(handler) }
    }
}

internal fun desktopDataDirectory(): File = File(
    System.getProperty("changedetection.dataDir") ?: File(System.getProperty("user.home"), ".change-detection-desktop").path,
).also { check(it.isDirectory || it.mkdirs()) { "Cannot create app data directory" } }

internal class DesktopFileStorage(
    private val directory: File = File(desktopDataDirectory(), "snapshots").also { it.mkdirs() },
    private val availableBytes: () -> Long = { directory.usableSpace },
) : FileStorage {
    private fun file(name: String): File {
        require(name.isNotBlank() && name == File(name).name && name != "." && name != "..")
        return File(directory, name)
    }
    override fun writeBytes(fileName: String, bytes: ByteArray) {
        val target = file(fileName)
        if (availableBytes() < bytes.size.toLong() + MIN_FREE_BYTES) {
            throw com.bernaferrari.changedetection.error.SnapshotStorageError(lowSpace = true)
        }
        var temporary: File? = null
        try {
            temporary = File.createTempFile("capture-", ".tmp", directory)
            temporary.outputStream().use { stream ->
                stream.write(bytes)
                stream.fd.sync()
            }
            java.nio.file.Files.move(temporary.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE)
        } catch (error: Exception) {
            throw com.bernaferrari.changedetection.error.SnapshotStorageError(lowSpace = availableBytes() < MIN_FREE_BYTES, cause = error)
        } finally {
            temporary?.delete()
        }
    }
    private companion object {
        const val MIN_FREE_BYTES = 32L * 1024 * 1024
    }
    override fun readBytes(fileName: String): ByteArray = file(fileName).readBytes()
    override fun deleteFile(fileName: String): Boolean = file(fileName).delete()
    override fun fileExists(fileName: String): Boolean = file(fileName).isFile
    override fun orphanCandidates(olderThan: Long): List<String> = directory.listFiles().orEmpty().filter { it.isFile && it.lastModified() < olderThan && (it.name.matches(Regex("[0-9a-fA-F-]{36}")) || it.name.matches(Regex("capture-[0-9]+\\.tmp"))) }.map { it.name }
}
