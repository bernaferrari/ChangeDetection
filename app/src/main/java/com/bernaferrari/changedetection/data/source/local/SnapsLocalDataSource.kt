package com.bernaferrari.changedetection.data.source.local

import android.arch.lifecycle.LiveData
import android.arch.paging.DataSource
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.annotation.VisibleForTesting
import com.bernaferrari.changedetection.data.MinimalSnap
import com.bernaferrari.changedetection.data.Snap
import com.bernaferrari.changedetection.data.source.SnapsDataSource
import com.bernaferrari.changedetection.extensions.cleanUpHtml
import com.bernaferrari.changedetection.extensions.readableFileSize
import com.bernaferrari.changedetection.util.AppExecutors
import com.orhanobut.logger.Logger
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import kotlin.math.roundToInt


/**
 * Concrete implementation of a data source as a db.
 * Inspired from Architecture Components MVVM sample app
 */
class SnapsLocalDataSource// Prevent direct instantiation.
private constructor(
    private val mAppExecutors: AppExecutors,
    private val mSnapsDao: SnapsDao
) : SnapsDataSource {


    override fun getMinimalSnaps(siteId: String): LiveData<List<MinimalSnap>> {
        return mSnapsDao.getAllMinimalSnapsForSiteId(siteId)
    }


    override fun getHeavySnapForPaging(siteId: String): DataSource.Factory<Int, Snap> {
        return mSnapsDao.getAllSnapsForSiteIdForPaging(siteId)
    }

    /**
     * get the most recent diffs without value.
     *
     * @param siteId the site url for filtering the diffs.
     */
    override fun getMostRecentMinimalSnaps(siteId: String, callback: (List<Int>) -> Unit) {
        val runnable = Runnable {
            val original = mSnapsDao.getLastSnapsSize(siteId)

            mAppExecutors.mainThread().execute {
                callback.invoke(original!!)
            }
        }

        mAppExecutors.diskIO().execute(runnable)
    }

    /**
     * get diffs for Paging Adapter.
     *
     * @param siteId the site url for filtering the diffs.
     */
    override fun getSnapForPaging(siteId: String): DataSource.Factory<Int, MinimalSnap> {
        return mSnapsDao.getAllMinimalSnapsForSiteIdForPaging(siteId)
    }

    override fun getSnapPair(
        originalId: String,
        newId: String,
        callback: SnapsDataSource.GetPairCallback
    ) {
        val runnable = Runnable {
            val original = mSnapsDao.getSnapById(originalId)
            val new = mSnapsDao.getSnapById(newId)

            mAppExecutors.mainThread().execute {
                if (original != null && new != null) {
                    callback.onSnapsLoaded(Pair(original, new))
                } else {
                    callback.onDataNotAvailable()
                }
            }
        }

        mAppExecutors.diskIO().execute(runnable)
    }

    override fun getSnap(snapId: String, callback: SnapsDataSource.GetSnapsCallback) {
        val runnable = Runnable {
            val diff = mSnapsDao.getSnapById(snapId)

            mAppExecutors.mainThread().execute {
                if (diff != null) {
                    callback.onSnapsLoaded(diff)
                } else {
                    callback.onDataNotAvailable()
                }
            }
        }

        mAppExecutors.diskIO().execute(runnable)
    }

    private fun compress(snap: Snap): Snap {

        // from observations, where a 18mb would become:
        // size | sampleSize
        // 1 - 18mb
        // 2 - 9mb
        // 3 - 3.5mb
        // 4 - 2.4mb
        // 5 - 1.8mb
        // 8 - 650kb
        // I came to a formula where estimatedFinalSize = size / (x^2/2).
        // for example, 18mb/(8*4) ~= 560kb ~= 650kb from the table.
        // so.. 600kb/size = 1/(x^2/2) => size/600kb = x^2/2 => x = sqrt(size/300kb)

        val options = BitmapFactory.Options()
        options.inSampleSize = Math.sqrt(snap.contentSize / 300000.0).roundToInt()

        if (options.inSampleSize == 0) {
            return snap
        }

        val bmp = BitmapFactory.decodeByteArray(snap.content, 0, snap.contentSize, options)

        val stream = ByteArrayOutputStream()
        Logger.d("Previous Size: " + snap.contentSize.readableFileSize())
        if (bmp.compress(Bitmap.CompressFormat.JPEG, 90, stream)) {
            val byteArray = stream.toByteArray()
            bmp.recycle()
            Logger.d("New Size: " + byteArray.size.readableFileSize())

            return snap.copy(contentSize = byteArray.size, content = byteArray)
        }
        return snap
    }

    override fun saveSnap(snap: Snap, callback: SnapsDataSource.GetSnapsCallback) {
        val saveRunnable = Runnable {
            val lastSnapValue = mSnapsDao.getLastSnapValueForSiteId(snap.siteId)

            val newSnap = if (snap.contentType.contains("image")) {
                compress(snap)
            } else {
                snap
            }

            // Uncomment for testing.
            // mSnapsDao.insertSnap(minimalSnap.copy(value = minimalSnap.value.plus(UUID.randomUUID().toString())))
            val wasSuccessful =
                if (newSnap.content.isNotEmpty() && lastSnapValue?.toString(Charset.defaultCharset())?.cleanUpHtml() != newSnap.content.toString(
                        Charset.defaultCharset()
                    ).cleanUpHtml()
                ) {
                    println(lastSnapValue)
                    Logger.d("Difference detected! Size went from ${lastSnapValue?.size} to ${snap.content.size}")
                    mSnapsDao.insertSnap(newSnap)
                    true
                } else {
                    Logger.d("Beep beep! No difference detected!")
                    false
                }

            mAppExecutors.mainThread().execute {
                if (wasSuccessful) {
                    callback.onSnapsLoaded(newSnap)
                } else {
                    callback.onDataNotAvailable()
                }
            }
        }

        mAppExecutors.diskIO().execute(saveRunnable)
    }

    override fun deleteSnap(snapId: String) {
        val deleteRunnable = Runnable { mSnapsDao.deleteSnapById(snapId) }
        mAppExecutors.diskIO().execute(deleteRunnable)
    }

    override fun deleteAllSnapsForSite(siteId: String) {
        val runnable = Runnable {
            mSnapsDao.deleteAllSnapsForSite(siteId)
        }

        mAppExecutors.diskIO().execute(runnable)
    }

    companion object {
        @Volatile
        private var INSTANCE: SnapsLocalDataSource? = null

        fun getInstance(
            appExecutors: AppExecutors,
            snapsDao: SnapsDao
        ): SnapsLocalDataSource {
            if (INSTANCE == null) {
                synchronized(SnapsLocalDataSource::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = SnapsLocalDataSource(appExecutors, snapsDao)
                    }
                }
            }
            return INSTANCE!!
        }

        @VisibleForTesting
        internal fun clearInstance() {
            INSTANCE = null
        }
    }
}
