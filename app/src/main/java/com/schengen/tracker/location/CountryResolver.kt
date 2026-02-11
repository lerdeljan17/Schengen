package com.schengen.tracker.location

import android.content.Context
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class CountryResolver(private val context: Context) {
    suspend fun resolveIsoCountryCode(latitude: Double, longitude: Double): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val geocoder = Geocoder(context, Locale.getDefault())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    var countryCode: String? = null
                    val latch = java.util.concurrent.CountDownLatch(1)
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        countryCode = addresses.firstOrNull()?.countryCode
                        latch.countDown()
                    }
                    latch.await()
                    countryCode
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 1)
                        ?.firstOrNull()
                        ?.countryCode
                }
            }.getOrNull()
        }
}
