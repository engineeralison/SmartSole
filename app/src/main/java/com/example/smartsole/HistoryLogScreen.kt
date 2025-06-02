package com.example.smartsole // Or your specific UI package

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.smartsole.ui.theme.SmartSoleTheme

@Composable
fun HistoryLogScreen(
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize() // Make the Box fill the entire screen
    ) {
        // 1. Background Image
        Image(
            painter = painterResource(id = R.drawable.dashboard), // Ensure dashboard.png is in res/drawable
            contentDescription = "Dashboard Background",
            modifier = Modifier.fillMaxSize(), // Image fills the Box
            contentScale = ContentScale.Crop    // Or FillBounds, depending on your image aspect ratio
        )

        // 2. Back Button (Top-Left)
        IconButton(
            onClick = onBackClicked,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to Home",
                tint = Color.Black,
                modifier = Modifier.size(36.dp)
            )
        }

        // 3. Centered Text
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp), // Add some horizontal padding for the text
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "User's Gait Pressure / Data",
                style = MaterialTheme.typography.headlineMedium, // Choose an appropriate style
                textAlign = TextAlign.Center,
                color = Color.Black // Adjust color for visibility against your dashboard.png
            )
        }
    }
}

@Preview(showBackground = true, device = "spec:shape=Normal,width=360,height=640,unit=dp,dpi=480")
@Composable
fun HistoryLogScreenPreview() {
    SmartSoleTheme {
        // In preview, you might want to use a solid color if dashboard.png isn't rendering
        // or to focus on the text and button placement.
        HistoryLogScreen(onBackClicked = {})
    }
}