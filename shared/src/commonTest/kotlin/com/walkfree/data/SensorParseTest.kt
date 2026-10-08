package com.walkfree.data

import com.walkfree.AppConfig
import com.walkfree.platform.ScanResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SensorParseTest {

    @Test
    fun `parse 12-byte BLE frame`() {
        // accelX=1234 millig → 1234 * 9.81 / 1000 = 12.10554 m/s²
        // accelY=0, accelZ=0, gyroX=100, gyroY=200, gyroZ=-50 (all millirad/s)
        val bytes = byteArrayOf(
            0xD2.toByte(), 0x04, 0x00, 0x00, 0x00, 0x00,   // accelX=1234, accelY=0, accelZ=0
            0x64.toByte(), 0x00, 0xC8.toByte(), 0x00, (256 - 50).toByte(), 0xFF.toByte()  // gyroX=100, gyroY=200, gyroZ=-50
        )
        val reading = MonitoringRepositoryImpl.parseSensorBytes(bytes, 1000L)

        assertEquals(1000L, reading.timestamp)
        assertEquals(1234.0 * AppConfig.bleAccelScale, reading.accelX, 1e-6)
        assertEquals(0.0, reading.accelY, 1e-10)
        assertEquals(0.0, reading.accelZ, 1e-10)
        assertEquals(100.0 * AppConfig.bleGyroScale, reading.gyroX, 1e-6)
        assertEquals(200.0 * AppConfig.bleGyroScale, reading.gyroY, 1e-6)
        assertEquals((-50.0) * AppConfig.bleGyroScale, reading.gyroZ, 1e-6)
    }

    @Test
    fun `sensor reading magnitude calculations`() {
        val reading = com.walkfree.model.SensorReading(
            timestamp = 1000L, accelX = 3.0, accelY = 4.0, accelZ = 0.0
        )
        assertEquals(5.0, reading.accelMagnitude, 1e-10)
        assertEquals(0.0, reading.gyroMagnitude, 1e-10)
    }

    @Test
    fun `scan result data class`() {
        val result = ScanResult(name = "WalkFree", address = "AA:BB:CC", rssi = -45)
        assertEquals("WalkFree", result.name)
        assertEquals("AA:BB:CC", result.address)
        assertEquals(-45, result.rssi)
    }
}
