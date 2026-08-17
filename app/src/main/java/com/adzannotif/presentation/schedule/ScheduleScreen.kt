package com.adzannotif.presentation.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adzannotif.presentation.common.WindowWidthSizeClass
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import androidx.compose.ui.res.stringResource
import com.adzannotif.R
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    widthSizeClass: WindowWidthSizeClass,
    viewModel: ScheduleViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val monthYearText = remember(state.currentYear, state.currentMonth) {
        YearMonth.of(state.currentYear, state.currentMonth)
            .format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
    }

    LaunchedEffect(state.monthlyRecords) {
        val todayIndex = state.monthlyRecords.indexOfFirst { it.date == state.todayDate }
        if (todayIndex >= 0) {
            listState.animateScrollToItem(maxOf(0, todayIndex - 1))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.schedule_monthly_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (state.location == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Text(
                        text = stringResource(R.string.schedule_location_unavailable),
                        modifier = Modifier.padding(20.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            val location = checkNotNull(state.location)
            val daysCountText = stringResource(R.string.schedule_days_count, state.monthlyRecords.size)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = if (widthSizeClass == WindowWidthSizeClass.EXPANDED) 32.dp else 16.dp)
            ) {
                // Month Header Controller
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.onPreviousMonth() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.schedule_prev_month)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = monthYearText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${location.name} • $daysCountText",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                            )
                        }

                        IconButton(onClick = { viewModel.onNextMonth() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = stringResource(R.string.schedule_next_month)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val timeZoneId = location.timeZoneId
                if (state.monthlyRecords.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Text(
                            text = "Jadwal belum tersedia untuk bulan ini. Periksa lokasi dan data waktu matahari.",
                            modifier = Modifier.padding(20.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else if (widthSizeClass == WindowWidthSizeClass.COMPACT) {
                    CompactScheduleList(
                        records = state.monthlyRecords,
                        todayDate = state.todayDate,
                        timeZoneId = timeZoneId,
                        listState = listState,
                    )
                } else {
                    MonthlyScheduleTable(
                        records = state.monthlyRecords,
                        todayDate = state.todayDate,
                        timeZoneId = timeZoneId,
                        listState = listState,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactScheduleList(
    records: List<com.adzannotif.domain.model.PrayerTimeRecord>,
    todayDate: kotlinx.datetime.LocalDate,
    timeZoneId: String,
    listState: androidx.compose.foundation.lazy.LazyListState,
) {
    val timeZone = TimeZone.of(timeZoneId)
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
    ) {
        items(records) { record ->
            val isToday = record.date == todayDate
            val times = listOf(
                "Subuh" to record.fajr,
                "Dzuhur" to record.dhuhr,
                "Ashar" to record.asr,
                "Maghrib" to record.maghrib,
                "Isya" to record.isha,
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isToday) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    },
                ),
                elevation = CardDefaults.cardElevation(if (isToday) 2.dp else 0.dp),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (isToday) "${record.date.dayOfMonth} • Hari ini" else "${record.date.dayOfMonth}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    times.chunked(2).forEach { rowTimes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rowTimes.forEach { (label, instant) ->
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    val local = instant.toLocalDateTime(timeZone)
                                    Text(
                                        text = String.format(Locale.US, "%02d:%02d", local.hour, local.minute),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                            if (rowTimes.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyScheduleTable(
    records: List<com.adzannotif.domain.model.PrayerTimeRecord>,
    todayDate: kotlinx.datetime.LocalDate,
    timeZoneId: String,
    listState: androidx.compose.foundation.lazy.LazyListState,
) {
    val timeZone = TimeZone.of(timeZoneId)
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TableHeaderCell("Tgl", 0.8f)
            TableHeaderCell("Subuh", 1f)
            TableHeaderCell("Dzuhur", 1f)
            TableHeaderCell("Ashar", 1f)
            TableHeaderCell("Maghrib", 1f)
            TableHeaderCell("Isya", 1f)
        }
    }

    Spacer(modifier = Modifier.height(6.dp))
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
    ) {
        items(records) { record ->
            val isToday = record.date == todayDate
            val localTimes = listOf(record.fajr, record.dhuhr, record.asr, record.maghrib, record.isha)
                .map { it.toLocalDateTime(timeZone) }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = if (isToday) 2.dp else 0.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TableBodyCell("${record.date.dayOfMonth}", 0.8f, isToday)
                    localTimes.forEach { local ->
                        TableBodyCell(String.format(Locale.US, "%02d:%02d", local.hour, local.minute), 1f, isToday)
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.TableHeaderCell(text: String, weight: Float) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.weight(weight),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun RowScope.TableBodyCell(text: String, weight: Float, isToday: Boolean) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier.weight(weight),
        textAlign = TextAlign.Center,
        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
    )
}
