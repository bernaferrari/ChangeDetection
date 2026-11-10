package com.bernaferrari.changedetection.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.bernaferrari.changedetection.background.MonitoringManager
import com.bernaferrari.changedetection.util.ApplicationScope
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal sealed interface BackupProgress {
    data object Idle : BackupProgress
    data object Working : BackupProgress
    data class Saved(val summary: BackupSummary) : BackupProgress
    data class Restored(val summary: ImportSummary) : BackupProgress
    data object SaveFailed : BackupProgress
    data object NotABackup : BackupProgress
    data object TooNew : BackupProgress
}

/** One backup or restore at a time, kept alive by the application so leaving Settings does not cut a file short. */
internal class BackupFiles(
    private val context: Context,
    private val service: BackupService,
    private val monitoring: MonitoringManager?,
    private val application: ApplicationScope,
) {
    private val progress = MutableStateFlow<BackupProgress>(BackupProgress.Idle)
    val state: StateFlow<BackupProgress> = progress

    private val appVersion: String
        get() = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()

    fun acknowledge() {
        if (progress.value != BackupProgress.Working) progress.value = BackupProgress.Idle
    }

    fun savePages(target: Uri) = save(target) { output ->
        val bytes = service.exportPages(appVersion)
        output.write(bytes)
        BackupSummary(pages = backupJson.decodeFromString<BackupManifest>(bytes.decodeToString()).sites.size, versions = 0)
    }

    fun saveEverything(target: Uri) = save(target) { output ->
        ZipBackupSink(output).use { service.exportEverything(it, appVersion) }
    }

    private fun save(target: Uri, write: suspend (java.io.OutputStream) -> BackupSummary) = run(
        failure = BackupProgress.SaveFailed,
        // A half-written file looks like a backup and is not one; remove it.
        onFailure = { runCatching { DocumentsContract.deleteDocument(context.contentResolver, target) } },
    ) {
        val output = context.contentResolver.openOutputStream(target, "wt") ?: throw IOException("Cannot open the chosen file")
        BackupProgress.Saved(output.use { write(it) })
    }

    fun restore(from: Uri, restoreSettings: Boolean) = run(failure = BackupProgress.NotABackup) {
        val input = context.contentResolver.openInputStream(from) ?: throw IOException("Cannot open the chosen file")
        val summary = input.use {
            when (val source = openBackup(it)) {
                is ZipBackupSource -> source.use { service.import(it, restoreSettings) }
                else -> service.import(source, restoreSettings)
            }
        }
        // New pages and restored schedule settings both change what the background checks must do.
        monitoring?.updateSchedule()
        BackupProgress.Restored(summary)
    }

    private fun run(failure: BackupProgress, onFailure: () -> Unit = {}, work: suspend () -> BackupProgress) {
        if (!progress.compareAndSet(BackupProgress.Idle, BackupProgress.Working)) return
        application.scope.launch {
            progress.value = try {
                withContext(Dispatchers.IO) { work() }
            } catch (cancelled: CancellationException) {
                progress.value = BackupProgress.Idle
                throw cancelled
            } catch (tooNew: BackupTooNewException) {
                BackupProgress.TooNew
            } catch (invalid: InvalidBackupException) {
                BackupProgress.NotABackup
            } catch (error: Exception) {
                onFailure()
                failure
            }
        }
    }

    companion object {
        @Volatile private var instance: BackupFiles? = null

        /** Null only when the app's services are not running, which cannot happen on a visible Settings screen. */
        fun get(context: Context, service: BackupService?, monitoring: MonitoringManager?, application: ApplicationScope?): BackupFiles? {
            instance?.let { return it }
            if (service == null || application == null) return null
            return synchronized(this) { instance ?: BackupFiles(context.applicationContext, service, monitoring, application).also { instance = it } }
        }
    }
}
