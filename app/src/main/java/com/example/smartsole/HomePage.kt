// HomePage.kt (Updated)
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
    onStartRecordingClicked: () -> Unit,
    onViewGraphClicked: () -> Unit,
    onViewHistoryClicked: () -> Unit,
    onBackClicked: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Spacer to push content down the screen
            Spacer(modifier = Modifier.height(170.dp))

            // Pressure Plot Button
            Image(
                painter = painterResource(id = R.drawable.pressure_plot),
                contentDescription = "Pressure Plot",
                modifier = Modifier
                    .width(230.dp)
                    .height(180.dp)
                    .clickable(onClick = onViewGraphClicked)
            )

            // Foot History Button - Now navigates to Bluetooth sensor page
            Image(
                painter = painterResource(id = R.drawable.foot_history),
                contentDescription = "Foot History",
                modifier = Modifier
                    .width(240.dp)
                    .height(140.dp)
                    .clickable(onClick = onViewHistoryClicked) // Goes to Bluetooth sensor page
            )
        }
    }
}
