package com.adzannotif.presentation.home

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adzannotif.core.prayer.Prayer
import com.adzannotif.presentation.common.WindowWidthSizeClass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    widthSizeClass: WindowWidthSizeClass,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AdzanNotif",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = state.location.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onAction(HomeUiAction.RefreshLocation) }) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Perbarui Lokasi GPS",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (state.isLoading || state.prayerTimes == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            val record = state.prayerTimes!!
            val displayPrayers = listOf(
                Prayer.IMSAK to record.imsak,
                Prayer.FAJR to record.fajr,
                Prayer.SUNRISE to record.sunrise,
                Prayer.DHUHR to record.dhuhr,
                Prayer.ASR to record.asr,
                Prayer.MAGHRIB to record.maghrib,
                Prayer.ISHA to record.isha,
            )

            if (widthSizeClass == WindowWidthSizeClass.EXPANDED) {
                // Dual pane layout for Large Tablets / Landscape
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        NextPrayerHeroCard(
                            nextPrayer = state.nextPrayer,
                            targetInstant = state.nextPrayerTarget ?: record.fajr,
                            countdownSecondsRemaining = state.countdownSeconds,
                            location = state.location,
                            hijriDateFormatted = state.hijriDateFormatted,
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1.2f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(displayPrayers) { (prayer, instant) ->
                            PrayerTimeRow(
                                prayer = prayer,
                                timeInstant = instant,
                                timeZoneId = state.location.timeZoneId,
                                isActivePrayer = state.currentPrayer == prayer,
                                alarmConfig = state.alarmSettings.getConfigForPrayer(prayer),
                                onToggleAlarm = { viewModel.onAction(HomeUiAction.TogglePrayerAlarm(prayer)) }
                            )
                        }
                    }
                }
            } else {
                // Compact / Medium Layout (Stacked)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        NextPrayerHeroCard(
                            nextPrayer = state.nextPrayer,
                            targetInstant = state.nextPrayerTarget ?: record.fajr,
                            countdownSecondsRemaining = state.countdownSeconds,
                            location = state.location,
                            hijriDateFormatted = state.hijriDateFormatted,
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Jadwal Sholat Hari Ini",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    items(displayPrayers) { (prayer, instant) ->
                        PrayerTimeRow(
                            prayer = prayer,
                            timeInstant = instant,
                            timeZoneId = state.location.timeZoneId,
                            isActivePrayer = state.currentPrayer == prayer,
                            alarmConfig = state.alarmSettings.getConfigForPrayer(prayer),
                            onToggleAlarm = { viewModel.onAction(HomeUiAction.TogglePrayerAlarm(prayer)) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
