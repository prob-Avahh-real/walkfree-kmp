// Android BLE adapter via Kable
package com.walkfree.platform

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import com.juul.kable.*
import com.walkfree.data.BleAdapter
import com.walkfree.data.ScanResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*

@SuppressLint("MissingPermission")
actual class BleAdapter(private val context: Context) : BleAdapter {
    private val _connectedDevice = MutableStateFlow<String?>(null)
    override val connectedDevice: StateFlow<String?> = _connectedDevice

    private var peripheral: Peripheral? = null
    private var connectionScope: CoroutineScope? = null

    override fun scan(): Flow<ScanResult> {
        val scanner = Scanner {
            filters {
                // 接受所有 BLE 设备，让 UI 层过滤
            }
        }
        return scanner.advertisements.map { ad ->
            ScanResult(
                name = ad.name ?: "未知设备",
                address = ad.address,
                rssi = ad.rssi
            )
        }.distinctUntilChanged { it.address }
    }

    override suspend fun connect(address: String) {
        peripheral?.let { connectionScope?.cancel() }

        val p = Peripheral(address)
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
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
        return p.observe(java.util.UUID.fromString(charUuid))
            .mapNotNull { it }  // 过滤 null
    }
}
