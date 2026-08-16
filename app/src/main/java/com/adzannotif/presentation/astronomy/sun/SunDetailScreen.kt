package com.adzannotif.presentation.astronomy.sun

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
import com.adzannotif.core.astronomy.internal.SunMath
import com.adzannotif.domain.model.LocationInfo
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
                title = {
                    Column {
                        Text("Detail Matahari & Fotografi", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = AstronomyStarWhite)
                        Text("Elevasi Fisik & Hisab Cahaya", style = MaterialTheme.typography.bodySmall, color = AstronomyTwilightCivil)
                    }
                },
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
                val location = uiState.location ?: LocationInfo.JAKARTA

                item {
                    PhysicallyAccurateSunArc(
                        sunInfo = sunInfo,
                        location = location,
                        scrubbedTime = uiState.scrubbedTimeMillis,
                        onScrubTime = { viewModel.setScrubbedTime(it) }
                    )
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
fun PhysicallyAccurateSunArc(
    sunInfo: SunInfo?,
    location: LocationInfo,
    scrubbedTime: Long?,
    onScrubTime: (Long?) -> Unit
) {
    val currentTime = System.currentTimeMillis()
    val activeTime = scrubbedTime ?: currentTime

    // Compute active real solar coordinates
    val activePos = remember(activeTime, location) {
        SunMath.computeSunPosition(location.latitude, location.longitude, 0.0, activeTime)
    }

    val noonMillis = sunInfo?.noonMillis ?: currentTime
    val dayStart = noonMillis - 43200000L // 12 hours before noon
    val dayEnd = noonMillis + 43200000L   // 12 hours after noon

    // Sample 48 real points across 24h
    val sampledPoints = remember(location, noonMillis) {
        (0..48).map { step ->
            val t = dayStart + step * 1800000L // 30 min step
            val pos = SunMath.computeSunPosition(location.latitude, location.longitude, 0.0, t)
            t to pos.altitude
        }
    }

    val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())

    Card(
        colors = CardDefaults.cardColors(containerColor = AstronomySurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AstronomySunAmber.copy(alpha = 0.3f)),
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
                        "Busur Elevasi Matahari Fisik (24 Jam)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AstronomyStarWhite
                    )
                    Text(
                        "Waktu: ${fmt.format(Date(activeTime))} • Alt: ${String.format("%.1f°", activePos.altitude)} • Az: ${String.format("%.1f°", activePos.azimuth)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (activePos.altitude >= 0) AstronomySunAmber else AstronomyTwilightCivil
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(AstronomySunAmber.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.WbSunny,
                        contentDescription = null,
                        tint = if (activePos.altitude >= 0) AstronomySunAmber else AstronomyTwilightCivil,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val w = size.width
                val h = size.height
                val horizonY = h * 0.65f // Horizon baseline (0 deg altitude)
                val peakY = h * 0.10f    // Max zenith altitude (90 deg)

                // 1. Draw Horizon Line (0 deg)
                drawLine(
                    color = AstronomyHorizon.copy(alpha = 0.8f),
                    start = Offset(0f, horizonY),
                    end = Offset(w, horizonY),
                    strokeWidth = 2f
                )

                // 2. Draw Civil Twilight line (-6 deg)
                val civilTwilightY = horizonY + (horizonY - peakY) * (6f / 90f)
                drawLine(
                    color = AstronomyTwilightCivil.copy(alpha = 0.3f),
                    start = Offset(0f, civilTwilightY),
                    end = Offset(w, civilTwilightY),
                    strokeWidth = 1f
                )

                // 3. Draw Continuous Real Astronomical Curve
                val curvePath = Path()
                sampledPoints.forEachIndexed { index, (time, alt) ->
                    val fraction = index / 48f
                    val x = w * fraction
                    // Map altitude: alt = 90 -> peakY, alt = 0 -> horizonY, alt = -90 -> below
                    val y = horizonY - (horizonY - peakY) * (alt.toFloat() / 90f)
                    if (index == 0) {
                        curvePath.moveTo(x, y.coerceIn(0f, h))
                    } else {
                        curvePath.lineTo(x, y.coerceIn(0f, h))
                    }
                }

                drawPath(
                    path = curvePath,
                    color = AstronomySunAmber.copy(alpha = 0.85f),
                    style = Stroke(width = 3.5f)
                )

                // 4. Draw Current / Scrubbed Sun Marker
                val activeFraction = ((activeTime - dayStart).toFloat() / 86400000f).coerceIn(0f, 1f)
                val markerX = w * activeFraction
                val markerY = (horizonY - (horizonY - peakY) * (activePos.altitude.toFloat() / 90f)).coerceIn(0f, h)

                // Marker Glow
                drawCircle(
                    color = (if (activePos.altitude >= 0) AstronomySunAmber else AstronomyTwilightCivil).copy(alpha = 0.25f),
                    radius = 20f,
                    center = Offset(markerX, markerY)
                )
                drawCircle(
                    color = if (activePos.altitude >= 0) AstronomySunAmber else AstronomyTwilightCivil,
                    radius = 9f,
                    center = Offset(markerX, markerY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.5f,
                    center = Offset(markerX, markerY)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Time Scrubber Slider
            var sliderProgress by remember(activeTime) {
                mutableFloatStateOf(((activeTime - dayStart).toFloat() / 86400000f).coerceIn(0f, 1f))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Simulasi Waktu: ${fmt.format(Date(activeTime))}", color = AstronomyStarWhite, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                if (scrubbedTime != null) {
                    TextButton(onClick = { onScrubTime(null) }, contentPadding = PaddingValues(0.dp)) {
                        Text("Reset Sekarang", color = AstronomySunAmber, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Slider(
                value = sliderProgress,
                onValueChange = {
                    sliderProgress = it
                    val newTime = dayStart + (it * 86400000f).toLong()
                    onScrubTime(newTime)
                },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = AstronomySunAmber,
                    activeTrackColor = AstronomyGoldenHour,
                    inactiveTrackColor = AstronomySurface
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("00:00 (Malam)", style = MaterialTheme.typography.labelSmall, color = AstronomyTwilightCivil)
                Text("12:00 (Siang)", style = MaterialTheme.typography.labelSmall, color = AstronomySunAmber)
                Text("24:00 (Malam)", style = MaterialTheme.typography.labelSmall, color = AstronomyTwilightCivil)
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
                    "Golden Hour (-4° s/d +6°) menghasilkan bayangan lembut dan warna emas hangat. Blue Hour (-6° s/d -4°) memberikan kontras biru pekat dan gradasi langit senja/fajar yang kaya.",
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, color = AstronomyStarWhite, style = MaterialTheme.typography.bodyMedium)
                    Text(time, color = AstronomySunAmber, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                }
                HorizontalDivider(color = AstronomyConstellationLine.copy(alpha = 0.2f), thickness = 0.5.dp)
            }
        }
    }
}
