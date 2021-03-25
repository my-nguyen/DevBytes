/*
 * Copyright (C) 2019 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.devbyteviewer

import android.app.Application
import android.os.Build
import androidx.work.*
import com.example.android.devbyteviewer.work.RefreshDataWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Override application to setup background work via WorkManager
 */
// DevByteApplication class is a good place to schedule the WorkManager
class DevByteApplication : Application() {

    private val applicationScope = CoroutineScope(Dispatchers.Default)

    /**
     * onCreate is called before the first screen is shown to the user.
     *
     * Use it to setup any background tasks, running expensive setup operations in a background
     * thread to avoid delaying app start.
     */
    override fun onCreate() {
        super.onCreate()
        delayedInit()
    }

    private fun delayedInit() {
        // start a coroutine
        applicationScope.launch {
            Timber.plant(Timber.DebugTree())
            setupRecurringWork()
        }
    }

    // A Worker defines a unit of work, and the WorkRequest defines how and when work should be run
    // Setup WorkManager background job to 'fetch' new network data daily
    private fun setupRecurringWork() {
        // create a Constraints object
        val constraints = Constraints.Builder()
                // constraint: the work request will only run when the device is on an unmetered network
                .setRequiredNetworkType(NetworkType.UNMETERED)
                // constraint: the work request should run only if the battery is not low
                .setRequiresBatteryNotLow(true)
                // constraint: the work request runs only when the device is charging
                .setRequiresCharging(true)
                // constraint: the work request runs only when the device is idle (when the user isn't actively using the device)
                .apply {
                    // This feature is only available in Android 6.0 (Marshmallow) and higher
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        setRequiresDeviceIdle(true)
                    }
                }
                .build()

        // create a periodic work request to run a RefreshDataWorker once a day
        val repeatingRequest = PeriodicWorkRequestBuilder<RefreshDataWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()
        // test request with a periodic repeat interval of 15 minutes
        /*val repeatingRequest = PeriodicWorkRequestBuilder<RefreshDataWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()*/

        Timber.d("Periodic Work request for sync is scheduled")

        // schedule the WorkRequest with WorkManager via enqueueUniquePeriodicWork(), which allows
        // you to add a uniquely named PeriodicWorkRequest to the queue, where only one
        // PeriodicWorkRequest of a particular name can be active at a time.
        // If pending (uncompleted) work exists with the same name, the ExistingPeriodicWorkPolicy.KEEP
        // parameter makes the WorkManager keep the previous periodic work and discard the new work request.
        WorkManager.getInstance()
                .enqueueUniquePeriodicWork(RefreshDataWorker.WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, repeatingRequest)
    }
}
