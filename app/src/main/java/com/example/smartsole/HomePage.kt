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

    // simulate real-time updates for demo
    LaunchedEffect(Unit) {
        while (true) {
            delay(30000) // Update every 30 seconds
            dailySteps += (1..5).random()
            lastUpdated = "Just now"
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top spacing
            Spacer(modifier = Modifier.height(120.dp))

            // Daily Stats Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Steps Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.95f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsWalk,
                            contentDescription = "Steps",
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = dailySteps.toString(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "Steps Today",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Time on Feet Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.95f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Time",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = timeOnFeet,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "Time on Feet",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
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

            Spacer(modifier = Modifier.height(4.dp))

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

            Spacer(modifier = Modifier.height(4.dp))

            // Bluetooth Connection Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isBluetoothConnected)
                        Color(0xFF4CAF50).copy(alpha = 0.9f)
                    else
                        Color(0xFFFF5722).copy(alpha = 0.9f)
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
                                contentColor = Color(0xFFFF5722)
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