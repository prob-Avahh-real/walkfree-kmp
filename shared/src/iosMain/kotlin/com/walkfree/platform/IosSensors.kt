// iOS phone sensors via CoreMotion (CMMotionManager)
package com.walkfree.platform

import com.walkfree.data.PhoneSensors
import com.walkfree.model.SensorReading
import kotlinx.cinterop.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.CoreMotion.CMMotionManager
import platform.CoreMotion.CMAccelerometerData
import platform.CoreMotion.CMDeviceMotion
import platform.Foundation.NSOperationQueue
import platform.darwin.NSObject

actual class PhoneSensors : PhoneSensors {
    private val motionManager = CMMotionManager()

    init {
        motionManager.accelerometerUpdateInterval = 0.02  // 50Hz
        motionManager.deviceMotionUpdateInterval = 0.02
    }

    override fun accelerometerStream(): Flow<SensorReading> = callbackFlow {
        if (!motionManager.isAccelerometerAvailable) {
            close()
            return@callbackFlow
        }
        motionManager.startAccelerometerUpdatesToQueue(NSOperationQueue.mainQueue) { data, _ ->
            data ?: return@startAccelerometerUpdatesToQueue
            trySend(SensorReading(
                timestamp = (data.timestamp * 1000).toLong(),
                accelX = data.acceleration.x,
                accelY = data.acceleration.y,
                accelZ = data.acceleration.z
            ))
        }
        awaitClose { motionManager.stopAccelerometerUpdates() }
    }

    override fun gyroscopeStream(): Flow<SensorReading> = callbackFlow {
        if (!motionManager.isDeviceMotionAvailable) {
            close()
            return@callbackFlow
        }
        motionManager.startDeviceMotionUpdatesToQueue(NSOperationQueue.mainQueue) { motion, _ ->
            motion ?: return@startDeviceMotionUpdatesToQueue
            trySend(SensorReading(
                timestamp = (motion.timestamp * 1000).toLong(),
                accelX = 0.0, accelY = 0.0, accelZ = 0.0,
                gyroX = motion.rotationRate.x,
                gyroY = motion.rotationRate.y,
                gyroZ = motion.rotationRate.z
            ))
        }
        awaitClose { motionManager.stopDeviceMotionUpdates() }
    }
}
