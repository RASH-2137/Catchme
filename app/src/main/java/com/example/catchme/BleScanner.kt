package com.example.catchme

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast

data class BleSignal(
    val deviceAddress: String,
    val deviceName: String,
    val brand: String,
    val rssi: Int,
    val timestamp: Long = System.currentTimeMillis()
)

class BleScanner(
    private val context: Context,
    private val onSignalDetected: (BleSignal) -> Unit,
    private val onStatusUpdate: (String) -> Unit
) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? get() = bluetoothManager.adapter
    var isScanning = false
        private set

    private var isReceiverRegistered = false

    // Classic Bluetooth Receiver (Catches CHETNA, boAt, Mivi, TVs)
    private val classicReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            if (intent?.action == BluetoothDevice.ACTION_FOUND) {
                try {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }

                    val rssi: Short = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (-65).toShort())

                    // Safe name extraction to prevent SecurityException
                    val rawName = try {
                        device?.name ?: "Nearby Device"
                    } catch (_: SecurityException) {
                        "Nearby Device"
                    }
                    val address = device?.address ?: "00:00:00:00"
                    val brand = categorizeDevice(rawName)

                    onSignalDetected(
                        BleSignal(
                            deviceAddress = address,
                            deviceName = rawName,
                            brand = brand,
                            rssi = rssi.toInt()
                        )
                    )
                } catch (e: Exception) {
                    // Safe catch
                }
            }
        }
    }

    // BLE Scanner (Catches Smartwatches & Beacons)
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let {
                try {
                    val rawName = try {
                        it.scanRecord?.deviceName ?: it.device.name ?: "Unknown"
                    } catch (_: SecurityException) {
                        "Unknown"
                    }
                    val brand = categorizeDevice(rawName)

                    onSignalDetected(
                        BleSignal(
                            deviceAddress = it.device.address,
                            deviceName = rawName,
                            brand = brand,
                            rssi = it.rssi
                        )
                    )
                } catch (e: Exception) {
                    // Safe catch
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "BLE Error: $errorCode", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun categorizeDevice(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.contains("chetna") || lower.contains("phone") || lower.contains("nord") -> "Phone"
            lower.contains("xtend") || lower.contains("watch") -> "Smartwatch"
            lower.contains("boat") -> "boAt"
            lower.contains("mivi") || lower.contains("pod") || lower.contains("ear") -> "Earbuds"
            lower.contains("apple") || lower.contains("iphone") -> "Apple"
            lower.contains("samsung") || lower.contains("galaxy") -> "Samsung"
            else -> if (name != "Unknown" && name != "Nearby Device") name else "Device"
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            Toast.makeText(context, "Turn ON Bluetooth!", Toast.LENGTH_LONG).show()
            onStatusUpdate("BT_OFF")
            return
        }

        try {
            onStatusUpdate("REGISTERING...")

            if (!isReceiverRegistered) {
                val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(classicReceiver, filter, Context.RECEIVER_EXPORTED)
                } else {
                    context.registerReceiver(classicReceiver, filter)
                }
                isReceiverRegistered = true
            }

            if (adapter.isDiscovering) adapter.cancelDiscovery()
            val classicOk = adapter.startDiscovery()
            onStatusUpdate(if (classicOk) "CLASSIC_OK" else "CLASSIC_FAIL")

            val bleScannerObj = adapter.bluetoothLeScanner
            if (bleScannerObj != null) {
                bleScannerObj.startScan(
                    null,
                    ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),
                    scanCallback
                )
                onStatusUpdate("SCANNING")
            } else {
                onStatusUpdate("BLE_NULL")
            }

            isScanning = true

        } catch (e: SecurityException) {
            onStatusUpdate("SEC_ERR")
            Toast.makeText(context, "Enable 'Nearby devices' in App Settings!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            onStatusUpdate("ERR: ${e.message?.take(20)}")
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!isScanning) return
        try {
            if (isReceiverRegistered) {
                context.unregisterReceiver(classicReceiver)
                isReceiverRegistered = false
            }
        } catch (_: Exception) {}

        try {
            bluetoothAdapter?.cancelDiscovery()
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (_: Exception) {}

        isScanning = false
    }
}