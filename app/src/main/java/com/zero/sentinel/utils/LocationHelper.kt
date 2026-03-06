package com.zero.sentinel.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object LocationHelper {

    private const val TAG = "LocationHelper"

    /**
     * Gets the current location on-demand.
     * This method assumes permissions (including background if called from worker) 
     * are already granted. It will suspend until a location is found or it times out.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location? = suspendCoroutine { continuation ->
        Log.d(TAG, "Requesting current location...")
        
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        // 1. Try to get last known location first for speed
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                // If we have a recent location (e.g. within last 10 minutes), use it
                val timeDelta = System.currentTimeMillis() - location.time
                if (timeDelta < 10 * 60 * 1000) {
                    Log.d(TAG, "Using fresh last known location from ${timeDelta / 1000}s ago.")
                    continuation.resume(location)
                    return@addOnSuccessListener
                }
            }

            Log.d(TAG, "Last known location unavailable or too old. Requesting fresh update.")
            // 2. Request a fresh location update
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMaxUpdates(1) // Only need one update
                .setMaxUpdateDelayMillis(10000) // Don't wait forever
                .build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    fusedLocationClient.removeLocationUpdates(this) // Stop listening
                    val newLocation = locationResult.lastLocation
                    if (newLocation != null) {
                        Log.d(TAG, "Received fresh location update.")
                        continuation.resume(newLocation)
                    } else {
                        Log.w(TAG, "Location update returned null.")
                        continuation.resume(null)
                    }
                }
            }

            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to request location updates", e)
                continuation.resume(null)
            }
        }.addOnFailureListener {
            Log.e(TAG, "Failed to get last known location", it)
            continuation.resume(null)
        }
    }

    /**
     * Formats a Location object into a readable string with a Google Maps link.
     */
    fun formatLocationMessage(location: Location?): String {
        if (location == null) return "📍 Location unavailable. Ensure GPS is on and permissions are granted."
        
        val lat = location.latitude
        val lon = location.longitude
        val acc = location.accuracy
        
        return """
            📍 *Location Retreived*
            Lat: `$lat`
            Lon: `$lon`
            Accuracy: `${acc}m`
            
            🗺 [Open in Google Maps](https://www.google.com/maps/search/?api=1&query=$lat,$lon)
        """.trimIndent()
    }
}
