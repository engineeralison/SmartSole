package com.example.smartsole

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun HomePage(
    onStartRecordingClicked: () -> Unit, // This parameter is still present but noted as not used in the current layout
    onViewGraphClicked: () -> Unit,
    onViewHistoryClicked: () -> Unit,  // <-- ADDED: New parameter for history navigation
    onBackClicked: () -> Unit,          // This parameter is still present but noted as not used in the current layout
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
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
            verticalArrangement = Arrangement.spacedBy(16.dp) // Add space between children of the Column
        ) {
            // Spacer to push content down the screen
            Spacer(modifier = Modifier.height(170.dp)) // Adjust this to control top spacing

            // Pressure Plot Button
            Image(
                painter = painterResource(id = R.drawable.pressure_plot),
                contentDescription = "Pressure Plot",
                modifier = Modifier
                    .width(230.dp)
                    .height(180.dp)
                    .clickable(onClick = onViewGraphClicked) // Correct: uses onViewGraphClicked
            )

            // Foot History Button
            Image(
                painter = painterResource(id = R.drawable.foot_history),
                contentDescription = "Foot History", // This is the button we're targeting
                modifier = Modifier
                    .width(240.dp)
                    .height(140.dp)
                    .clickable(onClick = onViewHistoryClicked) // <-- CHANGED: Now uses onViewHistoryClicked
            )

            // If you need more specific spacing only between the two images,
            // you can add a Spacer explicitly:
            // Spacer(modifier = Modifier.height(20.dp))
        }
    }
}