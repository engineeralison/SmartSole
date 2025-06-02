// Navigation.kt
package com.example.smartsole

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun Navigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomePage(
                onStartRecordingClicked = {
                    // Navigate to Bluetooth sensor page for recording
                    navController.navigate("bluetooth_sensor")
                },
                onViewGraphClicked = {
                    // Navigate to your existing graph page
                    // navController.navigate("graph")
                },
                onViewHistoryClicked = {
                    // Navigate to Bluetooth sensor page for history
                    navController.navigate("bluetooth_sensor")
                },
                onBackClicked = {
                    // Handle back navigation if needed
                }
            )
        }

        composable("bluetooth_sensor") {
            BluetoothSensorScreen(
                onBackClicked = {
                    navController.popBackStack()
                }
            )
        }

        // Add other destinations as needed
        // composable("graph") { YourGraphScreen() }
    }
}