package com.example.smartsole

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.util.*

// Data classes for sensor data
data class SensorPacket(
    val packetNumber: Long = 0,
    val timestamp: Long = 0,
    val fsrValues: List<Int> = listOf(0, 0, 0, 0, 0, 0),
    val accelX: Int = 0,
    val accelY: Int = 0,
    val accelZ: Int = 0,
    val gyroX: Int = 0,
    val gyroY: Int = 0,
    val gyroZ: Int = 0,
    val temperature: Float = 0.0f,
    val status: Int = 0,
    val checksum: String = "00",
    val connectionStatus: String = "DISC"
)



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothSensorScreen(
    wasConnected: Boolean = false,
    onBackClicked: () -> Unit = {},
    onConnectionStateChanged: (Boolean) -> Unit = {},
    onSensorDataReceived: (SensorPacket) -> Unit = {}
) {
    val context = LocalContext.current
    var isConnected by remember { mutableStateOf(false) }
    var deviceName by remember { mutableStateOf("Not Connected") }
    var latestPacket by remember { mutableStateOf(SensorPacket()) }
    var packetHistory by remember { mutableStateOf(listOf<SensorPacket>()) }
    var statusMessage by remember { mutableStateOf("Ready to connect") }
    var hasPermissions by remember { mutableStateOf(false) }

    val bluetoothManager = remember {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    val bluetoothAdapter = bluetoothManager.adapter

    // Nordic UART Service UUIDs (same as Arduino code)
    val UART_SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    val UART_TX_CHAR_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
    val UART_RX_CHAR_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
    val CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    var bluetoothGatt by remember { mutableStateOf<BluetoothGatt?>(null) }
    var dataBuffer by remember { mutableStateOf("") }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions.values.all { it }
        if (hasPermissions) {
            statusMessage = "Permissions granted. Ready to scan."
        } else {
            statusMessage = "Bluetooth permissions required"
        }
    }

    // Check permissions on startup
    LaunchedEffect(Unit) {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        hasPermissions = requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

        if (!hasPermissions) {
            permissionLauncher.launch(requiredPermissions)
        } else {
            statusMessage = "Ready to connect"
        }
    }

    // GATT Callback
    val gattCallback = remember {
        object : BluetoothGattCallback() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        isConnected = true
                        deviceName = gatt?.device?.name ?: "Unknown Device"
                        statusMessage = "Connected to $deviceName"
                        onConnectionStateChanged(true)
                        gatt?.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        isConnected = false
                        deviceName = "Not Connected"
                        statusMessage = "Disconnected"
                        onConnectionStateChanged(false)
                        bluetoothGatt?.close()
                        bluetoothGatt = null
                    }
                }
            }

            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val service = gatt?.getService(UART_SERVICE_UUID)
                    val txCharacteristic = service?.getCharacteristic(UART_TX_CHAR_UUID)

                    if (txCharacteristic != null) {
                        // Enable notifications
                        gatt.setCharacteristicNotification(txCharacteristic, true)
                        val descriptor = txCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
                        descriptor?.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                        gatt.writeDescriptor(descriptor)
                        statusMessage = "Ready to receive data"
                    }
                }
            }

            override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
                characteristic?.let {
                    val data = String(it.value)
                    dataBuffer += data

                    while (dataBuffer.contains('\n')) {
                        val lineEnd = dataBuffer.indexOf('\n')
                        val line = dataBuffer.substring(0, lineEnd).trim()
                        dataBuffer = dataBuffer.substring(lineEnd + 1)

                        if (line.isNotEmpty()) {
                            parseAndUpdatePacket(line) { packet ->
                                latestPacket = packet
                                packetHistory = (packetHistory + packet).takeLast(50) // Keep last 50 packets
                                onSensorDataReceived(packet)
                            }
                        }
                    }
                }
            }
        }
    }

    fun connectToDevice() {
        if (!hasPermissions) {
            statusMessage = "Bluetooth permissions required"
            return
        }

        if (bluetoothAdapter?.isEnabled != true) {
            statusMessage = "Please enable Bluetooth"
            return
        }

        statusMessage = "Scanning for nRF52-Sensors..."

        // Look for paired devices first
        val pairedDevices = bluetoothAdapter.bondedDevices
        var targetDevice: BluetoothDevice? = null

        pairedDevices?.forEach { device ->
            if (device.name == "nRF52-Sensors") {
                targetDevice = device
                return@forEach
            }
        }

        if (targetDevice != null) {
            statusMessage = "Connecting to ${targetDevice.name}..."
            bluetoothGatt = targetDevice.connectGatt(context, false, gattCallback)
        } else {
            statusMessage = "Device 'nRF52-Sensors' not found in paired devices. Please pair first."
        }
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        isConnected = false
        deviceName = "Not Connected"
        statusMessage = "Disconnected"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Back button and header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClicked) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            Text(
                text = "Smart Sole Sensor Monitor",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        // Connection Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isConnected) Color(0xFF4CAF50) else Color(0xFFFF9800)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = deviceName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        // Connection Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { connectToDevice() },
                enabled = !isConnected && hasPermissions,
                modifier = Modifier.weight(1f)
            ) {
                Text("Connect")
            }

            Button(
                onClick = { disconnect() },
                enabled = isConnected,
                modifier = Modifier.weight(1f)
            ) {
                Text("Disconnect")
            }
        }

        // Latest Data Display
        if (isConnected) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Latest Sensor Data",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // FSR Values
                    Text(
                        text = "Force Sensors:",
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        latestPacket.fsrValues.forEachIndexed { index, value ->
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (value > 100) Color(0xFFE3F2FD) else Color(0xFFF5F5F5)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "F$index",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = value.toString(),
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // IMU Data
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Accelerometer
                        Card(modifier = Modifier.weight(1f)) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Accelerometer",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("X: ${latestPacket.accelX}", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                Text("Y: ${latestPacket.accelY}", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                Text("Z: ${latestPacket.accelZ}", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                        }

                        // Gyroscope
                        Card(modifier = Modifier.weight(1f)) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Gyroscope",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("X: ${latestPacket.gyroX}", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                Text("Y: ${latestPacket.gyroY}", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                Text("Z: ${latestPacket.gyroZ}", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Status Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Temp: ${latestPacket.temperature}°C",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Packet: ${latestPacket.packetNumber}",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Status: ${getStatusText(latestPacket.status)}",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = when (latestPacket.status) {
                                0 -> Color.Green
                                1 -> Color.Blue
                                else -> Color.Red
                            }
                        )
                    }
                }
            }

            // Packet History
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Packet History (Latest ${packetHistory.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val listState = rememberLazyListState()

                    LaunchedEffect(packetHistory.size) {
                        if (packetHistory.isNotEmpty()) {
                            listState.animateScrollToItem(packetHistory.size - 1)
                        }
                    }

                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(packetHistory) { packet ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp)),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = formatPacketForDisplay(packet),
                                    modifier = Modifier.padding(8.dp),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun parseAndUpdatePacket(csvLine: String, onPacketParsed: (SensorPacket) -> Unit) {
    try {
        val parts = csvLine.split(",")
        if (parts.size >= 16) {
            val packet = SensorPacket(
                packetNumber = parts[0].toLongOrNull() ?: 0,
                timestamp = parts[1].toLongOrNull() ?: 0,
                fsrValues = parts.subList(2, 8).map { it.toIntOrNull() ?: 0 },
                accelX = parts[8].toIntOrNull() ?: 0,
                accelY = parts[9].toIntOrNull() ?: 0,
                accelZ = parts[10].toIntOrNull() ?: 0,
                gyroX = parts[11].toIntOrNull() ?: 0,
                gyroY = parts[12].toIntOrNull() ?: 0,
                gyroZ = parts[13].toIntOrNull() ?: 0,
                temperature = parts[14].toFloatOrNull() ?: 0.0f,
                status = parts[15].toIntOrNull() ?: 0,
                checksum = if (parts.size > 16) parts[16] else "00",
                connectionStatus = if (parts.size > 17) parts[17] else "CONN"
            )
            onPacketParsed(packet)
        }
    } catch (e: Exception) {
        // Ignore malformed packets
    }
}

fun getStatusText(status: Int): String {
    return when (status) {
        0 -> "OK"
        1 -> "IMU_ERROR"
        2 -> "NO_IMU"
        else -> "UNKNOWN"
    }
}

fun formatPacketForDisplay(packet: SensorPacket): String {
    return "#${packet.packetNumber} | T:${packet.timestamp} | " +
            "FSR:[${packet.fsrValues.joinToString(",")}] | " +
            "ACC:[${packet.accelX},${packet.accelY},${packet.accelZ}] | " +
            "GYR:[${packet.gyroX},${packet.gyroY},${packet.gyroZ}] | " +
            "TEMP:${packet.temperature}°C | ${getStatusText(packet.status)}"
}