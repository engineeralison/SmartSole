package com.example.smartsole

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

enum class Screen {
    Landing,
    Home,
    Graph,
    Data
}

@Composable
fun Navigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Landing.name) {
        composable(Screen.Landing.name) {
            LandingScreen(
                // "Get Started" button now navigates to the Home screen
                onGetStartedClicked = { navController.navigate(Screen.Home.name) }
            )
        }
        composable(Screen.Home.name) {
            // Composable for the new Home screen
            HomePage(
                // "Start Recording Data" button (define navigation later if needed)
                onStartRecordingClicked = { /* TODO: Implement navigation for Start Recording */ },
                // "View Graph" button navigates to the Graph screen
                onViewGraphClicked = { navController.navigate(Screen.Graph.name) },
                // Back button on Home navigates back to Landing
                onBackClicked = { navController.navigate(Screen.Landing.name) }
            )
        }
        composable(Screen.Graph.name) {
            GraphScreen(
                // Back button on Graph now navigates back to Home
                onBackClicked = { navController.navigate(Screen.Home.name) }
            )
        }
        composable(Screen.Data.name) {
            DataScreen(
                // Back button on Data now navigates back to Home
                onBackClicked = { navController.navigate(Screen.Home.name) }
            )
        }
    }
}