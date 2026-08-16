package com.adzannotif.presentation.astronomy.moon

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.adzannotif.domain.model.astronomy.CalendarDay
import com.adzannotif.domain.model.astronomy.MoonInfo
import com.adzannotif.presentation.theme.*

@Composable
fun MoonDetailScreen(
    navController: NavController,
    viewModel: MoonDetailViewModel = hiltViewModel()
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
            Text("Detail Bulan", style = MaterialTheme.typography.headlineMedium, color = AstronomyStarWhite)
        }

        if (uiState.isLoading) {
            item { CircularProgressIndicator(color = AstronomyStarWhite) }
        } else if (uiState.error != null) {
            item { Text(text = uiState.error!!, color = MaterialTheme.colorScheme.error) }
        } else {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    MoonPhaseIllustration(
                        phaseOrdinal = uiState.moonInfo?.phaseOrdinal ?: 0,
                        modifier = Modifier.size(120.dp)
                    )
                }
            }
            item {
                PhaseInfoRow(uiState.moonInfo)
            }
            item {
                RiseSetTransitCard(uiState.moonInfo)
            }
            item {
                DistanceCard(uiState.moonInfo)
            }
            item {
                MiniPhaseCalendar(uiState.calendarDays)
            }
        }
    }
}

@Composable
fun MoonPhaseIllustration(phaseOrdinal: Int, modifier: Modifier) {
    Canvas(modifier) {
        val radius = size.minDimension / 2
        val center = Offset(size.width / 2, size.height / 2)
        // Draw full disk (dark background)
        drawCircle(AstronomyBackgroundDeep, radius, center)

        // Basic phase rendering
        when (phaseOrdinal) {
            0 -> { /* New Moon */ }
            1 -> { drawCrescent(center, radius, isWaxing = true) }
            2 -> { drawHalf(center, radius, isRight = true) }
            3 -> { drawGibbous(center, radius, isWaxing = true) }
            4 -> { drawCircle(AstronomyMoonGold, radius, center) }
            5 -> { drawGibbous(center, radius, isWaxing = false) }
            6 -> { drawHalf(center, radius, isRight = false) }
            7 -> { drawCrescent(center, radius, isWaxing = false) }
        }
    }
}

// Drawing helpers using simplified paths for moon phases
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCrescent(center: Offset, radius: Float, isWaxing: Boolean) {
    val path = Path().apply {
        addOval(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
    }
    // Simplification for representation
    drawPath(path, AstronomyMoonGold, style = Fill)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHalf(center: Offset, radius: Float, isRight: Boolean) {
    drawArc(
        color = AstronomyMoonGold,
        startAngle = if (isRight) -90f else 90f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGibbous(center: Offset, radius: Float, isWaxing: Boolean) {
    drawCircle(AstronomyMoonGold, radius, center)
}


@Composable
fun PhaseInfoRow(moonInfo: MoonInfo?) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text("Fase", color = AstronomyStarWhite, style = MaterialTheme.typography.labelMedium)
            Text(
                moonInfo?.phaseName ?: "-",
                color = AstronomyMoonGold,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Column {
            Text("Iluminasi", color = AstronomyStarWhite, style = MaterialTheme.typography.labelMedium)
            Text(
                moonInfo?.let { "%.1f%%".format(it.illuminationPercent) } ?: "-",
                color = AstronomyStarWhite,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Column {
            Text("Umur", color = AstronomyStarWhite, style = MaterialTheme.typography.labelMedium)
            Text(
                moonInfo?.let { "%.1f hari".format(it.ageInDays) } ?: "-",
                color = AstronomyStarWhite,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun RiseSetTransitCard(moonInfo: MoonInfo?) {
    val fmt = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    Card(colors = CardDefaults.cardColors(containerColor = AstronomySurface), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Waktu", color = AstronomyStarWhite, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Terbit", color = AstronomyStarWhite)
                Text(moonInfo?.riseMillis?.let { fmt.format(java.util.Date(it)) } ?: "--:--", color = AstronomyStarWhite)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Transit", color = AstronomyStarWhite)
                Text(moonInfo?.transitMillis?.let { fmt.format(java.util.Date(it)) } ?: "--:--", color = AstronomyStarWhite)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Terbenam", color = AstronomyStarWhite)
                Text(moonInfo?.setMillis?.let { fmt.format(java.util.Date(it)) } ?: "--:--", color = AstronomyStarWhite)
            }
        }
    }
}

@Composable
fun DistanceCard(moonInfo: MoonInfo?) {
    val distText = moonInfo?.let { "%.0f km".format(it.distanceKm).let { d ->
        val perigee = if (it.isPerigee) " (Perigee)" else ""
        val apogee = if (it.isApogee) " (Apogee)" else ""
        d + perigee + apogee
    } } ?: "-"
    Card(colors = CardDefaults.cardColors(containerColor = AstronomySurface), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Jarak ke Bumi", color = AstronomyStarWhite, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(distText, color = AstronomyMoonGold, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun MiniPhaseCalendar(days: List<CalendarDay>) {
    Column {
        Text("30 Hari Kedepan", color = AstronomyStarWhite, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(30) { dayIndex ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MoonPhaseIllustration(phaseOrdinal = dayIndex % 8, modifier = Modifier.size(24.dp))
                    Text("${dayIndex + 1}", color = AstronomyStarWhite, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
