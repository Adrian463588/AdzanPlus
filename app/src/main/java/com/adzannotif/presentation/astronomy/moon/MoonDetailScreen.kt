package com.adzannotif.presentation.astronomy.moon

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.adzannotif.domain.model.astronomy.CalendarDay
import com.adzannotif.domain.model.astronomy.MoonInfo
import com.adzannotif.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoonDetailScreen(
    navController: NavController,
    viewModel: MoonDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedDayIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = AstronomyBackgroundDeep,
        topBar = {
            TopAppBar(
                title = {
                    val loc = uiState.location
                    Column {
                        Text("Detail & Fase Bulan", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = AstronomyStarWhite)
                        Text(
                    text = if (loc != null) "📍 ${loc.name} (${String.format("%.2f°", loc.latitude)}, ${String.format("%.2f°", loc.longitude)})" else "Fase, Iluminasi & Orbit",
                            style = MaterialTheme.typography.bodySmall,
                            color = AstronomyTwilightCivil
                        )
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
                        CircularProgressIndicator(color = AstronomyMoonGold)
                    }
                }
            } else if (uiState.error != null) {
                item { Text(text = uiState.error!!, color = MaterialTheme.colorScheme.error) }
            } else {
                val moonInfo = uiState.moonInfo

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        MoonPhaseHeroIllustration(
                            phaseOrdinal = moonInfo?.phaseOrdinal ?: 0,
                            phaseName = moonInfo?.phaseName ?: "Bulan",
                            modifier = Modifier.size(160.dp)
                        )
                    }
                }

                item {
                    PhaseInfoRow(moonInfo)
                }

                item {
                    RiseSetTransitCard(moonInfo)
                }

                item {
                    DistanceCard(moonInfo)
                }

                item {
                    InteractiveMiniPhaseCalendar(
                        calendarDays = uiState.calendarDays,
                        selectedIndex = selectedDayIndex,
                        onSelectDay = { selectedDayIndex = it }
                    )
                }
            }
        }
    }
}

@Composable
fun MoonPhaseHeroIllustration(phaseOrdinal: Int, phaseName: String, modifier: Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier) {
            val radius = size.minDimension / 2.2f
            val center = Offset(size.width / 2, size.height / 2)

            // Outer subtle glow
            drawCircle(
                color = AstronomyMoonGold.copy(alpha = 0.15f),
                radius = radius * 1.25f,
                center = center
            )
            // Dark base disk
            drawCircle(AstronomySurface, radius, center)
            drawCircle(AstronomyConstellationLine.copy(alpha = 0.3f), radius, center, style = Stroke(1.5f))

            // Phase illumination geometry
            when (phaseOrdinal) {
                0 -> { /* New moon dark disk */ }
                1 -> { drawCrescentPath(center, radius, isWaxing = true) }
                2 -> { drawHalfDisk(center, radius, isRight = true) }
                3 -> { drawGibbousPath(center, radius, isWaxing = true) }
                4 -> { drawCircle(AstronomyMoonGold, radius, center) }
                5 -> { drawGibbousPath(center, radius, isWaxing = false) }
                6 -> { drawHalfDisk(center, radius, isRight = false) }
                7 -> { drawCrescentPath(center, radius, isWaxing = false) }
                else -> { drawCircle(AstronomyMoonGold, radius, center) }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            phaseName,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = AstronomyMoonGold
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCrescentPath(
    center: Offset,
    radius: Float,
    isWaxing: Boolean
) {
    drawArc(
        color = AstronomyMoonGold,
        startAngle = if (isWaxing) -90f else 90f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2)
    )
    // Dark inner cutout
    val innerWidth = radius * 1.2f
    drawArc(
        color = AstronomySurface,
        startAngle = if (isWaxing) -90f else 90f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(center.x - (if (isWaxing) radius * 0.4f else radius * 0.8f), center.y - radius),
        size = Size(innerWidth, radius * 2)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHalfDisk(
    center: Offset,
    radius: Float,
    isRight: Boolean
) {
    drawArc(
        color = AstronomyMoonGold,
        startAngle = if (isRight) -90f else 90f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGibbousPath(
    center: Offset,
    radius: Float,
    isWaxing: Boolean
) {
    drawCircle(AstronomyMoonGold, radius, center)
    val cutWidth = radius * 0.6f
    drawArc(
        color = AstronomySurface,
        startAngle = if (isWaxing) 90f else -90f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(cutWidth, radius * 2)
    )
}

@Composable
fun PhaseInfoRow(moonInfo: MoonInfo?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AstronomySurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Fase", color = AstronomyTwilightCivil, style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    moonInfo?.phaseName ?: "-",
                    color = AstronomyMoonGold,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Column {
                Text("Iluminasi", color = AstronomyTwilightCivil, style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    moonInfo?.let { String.format("%.1f%%", it.illuminationPercent) } ?: "-",
                    color = AstronomyStarWhite,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Column {
                Text("Umur Bulan", color = AstronomyTwilightCivil, style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    moonInfo?.let { String.format("%.1f hari", it.ageInDays) } ?: "-",
                    color = AstronomyStarWhite,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun RiseSetTransitCard(moonInfo: MoonInfo?) {
    val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    Card(
        colors = CardDefaults.cardColors(containerColor = AstronomySurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Jadwal Bulan Hari Ini", color = AstronomyStarWhite, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Bulan Terbit (Moonrise)", color = AstronomyStarWhite, style = MaterialTheme.typography.bodyMedium)
                Text(moonInfo?.riseMillis?.let { fmt.format(Date(it)) } ?: "--:--", color = AstronomyMoonGold, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Transit (Titik Tertinggi)", color = AstronomyStarWhite, style = MaterialTheme.typography.bodyMedium)
                Text(moonInfo?.transitMillis?.let { fmt.format(Date(it)) } ?: "--:--", color = AstronomyStarWhite, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Bulan Terbenam (Moonset)", color = AstronomyStarWhite, style = MaterialTheme.typography.bodyMedium)
                Text(moonInfo?.setMillis?.let { fmt.format(Date(it)) } ?: "--:--", color = AstronomyStarWhite, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            }
        }
    }
}

@Composable
fun DistanceCard(moonInfo: MoonInfo?) {
    val dist = moonInfo?.distanceKm ?: 0.0
    val distText = if (dist > 0) String.format("%,.0f km", dist) else "-"
    val status = when {
        moonInfo?.isPerigee == true -> "Perigee (Terdekat ke Bumi)"
        moonInfo?.isApogee == true -> "Apogee (Terjauh dari Bumi)"
        else -> "Orbit Rata-Rata"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = AstronomySurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Jarak & Posisi Orbit", color = AstronomyStarWhite, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(distText, color = AstronomyMoonGold, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                Surface(
                    color = AstronomyBackgroundMid,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AstronomyConstellationLine.copy(alpha = 0.4f))
                ) {
                    Text(status, color = AstronomyTwilightCivil, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun InteractiveMiniPhaseCalendar(
    calendarDays: List<CalendarDay>,
    selectedIndex: Int,
    onSelectDay: (Int) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AstronomySurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Siklus Fase 30 Hari", color = AstronomyStarWhite, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text("Ketuk tanggal untuk melihat rincian fase", color = AstronomyTwilightCivil, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(12.dp))

            val displayDays = if (calendarDays.isNotEmpty()) calendarDays else (0..29).map {
                CalendarDay(0, it + 1, 8, 2026, it + 1, 2, 1448, "Safar", null, "Fase", it % 8, (it * 3.3).coerceAtMost(100.0), it == 0, it == 14, null, null, null)
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(displayDays) { index, day ->
                    val isSelected = index == selectedIndex
                    val emojis = listOf("🌑", "🌒", "🌓", "🌔", "🌕", "🌖", "🌗", "🌘")
                    val icon = emojis[day.moonPhaseOrdinal.coerceIn(0, 7)]

                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) AstronomyMoonGold.copy(alpha = 0.25f) else AstronomyBackgroundMid)
                            .border(
                                width = if (isSelected) 1.5.dp else 0.dp,
                                color = if (isSelected) AstronomyMoonGold else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectDay(index) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(icon, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Tgl ${day.gregorianDay}", color = if (isSelected) AstronomyMoonGold else AstronomyStarWhite, style = MaterialTheme.typography.labelSmall)
                        Text(String.format("%.0f%%", day.moonIlluminationPercent), color = AstronomyTwilightCivil, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
