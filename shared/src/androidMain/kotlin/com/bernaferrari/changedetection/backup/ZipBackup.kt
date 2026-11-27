package com.bernaferrari.changedetection.backup

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** A zip file starts with "PK"; a pages-and-settings backup starts with "{". */
internal fun looksLikeZip(firstBytes: ByteArray): Boolean = firstBytes.size >= 2 && firstBytes[0] == 'P'.code.toByte() && firstBytes[1] == 'K'.code.toByte()

/** Screenshots are already compressed, so speed matters more than ratio. */
internal class ZipBackupSink(output: OutputStream) :
    BackupSink,
    Closeable {
    private val zip = ZipOutputStream(BufferedOutputStream(output)).apply { setLevel(Deflater.BEST_SPEED) }

    override fun writeEntry(name: String, write: (OutputStream) -> Unit) {
        zip.putNextEntry(ZipEntry(name))
        write(zip)
        zip.closeEntry()
    }

    override fun close() = zip.close()
}

/** Entry names are only ever compared, never used as paths, and no entry is read past the caller's limit. */
internal class ZipBackupSource(input: InputStream) :
    BackupSource,
    Closeable {
    private val zip = ZipInputStream(BufferedInputStream(input))

    override fun nextName(): String? = zip.nextEntry?.name

    override fun currentStream(): InputStream = zip

    override fun close() = zip.close()
}

/** A picked file is either kind of backup; the first two bytes say which, and no more than a manifest's worth of a non-zip is read. */
internal fun openBackup(input: InputStream): BackupSource {
    val buffered = BufferedInputStream(input)
    buffered.mark(2)
    val first = buffered.read()
    val second = buffered.read()
    buffered.reset()
    if (first >= 0 && second >= 0 && looksLikeZip(byteArrayOf(first.toByte(), second.toByte()))) return ZipBackupSource(buffered)
    return ManifestOnlySource(buffered)
}
