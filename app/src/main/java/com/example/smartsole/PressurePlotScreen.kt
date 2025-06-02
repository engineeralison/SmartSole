package com.example.smartsole

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.smartsole.ui.theme.SmartSoleTheme
import kotlin.math.roundToInt
import kotlin.random.Random

data class SensorCircle(
    val id: Int,
    val defaultXPercent: Float,
    val defaultYPercent: Float,
    var isActive: Boolean = false,
    var pressureValue: Float = Random.nextFloat() // Example default pressure simulation
)

@Composable
fun PressurePlotScreen(
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sensorCirclesState = remember {
        mutableStateListOf(
            SensorCircle(id = 1, defaultXPercent = 0.27f, defaultYPercent = 0.24f),
            SensorCircle(id = 2, defaultXPercent = 0.47f, defaultYPercent = 0.26f),
            SensorCircle(id = 3, defaultXPercent = 0.71f, defaultYPercent = 0.39f),
            SensorCircle(id = 4, defaultXPercent = 0.65f, defaultYPercent = 0.59f),
            SensorCircle(id = 5, defaultXPercent = 0.58f, defaultYPercent = 0.83f),
            SensorCircle(id = 6, defaultXPercent = 0.44f, defaultYPercent = 0.91f)
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.pressure_plot_page),
            contentDescription = "Pressure Plot Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        IconButton(
            onClick = onBackClicked,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to Home",
                tint = Color.Black,
                modifier = Modifier.size(32.dp)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 56.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        var footMapContainerWidthPx by remember { mutableStateOf(0) }
        var footMapContainerHeightPx by remember { mutableStateOf(0) }
        val density = LocalDensity.current

        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .aspectRatio(
                    painterResource(id = R.drawable.footmapping).intrinsicSize.width /
                            painterResource(id = R.drawable.footmapping).intrinsicSize.height
                )
                .background(Color.Gray.copy(alpha = 0.1f))
                .onSizeChanged { size ->
                    footMapContainerWidthPx = size.width
                    footMapContainerHeightPx = size.height
                }
        ) {
            Image(
                painter = painterResource(id = R.drawable.footmapping),
                contentDescription = "Foot Mapping",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            if (footMapContainerWidthPx > 0 && footMapContainerHeightPx > 0) {
                sensorCirclesState.forEach { circleData ->
                    val minCircleSize = 18.dp
                    val maxCircleSize = 40.dp
                    val currentCircleSize = minCircleSize +
                            (maxCircleSize - minCircleSize) * circleData.pressureValue
                    val circleSizePx = with(density) { currentCircleSize.toPx() }

                    val circleColor = if (circleData.pressureValue > 0.1f) {
                        Color.Red.copy(
                            alpha = 0.5f + (circleData.pressureValue * 0.5f).coerceIn(0f, 0.5f)
                        )
                    } else {
                        Color.Blue.copy(alpha = 0.6f)
                    }

                    val offsetX = (circleData.defaultXPercent * footMapContainerWidthPx - circleSizePx / 2).roundToInt()
                    val offsetY = (circleData.defaultYPercent * footMapContainerHeightPx - circleSizePx / 2).roundToInt()

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(offsetX, offsetY) }
                            .size(currentCircleSize)
                            .clip(CircleShape)
                            .background(circleColor)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:shape=Normal,width=360,height=740,unit=dp,dpi=480")
@Composable
fun PressurePlotScreenPreview() {
    SmartSoleTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.DarkGray)
            )
            PressurePlotScreen(onBackClicked = {})
        }
    }
}
