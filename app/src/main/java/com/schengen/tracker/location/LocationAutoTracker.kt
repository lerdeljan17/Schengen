package com.schengen.tracker.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.schengen.tracker.data.StayRepository
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

sealed interface AutoLocationCheckResult {
    data class Updated(val countryCode: String, val inSchengen: Boolean) : AutoLocationCheckResult

    data object MissingPermission : AutoLocationCheckResult
    data object LocationUnavailable : AutoLocationCheckResult
    data object CountryUnavailable : AutoLocationCheckResult
}

class LocationAutoTracker(
    private val context: Context,
    private val repository: StayRepository
) {
    companion object {
        private const val MAX_LOCATION_AGE_MS = 3 * 60 * 1000L
    }

    suspend fun runCheck(): AutoLocationCheckResult {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) return AutoLocationCheckResult.MissingPermission

        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val now = System.currentTimeMillis()
        val highAccuracyLocation = runCatching {
            fusedClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).await()
        }.getOrNull()
        val balancedLocation = runCatching {
            fusedClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                CancellationTokenSource().token
            ).await()
        }.getOrNull()
        val lastLocation = runCatching { fusedClient.lastLocation.await() }.getOrNull()

        val location = listOf(highAccuracyLocation, balancedLocation, lastLocation)
            .filterNotNull()
            .firstOrNull { it.isUsable(now) }
            ?: return AutoLocationCheckResult.LocationUnavailable

        LocationGeofenceManager.upsertMovementAnchor(
            context = context,
            latitude = location.latitude,
            longitude = location.longitude
        )

        val countryResolver = CountryResolver(context)
        val countryCode = countryResolver.resolveIsoCountryCode(location.latitude, location.longitude)
            ?: return AutoLocationCheckResult.CountryUnavailable

        val inSchengen = countryCode in SchengenCountries.isoCodes
        repository.addAutoState(inSchengen, LocalDate.now())
        return AutoLocationCheckResult.Updated(countryCode, inSchengen)
    }

    private fun Location.isUsable(nowMillis: Long): Boolean {
        if (time <= 0L) return false
        val ageMillis = nowMillis - time
        return ageMillis in 0..MAX_LOCATION_AGE_MS
    }
}
