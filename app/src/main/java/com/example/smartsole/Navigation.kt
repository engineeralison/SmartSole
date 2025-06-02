package com.example.smartsole

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
// Make sure all your screen composables are imported if they are in different files
// import com.example.smartsole.LandingScreen
// import com.example.smartsole.HomePage
// import com.example.smartsole.PressurePlotScreen
// import com.example.smartsole.DataScreen
// import com.example.smartsole.HistoryLogScreen

enum class Screen {
    Landing,
    Home,
    Graph,      // This is used for PressurePlotScreen
    Data,       // Assuming you have a DataScreen
    HistoryLog  // For HistoryLogScreen
}

@Composable
fun Navigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Landing.name) {
        composable(Screen.Landing.name) {
            LandingScreen(
                onGetStartedClicked = { navController.navigate(Screen.Home.name) }
            )
        }
        composable(Screen.Home.name) {
            HomePage(
                onStartRecordingClicked = { /* TODO: Implement navigation for Start Recording */ },
                onViewGraphClicked = { navController.navigate(Screen.Graph.name) },
                // *** THIS IS THE KEY ADDITION/CHANGE FOR HISTORY LOG ***
                onViewHistoryClicked = { navController.navigate(Screen.HistoryLog.name) },
                onBackClicked = {
                    // Define behavior for back from Home. Navigating to Landing is one option.
                    // Or, if Home is a main screen, you might pop the whole back stack up to Landing
                    // or even finish the activity depending on your app's flow.
                    navController.popBackStack(Screen.Landing.name, inclusive = false)
                    // If LandingScreen should not be reachable by back press from Home after getting started:
                    // navController.navigate(Screen.Landing.name) { popUpTo(Screen.Landing.name) { inclusive = true } }
                    // Or, simpler if Landing is truly just a one-time entry:
                    // Consider if `onBackClicked` from HomePage should actually exit the app section
                    // or navigate to a different "up" destination. For now, popBackStack to Landing.
                }
            )
        }
        composable(Screen.Graph.name) { // This is the route for your pressure plot
            PressurePlotScreen(
                onBackClicked = { navController.popBackStack() } // Navigates back to the previous screen (Home)
            )
        }
        composable(Screen.Data.name) {
            DataScreen(
                // Navigates back to Home, or use popBackStack for more standard back behavior
                onBackClicked = { navController.navigate(Screen.Home.name) }
                // Alternative: onBackClicked = { navController.popBackStack() }
            )
        }

        // This route for HistoryLogScreen is correctly defined.
        composable(Screen.HistoryLog.name) {
            HistoryLogScreen(
                onBackClicked = { navController.popBackStack() } // Navigates back to the previous screen (Home)
            )
        }
    }
}