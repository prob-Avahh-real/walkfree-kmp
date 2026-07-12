// iOS BLE adapter via Kable (CoreBluetooth)
package com.walkfree.platform

import com.juul.kable.*
import com.walkfree.data.BleAdapter
import com.walkfree.data.ScanResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

actual class BleAdapter : BleAdapter {
    private val _connectedDevice = MutableStateFlow<String?>(null)
    override val connectedDevice: StateFlow<String?> = _connectedDevice

    private var peripheral: Peripheral? = null
    private var connectionScope: CoroutineScope? = null

    override fun scan(): Flow<ScanResult> {
        val scanner = Scanner { /* no filter */ }
        return scanner.advertisements.map { ad ->
            ScanResult(
                name = ad.name ?: "未知设备",
                address = ad.address,
                rssi = ad.rssi
            )
        }.distinctUntilChanged { it.address }
    }

    override suspend fun connect(address: String) {
        connectionScope?.cancel()
        val p = Peripheral(address)
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        scope.launch {
            p.connect()
            _connectedDevice.value = address
        }
        peripheral = p
        connectionScope = scope
    }

    override fun disconnect() {
        connectionScope?.cancel()
        connectionScope = null
        peripheral = null
        _connectedDevice.value = null
    }

    override fun observeNotifications(serviceUuid: String, charUuid: String): Flow<ByteArray> {
        val p = peripheral ?: return emptyFlow()
        return p.observe(platform.UUID.fromString(charUuid))
            .mapNotNull { it?.toByteArray() }
    }
}

// Helper: UUID → Kable expected type
private object platform {
    expect class UUID {
        companion object {
            fun fromString(uuid: String): UUID
        }
    }
    expect fun UUID.toByteArray(): ByteArray
}
