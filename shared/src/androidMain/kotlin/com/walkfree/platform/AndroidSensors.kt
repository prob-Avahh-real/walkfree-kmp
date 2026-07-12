// Android phone sensors (accelerometer, gyroscope) via SensorManager
package com.walkfree.platform

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.walkfree.data.PhoneSensors
import com.walkfree.model.SensorReading
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

actual class PhoneSensors(private val context: Context) : PhoneSensors {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    override fun accelerometerStream(): Flow<SensorReading> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event ?: return
                val now = System.currentTimeMillis()
                trySend(SensorReading(
                    timestamp = now,
                    accelX = event.values[0].toDouble(),
                    accelY = event.values[1].toDouble(),
                    accelZ = event.values[2].toDouble()
                ))
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_GAME) // ~50Hz
        awaitClose { sensorManager.unregisterListener(listener) }
    }

    override fun gyroscopeStream(): Flow<SensorReading> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event ?: return
                val now = System.currentTimeMillis()
                trySend(SensorReading(
                    timestamp = now,
                    accelX = 0.0, accelY = 0.0, accelZ = 0.0,
                    gyroX = event.values[0].toDouble(),
                    gyroY = event.values[1].toDouble(),
                    gyroZ = event.values[2].toDouble()
                ))
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, gyroSensor, SensorManager.SENSOR_DELAY_GAME)
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
