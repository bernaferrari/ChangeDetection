package com.bernaferrari.changedetection.repo.source.local

import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.async.prepare
import androidx.sqlite.async.step
import com.bernaferrari.changedetection.diagnostics.AppEventKind
import com.bernaferrari.changedetection.diagnostics.DiagnosticLog
import kotlinx.coroutines.Dispatchers

fun getRoomDatabase(
    builder: RoomDatabase.Builder<ChangeDatabase>,
    driver: SQLiteDriver,
    migrations: Array<out Migration> = emptyArray(),
): ChangeDatabase = builder
    .setDriver(driver)
    // Preserve the original public release. Retired development schemas may start fresh.
    .addMigrations(MIGRATION_1_4, MIGRATION_4_5, *migrations)
    .fallbackToDestructiveMigration(true)
    .addCallback(snapshotCleanupCallback)
    .addCallback(databaseDiagnosticsCallback)
    .setQueryCoroutineContext(Dispatchers.Default)
    .build()

/**
 * Records the schema version each time the database opens (the diagnostics sink turns a change into
 * "created" or "upgraded from X to Y"), and the destructive fallback above, which otherwise wipes
 * history without a trace.
 */
private val databaseDiagnosticsCallback = object : RoomDatabase.Callback() {
    override suspend fun onOpen(connection: SQLiteConnection) {
        val version = try {
            val statement = connection.prepare("PRAGMA user_version")
            try {
                if (statement.step()) statement.getLong(0).toInt() else -1
            } finally {
                statement.close()
            }
        } catch (_: Exception) {
            -1
        }
        DiagnosticLog.databaseOpened(version)
    }

    override suspend fun onDestructiveMigration(connection: SQLiteConnection) {
        DiagnosticLog.appEvent(AppEventKind.DATABASE_RESET)
    }
}
