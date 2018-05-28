package com.bernaferrari.changedetection

import android.content.Context
import com.bernaferrari.changedetection.data.source.DiffsRepository
import com.bernaferrari.changedetection.data.source.SitesRepository
import com.bernaferrari.changedetection.data.source.local.ChangeDatabase
import com.bernaferrari.changedetection.data.source.local.DiffsLocalDataSource
import com.bernaferrari.changedetection.data.source.local.SitesLocalDataSource
import com.bernaferrari.changedetection.util.AppExecutors
import kotlin.jvm.internal.Intrinsics.checkNotNull

/**
 * Enables injection of mock implementations for
 * [TasksDataSource] at compile time. This is useful for testing, since it allows us to use
 * a fake instance of the class to isolate the dependencies and run a test hermetically.
 *  * Inspired from Architecture Components MVVM sample app
 */
object Injection {

    fun provideDiffsRepository(context: Context): DiffsRepository {
        checkNotNull(context)
        val database = ChangeDatabase.getInstance(context)
        return DiffsRepository.getInstance(
            DiffsLocalDataSource.getInstance(
                AppExecutors(),
                database.diffsDao()
            )
        )
    }

    fun provideSitesRepository(context: Context): SitesRepository {
        checkNotNull(context)
        val database = ChangeDatabase.getInstance(context)
        return SitesRepository.getInstance(
            SitesLocalDataSource.getInstance(
                AppExecutors(),
                database.siteDao(),
                database.diffsDao()
            )
        )
    }
}
