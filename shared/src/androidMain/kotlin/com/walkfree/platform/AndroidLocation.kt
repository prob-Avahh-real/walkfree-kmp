// Android GPS / location via FusedLocationProviderClient
package com.walkfree.platform

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import com.walkfree.data.LocationProvider
import com.walkfree.model.GpsPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

@SuppressLint("MissingPermission")
actual class LocationProvider(private val context: Context) : LocationProvider {
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    private val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
        .setMinUpdateDistanceMeters(5f)
        .build()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun locationStream(): Flow<GpsPoint> = callbackFlow {
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    trySend(GpsPoint(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        timestamp = loc.time
                    ))
                }
            }
        }
        fusedClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
        awaitClose { fusedClient.removeLocationUpdates(callback) }
    }

    override suspend fun lastLocation(): GpsPoint? = withContext(Dispatchers.IO) {
        try {
            val loc: Location = fusedClient.lastLocation.await()
            GpsPoint(loc.latitude, loc.longitude, loc.time)
        } catch (e: Exception) {
            null
        }
    }
}
