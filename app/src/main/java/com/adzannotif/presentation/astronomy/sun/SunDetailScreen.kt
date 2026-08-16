package com.adzannotif.presentation.astronomy.sun

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.adzannotif.domain.model.astronomy.SunInfo
import com.adzannotif.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SunDetailScreen(
    navController: NavController,
    viewModel: SunDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AstronomyBackgroundDeep),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Detail Matahari", style = MaterialTheme.typography.headlineMedium, color = AstronomyStarWhite)
        }

        if (uiState.isLoading) {
            item { CircularProgressIndicator(color = AstronomyStarWhite) }
        } else if (uiState.error != null) {
            item { Text(text = uiState.error!!, color = MaterialTheme.colorScheme.error) }
        } else {
            val sunInfo = uiState.sunInfo
            item {
                SunArcVisualization(sunInfo)
            }
            item {
                TwilightBandTimeline()
            }
            item {
                KeyTimesList(sunInfo)
            }
            item {
                GoldenBlueHourDetailCards(sunInfo)
            }
        }
    }
}

@Composable
fun SunArcVisualization(sunInfo: SunInfo?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AstronomySurface),
        modifier = Modifier.fillMaxWidth().height(200.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val baselineY = size.height * 0.8f
            drawLine(
                color = AstronomyHorizon,
                start = Offset(0f, baselineY),
                end = Offset(size.width, baselineY),
                strokeWidth = 2f
            )

            val path = Path().apply {
                moveTo(0f, baselineY)
                quadraticTo(size.width / 2, -size.height * 0.3f, size.width, baselineY)
            }

            drawPath(
                path = path,
                color = AstronomySunAmber,
                style = Stroke(width = 4f)
            )

            // Current position dot if altitude is positive, or below horizon if negative
            val altitude = sunInfo?.altitude ?: 0.0
            val normalizedAlt = (altitude.coerceIn(-30.0, 90.0) / 90.0).toFloat()
            val dotY = baselineY - (baselineY * normalizedAlt * 0.9f)
            val dotX = size.width / 2 // Centered or mapped to azimuth

            drawCircle(
                color = AstronomyGoldenHour,
                radius = 8f,
                center = Offset(dotX, dotY)
            )
        }
    }
}

@Composable
fun TwilightBandTimeline() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Pita Senja & Fajar", color = AstronomyStarWhite, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Canvas(modifier = Modifier.fillMaxWidth().height(24.dp)) {
            val w = size.width
            val h = size.height
            drawRect(AstronomyTwilightAstro, Offset(0f, 0f), Size(w * 0.1f, h))
            drawRect(AstronomyTwilightNautical, Offset(w * 0.1f, 0f), Size(w * 0.1f, h))
            drawRect(AstronomyBlueHour, Offset(w * 0.2f, 0f), Size(w * 0.1f, h))
            drawRect(AstronomyGoldenHour, Offset(w * 0.3f, 0f), Size(w * 0.1f, h))
            drawRect(AstronomySunAmber, Offset(w * 0.4f, 0f), Size(w * 0.2f, h))
            drawRect(AstronomyGoldenHour, Offset(w * 0.6f, 0f), Size(w * 0.1f, h))
            drawRect(AstronomyBlueHour, Offset(w * 0.7f, 0f), Size(w * 0.1f, h))
            drawRect(AstronomyTwilightNautical, Offset(w * 0.8f, 0f), Size(w * 0.1f, h))
            drawRect(AstronomyTwilightAstro, Offset(w * 0.9f, 0f), Size(w * 0.1f, h))
        }
    }
}

@Composable
fun KeyTimesList(sunInfo: SunInfo?) {
    val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    fun formatTime(ms: Long?): String = ms?.let { fmt.format(Date(it)) } ?: "--:--"

    Card(
        colors = CardDefaults.cardColors(containerColor = AstronomySurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Waktu Penting", color = AstronomyStarWhite, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            val times = listOf(
                "Fajar Astronomis" to formatTime(sunInfo?.astronomicalDawnMillis),
                "Fajar Nautikal" to formatTime(sunInfo?.nauticalDawnMillis),
                "Fajar Sipil" to formatTime(sunInfo?.civilDawnMillis),
                "Matahari Terbit" to formatTime(sunInfo?.riseMillis),
                "Tengah Hari (Puncak)" to formatTime(sunInfo?.noonMillis),
                "Matahari Terbenam" to formatTime(sunInfo?.setMillis),
                "Senja Sipil" to formatTime(sunInfo?.civilDuskMillis),
                "Senja Nautikal" to formatTime(sunInfo?.nauticalDuskMillis),
                "Senja Astronomis" to formatTime(sunInfo?.astronomicalDuskMillis)
            )
            times.forEach { (label, time) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, color = AstronomyStarWhite)
                    Text(time, color = AstronomyStarWhite)
                }
            }
        }
    }
}

@Composable
fun GoldenBlueHourDetailCards(sunInfo: SunInfo?) {
    val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    fun formatWindow(start: Long?, end: Long?): String {
        return if (start != null && end != null) {
            "${fmt.format(Date(start))} - ${fmt.format(Date(end))}"
        } else {
            "--:-- - --:--"
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = AstronomySurface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Golden Hour", color = AstronomyGoldenHour, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Pagi: ${formatWindow(sunInfo?.morningGoldenHourStartMillis, sunInfo?.morningGoldenHourEndMillis)}", color = AstronomyStarWhite, style = MaterialTheme.typography.bodySmall)
                Text("Sore: ${formatWindow(sunInfo?.eveningGoldenHourStartMillis, sunInfo?.eveningGoldenHourEndMillis)}", color = AstronomyStarWhite, style = MaterialTheme.typography.bodySmall)
            }
        }
        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = AstronomySurface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Blue Hour", color = AstronomyBlueHour, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Pagi: ${formatWindow(sunInfo?.morningBlueHourStartMillis, sunInfo?.morningBlueHourEndMillis)}", color = AstronomyStarWhite, style = MaterialTheme.typography.bodySmall)
                Text("Sore: ${formatWindow(sunInfo?.eveningBlueHourStartMillis, sunInfo?.eveningBlueHourEndMillis)}", color = AstronomyStarWhite, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
