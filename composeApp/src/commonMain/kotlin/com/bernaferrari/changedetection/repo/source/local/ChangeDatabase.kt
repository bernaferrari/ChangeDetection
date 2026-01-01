package com.bernaferrari.changedetection.repo.source.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap

/**
 * The Room Database that contains the Site table.
 * Inspired from Architecture Components MVVM sample app
 */
@Database(entities = [Site::class, Snap::class], version = 3, exportSchema = false)
@TypeConverters(
    Site.ConstraintsConverter::class,
    Site.PairConverter::class
)
@ConstructedBy(ChangeDatabaseConstructor::class)
abstract class ChangeDatabase : RoomDatabase() {

    abstract fun siteDao(): SitesDao

    abstract fun snapsDao(): SnapsDao
}

// Helper for KMP Room instantiation
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object ChangeDatabaseConstructor : RoomDatabaseConstructor<ChangeDatabase>
