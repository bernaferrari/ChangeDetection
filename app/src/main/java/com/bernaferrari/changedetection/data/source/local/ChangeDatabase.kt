package com.bernaferrari.changedetection.data.source.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bernaferrari.changedetection.data.Site
import com.bernaferrari.changedetection.data.Snap

/**
 * The Room Database that contains the Site table.
 * Inspired from Architecture Components MVVM sample app
 */
@Database(entities = [(Site::class), (Snap::class)], version = 1, exportSchema = false)
abstract class ChangeDatabase : RoomDatabase() {

    abstract fun siteDao(): SitesDao

    abstract fun snapsDao(): SnapsDao
}
