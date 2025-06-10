package com.example.smartsole

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


@Composable
fun HomePage(
    onStartRecordingClicked: () -> Unit,
    onViewGraphClicked: () -> Unit,
    onViewHistoryClicked: () -> Unit,
    onViewLivePressureClicked: () -> Unit = {},
    onStartActivityTrackingClicked: () -> Unit = {},
    onConnectBluetoothClicked: () -> Unit = {},
    isBluetoothConnected: Boolean = false,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    // TODO: update this
    var dailySteps by remember { mutableStateOf(8247) }
    var timeOnFeet by remember { mutableStateOf("4h 32m") }
    var lastUpdated by remember { mutableStateOf("Just now") }

    // text for date
    val formattedDate = remember {
        LocalDate.now().format(
            DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.getDefault())
        )
    }
    // motivational quotes
    val motivationalQuotes = listOf(
        "You’ve got this!",
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
            dailySteps += (1..5).random()
            lastUpdated = "Just now"
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp) // Space between Pressure Plot & Foot History
        ) {
            // Top spacing
            Spacer(modifier = Modifier.height(130.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp) // Use spacing directly
            ) {
                // Steps Today button (takes equal space)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f), // Keeps it square
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.steps_today),
                        contentDescription = "Steps Today",
                        modifier = Modifier.fillMaxSize()
                    )
                    Text(
                        text = "$dailySteps steps",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.offset(x = (-27).dp, y = 12.dp)
                    )
                }

                // Time on Feet button (same size as above)
                Box(
                    modifier = Modifier
                        .weight(1.01f)
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.time_on_feet),
                        contentDescription = "Time on Feet",
                        modifier = Modifier.fillMaxSize()
                    )
                    Text(
                        text = timeOnFeet,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.offset(x = (-40).dp, y = 10.dp)
                    )
                }
            }

            // Last Updated Indicator
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                )
            ) {
                Text(
                    text = "Updated: $lastUpdated",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Pressure Plot Button
            Image(
                painter = painterResource(id = R.drawable.pressure_plot),
                contentDescription = "Pressure Plot",
                modifier = Modifier
                    .width(230.dp)
                    .height(160.dp)
                    .clickable(onClick = onViewGraphClicked)
            )

            Spacer(modifier = Modifier.height(0.dp))

            // Original Foot History Button
            Image(
                painter = painterResource(id = R.drawable.foot_history),
                contentDescription = "Foot History",
                modifier = Modifier
                    .width(240.dp)
                    .height(120.dp)
                    .clickable(onClick = onViewHistoryClicked)
            )

            Spacer(modifier = Modifier.height(40.dp))

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