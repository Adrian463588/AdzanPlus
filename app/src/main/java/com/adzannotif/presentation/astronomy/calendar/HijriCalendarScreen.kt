package com.adzannotif.presentation.astronomy.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.adzannotif.domain.model.astronomy.CalendarDay
import com.adzannotif.presentation.theme.*
import com.adzannotif.presentation.common.WindowWidthSizeClass
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HijriCalendarScreen(
    navController: NavController,
    widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.COMPACT,
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
                title = {
                    val loc = uiState.location
                    Column {
                        Text("Kalender Hijriah & Masehi", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = AstronomyStarWhite)
                        Text(
                    text = if (loc != null) "📍 ${loc.name} (${String.format(Locale.ROOT, "%.2f°", loc.latitude)}, ${String.format(Locale.ROOT, "%.2f°", loc.longitude)})" else "Waktu Shalat & Fase Bulan Harian",
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
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(AstronomyBackgroundDeep)
        ) {
            // Month Header Selector Card
            Card(
                colors = CardDefaults.cardColors(containerColor = AstronomySurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (widthSizeClass == WindowWidthSizeClass.COMPACT) 16.dp else 24.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.previousMonth() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, tint = AstronomyStarWhite, contentDescription = "Bulan Sebelumnya")
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val mName = if (uiState.month in 0..11) monthNamesIndo[uiState.month] else ""
                        Text(
                            "$mName ${uiState.year}",
                            color = AstronomyStarWhite,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        val hijriSummary = uiState.days.firstOrNull()?.let { "${it.hijriMonthName} ${it.hijriYear} H" } ?: "Kalender Hijriah"
                        Text(hijriSummary, color = AstronomyGoldenHour, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    }
                    IconButton(onClick = { viewModel.nextMonth() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, tint = AstronomyStarWhite, contentDescription = "Bulan Berikutnya")
                    }
                }
            }

            // Weekday labels header
            val weekdays = listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                weekdays.forEach {
                    Text(
                        text = it,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = AstronomyTwilightCivil,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AstronomyMoonGold)
                }
            } else if (uiState.error != null) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = uiState.error ?: "Kalender belum tersedia.",
                        color = AstronomyTwilightCivil,
                        textAlign = TextAlign.Center,
                    )
                    Button(onClick = viewModel::retry) { Text("Coba lagi") }
                }
            } else if (uiState.days.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Kalender belum tersedia untuk lokasi ini.",
                        color = AstronomyTwilightCivil,
                        textAlign = TextAlign.Center,
                    )
                    Button(onClick = viewModel::retry) { Text("Coba lagi") }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
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
            val fmt = SimpleDateFormat("HH:mm", Locale.ROOT).apply {
                timeZone = TimeZone.getTimeZone(uiState.location?.timeZoneId ?: TimeZone.getDefault().id)
            }
            ModalBottomSheet(
                onDismissRequest = { viewModel.dismissDay() },
                containerColor = AstronomySurface
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "${day.gregorianDay} ${if (day.gregorianMonth in 1..12) monthNamesIndo[day.gregorianMonth - 1] else ""} ${day.gregorianYear}",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = AstronomyStarWhite
                            )
                            Text(
                                "${day.hijriDay} ${day.hijriMonthName} ${day.hijriYear} H",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = AstronomyGoldenHour
                            )
                        }
                        Surface(
                            color = AstronomyBackgroundMid,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                day.moonPhaseName,
                                color = AstronomyMoonGold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = AstronomyConstellationLine.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Informasi Astronomis & Waktu",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AstronomyStarWhite
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (day.sunriseMillis != null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Matahari Terbit", color = AstronomyTwilightCivil)
                            Text(fmt.format(Date(day.sunriseMillis)), color = AstronomyStarWhite, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    if (day.sunsetMillis != null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Matahari Terbenam", color = AstronomyTwilightCivil)
                            Text(fmt.format(Date(day.sunsetMillis)), color = AstronomyStarWhite, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    if (day.goldenHourMorningStartMillis != null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Golden Hour Pagi", color = AstronomyGoldenHour)
                            Text(fmt.format(Date(day.goldenHourMorningStartMillis)), color = AstronomyGoldenHour, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    day.prayerTimes?.let { prayers ->
                        val prayerRows = listOf(
                            "Subuh" to prayers.fajr,
                            "Terbit" to prayers.sunrise,
                            "Dzuhur" to prayers.dhuhr,
                            "Ashar" to prayers.asr,
                            "Maghrib" to prayers.maghrib,
                            "Isya" to prayers.isha,
                        )
                        prayerRows.forEach { (label, instant) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(label, color = AstronomyTwilightCivil)
                                Text(
                                    fmt.format(Date(instant.epochSeconds * 1000L)),
                                    color = AstronomyStarWhite,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Iluminasi Bulan", color = AstronomyTwilightCivil)
                Text(String.format(Locale.ROOT, "%.1f%%", day.moonIlluminationPercent), color = AstronomyMoonGold, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(28.dp))
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
            .padding(1.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (day.isToday) 1.5.dp else if (day.goldenHourMorningStartMillis != null) 1.dp else 0.dp,
                color = if (day.isToday) AstronomyMoonGold else if (day.goldenHourMorningStartMillis != null) AstronomyGoldenHour.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .background(if (day.isToday) AstronomyMoonGold.copy(alpha = 0.25f) else AstronomySurface),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${day.gregorianDay}",
                color = if (day.isToday) AstronomyMoonGold else AstronomyStarWhite,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                "${day.hijriDay}",
                color = if (day.isToday) AstronomyStarWhite else AstronomyTwilightCivil,
                style = MaterialTheme.typography.labelSmall
            )
        }

        // Moon Phase icon indicator
        if (day.isNewMoon || day.isFullMoon) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(6.dp)
                    .background(AstronomyMoonGold, CircleShape)
            )
        }

        // Prayer indicators are shown only when this day has a real schedule.
        if (day.prayerTimes != null) {
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                repeat(5) {
                    Box(
                        modifier = Modifier
                            .size(2.5.dp)
                            .background(
                                if (day.isToday) MaterialTheme.colorScheme.primary else AstronomyTwilightCivil.copy(alpha = 0.7f),
                                CircleShape
                            )
                    )
                }
            }
        }
    }
}
