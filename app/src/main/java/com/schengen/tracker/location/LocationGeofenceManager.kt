package com.schengen.tracker.location

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await

object LocationGeofenceManager {
    private const val REQUEST_ID = "schengen_movement_anchor"
    private const val GEOFENCE_REQUEST_CODE = 1001
    private const val LOCATION_UPDATES_REQUEST_CODE = 1002
    private const val RADIUS_METERS = 5_000f
    private const val NOTIFICATION_RESPONSIVENESS_MS = 2 * 60 * 1000
    private const val LOCATION_UPDATE_INTERVAL_MS = 15 * 60 * 1000L
    private const val LOCATION_UPDATE_MIN_INTERVAL_MS = 2 * 60 * 1000L
    private const val LOCATION_UPDATE_MIN_DISTANCE_METERS = 2_000f
    const val ACTION_LOCATION_GEOFENCE_TRANSITION =
        "com.schengen.tracker.location.ACTION_LOCATION_GEOFENCE_TRANSITION"
    const val ACTION_LOCATION_MOVED =
        "com.schengen.tracker.location.ACTION_LOCATION_MOVED"

    suspend fun upsertMovementAnchor(context: Context, latitude: Double, longitude: Double): Boolean {
        if (!hasAnyLocationPermission(context)) return false
        val geofence = Geofence.Builder()
            .setRequestId(REQUEST_ID)
            .setCircularRegion(latitude, longitude, RADIUS_METERS)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
            .setNotificationResponsiveness(NOTIFICATION_RESPONSIVENESS_MS)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .build()
        val request = GeofencingRequest.Builder()
            .addGeofence(geofence)
            .build()
        val geofencingClient = LocationServices.getGeofencingClient(context)
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val geofencePendingIntent = geofencePendingIntent(context)
        val locationUpdatesPendingIntent = locationUpdatesPendingIntent(context)
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            LOCATION_UPDATE_INTERVAL_MS
        )
            .setMinUpdateIntervalMillis(LOCATION_UPDATE_MIN_INTERVAL_MS)
            .setMinUpdateDistanceMeters(LOCATION_UPDATE_MIN_DISTANCE_METERS)
            .build()
        return runCatching {
            geofencingClient.removeGeofences(geofencePendingIntent).await()
            geofencingClient.addGeofences(request, geofencePendingIntent).await()
            fusedClient.removeLocationUpdates(locationUpdatesPendingIntent).await()
            fusedClient.requestLocationUpdates(locationRequest, locationUpdatesPendingIntent).await()
        }.isSuccess
    }

    fun clear(context: Context) {
        val geofencingClient = LocationServices.getGeofencingClient(context)
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        runCatching { geofencingClient.removeGeofences(geofencePendingIntent(context)) }
        runCatching { fusedClient.removeLocationUpdates(locationUpdatesPendingIntent(context)) }
    }

    private fun geofencePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, LocationGeofenceReceiver::class.java).apply {
            action = ACTION_LOCATION_GEOFENCE_TRANSITION
        }
        return PendingIntent.getBroadcast(
            context,
            GEOFENCE_REQUEST_CODE,
            intent,
            pendingIntentFlags()
        )
    }

    private fun locationUpdatesPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, LocationGeofenceReceiver::class.java).apply {
            action = ACTION_LOCATION_MOVED
        }
        return PendingIntent.getBroadcast(
            context,
            LOCATION_UPDATES_REQUEST_CODE,
            intent,
            pendingIntentFlags()
        )
    }

    private fun pendingIntentFlags(): Int {
        val update = PendingIntent.FLAG_UPDATE_CURRENT
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            update or PendingIntent.FLAG_MUTABLE
        } else {
            update
        }
    }

    private fun hasAnyLocationPermission(context: Context): Boolean {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return hasFine || hasCoarse
    }
}
