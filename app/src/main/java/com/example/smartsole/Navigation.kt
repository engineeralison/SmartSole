package com.example.smartsole

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun Navigation() {
    val navController = rememberNavController()

    var isBluetoothConnected by remember { mutableStateOf(false) }
    var latestSensorData by remember { mutableStateOf<SensorPacket?>(null) }

    NavHost(navController = navController, startDestination = "landing") {

        composable("landing") {
            LandingScreen(
                onGetStartedClicked = { navController.navigate("home") }
            )
        }

        composable("home") {
            HomePage(
                onStartRecordingClicked = {
                    navController.navigate("bluetooth_sensor")
                },
                onViewGraphClicked = {
                    navController.navigate("live_pressure")
                },
                onViewHistoryClicked = {
                    navController.navigate("bluetooth_sensor")
                },
                onConnectBluetoothClicked = {
                    navController.navigate("bluetooth_sensor")
                },
                isBluetoothConnected = isBluetoothConnected,
                onBackClicked = {
                    // Handle back navigation
                }
            )
        }

        composable("bluetooth_sensor") {
            BluetoothSensorScreen(
                onBackClicked = {
                    navController.popBackStack()
                },
                onConnectionStateChanged = { connected ->
                    isBluetoothConnected = connected
                },
                onSensorDataReceived = { data ->
                    latestSensorData = data
                }
            )
        }

        composable("live_pressure") {
            LivePressureScreen(
                isConnected = isBluetoothConnected,
                sensorData = latestSensorData,
                onBackClicked = {
                    navController.popBackStack()
                },
                onConnectClicked = {
                    navController.navigate("bluetooth_sensor")
                }
            )
        }


        // graph screen
        composable("graph") {
            GraphScreen(
                onBackClicked = {
                    navController.popBackStack()
                }
            )
        }

        // history screen
        composable("history") {
            HistoryLogScreen(
                onBackClicked = {
                    navController.popBackStack()
                }
            )
        }

        // data screen
        composable("data") {
            DataScreen(
                onBackClicked = {
                    navController.popBackStack()
                }
            )
        }
    }
}