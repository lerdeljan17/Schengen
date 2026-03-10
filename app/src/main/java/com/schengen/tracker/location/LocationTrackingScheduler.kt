package com.schengen.tracker.location

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object LocationTrackingScheduler {
    private const val PERIODIC_WORK_NAME = "schengen_location_tracking_periodic"
    private const val IMMEDIATE_WORK_NAME = "schengen_location_tracking_immediate"

    fun schedule(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val work = PeriodicWorkRequestBuilder<LocationTrackingWorker>(6, TimeUnit.HOURS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            work
        )
        enqueueImmediateCheck(context)
    }

    fun enqueueImmediateCheck(context: Context) {
        val work = OneTimeWorkRequestBuilder<LocationTrackingWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            work
        )
    }

    fun cancel(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
        workManager.cancelUniqueWork(IMMEDIATE_WORK_NAME)
        LocationGeofenceManager.clear(context)
    }

    suspend fun isScheduled(context: Context): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val workManager = WorkManager.getInstance(context)
            val infos = workManager.getWorkInfosForUniqueWork(PERIODIC_WORK_NAME).get() +
                workManager.getWorkInfosForUniqueWork(IMMEDIATE_WORK_NAME).get()
            infos.any { info ->
                info.state == WorkInfo.State.ENQUEUED ||
                    info.state == WorkInfo.State.RUNNING ||
                    info.state == WorkInfo.State.BLOCKED
            }
        }.getOrDefault(false)
    }
}
