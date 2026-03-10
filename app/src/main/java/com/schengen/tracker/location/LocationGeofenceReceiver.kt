package com.schengen.tracker.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.LocationResult

class LocationGeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            LocationGeofenceManager.ACTION_LOCATION_MOVED -> {
                if (LocationResult.hasResult(intent)) {
                    LocationTrackingScheduler.enqueueImmediateCheck(context.applicationContext)
                }
            }

            LocationGeofenceManager.ACTION_LOCATION_GEOFENCE_TRANSITION -> {
                val event = GeofencingEvent.fromIntent(intent) ?: return
                if (event.hasError()) return
                if (event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_EXIT) return
                LocationTrackingScheduler.enqueueImmediateCheck(context.applicationContext)
            }
        }
    }
}
