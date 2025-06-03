// DailyStatsManager.kt
package com.example.smartsole

import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

data class DailyStats(
    val date: String,
    val steps: Int,
    val timeOnFeetMinutes: Int,
    val caloriesBurned: Int,
    val distanceMeters: Int,
    val activeSessions: Int
) {
    val timeOnFeetFormatted: String
        get() = "${timeOnFeetMinutes / 60}h ${timeOnFeetMinutes % 60}m"

    val distanceKm: Float
        get() = distanceMeters / 1000f
}

class DailyStatsManager {
    private var _currentStats = mutableStateOf(
        DailyStats(
            date = getCurrentDate(),
            steps = 0,
            timeOnFeetMinutes = 0,
            caloriesBurned = 0,
            distanceMeters = 0,
            activeSessions = 0
        )
    )

    val currentStats: State<DailyStats> = _currentStats

    private var lastStepDetection = 0L
    private var lastPressureSum = 0
    private var sessionStartTime: Long? = null
    private var isCurrentlyActive = false

    // Step detection algorithm using pressure sensors
    fun processSensorData(sensorPacket: SensorPacket) {
        val currentTime = System.currentTimeMillis()
        val totalPressure = sensorPacket.fsrValues.sum()

        // Step detection logic
        detectStep(totalPressure, currentTime)

        // Activity tracking
        updateActivityStatus(totalPressure, currentTime)

        // Update calories (rough estimation)
        updateCalories()
    }

    private fun detectStep(totalPressure: Int, currentTime: Long) {
        // Simple step detection: look for pressure peaks
        val pressureDiff = abs(totalPressure - lastPressureSum)
        val timeDiff = currentTime - lastStepDetection

        // Step criteria: significant pressure change + reasonable timing
        if (pressureDiff > 300 && timeDiff > 300 && totalPressure > 500) {
            addStep()
            lastStepDetection = currentTime
        }

        lastPressureSum = totalPressure
    }

    private fun updateActivityStatus(totalPressure: Int, currentTime: Long) {
        val isActive = totalPressure > 200 // Threshold for being "on feet"

        if (isActive && !isCurrentlyActive) {
            // Started being active
            sessionStartTime = currentTime
            isCurrentlyActive = true

            _currentStats.value = _currentStats.value.copy(
                activeSessions = _currentStats.value.activeSessions + 1
            )
        } else if (!isActive && isCurrentlyActive) {
            // Stopped being active
            sessionStartTime?.let { startTime ->
                val sessionDuration = ((currentTime - startTime) / 60000).toInt() // minutes
                _currentStats.value = _currentStats.value.copy(
                    timeOnFeetMinutes = _currentStats.value.timeOnFeetMinutes + sessionDuration
                )
            }
            isCurrentlyActive = false
            sessionStartTime = null
        }
    }

    private fun addStep() {
        val newSteps = _currentStats.value.steps + 1
        val newDistance = (newSteps * 0.78f).toInt() // Average step length 78cm

        _currentStats.value = _currentStats.value.copy(
            steps = newSteps,
            distanceMeters = newDistance
        )
    }

    private fun updateCalories() {
        // Rough calorie calculation: 0.04 calories per step for average person
        val estimatedCalories = (_currentStats.value.steps * 0.04f).toInt()
        _currentStats.value = _currentStats.value.copy(
            caloriesBurned = estimatedCalories
        )
    }

    fun resetDailyStats() {
        _currentStats.value = DailyStats(
            date = getCurrentDate(),
            steps = 0,
            timeOnFeetMinutes = 0,
            caloriesBurned = 0,
            distanceMeters = 0,
            activeSessions = 0
        )
    }

    // Simulate realistic daily progression for demo
    suspend fun simulateRealisticProgression() {
        while (true) {
            delay(60000) // Update every minute

            // Simulate gradual increase throughout the day
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val activityLevel = when (hour) {
                in 6..8 -> 0.3f // Morning activity
                in 9..11 -> 0.6f // Active morning
                in 12..13 -> 0.4f // Lunch break
                in 14..17 -> 0.8f // Afternoon peak
                in 18..20 -> 0.5f // Evening activity
                else -> 0.1f // Low activity
            }

            // Add realistic steps based on time of day
            if (activityLevel > 0.2f && kotlin.random.Random.nextFloat() < activityLevel) {
                repeat(kotlin.random.Random.nextInt(1, 5)) {
                    addStep()
                }

                // Occasionally add time on feet
                if (kotlin.random.Random.nextFloat() < 0.3f) {
                    _currentStats.value = _currentStats.value.copy(
                        timeOnFeetMinutes = _currentStats.value.timeOnFeetMinutes + 1
                    )
                }
            }
        }
    }

    private fun getCurrentDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    // Health insights based on daily stats
    fun getHealthInsights(): List<String> {
        val stats = _currentStats.value
        val insights = mutableListOf<String>()

        when {
            stats.steps >= 10000 -> insights.add("🎉 Excellent! You've reached 10,000+ steps today!")
            stats.steps >= 7500 -> insights.add("👍 Great job! You're close to your 10,000 step goal!")
            stats.steps >= 5000 -> insights.add("💪 Good progress! Keep moving to reach your step goal!")
            else -> insights.add("🚶 Start moving! Every step counts toward better health!")
        }

        when {
            stats.timeOnFeetMinutes >= 300 -> insights.add("⏰ Great active time! You've been on your feet for 5+ hours!")
            stats.timeOnFeetMinutes >= 180 -> insights.add("🕐 Good activity level! Consider a few more active periods!")
            stats.timeOnFeetMinutes >= 60 -> insights.add("⌚ Nice start! Try to increase your time on feet!")
            else -> insights.add("⏱️ Remember to take regular breaks from sitting!")
        }

        if (stats.activeSessions >= 8) {
            insights.add("🔥 Amazing! You've had lots of active sessions today!")
        } else if (stats.activeSessions >= 5) {
            insights.add("✨ Good job breaking up your day with activity!")
        }

        return insights
    }
}

// Composable hook for using the stats manager
@Composable
fun rememberDailyStatsManager(): DailyStatsManager {
    val manager = remember { DailyStatsManager() }

    // Start realistic simulation for demo purposes
    LaunchedEffect(manager) {
        manager.simulateRealisticProgression()
    }

    return manager
}