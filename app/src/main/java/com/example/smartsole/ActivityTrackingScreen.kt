package com.example.smartsole

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.exp
import kotlin.math.roundToInt

// Activity recognition models - Updated to 4 activities
enum class ActivityType(val displayName: String, val color: Color, val icon: ImageVector) {
    SITTING("Sitting", Color(0xFF9C27B0), Icons.Default.Chair),
    STANDING("Standing", Color(0xFF2196F3), Icons.Default.Accessibility),
    WALKING("Walking", Color(0xFF4CAF50), Icons.AutoMirrored.Filled.DirectionsWalk),
    STAIRS("Climbing Stairs", Color(0xFFFF9800), Icons.Default.Stairs),
    UNKNOWN("Unknown", Color(0xFF757575), Icons.Default.QuestionMark)
}

data class StoredSensorData(
    val timestamp: Long,
    val fsrValues: List<Int>,
    val accelX: Int,
    val accelY: Int,
    val accelZ: Int,
    val gyroX: Int,
    val gyroY: Int,
    val gyroZ: Int,
    val detectedActivity: ActivityType = ActivityType.UNKNOWN,
    val confidence: Float = 0f
) : Serializable

data class ActivityBreakdown(
    val activity: ActivityType,
    val totalTimeMs: Long,
    val percentage: Float
) : Serializable

data class ActivitySession(
    val id: String,
    val activity: ActivityType,
    val startTime: Long,
    val endTime: Long,
    val durationMs: Long,
    val sensorData: List<StoredSensorData>,
    val activityBreakdown: List<ActivityBreakdown>
) : Serializable {
    val durationFormatted: String
        get() {
            val minutes = (durationMs / 60000).toInt()
            val seconds = ((durationMs % 60000) / 1000).toInt()
            return String.format("%02d:%02d", minutes, seconds)
        }

    val startTimeFormatted: String
        get() = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(startTime))
}

// Storage manager for activity sessions
class ActivitySessionStorage(private val context: Context) {
    private val fileName = "activity_sessions.dat"

    suspend fun saveSession(session: ActivitySession) = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, fileName)
            val sessions = loadAllSessions().toMutableList()
            sessions.add(0, session) // Add to beginning

            FileOutputStream(file).use { fos ->
                ObjectOutputStream(fos).use { oos ->
                    oos.writeObject(sessions)
                }
            }
            Log.d("ActivityStorage", "Session saved: ${session.id}")
        } catch (e: Exception) {
            Log.e("ActivityStorage", "Failed to save session: ${e.message}")
        }
    }

    suspend fun loadAllSessions(): List<ActivitySession> = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, fileName)
            if (!file.exists()) return@withContext emptyList()

            FileInputStream(file).use { fis ->
                ObjectInputStream(fis).use { ois ->
                    @Suppress("UNCHECKED_CAST")
                    return@withContext ois.readObject() as List<ActivitySession>
                }
            }
        } catch (e: Exception) {
            Log.e("ActivityStorage", "Failed to load sessions: ${e.message}")
            return@withContext emptyList()
        }
    }

    suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, fileName)
            val sessions = loadAllSessions().toMutableList()
            sessions.removeAll { it.id == sessionId }

            FileOutputStream(file).use { fos ->
                ObjectOutputStream(fos).use { oos ->
                    oos.writeObject(sessions)
                }
            }
            Log.d("ActivityStorage", "Session deleted: $sessionId")
        } catch (e: Exception) {
            Log.e("ActivityStorage", "Failed to delete session: ${e.message}")
        }
    }
}

// Simple TensorFlow Lite Activity Recognition for Shoe Insole
class SimpleShoeInsoleAI(private val context: Context) {
    private var interpreter: Interpreter? = null
    private val dataBuffer = mutableListOf<FloatArray>()
    private var isModelLoaded = false
    private var sampleCount = 0

    // Updated to 4 activities in correct order
    private val activityLabels = arrayOf(
        "SITTING", "STAIRS", "STANDING", "WALKING"
    )

    suspend fun loadModel(): Boolean {
        return try {
            val modelBuffer = FileUtil.loadMappedFile(context, "smart_sole_model.tflite")
            interpreter = Interpreter(modelBuffer)
            isModelLoaded = true
            Log.d("ShoeInsoleAI", "Model loaded successfully")
            true
        } catch (e: Exception) {
            Log.e("ShoeInsoleAI", "Failed to load model: ${e.message}")
            false
        }
    }

    fun addSensorData(sensorPacket: SensorPacket): Pair<ActivityType, Float>? {
        if (!isModelLoaded) return null

        // Create normalized features: 6 FSR + 3 accel + 3 gyro = 12 features
        val features = FloatArray(12)

        // FSR sensors (0-5): normalize to 0-1
        sensorPacket.fsrValues.forEachIndexed { i, value ->
            if (i < 6) features[i] = (value / 1024f).coerceIn(0f, 1f)
        }

        // Accelerometer (6-8): normalize to -1 to 1
        features[6] = (sensorPacket.accelX / 16384f).coerceIn(-1f, 1f)
        features[7] = (sensorPacket.accelY / 16384f).coerceIn(-1f, 1f)
        features[8] = (sensorPacket.accelZ / 16384f).coerceIn(-1f, 1f)

        // Gyroscope (9-11): normalize to -1 to 1
        features[9] = (sensorPacket.gyroX / 16384f).coerceIn(-1f, 1f)
        features[10] = (sensorPacket.gyroY / 16384f).coerceIn(-1f, 1f)
        features[11] = (sensorPacket.gyroZ / 16384f).coerceIn(-1f, 1f)

        // Add to buffer
        dataBuffer.add(features)
        sampleCount++

        // Keep exactly 20 samples
        if (dataBuffer.size > 20) {
            dataBuffer.removeAt(0)
        }

        // Run inference every 20 samples (when buffer is full)
        if (dataBuffer.size == 20) {
            return runInference()
        }

        return null
    }

    private fun runInference(): Pair<ActivityType, Float> {
        return try {
            // Prepare input as 3D array for RNN: [batch, timestep, features]
            val inputArray = Array(1) { Array(20) { FloatArray(12) } }

            dataBuffer.forEachIndexed { timeIndex, features ->
                features.copyInto(inputArray[0][timeIndex])
            }

            val outputArray = Array(1) { FloatArray(4) }

            interpreter?.run(inputArray, outputArray)

            val probabilities = outputArray[0]

            // Apply softmax
            val maxLogit = probabilities.maxOrNull() ?: 0f
            val exps = probabilities.map { exp((it - maxLogit).toDouble()).toFloat() }
            val sumExps = exps.sum()
            val softmaxProbs = if (sumExps > 0) exps.map { it / sumExps }.toFloatArray() else probabilities

            val maxIndex = softmaxProbs.indices.maxByOrNull { softmaxProbs[it] } ?: 0
            val confidence = softmaxProbs[maxIndex]

            val activity = when (maxIndex) {
                0 -> ActivityType.SITTING
                1 -> ActivityType.STAIRS
                2 -> ActivityType.STANDING
                3 -> ActivityType.WALKING
                else -> ActivityType.UNKNOWN
            }

            Pair(activity, confidence)

        } catch (e: Exception) {
            Log.e("ShoeInsoleAI", "Inference failed: ${e.message}")
            Pair(ActivityType.UNKNOWN, 0f)
        }
    }

    fun getBufferStatus() = "${dataBuffer.size}/20 records"

    fun reset() {
        dataBuffer.clear()
        sampleCount = 0
    }

    fun close() {
        interpreter?.close()
    }
}

@Composable
fun FSRDataViewer(
    sensorData: List<StoredSensorData>,
    activityBreakdown: List<ActivityBreakdown>,
    modifier: Modifier = Modifier
) {
    var currentIndex by remember { mutableStateOf(0) }

    if (sensorData.isEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.Gray.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No sensor data available", color = Color.Gray)
            }
        }
        return
    }

    val currentData = if (currentIndex < sensorData.size) sensorData[currentIndex] else sensorData.first()
    val currentActivity = currentData.detectedActivity
    val currentConfidence = currentData.confidence

    Column(modifier = modifier) {
        // Current activity indicator with confidence
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = currentActivity.color.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = currentActivity.icon,
                        contentDescription = currentActivity.displayName,
                        modifier = Modifier.size(20.dp),
                        tint = currentActivity.color
                    )
                    Text(
                        "Activity: ${currentActivity.displayName}",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }

                if (currentConfidence > 0) {
                    Text(
                        "${(currentConfidence * 100).toInt()}%",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // FSR visualization
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("FSR Pressure Data", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    var footImageWidth by remember { mutableStateOf(0) }
                    var footImageHeight by remember { mutableStateOf(0) }
                    val density = LocalDensity.current

                    // Pressure sensor positions
                    val pressureSensors = remember {
                        listOf(
                            Pair(0.44f, 0.91f), // FSR 0: Heel back
                            Pair(0.58f, 0.83f), // FSR 1: Heel front
                            Pair(0.71f, 0.39f), // FSR 2: Mid foot
                            Pair(0.50f, 0.30f), // FSR 3: Arch
                            Pair(0.24f, 0.28f), // FSR 4: Ball of foot
                            Pair(0.20f, 0.10f)  // FSR 5: Toe area
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .aspectRatio(0.6f)
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
                            currentData.fsrValues.forEachIndexed { index, value ->
                                if (index < pressureSensors.size) {
                                    val (xPercent, yPercent) = pressureSensors[index]
                                    val pressure = (value / 1024f).coerceIn(0f, 1f)
                                    val sensorSize = 16.dp + (pressure * 24).dp
                                    val sensorSizePx = with(density) { sensorSize.toPx() }

                                    val offsetX = (xPercent * footImageWidth - sensorSizePx / 2).roundToInt()
                                    val offsetY = (yPercent * footImageHeight - sensorSizePx / 2).roundToInt()

                                    val sensorColor = when {
                                        pressure > 0.7f -> Color.Red
                                        pressure > 0.4f -> Color(0xFFFF9800)
                                        pressure > 0.1f -> Color.Yellow
                                        else -> Color.Blue.copy(alpha = 0.3f)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .offset { IntOffset(offsetX, offsetY) }
                                            .size(sensorSize)
                                            .clip(CircleShape)
                                            .background(sensorColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (pressure > 0.1f) {
                                            Text(
                                                text = "${(pressure * 100).toInt()}",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Time slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Sample ${currentIndex + 1} of ${sensorData.size}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(currentData.timestamp)),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    Slider(
                        value = currentIndex.toFloat(),
                        onValueChange = { currentIndex = it.toInt() },
                        valueRange = 0f..(sensorData.size - 1).toFloat(),
                        steps = sensorData.size - 2,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = currentActivity.color,
                            activeTrackColor = currentActivity.color.copy(alpha = 0.6f)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Start", fontSize = 10.sp, color = Color.Gray)
                        Text("End", fontSize = 10.sp, color = Color.Gray)
                    }
                }

                // Raw values display
                LazyColumn(
                    modifier = Modifier.height(100.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        Text("FSR Values: [${currentData.fsrValues.joinToString(", ")}]", fontSize = 12.sp)
                        Text("Accel: [${currentData.accelX}, ${currentData.accelY}, ${currentData.accelZ}]", fontSize = 12.sp)
                        Text("Gyro: [${currentData.gyroX}, ${currentData.gyroY}, ${currentData.gyroZ}]", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityBreakdownChart(
    breakdown: List<ActivityBreakdown>,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Activity Breakdown", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            breakdown.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = item.activity.icon,
                            contentDescription = item.activity.displayName,
                            modifier = Modifier.size(16.dp),
                            tint = item.activity.color
                        )
                        Text(item.activity.displayName, fontSize = 14.sp)
                    }

                    Text(
                        "${item.percentage.toInt()}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = item.activity.color
                    )
                }

                // Progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(item.percentage / 100f)
                            .fillMaxHeight()
                            .background(item.activity.color, RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityTrackingScreen(
    isConnected: Boolean = false,
    sensorData: SensorPacket? = null,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val aiEngine = remember { SimpleShoeInsoleAI(context) }
    val storage = remember { ActivitySessionStorage(context) }

    var isModelLoaded by remember { mutableStateOf(false) }
    var isTracking by remember { mutableStateOf(false) }
    var bufferStatus by remember { mutableStateOf("0/20 records") }

    var currentActivity by remember { mutableStateOf(ActivityType.UNKNOWN) }
    var currentConfidence by remember { mutableStateOf(0f) }
    var sessionStartTime by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf("00:00") }
    var feedback by remember { mutableStateOf("Loading AI model...") }
    var sessions by remember { mutableStateOf(listOf<ActivitySession>()) }
    var selectedSession by remember { mutableStateOf<ActivitySession?>(null) }

    // Activity tracking for current session - enhanced with activity predictions
    var sessionSensorData by remember { mutableStateOf(listOf<StoredSensorData>()) }
    var activityTimestamps by remember { mutableStateOf(listOf<Pair<ActivityType, Long>>()) }
    var shouldSaveSession by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<String?>(null) }

    // Handle session deletion
    LaunchedEffect(sessionToDelete) {
        sessionToDelete?.let { sessionId ->
            storage.deleteSession(sessionId)
            sessions = storage.loadAllSessions()
            sessionToDelete = null
        }
    }

    // Handle session saving when tracking stops
    LaunchedEffect(shouldSaveSession) {
        if (shouldSaveSession && sessionSensorData.isNotEmpty()) {
            val endTime = System.currentTimeMillis()
            val totalDuration = endTime - sessionStartTime

            // Calculate activity breakdown from stored sensor data
            val activityCounts = sessionSensorData.groupBy { it.detectedActivity }
                .mapValues { (_, dataList) -> dataList.size }

            val totalSamples = sessionSensorData.size.toFloat()
            val activityBreakdown = activityCounts.map { (activity, count) ->
                ActivityBreakdown(
                    activity = activity,
                    totalTimeMs = (totalDuration * (count / totalSamples)).toLong(),
                    percentage = (count / totalSamples) * 100f
                )
            }.sortedByDescending { it.percentage }

            val session = ActivitySession(
                id = UUID.randomUUID().toString(),
                activity = activityBreakdown.firstOrNull()?.activity ?: currentActivity,
                startTime = sessionStartTime,
                endTime = endTime,
                durationMs = totalDuration,
                sensorData = sessionSensorData,
                activityBreakdown = activityBreakdown
            )

            // Save session
            storage.saveSession(session)
            sessions = storage.loadAllSessions()
            shouldSaveSession = false
        }
    }

    // Load model and sessions
    LaunchedEffect(Unit) {
        isModelLoaded = aiEngine.loadModel()
        feedback = if (isModelLoaded) {
            "AI model ready. Connect Smart Sole to start tracking."
        } else {
            "Failed to load model. Check smart_sole_model.tflite in assets."
        }
        sessions = storage.loadAllSessions()
    }

    // Process sensor data and store activity predictions
    LaunchedEffect(sensorData, isTracking) {
        if (isTracking && sensorData != null && isModelLoaded) {
            bufferStatus = aiEngine.getBufferStatus()

            // Always store sensor data with current activity prediction
            val storedData = StoredSensorData(
                timestamp = System.currentTimeMillis(),
                fsrValues = sensorData.fsrValues,
                accelX = sensorData.accelX,
                accelY = sensorData.accelY,
                accelZ = sensorData.accelZ,
                gyroX = sensorData.gyroX,
                gyroY = sensorData.gyroY,
                gyroZ = sensorData.gyroZ,
                detectedActivity = currentActivity,
                confidence = currentConfidence
            )
            sessionSensorData = sessionSensorData + storedData

            // Get new activity prediction
            aiEngine.addSensorData(sensorData)?.let { (activity, confidence) ->
                val previousActivity = currentActivity
                currentActivity = activity
                currentConfidence = confidence

                // Track activity changes for timestamp logging
                if (activity != previousActivity) {
                    activityTimestamps = activityTimestamps + Pair(activity, System.currentTimeMillis())
                }

                if (sessionStartTime > 0) {
                    val diffMs = System.currentTimeMillis() - sessionStartTime
                    val minutes = (diffMs / 60000).toInt()
                    val seconds = ((diffMs % 60000) / 1000).toInt()
                    duration = String.format("%02d:%02d", minutes, seconds)
                }

                feedback = when (activity) {
                    ActivityType.SITTING -> "💺 Currently sitting"
                    ActivityType.STANDING -> "🧍 Currently standing"
                    ActivityType.WALKING -> "🚶 Currently walking"
                    ActivityType.STAIRS -> "🪜 Climbing stairs"
                    else -> "🔍 Analyzing movement..."
                }
            }
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose { aiEngine.close() }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Header
        TopAppBar(
            title = { Text("AI Activity Tracking", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBackClicked) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                if (isConnected && isModelLoaded) {
                    IconButton(
                        onClick = {
                            if (isTracking) {
                                // Stop tracking and trigger save
                                isTracking = false
                                shouldSaveSession = true
                                aiEngine.reset()
                                bufferStatus = "0/20 records"
                            } else {
                                // Start tracking
                                isTracking = true
                                sessionStartTime = System.currentTimeMillis()
                                sessionSensorData = emptyList()
                                activityTimestamps = listOf(Pair(currentActivity, sessionStartTime))
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isTracking) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isTracking) "Stop" else "Start",
                            tint = if (isTracking) Color.Red else Color.Green
                        )
                    }
                }
            }
        )

        // Connection status
        if (!isConnected) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFF5722).copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.BluetoothDisabled,
                        contentDescription = "Not Connected",
                        modifier = Modifier.size(48.dp),
                        tint = Color(0xFFFF5722)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Connect Smart Sole for AI Tracking", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Requires 20Hz sensor data", fontSize = 14.sp, color = Color.Gray)
                }
            }
        }

        if (isConnected && isModelLoaded) {
            // Current activity (only when tracking)
            if (isTracking) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = currentActivity.color.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = currentActivity.icon,
                                contentDescription = currentActivity.displayName,
                                modifier = Modifier.size(32.dp),
                                tint = currentActivity.color
                            )
                            Column {
                                Text(currentActivity.displayName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Text("Duration: $duration", fontSize = 14.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Session history or detailed view
        if (selectedSession != null) {
            // Detailed session view
            Column(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Session Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { selectedSession = null }) {
                        Text("Back to List")
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Session Info", fontWeight = FontWeight.Bold)
                                Text("Started: ${selectedSession!!.startTimeFormatted}")
                                Text("Duration: ${selectedSession!!.durationFormatted}")
                                Text("Sensor Samples: ${selectedSession!!.sensorData.size}")
                            }
                        }
                    }

                    item {
                        ActivityBreakdownChart(selectedSession!!.activityBreakdown)
                    }

                    item {
                        FSRDataViewer(selectedSession!!.sensorData, selectedSession!!.activityBreakdown)
                    }
                }
            }
        } else {
            // Session list
            Column(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp)) {
                Text(
                    "Activity Sessions",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sessions) { session ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSession = session },
                            colors = CardDefaults.cardColors(containerColor = session.activity.color.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        session.activity.icon,
                                        contentDescription = session.activity.displayName,
                                        modifier = Modifier.size(24.dp),
                                        tint = session.activity.color
                                    )
                                    Column {
                                        Text("${session.startTimeFormatted} - ${session.durationFormatted}", fontWeight = FontWeight.Medium)
                                        Text(
                                            "${session.sensorData.size} samples",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            sessionToDelete = session.id
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete Session",
                                            tint = Color.Red,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = "View Details",
                                        tint = Color.Gray
                                    )
                                }
                            }
                        }
                    }

                    if (sessions.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.Gray.copy(alpha = 0.1f))
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.History,
                                        contentDescription = "No Sessions",
                                        modifier = Modifier.size(48.dp),
                                        tint = Color.Gray
                                    )
                                    Text("No sessions yet", fontSize = 16.sp, color = Color.Gray)
                                    Text("Start tracking to see activity history", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}