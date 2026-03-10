package com.schengen.tracker.location

import android.content.Context
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.net.HttpURLConnection
import java.net.URL

class CountryResolver(private val context: Context) {
    suspend fun resolveIsoCountryCode(latitude: Double, longitude: Double): String? =
        withContext(Dispatchers.IO) {
            val platform = resolveWithPlatformGeocoder(latitude, longitude)
            if (platform != null) return@withContext platform
            resolveWithNominatim(latitude, longitude)
        }

    private fun resolveWithPlatformGeocoder(latitude: Double, longitude: Double): String? {
        if (!Geocoder.isPresent()) return null
        return runCatching {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                var countryCode: String? = null
                val latch = CountDownLatch(1)
                geocoder.getFromLocation(
                    latitude,
                    longitude,
                    1,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<android.location.Address>) {
                            countryCode = addresses.firstOrNull()?.countryCode
                            latch.countDown()
                        }

                        override fun onError(errorMessage: String?) {
                            latch.countDown()
                        }
                    }
                )
                latch.await(5, TimeUnit.SECONDS)
                countryCode
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitude, longitude, 1)
                    ?.firstOrNull()
                    ?.countryCode
            }
        }.getOrNull()?.uppercase(Locale.ROOT)
    }

    private fun resolveWithNominatim(latitude: Double, longitude: Double): String? {
        val endpoint =
            "https://nominatim.openstreetmap.org/reverse?format=jsonv2&addressdetails=1&zoom=5" +
                "&lat=$latitude&lon=$longitude"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5_000
            readTimeout = 5_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "SchengenTracker/1.0 (Android)")
        }
        return runCatching {
            connection.inputStream.bufferedReader().use { reader ->
                val body = reader.readText()
                JSONObject(body)
                    .optJSONObject("address")
                    ?.optString("country_code")
                    ?.takeIf { it.isNotBlank() }
                    ?.uppercase(Locale.ROOT)
            }
        }.getOrNull().also {
            connection.disconnect()
        }
    }
}
