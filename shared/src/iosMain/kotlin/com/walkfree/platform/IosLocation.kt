// iOS location via CoreLocation (CLLocationManager)
package com.walkfree.platform

import com.walkfree.data.LocationProvider
import com.walkfree.model.GpsPoint
import kotlinx.cinterop.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.CoreLocation.*
import platform.Foundation.NSOperationQueue
import platform.darwin.NSObject

actual class LocationProvider : LocationProvider {
    private val locationManager = CLLocationManager()
    private val delegate = LocationDelegate()

    init {
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.distanceFilter = 5.0
        locationManager.delegate = delegate
    }

    override fun locationStream(): Flow<GpsPoint> = callbackFlow {
        delegate.onLocation = { loc ->
            trySend(GpsPoint(
                latitude = loc.coordinate.latitude,
                longitude = loc.coordinate.longitude,
                timestamp = (loc.timestamp.timeIntervalSince1970 * 1000).toLong()
            ))
        }
        locationManager.startUpdatingLocation()
        awaitClose { locationManager.stopUpdatingLocation() }
    }

    override suspend fun lastLocation(): GpsPoint? {
        val loc = locationManager.location ?: return null
        return GpsPoint(
            latitude = loc.coordinate.latitude,
            longitude = loc.coordinate.longitude,
            timestamp = (loc.timestamp.timeIntervalSince1970 * 1000).toLong()
        )
    }
}

class LocationDelegate : NSObject(), CLLocationManagerDelegateProtocol {
    var onLocation: ((CLLocation) -> Unit)? = null

    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val locations = didUpdateLocations as List<CLLocation>
        locations.lastOrNull()?.let { onLocation?.invoke(it) }
    }
}
