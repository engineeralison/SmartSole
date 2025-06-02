package com.example.smartsole

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
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
import com.example.smartsole.ui.theme.Beige

@Composable
fun LandingScreen(
    onGetStartedClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        // Background image
        Image(
            painter = painterResource(id = R.drawable.onboarding),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(35.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top logo
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = stringResource(R.string.icon_description),
                modifier = Modifier
                    .size(250.dp)
                    .align(Alignment.CenterHorizontally)
                    .offset(y = 80.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Welcome text and button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {

                // Clickable image button
                Image(
                    painter = painterResource(id = R.drawable.button_get_started),
                    contentDescription = "Get Started",
                    modifier = Modifier
                        .height(56.dp)
                        .fillMaxWidth()
                        .clickable { onGetStartedClicked() }
                )
            }
        }
    }
}
