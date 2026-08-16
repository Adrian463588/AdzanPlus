package com.adzannotif.presentation.astronomy.starmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.adzannotif.domain.model.astronomy.StarMapData
import com.adzannotif.presentation.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun StarMapScreen(
    navController: NavController,
    viewModel: StarMapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AstronomyBackgroundDeep)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            SkyChart(data = uiState.starMapData, modifier = Modifier.fillMaxSize())

            // Legend Chip
            Surface(
                color = AstronomySurface.copy(alpha = 0.8f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
            ) {
                Text(
                    "Skala Magnitudo • Putar/Geser Peta",
                    color = AstronomyStarWhite,
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        TimeSlider(
            currentMillis = uiState.observedMillis,
            onTimeChanged = { viewModel.setObservedTime(it) },
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun SkyChart(data: StarMapData?, modifier: Modifier) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Canvas(
        modifier = modifier
            .background(AstronomyBackgroundDeep)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 4f)
                    offset += pan
                }
            }
    ) {
        val center = Offset(size.width / 2 + offset.x, size.height / 2 + offset.y)
        val chartRadius = minOf(size.width, size.height) / 2 * scale

        // Draw horizon circle and compass marks
        drawCircle(AstronomyHorizon.copy(alpha = 0.4f), chartRadius, center, style = Stroke(2f))

        fun project(azimuthDeg: Double, altitudeDeg: Double): Offset? {
            if (altitudeDeg < -5.0) return null
            val r = chartRadius * (1.0 - (altitudeDeg.coerceIn(0.0, 90.0) / 90.0))
            val angle = Math.toRadians(azimuthDeg - 90.0)
            val x = center.x + (r * cos(angle)).toFloat()
            val y = center.y + (r * sin(angle)).toFloat()
            return Offset(x, y)
        }

        if (data != null) {
            val starPositions = mutableMapOf<Int, Offset>()

            // 1. Draw Constellation Lines
            data.constellations.forEach { constellation ->
                constellation.lines.forEach { (fromHip, toHip) ->
                    val fromPos = data.visibleStars.find { it.hipId == fromHip }?.let { project(it.azimuth, it.altitude) }
                    val toPos = data.visibleStars.find { it.hipId == toHip }?.let { project(it.azimuth, it.altitude) }
                    if (fromPos != null && toPos != null) {
                        drawLine(
                            color = AstronomyConstellationLine.copy(alpha = 0.5f),
                            start = fromPos,
                            end = toPos,
                            strokeWidth = 1.5f
                        )
                    }
                }
            }

            // 2. Draw Stars
            data.visibleStars.forEach { star ->
                val pos = project(star.azimuth, star.altitude)
                if (pos != null) {
                    starPositions[star.hipId] = pos
                    val starRadius = (4.5 - star.magnitude).coerceIn(1.0, 6.0).toFloat()
                    val starAlpha = (1.0 - (star.magnitude / 5.0)).coerceIn(0.3, 1.0).toFloat()
                    drawCircle(
                        color = AstronomyStarWhite.copy(alpha = starAlpha),
                        radius = starRadius,
                        center = pos
                    )
                }
            }

            // 3. Draw Sun Marker if above horizon
            project(data.sunAzimuth, data.sunAltitude)?.let { sunPos ->
                drawCircle(color = AstronomySunAmber, radius = 9f, center = sunPos)
                drawCircle(color = AstronomyGoldenHour.copy(alpha = 0.4f), radius = 16f, center = sunPos)
            }

            // 4. Draw Moon Marker if above horizon
            project(data.moonAzimuth, data.moonAltitude)?.let { moonPos ->
                drawCircle(color = AstronomyMoonGold, radius = 7f, center = moonPos)
            }
        }
    }
}

@Composable
fun TimeSlider(currentMillis: Long, onTimeChanged: (Long) -> Unit, modifier: Modifier) {
    var sliderValue by remember { mutableFloatStateOf(0f) }

    Column(modifier = modifier) {
        val sign = if (sliderValue > 0) "+" else ""
        Text("Geser Waktu: Sekarang $sign${sliderValue.toInt()} Jam", color = AstronomyStarWhite, style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = sliderValue,
            onValueChange = {
                sliderValue = it
            },
            onValueChangeFinished = {
                val newMillis = System.currentTimeMillis() + (sliderValue * 60 * 60 * 1000L).toLong()
                onTimeChanged(newMillis)
            },
            valueRange = -12f..12f,
            colors = SliderDefaults.colors(
                thumbColor = AstronomySunAmber,
                activeTrackColor = AstronomyGoldenHour,
                inactiveTrackColor = AstronomySurface
            )
        )
    }
}
