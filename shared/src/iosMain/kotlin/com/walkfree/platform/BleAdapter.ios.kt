package com.walkfree.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

actual class BleAdapter {
    private val _connectedDevice = MutableStateFlow<String?>(null)
    actual val connectedDevice: StateFlow<String?> = _connectedDevice

    actual fun scan(): Flow<ScanResult> = emptyFlow()

    actual suspend fun connect(address: String) {
        _connectedDevice.value = address
    }

    actual fun disconnect() {
        _connectedDevice.value = null
    }

    actual fun observeNotifications(serviceUuid: String, charUuid: String): Flow<ByteArray> = emptyFlow()
}
