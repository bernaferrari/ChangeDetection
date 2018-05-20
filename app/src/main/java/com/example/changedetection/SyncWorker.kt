package com.example.changedetection

import androidx.work.Worker
import com.orhanobut.logger.Logger
import android.databinding.adapters.TimePickerBindingAdapter.getHour
import org.threeten.bp.LocalTime


class SyncWorker : Worker() {

    override fun doWork(): Worker.WorkerResult {

        // Do the work here--in this case, compress the stored images.
        // In this example no parameters are passed; the site is
        // assumed to be "compress the whole library."
        val now = LocalTime.now()

        Logger.d("Doing background work! " + now.hour + ":" + now.minute)

        // Indicate success or failure with your return value:
        return WorkerResult.SUCCESS

        // (Returning RETRY tells WorkManager to try this site again
        // later; FAILURE says not to try again.)
    }
}