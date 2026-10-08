package com.walkfree.data

import com.walkfree.AppConfig
import com.walkfree.engine.IDetectionEngine
import com.walkfree.model.*
import com.walkfree.platform.BleAdapter
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

class MonitoringRepositoryImpl(
    private val ble: BleAdapter,
    private val sensors: PhoneSensors,
    private val location: LocationProvider,
    private val detectionEngine: IDetectionEngine,
    private val sessionStore: ISessionStore = InMemorySessionStore()
) : IMonitoringRepository {

    private val _currentSession = MutableStateFlow<MonitoringSession?>(null)
    override val currentSession: StateFlow<MonitoringSession?> = _currentSession
    private val _isMonitoring = MutableStateFlow(false)
    override val isMonitoring: StateFlow<Boolean> = _isMonitoring
    private val _latestReading = MutableStateFlow<SensorReading?>(null)
    override val latestReading: StateFlow<SensorReading?> = _latestReading

    private var lastSampleTime = 0L

    override fun sensorStream(): Flow<SensorReading> = flow {
        var bleConnected = false
        ble.observeNotifications(
            "0000180a-0000-1000-8000-00805f9b34fb",
            "00002a58-0000-1000-8000-00805f9b34fb"
        ).collect { bytes ->
            bleConnected = true
            val reading = MonitoringRepositoryImpl.parseSensorBytes(bytes, currentTimeMillis())
            val downsampled = tryEmitDownsampled(reading)
            if (downsampled != null) emit(downsampled)
        }
        if (!bleConnected) throw Exception("BLE disconnected")
    }

    override fun phoneSensorStream(): Flow<SensorReading> = flow {
        try { sensorStream().collect { emit(it) } }
        catch (_: Exception) {
            sensors.accelerometerStream().collect { reading ->
                val downsampled = tryEmitDownsampled(reading)
                if (downsampled != null) emit(downsampled)
            }
        }
    }

    override fun startSession(mode: ActivityMode): MonitoringSession {
        val session = MonitoringSession(id = "SES${currentTimeMillis()}", startTime = currentTimeMillis(), mode = mode)
        _currentSession.value = session; _isMonitoring.value = true
        return session
    }

    override fun stopSession(): MonitoringSession? {
        val session = _currentSession.value?.copy(endTime = currentTimeMillis())
        _currentSession.value = null; _isMonitoring.value = false
        if (session != null) { /* persistence handled by getSessionsAsync callers */ }
        return session
    }

    override fun getSessions(): List<MonitoringSession> = emptyList()
    suspend fun getSessionsAsync(): List<MonitoringSession> = sessionStore.loadAll()

    private fun tryEmitDownsampled(reading: SensorReading): SensorReading? {
        val now = currentTimeMillis()
        if (now - lastSampleTime >= AppConfig.downsampleIntervalMs) {
            lastSampleTime = now
            _latestReading.value = reading
            return reading
        }
        return null
    }

    companion object {
        fun parseSensorBytes(bytes: ByteArray, timestampMs: Long): SensorReading {
            val buf = bytes
            fun readI16(i: Int): Short = ((buf[i].toInt() and 0xFF) or ((buf[i + 1].toInt() and 0xFF) shl 8)).toShort()
            return SensorReading(
                timestamp = timestampMs,
                accelX = readI16(AppConfig.BleOffsets.accelX).toDouble() * AppConfig.bleAccelScale,
                accelY = readI16(AppConfig.BleOffsets.accelY).toDouble() * AppConfig.bleAccelScale,
                accelZ = readI16(AppConfig.BleOffsets.accelZ).toDouble() * AppConfig.bleAccelScale,
                gyroX = readI16(AppConfig.BleOffsets.gyroX).toDouble() * AppConfig.bleGyroScale,
                gyroY = readI16(AppConfig.BleOffsets.gyroY).toDouble() * AppConfig.bleGyroScale,
                gyroZ = readI16(AppConfig.BleOffsets.gyroZ).toDouble() * AppConfig.bleGyroScale
            )
        }
        fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
    }
}

/// 内存持久化 — Mutex 线程安全
class InMemorySessionStore : ISessionStore {
    private val store = mutableListOf<MonitoringSession>()
    private val mutex = Mutex()

    override suspend fun save(session: MonitoringSession) {
        mutex.withLock { store.add(session.copy()) }
    }

    override suspend fun loadAll(): List<MonitoringSession> =
        mutex.withLock { store.toList().sortedByDescending { it.startTime } }

    override suspend fun delete(id: String) {
        mutex.withLock { store.removeAll { it.id == id } }
    }
}
