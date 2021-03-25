package com.example.android.devbyteviewer.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.android.devbyteviewer.database.getDatabase
import com.example.android.devbyteviewer.repository.VideosRepository
import retrofit2.HttpException

// this is a Worker class, where you define the actual work (the task) to run in the background.
// You extend this class and override the doWork() method. The doWork() method is where you put code
// to be performed in the background, such as syncing data with the server or processing images.
class RefreshDataWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    companion object {
        const val WORK_NAME = "com.example.android.devbyteviewer.work.RefreshDataWorker"
    }

    // The doWork() method inside the Worker class is called on a background thread. The method
    // performs work synchronously, and should return a ListenableWorker.Result object. The Android
    // system gives a Worker a maximum of 10 minutes to finish its execution and return a
    // ListenableWorker.Result object. After this time has expired, the system forcefully stops the Worker
    override suspend fun doWork(): Result {
        val database = getDatabase(applicationContext)
        val repository = VideosRepository(database)
        try {
            repository.refreshVideos()
        } catch (e: HttpException) {
            return Result.retry()
        }
        return Result.success()
    }
}