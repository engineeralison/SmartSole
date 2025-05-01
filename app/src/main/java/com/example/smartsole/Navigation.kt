package com.example.smartsole

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

// Below are the current three different screens for our app
// Landing: landing page that user first approaches
// Graph: contains the pressure mapping of foot
// Data: contains just general basic data chart of data
enum class Screen {
    Landing,
    Graph,
    Data
}

// Manages transitions from different screens
@Composable
fun Navigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Landing.name) {
        composable(Screen.Landing.name) {
            LandingScreen(
                onViewPressurePlotClicked = { navController.navigate(Screen.Graph.name) },
                onViewPressureDataClicked = { navController.navigate(Screen.Data.name) }
            )
        }
        composable(Screen.Graph.name) {
            GraphScreen(onBackClicked = { navController.navigate(Screen.Landing.name) })
        }
        composable(Screen.Data.name) {
            DataScreen(onBackClicked = { navController.navigate(Screen.Landing.name) })
        }
    }
}