package com.example.smartsole

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smartsole.Header

@Composable
fun LandingScreen(
    onViewPressurePlotClicked: () -> Unit,
    onViewPressureDataClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Header()

        Spacer(modifier = Modifier.padding(16.dp))

        Text(
            text = "Welcome User!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.padding(16.dp))
        Image(
            painter = painterResource(id = R.drawable.footprint),
            contentDescription = stringResource(R.string.footprint_description)
        )
        Spacer(modifier = Modifier.padding(16.dp))

        Button(
            onClick = onViewPressurePlotClicked,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp) // Added padding and fillMaxWidth
        ) {
            Text(text = "View Pressure Plot", style = MaterialTheme.typography.headlineSmall) // Added a style
        }

        Spacer(modifier = Modifier.padding(8.dp))

        Button(
            onClick = onViewPressureDataClicked,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp) // Added padding and fillMaxWidth
        ) {
            Text(text = "View Pressure Data", style = MaterialTheme.typography.headlineSmall) // Added a style
        }
    }
}