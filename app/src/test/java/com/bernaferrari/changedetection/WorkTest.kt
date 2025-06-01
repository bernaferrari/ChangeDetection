package com.bernaferrari.changedetection

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import org.junit.Assert
import org.junit.Test


class WorkTest {
    @Test
    fun addition_isCorrect() {

        val photoWork = OneTimeWorkRequest.Builder(
            SyncWorker::class.java)
            .build()

        WorkManager.getInstance().beginUniqueWork("work2", ExistingWorkPolicy.REPLACE, photoWork).enqueue()

        Assert.assertEquals(4, 2 + 2)
    }


}
