package com.example.smartsole

import android.app.Application
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import java.util.*

class BluetoothHelper(private val context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val bleScanner = bluetoothAdapter.bluetoothLeScanner

    private var scanning = false
    private var bluetoothGatt: BluetoothGatt? = null
    private var onConnectionStateChange: ((Boolean) -> Unit)? = null

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter.isEnabled

    fun startScanning(onDeviceFound: (BluetoothDevice) -> Unit) {
        if (scanning) return
        scanning = true

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val name = device.name ?: "Unnamed"
                Log.d("BLE", "Found: $name - ${device.address}")
                if (name.contains("Adafruit", ignoreCase = true)) { // customize this filter
                    onDeviceFound(device)
                    stopScanning(this)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e("BLE", "Scan failed with error: $errorCode")
                scanning = false
            }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        bleScanner.startScan(null, settings, scanCallback)
        Log.i("BLE", "BLE scanning started")
    }

    private fun stopScanning(scanCallback: ScanCallback) {
        if (scanning) {
            bleScanner.stopScan(scanCallback)
            scanning = false
            Log.i("BLE", "BLE scanning stopped")
        }
    }

    fun connectToDevice(device: BluetoothDevice, onConnectionStateChange: (Boolean) -> Unit) {
        this.onConnectionStateChange = onConnectionStateChange
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
        Log.i("BLE", "Connecting to GATT...")
    }

    fun writeCharacteristic(data: ByteArray) {
        bluetoothGatt?.let { gatt ->
            val service = gatt.getService(MY_SERVICE_UUID)
            val characteristic = service?.getCharacteristic(MY_CHARACTERISTIC_UUID)
            
            characteristic?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeCharacteristic(it, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                } else {
                    @Suppress("DEPRECATION")
                    it.value = data
                    @Suppress("DEPRECATION")
                    gatt.writeCharacteristic(it)
                }
            }
        }
    }

    fun readCharacteristic() {
        bluetoothGatt?.let { gatt ->
            val service = gatt.getService(MY_SERVICE_UUID)
            val characteristic = service?.getCharacteristic(MY_CHARACTERISTIC_UUID)
            
            characteristic?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.readCharacteristic(it)
                } else {
                    @Suppress("DEPRECATION")
                    gatt.readCharacteristic(it)
                }
            }
        }
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("BLE", "Connected to GATT server.")
                gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
                gatt.discoverServices()
                onConnectionStateChange?.invoke(true)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i("BLE", "Disconnected from GATT server.")
                gatt.close()
                onConnectionStateChange?.invoke(false)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("BLE", "Services discovered:")
                for (service in gatt.services) {
                    Log.i("BLE", "Service: ${service.uuid}")
                    for (characteristic in service.characteristics) {
                        Log.i("BLE", "  ↳ Characteristic: ${characteristic.uuid}")
                    }
                }
            } else {
                Log.e("BLE", "Service discovery failed with status: $status")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w("BLE", "Missing BLUETOOTH_CONNECT permission.")
                return
            }

            Log.i("BLE", "Characteristic changed: ${value.joinToString()}")
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("BLE", "Characteristic read: ${value.joinToString()}")
            } else {
                Log.e("BLE", "Characteristic read failed with status: $status")
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("BLE", "Characteristic write successful")
            } else {
                Log.e("BLE", "Characteristic write failed with status: $status")
            }
        }
    }

    companion object {
        // Replace with actual values from Adafruit firmware if known
        val MY_SERVICE_UUID: UUID = UUID.fromString("00000001-627E-47E5-A3FC-DDABD97AA966")
        val MY_CHARACTERISTIC_UUID: UUID = UUID.fromString("00000002-627E-47E5-A3FC-DDABD97AA966") // Example
        val YOUR_DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb") // Standard CCCD descriptor
    }
}
