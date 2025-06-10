// LivePressureScreen.kt
package com.example.smartsole

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

data class PressureSensor(
    val id: Int,
    val xPercent: Float,
    val yPercent: Float,
    var pressure: Float = 0f,
    var isActive: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LivePressureScreen(
    isConnected: Boolean = false,
    sensorData: SensorPacket? = null,
    onBackClicked: () -> Unit,
    onConnectClicked: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Pressure sensor positions mapped to foot anatomy
    val pressureSensors = remember {
        mutableStateListOf(
            PressureSensor(id = 5, xPercent = 0.44f, yPercent = 0.91f),  // Heel back
            PressureSensor(id = 4, xPercent = 0.58f, yPercent = 0.83f), // Heel front
            PressureSensor(id = 3, xPercent = 0.71f, yPercent = 0.39f), // Mid foot
            PressureSensor(id = 2, xPercent = 0.50f, yPercent = 0.30f), // Arch
            PressureSensor(id = 1, xPercent = 0.24f, yPercent = 0.28f), // Ball of foot
            PressureSensor(id = 0, xPercent = 0.20f, yPercent = 0.10f), // Toe area
        )
    }

    // Update sensor data when new packet arrives
    LaunchedEffect(sensorData) {
        sensorData?.let { packet ->
            pressureSensors.forEachIndexed { index, sensor ->
                if (index < packet.fsrValues.size) {
                    sensor.pressure = (packet.fsrValues[index] / 1024f).coerceIn(0f, 1f)
                    sensor.isActive = packet.fsrValues[index] > 50
                }
            }
        }
    }

    // Animation for sensor pulsing
    val infiniteTransition = rememberInfiniteTransition(label = "pressure_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Header with connection status
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Live Pressure Monitor",
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = if (isConnected) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                        contentDescription = "Connection Status",
                        tint = if (isConnected) Color(0xFF4CAF50) else Color(0xFFFF5722),
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClicked) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        )

        if (!isConnected) {
            // Connection prompt
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFF5722).copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Connect to Smart Sole",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Connect your Smart Sole device to view live pressure data",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onConnectClicked) {
                        Text("Connect Device")
                    }
                }
            }
        }

        // Pressure visualization area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            var footImageWidth by remember { mutableStateOf(0) }
            var footImageHeight by remember { mutableStateOf(0) }
            val density = LocalDensity.current

            // Foot outline image
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .aspectRatio(0.6f)
                    .offset(y = (-45).dp) // 👈 Move the whole box up (adjust -24 to your preference)
                    .onSizeChanged { size ->
                        footImageWidth = size.width
                        footImageHeight = size.height
                    }
            ) {
                // Background foot image
                Image(
                    painter = painterResource(id = R.drawable.footmapping),
                    contentDescription = "Foot Outline",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // Pressure sensors overlay
                if (footImageWidth > 0 && footImageHeight > 0) {
                    pressureSensors.forEach { sensor ->
                        val sensorSize = 20.dp + (sensor.pressure * 30).dp
                        val sensorSizePx = with(density) { sensorSize.toPx() }

                        val offsetX = (sensor.xPercent * footImageWidth - sensorSizePx / 2).roundToInt()
                        val offsetY = (sensor.yPercent * footImageHeight - sensorSizePx / 2).roundToInt()

                        val sensorColor = when {
                            sensor.pressure > 0.7f -> Color.Red
                            sensor.pressure > 0.4f -> Color(0xFFFF9800)
                            sensor.pressure > 0.1f -> Color.Yellow
                            else -> Color.Blue.copy(alpha = 0.3f)
                        }

                        Box(
                            modifier = Modifier
                                .offset { IntOffset(offsetX, offsetY) }
                                .size(sensorSize)
                                .clip(CircleShape)
                                .background(
                                    sensorColor.copy(
                                        alpha = if (sensor.isActive) pulseAlpha else 0.5f
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // Pressure value text
                            if (sensor.pressure > 0.1f) {
                                Text(
                                    text = "${(sensor.pressure * 100).toInt()}%",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Pressure scale indicator
                // Pressure scale indicator - now centered
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 70.dp), // 👈 pushes it downward (adjust as needed)
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                Text(
                        text = "Pressure Scale",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color.Blue.copy(alpha = 0.5f))
                        )
                        Text("Low", fontSize = 8.sp)

                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color.Yellow)
                        )
                        Text("Med", fontSize = 8.sp)

                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                        )
                        Text("High", fontSize = 8.sp)
                    }
                }
            }
        }

        // Real-time data display
        if (isConnected && sensorData != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Real-time Sensor Data",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sensor readings grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        sensorData.fsrValues.forEachIndexed { index, value ->
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = when {
                                        value > 700 -> Color.Red.copy(alpha = 0.2f)
                                        value > 400 -> Color.Blue.copy(alpha = 0.2f)
                                        value > 100 -> Color.Yellow.copy(alpha = 0.2f)
                                        else -> Color.Gray.copy(alpha = 0.1f)
                                    }
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "S${index + 1}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = value.toString(),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Additional metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Total Pressure",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "${sensorData.fsrValues.sum()}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Active Sensors",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "${sensorData.fsrValues.count { it > 50 }}/6",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Packet #",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "${sensorData.packetNumber}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}