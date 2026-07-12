package com.walkfree.model

import kotlinx.serialization.Serializable

/// 运动障碍类型
enum class MovementDisorderType(val label: String) {
    FREEZING_OF_GAIT("步态冻结"),
    RESTING_TREMOR("静止性震颤"),
    BRADYKINESIA("运动迟缓"),
    POSTURAL_INSTABILITY("姿势不稳"),
    NORMAL("正常")
}

enum class Severity(val label: String) {
    MILD("轻度"), MODERATE("中度"), SEVERE("重度")
}

enum class ActivityMode { INDOOR, OUTDOOR }

/// 传感器读数 (6 轴)
data class SensorReading(
    val timestamp: Long,      // 毫秒时间戳
    val accelX: Double,       // m/s²
    val accelY: Double,
    val accelZ: Double,
    val gyroX: Double = 0.0,  // rad/s
    val gyroY: Double = 0.0,
    val gyroZ: Double = 0.0
) {
    val accelMagnitude: Double get() =
        kotlin.math.sqrt(accelX * accelX + accelY * accelY + accelZ * accelZ)

    val gyroMagnitude: Double get() =
        kotlin.math.sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ)
}

/// 检测事件
data class DetectionEvent(
    val id: String,
    val timestamp: Long,
    val type: MovementDisorderType,
    val severity: Severity = Severity.MODERATE,
    val confidence: Double = 0.0,          // 0.0-1.0
    val durationSeconds: Double = 0.0,
    val freezeIndex: Double? = null,
    val tremorFrequency: Double? = null,    // Hz
    val movementAmplitude: Double? = null   // m/s²
)

@Serializable
data class GpsPoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)

@Serializable
data class MonitoringSession(
    val id: String,
    val startTime: Long,
    val endTime: Long? = null,
    val mode: ActivityMode = ActivityMode.INDOOR,
    val events: List<DetectionEvent> = emptyList(),
    val totalDistanceMeters: Double = 0.0,
    val totalSteps: Int = 0,
    val gpsPath: List<GpsPoint> = emptyList()
) {
    val fogCount: Int get() = events.count { it.type == MovementDisorderType.FREEZING_OF_GAIT }
    val tremorCount: Int get() = events.count { it.type == MovementDisorderType.RESTING_TREMOR }
    val bradykinesiaCount: Int get() = events.count { it.type == MovementDisorderType.BRADYKINESIA }
}

@Serializable
data class MedicationRecord(
    val id: String,
    val recordedAt: Long,
    val medicationName: String,
    val dosageMg: Double,
    val note: String? = null
)
