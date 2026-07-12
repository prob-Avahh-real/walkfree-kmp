package com.walkfree.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

data class ScanResult(val name: String?, val address: String, val rssi: Int)

expect class BleAdapter {
    fun scan(): Flow<ScanResult>
    suspend fun connect(address: String)
    fun disconnect()
    fun observeNotifications(serviceUuid: String, charUuid: String): Flow<ByteArray>
    val connectedDevice: StateFlow<String?>
}
