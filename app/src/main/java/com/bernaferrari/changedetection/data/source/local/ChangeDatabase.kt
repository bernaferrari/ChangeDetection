package com.bernaferrari.changedetection.data.source.local

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context

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

    companion object {

        private val sLock = Any()
        private var INSTANCE: ChangeDatabase? = null

        fun getInstance(context: Context): ChangeDatabase {
            synchronized(sLock) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.applicationContext,
                        ChangeDatabase::class.java, "Changes.db"
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                }
                return INSTANCE!!
            }
        }
    }
}
