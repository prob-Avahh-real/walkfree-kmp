package com.walkfree.data

import com.walkfree.model.GpsPoint
import com.walkfree.model.SensorReading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

actual class PhoneSensors {
    actual fun accelerometerStream(): Flow<SensorReading> = emptyFlow()
    actual fun gyroscopeStream(): Flow<SensorReading> = emptyFlow()
}

actual class LocationProvider {
    actual fun locationStream(): Flow<GpsPoint> = emptyFlow()
    actual suspend fun lastLocation(): GpsPoint? = null
}
