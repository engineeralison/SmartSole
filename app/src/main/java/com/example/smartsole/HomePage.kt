package com.example.smartsole

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

// Algorithm for real-time step detection and activity tracking
class SensorBasedTracker {
    private var lastStepTime = 0L
    private var lastPressureSum = 0
    private var stepCount = 0
    private var activeStartTime: Long? = null
    private var totalActiveTimeMs = 0L
    private var isCurrentlyActive = false

    // Step detection parameters
    private val MIN_STEP_INTERVAL_MS = 300 // Minimum time between steps
    private val STEP_PRESSURE_THRESHOLD = 700 // Pressure change needed for step detection
    private val ACTIVITY_THRESHOLD = 600 // Pressure threshold for "on feet"

    // Buffers for smoothing
    private val pressureHistory = mutableListOf<Int>()
    private val accelHistory = mutableListOf<Triple<Int, Int, Int>>()
    private val BUFFER_SIZE = 5

    fun processSensorData(packet: SensorPacket): Pair<Int, String> {
        val currentTime = System.currentTimeMillis()

        // Calculate pressure from main step sensors (FSR 2, 3, 4, 5)
        // FSR 0 and 1 excluded as they have baseline pressure from shoe weight
        val stepSensorPressure = packet.fsrValues.drop(2).take(4).sum() // FSR 2, 3, 4, 5
        val totalPressure = packet.fsrValues.sum() // Still use total for activity tracking

        // Add to pressure history for smoothing
        pressureHistory.add(stepSensorPressure)
        if (pressureHistory.size > BUFFER_SIZE) {
            pressureHistory.removeAt(0)
        }

        // Add accelerometer data for validation
        accelHistory.add(Triple(packet.accelX, packet.accelY, packet.accelZ))
        if (accelHistory.size > BUFFER_SIZE) {
            accelHistory.removeAt(0)
        }

        // Detect steps using main step sensors (FSR 2-5)
        detectStep(stepSensorPressure, currentTime, packet)

        // Track time on feet using total pressure
        updateTimeOnFeet(totalPressure, currentTime)

        // Format time on feet for display
        val timeOnFeetFormatted = formatTimeOnFeet(totalActiveTimeMs)

        return Pair(stepCount, timeOnFeetFormatted)
    }

    private fun detectStep(stepSensorPressure: Int, currentTime: Long, packet: SensorPacket) {
        // Multi-factor step detection algorithm using FSR 2, 3, 4, 5

        // 1. Pressure-based detection (using main step sensors only)
        val pressureDiff = abs(stepSensorPressure - lastPressureSum)
        val timeSinceLastStep = currentTime - lastStepTime

        // 2. Accelerometer magnitude for validation
        val accelMagnitude = sqrt(
            (packet.accelX * packet.accelX +
                    packet.accelY * packet.accelY +
                    packet.accelZ * packet.accelZ).toDouble()
        )

        // 3. Check for foot pressure distribution in main sensors
        val heelPressure = packet.fsrValues.getOrNull(4)?.plus(packet.fsrValues.getOrNull(5) ?: 0) ?: 0
        val midFootPressure = packet.fsrValues.getOrNull(2)?.plus(packet.fsrValues.getOrNull(3) ?: 0) ?: 0

        // Step detection criteria (focusing on FSR 2-5):
        val hasSignificantPressureChange = pressureDiff > STEP_PRESSURE_THRESHOLD
        val hasMinimumStepPressure = stepSensorPressure > 400 // Lower threshold for step sensors
        val hasProperTiming = timeSinceLastStep > MIN_STEP_INTERVAL_MS
        val hasAccelMovement = accelMagnitude > 8000 // Threshold for movement
        val hasMainFootPattern = heelPressure > 100 || midFootPressure > 100

        // Detect step if multiple criteria are met
        if (hasSignificantPressureChange &&
            hasMinimumStepPressure &&
            hasProperTiming &&
            (hasAccelMovement || hasMainFootPattern)) {

            stepCount++
            lastStepTime = currentTime
        }

        lastPressureSum = stepSensorPressure // Track step sensor pressure changes
    }

    private fun updateTimeOnFeet(totalPressure: Int, currentTime: Long) {
        val isActive = totalPressure > ACTIVITY_THRESHOLD

        if (isActive && !isCurrentlyActive) {
            // Started being active
            activeStartTime = currentTime
            isCurrentlyActive = true
        } else if (!isActive && isCurrentlyActive) {
            // Stopped being active - add to total time
            activeStartTime?.let { startTime ->
                totalActiveTimeMs += (currentTime - startTime)
            }
            isCurrentlyActive = false
            activeStartTime = null
        }
    }

    private fun formatTimeOnFeet(totalMs: Long): String {
        // Add current session time if actively standing/walking
        val currentSessionMs = if (isCurrentlyActive && activeStartTime != null) {
            System.currentTimeMillis() - activeStartTime!!
        } else 0L

        val totalTimeMs = totalMs + currentSessionMs
        val totalMinutes = totalTimeMs / 60000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "0m"
        }
    }

    fun reset() {
        stepCount = 0
        totalActiveTimeMs = 0L
        activeStartTime = null
        isCurrentlyActive = false
        pressureHistory.clear()
        accelHistory.clear()
    }

    fun getCurrentStats() = Pair(stepCount, formatTimeOnFeet(totalActiveTimeMs))
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomePage(
    onStartRecordingClicked: () -> Unit,
    onViewGraphClicked: () -> Unit,
    onViewHistoryClicked: () -> Unit,
    onViewLivePressureClicked: () -> Unit = {},
    onStartActivityTrackingClicked: () -> Unit = {},
    onConnectBluetoothClicked: () -> Unit = {},
    isBluetoothConnected: Boolean = false,
    sensorData: SensorPacket? = null, // Added sensor data parameter
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Sensor-based tracker
    val tracker = remember { SensorBasedTracker() }

    // Real-time stats from sensor data
    var dailySteps by remember { mutableStateOf(0) }
    var timeOnFeet by remember { mutableStateOf("0m") }
    var lastUpdated by remember { mutableStateOf("Disconnected") }

    // Process sensor data when it arrives
    LaunchedEffect(sensorData) {
        sensorData?.let { packet ->
            val (steps, timeFormatted) = tracker.processSensorData(packet)
            dailySteps = steps
            timeOnFeet = timeFormatted
            lastUpdated = "Just now"
        }
    }

    // Update last updated based on Bluetooth connection
    LaunchedEffect(isBluetoothConnected) {
        if (!isBluetoothConnected) {
            // Reset when disconnected
            tracker.reset()
            lastUpdated = "Disconnected"
        } else {
            // When connected, update status
            lastUpdated = "Connected"
        }
    }

    // text for date
    val formattedDate = remember {
        LocalDate.now().format(
            DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.getDefault())
        )
    }
    // motivational quotes
    val motivationalQuotes = listOf(
        "You've got this!",
        "One step at a time.",
        "Keep moving forward.",
        "Progress, not perfection.",
        "Push yourself — no one else will do it for you.",
        "Stay strong, your future self will thank you.",
        "Discipline over motivation.",
        "Fall seven times, stand up eight."
    )
    var currentQuote by remember { mutableStateOf(motivationalQuotes.random()) }

    // simulate real-time updates for demo
    LaunchedEffect(Unit) {
        while (true) {
            delay(30000) // Update every 30 seconds
            if (!isBluetoothConnected) {
                dailySteps += (1..5).random()
                lastUpdated = "Just now"
            }
        }
    }
    // for quotes, changes every minute
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000) // Update every minute
            currentQuote = motivationalQuotes.random()
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.dashboard),
            contentDescription = "Dashboard Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top spacing
            Spacer(modifier = Modifier.height(100.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly // Centers the content
            ) {
                // Steps Today button
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.steps_today),
                        contentDescription = "Steps Today",
                        modifier = Modifier.size(140.dp) // Reduced from 160.dp
                    )
                    Text(
                        text = "$dailySteps steps",
                        fontSize = 16.sp, // Slightly smaller text
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.offset(x = (-20).dp, y = 8.dp) // Adjusted for smaller image
                    )
                }

                // Time on Feet button
                Box(
                    modifier = Modifier.offset(y = (-4).dp), // Shift up
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.time_on_feet),
                        contentDescription = "Time on Feet",
                        modifier = Modifier.size(150.dp) // Reduced from 178.dp, maintaining ratio
                    )
                    Text(
                        text = timeOnFeet,
                        fontSize = 16.sp, // Slightly smaller text
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.offset(x = (-37).dp, y = 7.dp) // Adjusted for smaller image
                    )
                }
            }

            // Small spacing after the cards
            Spacer(modifier = Modifier.height(4.dp))

            // Last Updated Indicator - pulled up with negative margin
            Card(
                modifier = Modifier.offset(y = (-30).dp), // Pull the card up
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                )
            ) {
                Text(
                    text = "Updated: $lastUpdated",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 0.dp),
                    fontSize = 12.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height((-30).dp))

            // Pressure Plot Button
            Image(
                painter = painterResource(id = R.drawable.pressure_plot),
                contentDescription = "Pressure Plot",
                modifier = Modifier
                    .width(230.dp)
                    .height(160.dp)
                    .clickable(onClick = onViewGraphClicked)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Original Foot History Button
            Image(
                painter = painterResource(id = R.drawable.foot_history),
                contentDescription = "Foot History",
                modifier = Modifier
                    .width(240.dp)
                    .height(120.dp)
                    .clickable(onClick = onViewHistoryClicked)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Current Day and Motivational Quote
            Text(
                text = formattedDate,
                fontSize = 18.sp,
                color = Color.Black,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "\"$currentQuote\"",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier
                    .background(Color(0xFF424242).copy(alpha = 0.7f), shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Bluetooth Connection Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isBluetoothConnected)
                        Color(0xFF4CAF50).copy(alpha = 0.9f)
                    else
                        Color(0x00000000).copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onConnectBluetoothClicked)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isBluetoothConnected)
                                Icons.Default.BluetoothConnected
                            else
                                Icons.Default.Bluetooth,
                            contentDescription = "Bluetooth Status",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = if (isBluetoothConnected) "Smart Sole Connected" else "Connect Smart Sole",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isBluetoothConnected) "Ready to track" else "Tap to connect",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                    if (!isBluetoothConnected) {
                        Button(
                            onClick = onConnectBluetoothClicked,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Connect")
                        }
                    }
                }
            }
        }
    }
}