package com.walkfree.data

import com.walkfree.model.*
import com.walkfree.platform.ScanResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

expect class BleAdapter {
    fun scan(): Flow<ScanResult>
    suspend fun connect(address: String)
    fun disconnect()
    fun observeNotifications(serviceUuid: String, charUuid: String): Flow<ByteArray>
    val connectedDevice: StateFlow<String?>
}

expect class PhoneSensors {
    fun accelerometerStream(): Flow<SensorReading>
    fun gyroscopeStream(): Flow<SensorReading>
}

expect class LocationProvider {
    fun locationStream(): Flow<GpsPoint>
    suspend fun lastLocation(): GpsPoint?
}

interface IMonitoringRepository {
    val currentSession: StateFlow<MonitoringSession?>
    val isMonitoring: StateFlow<Boolean>
    val latestReading: StateFlow<SensorReading?>

    fun sensorStream(): Flow<SensorReading>
    fun phoneSensorStream(): Flow<SensorReading>

    fun startSession(mode: ActivityMode): MonitoringSession
    fun stopSession(): MonitoringSession?
    fun getSessions(): List<MonitoringSession>
}

interface ISessionStore {
    suspend fun save(session: MonitoringSession)
    suspend fun loadAll(): List<MonitoringSession>
    suspend fun delete(id: String)
}
