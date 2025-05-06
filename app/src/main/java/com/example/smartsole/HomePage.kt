package com.example.smartsole

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.smartsole.ui.theme.Beige // Your primary beige color
import com.example.smartsole.Header
// Define some beige variants for the radial gradient
val CenterBeige = Color(0xFFF5F5DC) // Lighter beige for the center
val OuterBeige = Color(0xFFC3B091) // Darker beige for the outer edges

@Composable
fun HomePage(
    onStartRecordingClicked: () -> Unit,
    onViewGraphClicked: () -> Unit,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                .padding(horizontal = 16.dp), // Apply horizontal padding here
            // No verticalArrangement or Spacers for top-down layout
        ) {
            // Small description above title
            Text(
                text = "Everyday we're running",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 4.dp)
                    .align(Alignment.Start)
            )

            // "Hello User!" title
            Text(
                text = "Hello User!",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .align(Alignment.Start)
            )

            // Container for "My Plan" and buttons with radial gradient backdrop
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f) // Container takes up 90% of the width
                    .wrapContentHeight() // Wrap content to take minimum height
                    .background(
                        brush = Brush.radialGradient( // Apply a radial gradient
                            colors = listOf(CenterBeige, OuterBeige) // Colors for radial gradient
                        ),
                        shape = MaterialTheme.shapes.medium // Optional: rounded corners
                    )
                    .padding(24.dp) // Increased padding inside the gradient container
                    .align(Alignment.CenterHorizontally), // Center this container horizontally
                horizontalAlignment = Alignment.CenterHorizontally, // Center content within the gradient container
                verticalArrangement = Arrangement.spacedBy(16.dp) // Increased spacing between items in this column
            ) {
                // "My Plan" subtitle inside the gradient container
                Text(
                    text = "My Plan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Column to center the buttons within the gradient container
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp) // Increased spacing between buttons
                ) {
                    // "Start Recording Data" button
                    Button(
                        onClick = onStartRecordingClicked,
                        modifier = Modifier
                            .fillMaxWidth(0.9f), // Buttons fill 90% of the container width
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                    ) {
                        Text(
                            "Start Recording Data",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // "View Graph" button
                    Button(
                        onClick = onViewGraphClicked,
                        modifier = Modifier
                            .fillMaxWidth(0.9f), // Buttons fill 90% of the container width
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