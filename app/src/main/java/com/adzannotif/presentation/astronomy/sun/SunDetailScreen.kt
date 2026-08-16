package com.adzannotif.presentation.astronomy.sun

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.adzannotif.domain.model.astronomy.SunInfo
import com.adzannotif.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SunDetailScreen(
    navController: NavController,
    viewModel: SunDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = AstronomyBackgroundDeep,
        topBar = {
            TopAppBar(
                title = { Text("Detail Matahari & Fotografi", color = AstronomyStarWhite) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = AstronomyStarWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AstronomyBackgroundDeep)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AstronomyBackgroundDeep),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AstronomySunAmber)
                    }
                }
            } else if (uiState.error != null) {
                item { Text(text = uiState.error!!, color = MaterialTheme.colorScheme.error) }
            } else {
                val sunInfo = uiState.sunInfo

                item {
                    SunArcVisualization(sunInfo)
                }
                item {
                    GoldenBlueHourDetailCards(sunInfo)
                }
                item {
                    PhotographyTipsCard(sunInfo)
                }
                item {
                    TwilightBandTimeline()
                }
                item {
                    KeyTimesList(sunInfo)
                }
            }
        }
    }
}

@Composable
fun SunArcVisualization(sunInfo: SunInfo?) {
    var selectedHourFraction by remember { mutableFloatStateOf(0.5f) } // 0.0 to 1.0 (0h to 24h)

    val altitude = sunInfo?.altitude ?: 0.0
    val azimuth = sunInfo?.azimuth ?: 0.0

    Card(
        colors = CardDefaults.cardColors(containerColor = AstronomySurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Busur Elevasi Matahari",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AstronomyStarWhite
                    )
                    Text(
                        "Posisi saat ini: Alt ${String.format("%.1f°", altitude)}, Az ${String.format("%.1f°", azimuth)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AstronomySunAmber
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(AstronomySunAmber.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.WbSunny, contentDescription = null, tint = AstronomySunAmber, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                val baselineY = size.height * 0.75f
                val w = size.width

                // Horizon line
                drawLine(
                    color = AstronomyHorizon.copy(alpha = 0.7f),
                    start = Offset(0f, baselineY),
                    end = Offset(w, baselineY),
                    strokeWidth = 2f
                )

                // Sun Arc Parabola
                val path = Path().apply {
                    moveTo(w * 0.1f, baselineY)
                    quadraticTo(w * 0.5f, size.height * 0.1f, w * 0.9f, baselineY)
                }

                drawPath(
                    path = path,
                    color = AstronomySunAmber.copy(alpha = 0.6f),
                    style = Stroke(width = 3f)
                )

                // Current Sun Dot position
                val normAlt = (altitude.coerceIn(-10.0, 90.0) / 90.0).toFloat()
                val dotY = (baselineY - (baselineY - size.height * 0.1f) * normAlt).coerceIn(size.height * 0.05f, baselineY + 20f)
                val dotX = w * 0.5f // Solar noon centered

                // Glow circle
                drawCircle(color = AstronomySunAmber.copy(alpha = 0.25f), radius = 18f, center = Offset(dotX, dotY))
                drawCircle(color = AstronomySunAmber, radius = 8f, center = Offset(dotX, dotY))
                drawCircle(color = Color.White, radius = 3f, center = Offset(dotX, dotY))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Timur (Terbit)", style = MaterialTheme.typography.labelSmall, color = AstronomyTwilightCivil)
                Text("Zenith (Puncak)", style = MaterialTheme.typography.labelSmall, color = AstronomySunAmber)
                Text("Barat (Terbenam)", style = MaterialTheme.typography.labelSmall, color = AstronomyTwilightCivil)
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

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = AstronomySurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, AstronomyGoldenHour.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(AstronomyGoldenHour, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Golden Hour", color = AstronomyGoldenHour, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Pagi: ${formatWindow(sunInfo?.morningGoldenHourStartMillis, sunInfo?.morningGoldenHourEndMillis)}", color = AstronomyStarWhite, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Sore: ${formatWindow(sunInfo?.eveningGoldenHourStartMillis, sunInfo?.eveningGoldenHourEndMillis)}", color = AstronomyStarWhite, style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = AstronomySurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, AstronomyBlueHour.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(AstronomyBlueHour, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Blue Hour", color = AstronomyBlueHour, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Pagi: ${formatWindow(sunInfo?.morningBlueHourStartMillis, sunInfo?.morningBlueHourEndMillis)}", color = AstronomyStarWhite, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Sore: ${formatWindow(sunInfo?.eveningBlueHourStartMillis, sunInfo?.eveningBlueHourEndMillis)}", color = AstronomyStarWhite, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun PhotographyTipsCard(sunInfo: SunInfo?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AstronomySurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Filled.CameraAlt,
                contentDescription = null,
                tint = AstronomyGoldenHour,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    "Panduan Fotografi & Pencahayaan",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = AstronomyStarWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Golden Hour menghasilkan bayangan lembut dan warna emas hangat, sangat ideal untuk portrait dan landscape. Blue Hour memberikan kontras dramatis antara langit biru pekat dan lampu kota.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AstronomyTwilightCivil
                )
            }
        }
    }
}

@Composable
fun TwilightBandTimeline() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Pita Gradasi Fajar & Senja", color = AstronomyStarWhite, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(8.dp))
        Canvas(modifier = Modifier.fillMaxWidth().height(28.dp)) {
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
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Jadwal Waktu Matahari & Senja", color = AstronomyStarWhite, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(12.dp))
            val times = listOf(
                "Fajar Astronomis (-18°)" to formatTime(sunInfo?.astronomicalDawnMillis),
                "Fajar Nautikal (-12°)" to formatTime(sunInfo?.nauticalDawnMillis),
                "Fajar Sipil (-6°)" to formatTime(sunInfo?.civilDawnMillis),
                "Matahari Terbit (Sunrise)" to formatTime(sunInfo?.riseMillis),
                "Tengah Hari (Solar Noon)" to formatTime(sunInfo?.noonMillis),
                "Matahari Terbenam (Sunset)" to formatTime(sunInfo?.setMillis),
                "Senja Sipil (-6°)" to formatTime(sunInfo?.civilDuskMillis),
                "Senja Nautikal (-12°)" to formatTime(sunInfo?.nauticalDuskMillis),
                "Senja Astronomis (-18°)" to formatTime(sunInfo?.astronomicalDuskMillis)
            )
            times.forEach { (label, time) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, color = AstronomyStarWhite, style = MaterialTheme.typography.bodyMedium)
                    Text(time, color = AstronomySunAmber, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                }
                HorizontalDivider(color = AstronomySurface.copy(alpha = 0.5f), thickness = 0.5.dp)
            }
        }
    }
}
