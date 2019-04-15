package com.bernaferrari.changedetection.repo.source.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap

/**
 * The Room Database that contains the Site table.
 * Inspired from Architecture Components MVVM sample app
 */
@Database(entities = [(Site::class), (Snap::class)], version = 1, exportSchema = false)
abstract class ChangeDatabase : RoomDatabase() {

    abstract fun siteDao(): SitesDao

    abstract fun snapsDao(): SnapsDao
}
