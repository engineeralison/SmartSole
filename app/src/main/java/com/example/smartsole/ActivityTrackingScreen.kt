package com.example.smartsole

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp
import kotlin.random.Random

// Activity recognition models - Updated to 4 activities
enum class ActivityType(val displayName: String, val color: Color, val icon: ImageVector) {
    SITTING("Sitting", Color(0xFF9C27B0), Icons.Default.Chair),
    STANDING("Standing", Color(0xFF2196F3), Icons.Default.Accessibility),
    WALKING("Walking", Color(0xFF4CAF50), Icons.AutoMirrored.Filled.DirectionsWalk),
    STAIRS("Climbing Stairs", Color(0xFFFF9800), Icons.Default.Stairs),
    UNKNOWN("Unknown", Color(0xFF757575), Icons.Default.QuestionMark)
}

data class ActivitySession(
    val activity: ActivityType,
    val startTime: String,
    val duration: String,
    val confidence: Float,
    val steps: Int? = null,
    val calories: Int? = null
)

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

        // Log the input features for debugging
        Log.d("ShoeInsoleAI", "Raw FSR: [${sensorPacket.fsrValues.joinToString(",")}]")
        Log.d("ShoeInsoleAI", "Raw IMU: accel=[${sensorPacket.accelX},${sensorPacket.accelY},${sensorPacket.accelZ}] gyro=[${sensorPacket.gyroX},${sensorPacket.gyroY},${sensorPacket.gyroZ}]")
        Log.d("ShoeInsoleAI", "Processed features: [${features.joinToString(",") { "%.3f".format(it) }}]")

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

            // Log the full input array being sent to the model
            Log.d("ShoeInsoleAI", "=== FULL MODEL INPUT ===")
            inputArray[0].forEachIndexed { timeIndex, features ->
                Log.d("ShoeInsoleAI", "  Time $timeIndex: [${features.joinToString(",") { "%.3f".format(it) }}]")
            }
            Log.d("ShoeInsoleAI", "=== END MODEL INPUT ===")

            val outputArray = Array(1) { FloatArray(4) }

            interpreter?.run(inputArray, outputArray)

            val probabilities = outputArray[0]

            // Log model output
            Log.d("ShoeInsoleAI", "Raw model output: [${probabilities.joinToString(",") { "%.6f".format(it) }}]")

            // Apply softmax
            val maxLogit = probabilities.maxOrNull() ?: 0f
            val exps = probabilities.map { exp((it - maxLogit).toDouble()).toFloat() }
            val sumExps = exps.sum()
            val softmaxProbs = if (sumExps > 0) exps.map { it / sumExps }.toFloatArray() else probabilities

            val maxIndex = softmaxProbs.indices.maxByOrNull { softmaxProbs[it] } ?: 0
            val confidence = softmaxProbs[maxIndex]

            Log.d("ShoeInsoleAI", "Softmax probs: [${softmaxProbs.joinToString(",") { "%.6f".format(it) }}]")
            Log.d("ShoeInsoleAI", "Predicted index: $maxIndex, confidence: ${(confidence * 100).toInt()}%")

            val activity = when (maxIndex) {
                0 -> ActivityType.SITTING
                1 -> ActivityType.STAIRS
                2 -> ActivityType.STANDING
                3 -> ActivityType.WALKING
                else -> ActivityType.UNKNOWN
            }

            Log.d("ShoeInsoleAI", "Prediction: ${activityLabels[maxIndex]} (${(confidence * 100).toInt()}%)")
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
    var isModelLoaded by remember { mutableStateOf(false) }
    var isTracking by remember { mutableStateOf(false) }
    var bufferStatus by remember { mutableStateOf("0/20 records") }

    var currentActivity by remember { mutableStateOf(ActivityType.UNKNOWN) }
    var confidence by remember { mutableStateOf(0f) }
    var sessionStartTime by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf("00:00") }
    var feedback by remember { mutableStateOf("Loading AI model...") }
    var sessions by remember { mutableStateOf(listOf<ActivitySession>()) }

    // Load model
    LaunchedEffect(Unit) {
        isModelLoaded = aiEngine.loadModel()
        feedback = if (isModelLoaded) {
            "AI model ready. Connect Smart Sole to start tracking."
        } else {
            "Failed to load model. Check smart_sole_model.tflite in assets."
        }
    }

    // Process sensor data
    LaunchedEffect(sensorData, isTracking) {
        if (isTracking && sensorData != null && isModelLoaded) {
            bufferStatus = aiEngine.getBufferStatus()

            aiEngine.addSensorData(sensorData)?.let { (activity, conf) ->
                currentActivity = activity
                confidence = conf

                if (sessionStartTime > 0) {
                    val diffMs = System.currentTimeMillis() - sessionStartTime
                    val minutes = (diffMs / 60000).toInt()
                    val seconds = ((diffMs % 60000) / 1000).toInt()
                    duration = String.format("%02d:%02d", minutes, seconds)
                }

                feedback = when (activity) {
                    ActivityType.SITTING -> "💺 Sitting detected (${(conf * 100).toInt()}% confidence)"
                    ActivityType.STANDING -> "🧍 Standing detected (${(conf * 100).toInt()}% confidence)"
                    ActivityType.WALKING -> "🚶 Walking detected (${(conf * 100).toInt()}% confidence)"
                    ActivityType.STAIRS -> "🪜 Stairs detected (${(conf * 100).toInt()}% confidence)"
                    else -> "🔍 Analyzing movement patterns..."
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
                                if (sessionStartTime > 0) {
                                    val session = ActivitySession(
                                        activity = currentActivity,
                                        startTime = "Now",
                                        duration = duration,
                                        confidence = confidence,
                                        steps = Random.nextInt(50, 200),
                                        calories = Random.nextInt(10, 50)
                                    )
                                    sessions = listOf(session) + sessions
                                }
                                isTracking = false
                                sessionStartTime = 0
                                aiEngine.reset()
                                bufferStatus = "0/20 records"
                            } else {
                                isTracking = true
                                sessionStartTime = System.currentTimeMillis()
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

        // Model status
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isModelLoaded) Color(0xFF4CAF50).copy(alpha = 0.1f) else Color(0xFFFF5722).copy(alpha = 0.1f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isModelLoaded) Icons.Default.Psychology else Icons.Default.Warning,
                        contentDescription = "Model Status",
                        tint = if (isModelLoaded) Color(0xFF4CAF50) else Color(0xFFFF5722)
                    )
                    Text("TensorFlow Lite Model", fontWeight = FontWeight.Bold)
                }
                Text(
                    text = if (isModelLoaded) "4 activities: Sitting, Standing, Walking, Stairs" else "Model failed to load",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                if (isTracking) {
                    Text("Buffer: $bufferStatus", fontSize = 12.sp, color = Color.Blue, fontWeight = FontWeight.Medium)
                }
            }
        }

        if (isConnected && isModelLoaded) {
            // Current activity
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

                    Text(
                        text = "${(confidence * 100).toInt()}%",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentActivity.color
                    )
                }
            }

            // AI feedback
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = "AI Feedback",
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(feedback, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Session history
            Text(
                "Activity Sessions",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sessions) { session ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = session.activity.color.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    session.activity.icon,
                                    contentDescription = session.activity.displayName,
                                    modifier = Modifier.size(24.dp),
                                    tint = session.activity.color
                                )
                                Column {
                                    Text(session.activity.displayName, fontWeight = FontWeight.Medium)
                                    Text(
                                        "${session.duration} • ${(session.confidence * 100).toInt()}%",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                session.steps?.let { Text("$it steps", fontSize = 12.sp, color = Color.Gray) }
                                session.calories?.let { Text("$it cal", fontSize = 12.sp, color = Color.Gray) }
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