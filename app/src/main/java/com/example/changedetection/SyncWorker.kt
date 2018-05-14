package com.example.changedetection

import androidx.work.Worker


class SyncWorker : Worker() {

    override fun doWork(): Worker.WorkerResult {

        // Do the work here--in this case, compress the stored images.
        // In this example no parameters are passed; the task is
        // assumed to be "compress the whole library."


        // Indicate success or failure with your return value:
        return WorkerResult.SUCCESS

        // (Returning RETRY tells WorkManager to try this task again
        // later; FAILURE says not to try again.)
    }
}