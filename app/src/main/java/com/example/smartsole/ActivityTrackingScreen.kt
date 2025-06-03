// ActivityTrackingScreen.kt
package com.example.smartsole

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

// Activity recognition models
enum class ActivityType(val displayName: String, val color: Color, val icon: ImageVector) {
    WALKING("Walking", Color(0xFF4CAF50), Icons.Default.DirectionsWalk),
    RUNNING("Running", Color(0xFFFF5722), Icons.Default.DirectionsRun),
    STANDING("Standing", Color(0xFF2196F3), Icons.Default.Accessibility),
    SITTING("Sitting", Color(0xFF9C27B0), Icons.Default.Chair),
    CLIMBING_STAIRS("Climbing Stairs", Color(0xFFFF9800), Icons.Default.Stairs),
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

data class ActivityMetrics(
    val currentActivity: ActivityType,
    val confidence: Float,
    val duration: String,
    val totalSteps: Int,
    val totalCalories: Int,
    val sessionsToday: Int
)

// ML Activity Recognition Engine (Simplified)
class ActivityRecognitionEngine {
    private val activityPatterns = mapOf(
        ActivityType.WALKING to listOf(
            // Walking pattern: consistent medium pressure, rhythmic
            listOf(200, 150, 100, 250, 300, 200),
            listOf(180, 160, 120, 280, 320, 180)
        ),
        ActivityType.RUNNING to listOf(
            // Running pattern: high pressure, rapid changes
            listOf(400, 200, 150, 500, 600, 400),
            listOf(450, 180, 120, 520, 650, 380)
        ),
        ActivityType.STANDING to listOf(
            // Standing pattern: steady distributed pressure
            listOf(150, 150, 150, 150, 150, 150),
            listOf(140, 140, 140, 140, 140, 140)
        )
    )

    fun recognizeActivity(sensorData: List<Int>, historicalData: List<List<Int>>): Pair<ActivityType, Float> {
        if (sensorData.all { it < 50 }) {
            return Pair(ActivityType.SITTING, 0.95f)
        }

        val totalPressure = sensorData.sum()
        val variance = calculateVariance(sensorData)
        val maxPressure = sensorData.maxOrNull() ?: 0

        return when {
            totalPressure > 2000 && variance > 5000 -> Pair(ActivityType.RUNNING, 0.85f)
            totalPressure > 800 && variance > 2000 -> Pair(ActivityType.WALKING, 0.78f)
            totalPressure > 300 && variance < 1000 -> Pair(ActivityType.STANDING, 0.82f)
            maxPressure > 800 && sensorData[1] > sensorData[4] -> Pair(ActivityType.CLIMBING_STAIRS, 0.72f)
            else -> Pair(ActivityType.UNKNOWN, 0.45f)
        }
    }

    private fun calculateVariance(data: List<Int>): Double {
        val mean = data.average()
        return data.map { (it - mean) * (it - mean) }.average()
    }

    fun generateFeedback(activity: ActivityType, confidence: Float, duration: String): String {
        return when (activity) {
            ActivityType.WALKING -> "Great pace! Keep up the steady walking rhythm."
            ActivityType.RUNNING -> "Excellent workout! Your running form looks good."
            ActivityType.STANDING -> "Good posture detected. Consider shifting weight occasionally."
            ActivityType.SITTING -> "You've been sitting for $duration. Consider taking a short walk."
            ActivityType.CLIMBING_STAIRS -> "Nice stair climbing! Great for cardiovascular health."
            ActivityType.UNKNOWN -> "Activity pattern detected. Keep moving!"
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
    val recognitionEngine = remember { ActivityRecognitionEngine() }
    var isTracking by remember { mutableStateOf(false) }
    var currentMetrics by remember {
        mutableStateOf(
            ActivityMetrics(
                currentActivity = ActivityType.UNKNOWN,
                confidence = 0f,
                duration = "00:00",
                totalSteps = 0,
                totalCalories = 0,
                sessionsToday = 0
            )
        )
    }

    var activitySessions by remember { mutableStateOf(listOf<ActivitySession>()) }
    var sessionStartTime by remember { mutableStateOf(0L) }
    var currentFeedback by remember { mutableStateOf("Connect your Smart Sole to start tracking") }

    // Track activity recognition
    LaunchedEffect(sensorData, isTracking) {
        if (isTracking && sensorData != null) {
            val (activity, confidence) = recognitionEngine.recognizeActivity(
                sensorData.fsrValues,
                emptyList() // Would store historical data in real implementation
            )

            val currentTime = System.currentTimeMillis()
            val duration = if (sessionStartTime > 0) {
                val diffMs = currentTime - sessionStartTime
                val minutes = (diffMs / 60000).toInt()
                val seconds = ((diffMs % 60000) / 1000).toInt()
                String.format("%02d:%02d", minutes, seconds)
            } else "00:00"

            currentMetrics = currentMetrics.copy(
                currentActivity = activity,
                confidence = confidence,
                duration = duration
            )

            currentFeedback = recognitionEngine.generateFeedback(activity, confidence, duration)
        }
    }

    // Animate confidence indicator
    val infiniteTransition = rememberInfiniteTransition(label = "confidence_pulse")
    val confidencePulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "confidence_pulse"
    )

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Header
        TopAppBar(
            title = {
                Text(
                    text = "Activity Tracking",
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClicked) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                if (isConnected) {
                    IconButton(
                        onClick = {
                            if (isTracking) {
                                // Stop tracking and save session
                                if (sessionStartTime > 0) {
                                    val session = ActivitySession(
                                        activity = currentMetrics.currentActivity,
                                        startTime = "Now",
                                        duration = currentMetrics.duration,
                                        confidence = currentMetrics.confidence,
                                        steps = Random.nextInt(50, 200),
                                        calories = Random.nextInt(10, 50)
                                    )
                                    activitySessions = listOf(session) + activitySessions
                                }
                                isTracking = false
                                sessionStartTime = 0
                            } else {
                                isTracking = true
                                sessionStartTime = System.currentTimeMillis()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isTracking) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isTracking) "Stop Tracking" else "Start Tracking",
                            tint = if (isTracking) Color.Red else Color.Green
                        )
                    }
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
                    Icon(
                        imageVector = Icons.Default.BluetoothDisabled,
                        contentDescription = "Not Connected",
                        modifier = Modifier.size(48.dp),
                        tint = Color(0xFFFF5722)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Connect Smart Sole for AI Activity Tracking",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Real-time ML-powered activity recognition requires sensor data",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            // Current Activity Status
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = currentMetrics.currentActivity.color.copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = currentMetrics.currentActivity.icon,
                                contentDescription = currentMetrics.currentActivity.displayName,
                                modifier = Modifier.size(32.dp),
                                tint = currentMetrics.currentActivity.color
                            )
                            Column {
                                Text(
                                    text = currentMetrics.currentActivity.displayName,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Duration: ${currentMetrics.duration}",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        // Confidence indicator
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(
                                modifier = Modifier.size(60.dp)
                            ) {
                                val strokeWidth = 6.dp.toPx()
                                val radius = (size.minDimension - strokeWidth) / 2

                                // Background circle
                                drawCircle(
                                    color = Color.Gray.copy(alpha = 0.3f),
                                    radius = radius,
                                    style = Stroke(strokeWidth, cap = StrokeCap.Round)
                                )

                                // Confidence arc
                                drawArc(
                                    color = currentMetrics.currentActivity.color.copy(alpha = confidencePulse),
                                    startAngle = -90f,
                                    sweepAngle = 360f * currentMetrics.confidence,
                                    useCenter = false,
                                    style = Stroke(strokeWidth, cap = StrokeCap.Round)
                                )
                            }
                            Text(
                                text = "${(currentMetrics.confidence * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // AI Feedback
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "AI Feedback",
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = currentFeedback,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Today's Statistics
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.95f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsWalk,
                            contentDescription = "Steps",
                            tint = Color(0xFF2196F3)
                        )
                        Text(
                            text = "${currentMetrics.totalSteps}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Steps",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.95f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Calories",
                            tint = Color(0xFFFF5722)
                        )
                        Text(
                            text = "${currentMetrics.totalCalories}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Calories",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.95f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = "Sessions",
                            tint = Color(0xFF9C27B0)
                        )
                        Text(
                            text = "${currentMetrics.sessionsToday}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sessions",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Activity History
            Text(
                text = "Recent Activity Sessions",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(activitySessions) { session ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = session.activity.color.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = session.activity.icon,
                                    contentDescription = session.activity.displayName,
                                    modifier = Modifier.size(24.dp),
                                    tint = session.activity.color
                                )
                                Column {
                                    Text(
                                        text = session.activity.displayName,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${session.duration} • ${(session.confidence * 100).toInt()}% confidence",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                session.steps?.let {
                                    Text(
                                        text = "$it steps",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                                session.calories?.let {
                                    Text(
                                        text = "$it cal",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }

                if (activitySessions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Gray.copy(alpha = 0.1f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "No Sessions",
                                    modifier = Modifier.size(48.dp),
                                    tint = Color.Gray
                                )
                                Text(
                                    text = "No activity sessions yet",
                                    fontSize = 16.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "Start tracking to see your activity history",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}