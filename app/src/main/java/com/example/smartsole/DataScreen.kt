package com.example.smartsole

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smartsole.Header
import com.example.smartsole.ui.theme.Beige

@Composable
fun DataScreen(onBackClicked: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Beige),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Header(onBackClicked = onBackClicked)
        Text(text = "Data Screen", fontWeight = FontWeight.Bold)
    }
}