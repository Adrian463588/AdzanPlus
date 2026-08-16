package com.adzannotif.presentation.astronomy.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.adzannotif.domain.model.astronomy.CalendarDay
import com.adzannotif.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HijriCalendarScreen(
    navController: NavController,
    viewModel: HijriCalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val monthNamesIndo = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    Scaffold(
        containerColor = AstronomyBackgroundDeep,
        topBar = {
            TopAppBar(
                title = { Text("Kalender Hijriah & Masehi", color = AstronomyStarWhite) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AstronomyBackgroundDeep)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.previousMonth() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, tint = AstronomyStarWhite, contentDescription = "Bulan Sebelumnya")
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val mName = if (uiState.month in 0..11) monthNamesIndo[uiState.month] else ""
                    Text("$mName ${uiState.year}", color = AstronomyStarWhite, style = MaterialTheme.typography.titleLarge)
                    val hijriSummary = uiState.days.firstOrNull()?.let { "${it.hijriMonthName} ${it.hijriYear} H" } ?: "Kalender Hijriah"
                    Text(hijriSummary, color = AstronomyGoldenHour, style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = { viewModel.nextMonth() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, tint = AstronomyStarWhite, contentDescription = "Bulan Berikutnya")
                }
            }

            // Weekday labels
            val weekdays = listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                weekdays.forEach {
                    Text(
                        text = it,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = AstronomyStarWhite,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AstronomyStarWhite)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(uiState.days) { day ->
                        CalendarCell(day = day, onClick = { viewModel.selectDay(day) })
                    }
                }
            }
        }

        // Selected Day Bottom Sheet
        if (uiState.selectedDay != null) {
            val day = uiState.selectedDay!!
            val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
            ModalBottomSheet(
                onDismissRequest = { viewModel.dismissDay() },
                containerColor = AstronomySurface
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        "${day.gregorianDay} ${if (day.gregorianMonth in 1..12) monthNamesIndo[day.gregorianMonth - 1] else ""} ${day.gregorianYear}",
                        style = MaterialTheme.typography.titleLarge,
                        color = AstronomyStarWhite
                    )
                    Text(
                        "${day.hijriDay} ${day.hijriMonthName} ${day.hijriYear} H",
                        style = MaterialTheme.typography.titleMedium,
                        color = AstronomyGoldenHour
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Fase Bulan: ${day.moonPhaseName} (Iluminasi: ${String.format("%.1f%%", day.moonIlluminationPercent)})", color = AstronomyStarWhite, style = MaterialTheme.typography.bodyMedium)
                    if (day.sunriseMillis != null) {
                        Text("Matahari Terbit: ${fmt.format(Date(day.sunriseMillis))}", color = AstronomyStarWhite, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (day.sunsetMillis != null) {
                        Text("Matahari Terbenam: ${fmt.format(Date(day.sunsetMillis))}", color = AstronomyStarWhite, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (day.goldenHourMorningStartMillis != null) {
                        Text("Golden Hour Pagi: ${fmt.format(Date(day.goldenHourMorningStartMillis))}", color = AstronomyGoldenHour, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun CalendarCell(day: CalendarDay, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clickable(onClick = onClick)
            .border(
                width = if (day.goldenHourMorningStartMillis != null) 1.5.dp else 0.dp,
                color = if (day.goldenHourMorningStartMillis != null) AstronomyGoldenHour.copy(alpha = 0.6f) else Color.Transparent
            )
            .background(if (day.isToday) MaterialTheme.colorScheme.primaryContainer else AstronomySurface),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${day.gregorianDay}",
                color = if (day.isToday) MaterialTheme.colorScheme.onPrimaryContainer else AstronomyStarWhite,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "${day.hijriDay}",
                color = AstronomyTwilightCivil,
                style = MaterialTheme.typography.labelSmall
            )
        }

        // Moon Phase icon indicator
        if (day.isNewMoon || day.isFullMoon) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(6.dp)
                    .background(AstronomyMoonGold, CircleShape)
            )
        }

        // Prayer dots
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            repeat(5) {
                Box(modifier = Modifier.size(2.dp).background(AstronomyStarWhite.copy(alpha = 0.7f), CircleShape))
            }
        }
    }
}
