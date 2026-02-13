package com.schengen.tracker.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.schengen.tracker.SchengenApp
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

class LocationTrackingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val hasFine = ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) return Result.retry()

        val fusedClient = LocationServices.getFusedLocationProviderClient(applicationContext)
        val location = runCatching {
            fusedClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                CancellationTokenSource().token
            ).await()
        }.getOrNull() ?: return Result.retry()

        val countryResolver = CountryResolver(applicationContext)
        val iso = countryResolver.resolveIsoCountryCode(location.latitude, location.longitude)
            ?: return Result.retry()

        val inSchengen = iso.uppercase() in SchengenCountries.isoCodes
        val repository = (applicationContext as SchengenApp).repository
        repository.addAutoState(inSchengen, LocalDate.now())
        return Result.success()
    }
}
