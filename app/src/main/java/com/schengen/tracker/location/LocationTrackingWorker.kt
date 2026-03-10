package com.schengen.tracker.location

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.schengen.tracker.SchengenApp

class LocationTrackingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val repository = (applicationContext as SchengenApp).repository
        val tracker = LocationAutoTracker(applicationContext, repository)

        return when (tracker.runCheck()) {
            AutoLocationCheckResult.MissingPermission -> Result.success()

            AutoLocationCheckResult.LocationUnavailable -> Result.retry()

            AutoLocationCheckResult.CountryUnavailable -> Result.retry()

            is AutoLocationCheckResult.Updated -> Result.success()
        }
    }
}
