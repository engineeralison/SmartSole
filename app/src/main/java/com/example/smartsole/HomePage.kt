package com.example.smartsole

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.smartsole.ui.theme.Beige

val CenterBeige = Color(0xFFF5F5DC)
val OuterBeige = Color(0xFFC3B091)

@Composable
fun HomePage(
    onStartRecordingClicked: () -> Unit,
    onViewGraphClicked: () -> Unit,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bluetoothHelper = remember { BluetoothHelper(context) }
    var isConnected by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Beige),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Header(onBackClicked = onBackClicked)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Everyday we're running",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 4.dp)
                    .align(Alignment.Start)
            )

            Text(
                text = "Hello User!",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .align(Alignment.Start)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
                    .background(
                        brush = Brush.radialGradient(colors = listOf(CenterBeige, OuterBeige)),
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(24.dp)
                    .align(Alignment.CenterHorizontally),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "My Plan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (isConnected) {
                                bluetoothHelper.disconnect()
                                isConnected = false
                            } else if (!isScanning) {
                                if (bluetoothHelper.isBluetoothEnabled()) {
                                    isScanning = true
                                    bluetoothHelper.startScanning { device ->
                                        Toast.makeText(context, "Connecting to ${device.name}", Toast.LENGTH_SHORT).show()
                                        bluetoothHelper.connectToDevice(device) { connected ->
                                            isConnected = connected
                                            if (connected) {
                                                Toast.makeText(context, "Connected to ${device.name}", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Disconnected from ${device.name}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "Please enable Bluetooth", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(0.9f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                    ) {
                        Text(
                            if (isConnected) "Disconnect Device" else "Connect Device",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Button(
                        onClick = onViewGraphClicked,
                        modifier = Modifier.fillMaxWidth(0.9f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                    ) {
                        Text(
                            "View Pressure Graph",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
