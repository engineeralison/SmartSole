package com.example.smartsole

import androidx.compose.foundation.Image
import androidx.compose.material3.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.smartsole.Header
import com.example.smartsole.ui.theme.Beige

@Composable
fun LandingScreen(
    onGetStartedClicked: () -> Unit, // Changed parameter name
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Beige), // Apply the beige background
        horizontalAlignment = Alignment.CenterHorizontally
    )  {
        // Image filling the top half
        Image(
            painter = painterResource(id = R.drawable.icon), // Assuming your image is named smartsole
            contentDescription = stringResource(R.string.icon_description),
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.6f), // Make image fill 60% of parent height
            contentScale = ContentScale.Crop // Crop the image to fill the bounds
        )

        // Content below the image
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.40f) // Content fills the bottom 50%
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly // Distribute space evenly
        ) {
            // "Welcome to Smart Sole" title
            Text(
                text = "Welcome to Smart Sole",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Mini description
            Text(
                text = "Your real-time foot movement companion. Track pressure, optimize your gait, and prevent injury.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // "Get Started" button
            Button(
                onClick = onGetStartedClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp), // Give the button a fixed height
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black) // Black background
            ) {
                Text("Get Started", color = Color.White) // White text
            }
        }
    }
}