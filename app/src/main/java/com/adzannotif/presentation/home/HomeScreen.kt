package com.adzannotif.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val infiniteTransition = rememberInfiniteTransition(label = "gps_refresh")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AdzanPlus",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Online/Offline live badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (state.isOnline) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(
                                                color = if (state.isOnline) Color(0xFF10B981) else Color(0xFF9CA3AF),
                                                shape = CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (state.isOnline) "Online" else "Offline",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.Medium,
                                        color = if (state.isOnline) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (state.location.isAutoDetected) "${state.location.name} (GPS Otomatis)" else state.location.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.onAction(HomeUiAction.RefreshLocation) },
                        enabled = !state.isRefreshingGps
                    ) {
                        Icon(
                            imageVector = if (state.isRefreshingGps) Icons.Default.Refresh else Icons.Default.MyLocation,
                            contentDescription = "Perbarui Lokasi GPS",
                            modifier = if (state.isRefreshingGps) Modifier.rotate(rotationAngle) else Modifier,
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Jadwal Sholat Hari Ini",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Akurasi Astronomis",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
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
