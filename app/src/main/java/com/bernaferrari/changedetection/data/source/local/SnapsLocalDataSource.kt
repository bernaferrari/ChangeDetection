package com.bernaferrari.changedetection.data.source.local

import android.arch.lifecycle.LiveData
import android.arch.paging.DataSource
import android.content.Context
import android.support.annotation.VisibleForTesting
import com.bernaferrari.changedetection.Application
import com.bernaferrari.changedetection.data.ContentTypeInfo
import com.bernaferrari.changedetection.data.Snap
import com.bernaferrari.changedetection.data.source.SnapsDataSource
import com.bernaferrari.changedetection.extensions.cleanUpHtml
import com.bernaferrari.changedetection.util.AppExecutors
import com.orhanobut.logger.Logger
import java.nio.charset.Charset


/**
 * Concrete implementation of a data source as a db.
 * Inspired from Architecture Components MVVM sample app
 */
class SnapsLocalDataSource// Prevent direct instantiation.
private constructor(
    private val mAppExecutors: AppExecutors,
    private val mSnapsDao: SnapsDao
) : SnapsDataSource {

    override fun deleteSnapsForSiteIdAndContentType(siteId: String, contentType: String) {
        val runnable = Runnable {
            mSnapsDao.getAllSnapsForSiteIdAndContentType(siteId, contentType).forEach {
                Application.instance.deleteFile(it.snapId)
                mSnapsDao.deleteSnapById(it.snapId)
            }
        }

        mAppExecutors.diskIO().execute(runnable)
    }

    override fun getSnapsFiltered(
        siteId: String,
        filter: String,
        callback: (LiveData<List<Snap>>) -> Unit
    ) {
        val runnable = Runnable {
            val liveSnaps = mSnapsDao.getAllSnapsForSiteIdFilteredWithLiveData(siteId, filter)

            mAppExecutors.mainThread().execute {
                callback.invoke(liveSnaps)
            }
        }

        mAppExecutors.diskIO().execute(runnable)
    }

    override fun getContentTypeInfo(siteId: String, callback: (List<ContentTypeInfo>) -> Unit) {
        val runnable = Runnable {

            // "SELECT count(*), contentType..." generates an error from Room, so this is needed.
            val listOfContentTypes = mutableListOf<ContentTypeInfo>()
            mSnapsDao.getContentTypesCount(siteId).forEachIndexed { index, count ->
                listOfContentTypes += ContentTypeInfo(
                    mSnapsDao.getContentTypesParams(siteId)[index],
                    count
                )
            }

            mSnapsDao.getContentTypesParams(siteId)
            mAppExecutors.mainThread().execute {
                callback.invoke(listOfContentTypes)
            }
        }

        mAppExecutors.diskIO().execute(runnable)
    }


    override fun getMostRecentSnap(siteId: String, callback: ((Snap?) -> (Unit))) {
        val runnable = Runnable {
            val snap = mSnapsDao.getLastSnapForSiteId(siteId)

            mAppExecutors.mainThread().execute {
                callback.invoke(snap)
            }
        }

        mAppExecutors.diskIO().execute(runnable)
    }


    override fun getSnaps(siteId: String, callback: ((LiveData<List<Snap>>) -> (Unit))) {
        val runnable = Runnable {
            val liveSnaps = mSnapsDao.getAllSnapsForSiteIdWithLiveData(siteId)

            mAppExecutors.mainThread().execute {
                callback.invoke(liveSnaps)
            }
        }

        mAppExecutors.diskIO().execute(runnable)
    }

    /**
     * get diffs for Paging Adapter.
     *
     * @param siteId the site url for filtering the diffs.
     */
    override fun getSnapForPaging(siteId: String, filter: String): DataSource.Factory<Int, Snap> {
        return mSnapsDao.getSnapsForSiteIdForPaging(siteId, filter)
    }

    override fun getSnapPair(
        originalId: String,
        newId: String,
        callback: ((Pair<Pair<Snap, ByteArray>, Pair<Snap, ByteArray>>) -> (Unit))
    ) {
        val runnable = Runnable {
            val originalSnap = mSnapsDao.getSnapById(originalId)!!
            val newSnap = mSnapsDao.getSnapById(newId)!!

            val originalContent = Application.instance.openFileInput(originalId).readBytes()
            val newContent = Application.instance.openFileInput(newId).readBytes()

            mAppExecutors.mainThread().execute {
                callback.invoke(
                    Pair(
                        Pair(originalSnap, originalContent),
                        Pair(newSnap, newContent)
                    )
                )
            }
        }

        mAppExecutors.diskIO().execute(runnable)
    }

    override fun getSnapContent(snapId: String, callback: ((ByteArray) -> (Unit))) {
        val runnable = Runnable {
            val content = Application.instance.openFileInput(snapId).readBytes()

            mAppExecutors.mainThread().execute {
                callback.invoke(content)
            }
        }

        mAppExecutors.diskIO().execute(runnable)
    }

//    private fun imageCompressor(snap: Snap): Snap {
//
////        // from observations, where a 18mb would become:
////        // size | sampleSize
////        // 1 - 18mb
////        // 2 - 9mb
////        // 3 - 3.5mb
////        // 4 - 2.4mb
////        // 5 - 1.8mb
////        // 8 - 650kb
////        // I came to a formula where estimatedFinalSize = size / (x^2/2).
////        // for example, 18mb/(8*4) ~= 560kb ~= 650kb from the table.
////        // so.. 600kb/size = 1/(x^2/2) => size/600kb = x^2/2 => x = sqrt(size/300kb)
////
////        val options = BitmapFactory.Options()
////        options.inSampleSize = Math.sqrt(snap.contentSize / 300000.0).roundToInt()
////
////        if (options.inSampleSize == 0) {
////            return snap
////        }
////
////        val bmp = BitmapFactory.decodeByteArray(snap.content, 0, snap.contentSize, options)
////
////        val stream = ByteArrayOutputStream()
////        Logger.i("Previous Size: " + snap.contentSize.readableFileSize())
////        if (bmp.compress(Bitmap.CompressFormat.JPEG, 90, stream)) {
////            val byteArray = stream.toByteArray()
////            bmp.recycle()
////            Logger.i("New Size: " + byteArray.size.readableFileSize())
////
////            return snap.copy(contentSize = byteArray.size, content = byteArray)
////        }
////        return snap
//    }


    private fun fileExists(context: Context, filename: String?): Boolean {
        if (filename == null) {
            return false
        }
        val file = context.getFileStreamPath(filename)
        return file != null && file.exists()
    }

    override fun saveSnap(
        snap: Snap,
        content: ByteArray,
        callback: SnapsDataSource.GetSnapsCallback
    ) {
        val saveRunnable = Runnable {

            val lastSnapValue = mSnapsDao.getLastSnapForSiteId(snap.siteId).let { previousValue ->
                if (fileExists(Application.instance, previousValue?.snapId)) {
                    Application.instance.openFileInput(previousValue?.snapId).readBytes()
                } else {
                    ByteArray(0)
                }
            }

            // uncomment for testing:
            //  val lastSnapValue = ByteArray(0)

            // Uncomment for testing.
            // mSnapsDao.insertSnap(snap.copy(value = snap.value.plus(UUID.randomUUID().toString())))
            val wasSuccessful =
                if (content.isNotEmpty() && lastSnapValue.toString(Charset.defaultCharset()).cleanUpHtml() != content.toString(
                        Charset.defaultCharset()
                    ).cleanUpHtml()
                ) {
                    println(lastSnapValue)
                    Logger.d("Difference detected! Size went from ${lastSnapValue.size} to ${content.size}")
                    mSnapsDao.insertSnap(snap)

                    Application.instance.openFileOutput(snap.snapId, Context.MODE_PRIVATE).use {
                        it.write(content)
                    }

                    true
                } else {
                    Logger.d("Beep beep! No difference detected!")
                    false
                }

            mAppExecutors.mainThread().execute {
                if (wasSuccessful) {
                    callback.onSnapsLoaded(snap)
                } else {
                    callback.onDataNotAvailable()
                }
            }
        }

        mAppExecutors.diskIO().execute(saveRunnable)
    }

    override fun deleteSnap(snapId: String) {
        val deleteRunnable = Runnable {
            Application.instance.deleteFile(snapId)
            mSnapsDao.deleteSnapById(snapId)
        }
        mAppExecutors.diskIO().execute(deleteRunnable)
    }

    override fun deleteAllSnaps(siteId: String) {
        val runnable = Runnable {
            mSnapsDao.getAllSnapsForSiteId(siteId).forEach {
                Application.instance.deleteFile(it.snapId)
                mSnapsDao.deleteSnapById(it.snapId)
            }
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
