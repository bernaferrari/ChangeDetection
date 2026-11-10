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

/** Every migration between schema versions that reached the public. */
internal val PUBLIC_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_4, MIGRATION_4_5, MIGRATION_5_6)

/** Schema versions that only ever existed in development builds, so a database at one of them may start fresh. */
internal val RETIRED_DEVELOPMENT_SCHEMAS = intArrayOf(2, 3)

fun getRoomDatabase(
    builder: RoomDatabase.Builder<ChangeDatabase>,
    driver: SQLiteDriver,
    migrations: Array<out Migration> = PUBLIC_MIGRATIONS,
): ChangeDatabase = builder
    .setDriver(driver)
    .addMigrations(*migrations)
    // Only retired development schemas and downgrades start fresh. A public version with no migration
    // path must fail to open, because a silent reset would erase the only copy of the user's history.
    .fallbackToDestructiveMigrationFrom(true, *RETIRED_DEVELOPMENT_SCHEMAS)
    .fallbackToDestructiveMigrationOnDowngrade(true)
    .addCallback(snapshotCleanupCallback)
    .addCallback(databaseDiagnosticsCallback)
    .setQueryCoroutineContext(Dispatchers.Default)
    .build()

/**
 * Records the schema version each time the database opens (the diagnostics sink turns a change into
 * "created" or "upgraded from X to Y"), and a destructive reset of a retired development schema or a downgrade.
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
