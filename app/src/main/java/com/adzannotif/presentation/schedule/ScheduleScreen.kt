package com.adzannotif.presentation.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    widthSizeClass: WindowWidthSizeClass,
    viewModel: ScheduleViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val monthNames = listOf(
        "", "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

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
                        text = "Jadwal Sholat Bulanan",
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
        } else {
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
                                contentDescription = "Bulan Sebelumnya"
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${monthNames[state.currentMonth]} ${state.currentYear}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${state.location.name} • ${state.monthlyRecords.size} Hari",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                            )
                        }

                        IconButton(onClick = { viewModel.onNextMonth() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Bulan Berikutnya"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Table Header
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tgl", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
                        Text("Subuh", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text("Dzuhur", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text("Ashar", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text("Maghrib", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text("Isya", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Monthly Table Rows
                val tz = TimeZone.of(state.location.timeZoneId)
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(state.monthlyRecords) { record ->
                        val isToday = record.date == state.todayDate

                        val fSubuh = record.fajr.toLocalDateTime(tz)
                        val fDzuhur = record.dhuhr.toLocalDateTime(tz)
                        val fAshar = record.asr.toLocalDateTime(tz)
                        val fMaghrib = record.maghrib.toLocalDateTime(tz)
                        val fIsya = record.isha.toLocalDateTime(tz)

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isToday) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shadowElevation = if (isToday) 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${record.date.dayOfMonth}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.weight(0.8f),
                                    textAlign = TextAlign.Center,
                                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(String.format(Locale.US, "%02d:%02d", fSubuh.hour, fSubuh.minute), style = MaterialTheme.typography.bodyMedium, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text(String.format(Locale.US, "%02d:%02d", fDzuhur.hour, fDzuhur.minute), style = MaterialTheme.typography.bodyMedium, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text(String.format(Locale.US, "%02d:%02d", fAshar.hour, fAshar.minute), style = MaterialTheme.typography.bodyMedium, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text(String.format(Locale.US, "%02d:%02d", fMaghrib.hour, fMaghrib.minute), style = MaterialTheme.typography.bodyMedium, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text(String.format(Locale.US, "%02d:%02d", fIsya.hour, fIsya.minute), style = MaterialTheme.typography.bodyMedium, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
