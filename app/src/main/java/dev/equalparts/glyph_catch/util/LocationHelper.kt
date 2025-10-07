package dev.equalparts.glyph_catch.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Helper class for location-related operations.
 */
object LocationHelper {
    private const val TAG = "LocationHelper"
    private const val MAX_RESULTS = 1

    /**
     * Gets the current device location using coarse location permission. Returns null if
     * permission not granted or location not available.
     */
    suspend fun getCurrentLocation(context: Context): LocationResult? = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return@withContext null
        }

        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)

            for (provider in providers) {
                if (locationManager.isProviderEnabled(provider)) {
                    val location = locationManager.getLastKnownLocation(provider)
                    if (location != null) {
                        val cityName = getCityFromCoordinates(
                            context,
                            location.latitude,
                            location.longitude
                        )
                        return@withContext LocationResult(
                            latitude = location.latitude.toFloat(),
                            longitude = location.longitude.toFloat(),
                            cityName = cityName
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current location", e)
        }

        return@withContext null
    }

    /**
     * Geocodes a city name to coordinates.
     */
    suspend fun geocodeCity(context: Context, cityName: String): LocationResult? {
        if (cityName.isBlank()) {
            return null
        }

        return try {
            suspendCoroutine { continuation ->
                val geocoder = Geocoder(context, Locale.getDefault())

                geocoder.getFromLocationName(
                    cityName,
                    MAX_RESULTS,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<android.location.Address>) {
                            if (addresses.isNotEmpty()) {
                                val address = addresses[0]
                                continuation.resume(
                                    LocationResult(
                                        latitude = address.latitude.toFloat(),
                                        longitude = address.longitude.toFloat(),
                                        cityName = formatCityName(address)
                                    )
                                )
                            } else {
                                continuation.resume(null)
                            }
                        }

                        override fun onError(errorMessage: String?) {
                            Log.e(TAG, "Geocoding error: $errorMessage")
                            continuation.resume(null)
                        }
                    }
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error geocoding city: $cityName", e)
            null
        }
    }

    /**
     * Reverse geocodes coordinates to get city name using Android 13+ async API.
     */
    private suspend fun getCityFromCoordinates(context: Context, latitude: Double, longitude: Double): String? =
        suspendCoroutine { continuation ->
            try {
                val geocoder = Geocoder(context, Locale.getDefault())

                geocoder.getFromLocation(
                    latitude,
                    longitude,
                    MAX_RESULTS,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<android.location.Address>) {
                            if (addresses.isNotEmpty()) {
                                continuation.resume(formatCityName(addresses[0]))
                            } else {
                                continuation.resume(null)
                            }
                        }

                        override fun onError(errorMessage: String?) {
                            Log.e(TAG, "Reverse geocoding error: $errorMessage")
                            continuation.resume(null)
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error reverse geocoding", e)
                continuation.resume(null)
            }
        }

    /**
     * Formats an address into a readable city name.
     */
    private fun formatCityName(address: android.location.Address): String {
        val parts = mutableListOf<String>()
        address.locality?.let { parts.add(it) }
        address.adminArea?.let { parts.add(it) }
        if (parts.isEmpty()) {
            address.countryName?.let { parts.add(it) }
        }
        return parts.joinToString(", ").ifEmpty { "???" }
    }

    data class LocationResult(val latitude: Float, val longitude: Float, val cityName: String?)
}
