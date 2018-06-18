package com.bernaferrari.changedetection.data.source.local

import android.arch.lifecycle.LiveData
import android.arch.paging.DataSource
import android.content.Context
import android.support.annotation.VisibleForTesting
import com.bernaferrari.changedetection.Application
import com.bernaferrari.changedetection.data.ContentTypeInfo
import com.bernaferrari.changedetection.data.Snap
import com.bernaferrari.changedetection.data.source.Result
import com.bernaferrari.changedetection.data.source.SnapsDataSource
import com.bernaferrari.changedetection.extensions.cleanUpHtml
import com.bernaferrari.changedetection.util.AppExecutors
import com.orhanobut.logger.Logger
import kotlinx.coroutines.experimental.withContext
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

    override suspend fun deleteAllSnaps(siteId: String) {
        mSnapsDao.getAllSnapsForSiteId(siteId).forEach {
            Application.instance.deleteFile(it.snapId)
            mSnapsDao.deleteSnapById(it.snapId)
        }
    }

    override suspend fun deleteSnap(snapId: String) = withContext(mAppExecutors.ioContext) {
        Application.instance.deleteFile(snapId)
        mSnapsDao.deleteSnapById(snapId)
    }

    override suspend fun getSnaps(siteId: String): LiveData<List<Snap>> =
        withContext(mAppExecutors.ioContext) {
            mSnapsDao.getAllSnapsForSiteIdWithLiveData(siteId)
        }

    override suspend fun getSnapsFiltered(siteId: String, filter: String): LiveData<List<Snap>> =
        withContext(mAppExecutors.ioContext) {
            mSnapsDao.getAllSnapsForSiteIdFilteredWithLiveData(siteId, filter)
        }

    override suspend fun getSnapContent(snapId: String): ByteArray =
        withContext(mAppExecutors.ioContext) {
            Application.instance.openFileInput(snapId).readBytes()
        }

    override suspend fun saveSnap(snap: Snap, content: ByteArray): Result<Snap> =
        withContext(mAppExecutors.ioContext) {

            val lastSnapValue = mSnapsDao.getLastSnapForSiteId(snap.siteId).let { previousValue ->
                if (fileExists(Application.instance, previousValue?.snapId)) {
                    Application.instance.openFileInput(previousValue?.snapId).readBytes()
                } else {
                    ByteArray(0)
                }
            }

            // uncomment for testing:
            //  val lastSnapValue = ByteArray(0)
            // mSnapsDao.insertSnap(snap.copy(value = snap.value.plus(UUID.randomUUID().toString())))

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

                Result.Success(snap)
            } else {
                Logger.d("Beep beep! No difference detected!")
                Result.Error()
            }
        }

    override suspend fun deleteSnapsForSiteIdAndContentType(siteId: String, contentType: String) =
        withContext(mAppExecutors.ioContext) {
            mSnapsDao.getAllSnapsForSiteIdAndContentType(siteId, contentType).forEach {
                Application.instance.deleteFile(it.snapId)
                mSnapsDao.deleteSnapById(it.snapId)
            }
        }

    override suspend fun getContentTypeInfo(siteId: String): List<ContentTypeInfo> =
        withContext(mAppExecutors.ioContext) {

            // "SELECT count(*), contentType..." generates an error from Room, so this is needed.
            val listOfContentTypes = mutableListOf<ContentTypeInfo>()
            mSnapsDao.getContentTypesCount(siteId).forEachIndexed { index, count ->
                listOfContentTypes += ContentTypeInfo(
                    mSnapsDao.getContentTypesParams(siteId)[index],
                    count
                )
            }

            mSnapsDao.getContentTypesParams(siteId)

            listOfContentTypes
    }

    override suspend fun getMostRecentSnap(siteId: String): Snap? =
        withContext(mAppExecutors.ioContext) {
            mSnapsDao.getLastSnapForSiteId(siteId)
        }

    override suspend fun getSnapForPaging(
        siteId: String,
        filter: String
    ): DataSource.Factory<Int, Snap> = withContext(mAppExecutors.ioContext) {
        mSnapsDao.getSnapsForSiteIdForPaging(siteId, filter)
    }

    override suspend fun getSnapPair(
        originalId: String,
        newId: String
    ): Pair<Pair<Snap, ByteArray>, Pair<Snap, ByteArray>> = withContext(mAppExecutors.ioContext) {

        val originalSnap = mSnapsDao.getSnapById(originalId)!!
        val newSnap = mSnapsDao.getSnapById(newId)!!

        val originalContent = Application.instance.openFileInput(originalId).readBytes()
        val newContent = Application.instance.openFileInput(newId).readBytes()

        Pair(
            Pair(originalSnap, originalContent),
            Pair(newSnap, newContent)
        )
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
