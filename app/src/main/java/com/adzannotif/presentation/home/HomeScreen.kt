package com.adzannotif.presentation.home

import android.provider.Settings as AndroidSettings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adzannotif.R
import com.adzannotif.core.prayer.Prayer
import com.adzannotif.domain.model.AllAlarmSettings
import com.adzannotif.domain.model.LocationInfo
import com.adzannotif.domain.model.PrayerTimeRecord
import com.adzannotif.presentation.common.WindowWidthSizeClass
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    widthSizeClass: WindowWidthSizeClass,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val motionEnabled = rememberMotionAnimationsEnabled()
    val refreshRotation by animateFloatAsState(
        targetValue = if (state.isRefreshingGps && motionEnabled) 360f else 0f,
        animationSpec = if (motionEnabled) tween(durationMillis = 450) else snap(),
        label = "location_refresh_rotation",
    )
    val locationLabel = state.location?.let { location ->
        if (location.isAutoDetected) {
            stringResource(R.string.location_gps_auto, location.name)
        } else {
            location.name
        }
    } ?: stringResource(R.string.location_unavailable)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = locationLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (state.isOnline) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = if (state.isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (state.isOnline) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                            Text(
                                text = stringResource(
                                    if (state.isOnline) R.string.status_online else R.string.status_offline,
                                ),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.onAction(HomeUiAction.RefreshLocation) },
                        enabled = !state.isRefreshingGps,
                    ) {
                        Icon(
                            imageVector = if (state.isRefreshingGps) {
                                Icons.Default.Refresh
                            } else {
                                Icons.Default.MyLocation
                            },
                            contentDescription = stringResource(R.string.refresh_location),
                            modifier = Modifier.rotate(refreshRotation),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        val location = state.location
        val prayerTimes = state.prayerTimes
        val nextPrayer = state.nextPrayer
        val nextPrayerTarget = state.nextPrayerTarget

        when {
            state.isLoading -> LoadingState(modifier = Modifier.padding(innerPadding))
            location == null || prayerTimes == null || nextPrayer == null || nextPrayerTarget == null -> {
                UnavailableState(
                    modifier = Modifier.padding(innerPadding),
                    onRetry = { viewModel.onAction(HomeUiAction.RefreshLocation) },
                )
            }
            else -> {
                HomeContent(
                    widthSizeClass = widthSizeClass,
                    innerPadding = innerPadding,
                    location = location,
                    prayerTimes = prayerTimes,
                    nextPrayer = nextPrayer,
                    nextPrayerTarget = nextPrayerTarget,
                    countdownSeconds = state.countdownSeconds,
                    hijriDateFormatted = state.hijriDateFormatted,
                    currentPrayer = state.currentPrayer,
                    alarmSettings = state.alarmSettings,
                    locationError = state.locationError,
                    onToggleAlarm = { prayer ->
                        viewModel.onAction(HomeUiAction.TogglePrayerAlarm(prayer))
                    },
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    widthSizeClass: WindowWidthSizeClass,
    innerPadding: PaddingValues,
    location: LocationInfo,
    prayerTimes: PrayerTimeRecord,
    nextPrayer: Prayer,
    nextPrayerTarget: kotlinx.datetime.Instant,
    countdownSeconds: Long,
    hijriDateFormatted: String?,
    currentPrayer: Prayer?,
    alarmSettings: AllAlarmSettings,
    locationError: String?,
    onToggleAlarm: (Prayer) -> Unit,
) {
    val displayPrayers = listOf(
        Prayer.IMSAK to prayerTimes.imsak,
        Prayer.FAJR to prayerTimes.fajr,
        Prayer.SUNRISE to prayerTimes.sunrise,
        Prayer.DHUHR to prayerTimes.dhuhr,
        Prayer.ASR to prayerTimes.asr,
        Prayer.MAGHRIB to prayerTimes.maghrib,
        Prayer.ISHA to prayerTimes.isha,
    )

    if (widthSizeClass == WindowWidthSizeClass.COMPACT) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            locationError?.let { message ->
                item { LocationErrorCard(message = message) }
            }
            item {
                NextPrayerHeroCard(
                    nextPrayer = nextPrayer,
                    targetInstant = nextPrayerTarget,
                    countdownSecondsRemaining = countdownSeconds,
                    location = location,
                    hijriDateFormatted = hijriDateFormatted,
                )
            }
            item {
                Text(
                    text = stringResource(R.string.prayer_schedule_today),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            items(displayPrayers, key = { it.first.name }) { (prayer, instant) ->
                PrayerTimeRow(
                    prayer = prayer,
                    timeInstant = instant,
                    timeZoneId = location.timeZoneId,
                    isActivePrayer = currentPrayer == prayer,
                    alarmConfig = alarmSettings.getConfigForPrayer(prayer),
                    onToggleAlarm = { onToggleAlarm(prayer) },
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = if (widthSizeClass == WindowWidthSizeClass.EXPANDED) 24.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            locationError?.let { message ->
                LocationErrorCard(message = message)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .weight(if (widthSizeClass == WindowWidthSizeClass.EXPANDED) 0.9f else 1f)
                        .fillMaxHeight(),
                ) {
                    NextPrayerHeroCard(
                        nextPrayer = nextPrayer,
                        targetInstant = nextPrayerTarget,
                        countdownSecondsRemaining = countdownSeconds,
                        location = location,
                        hijriDateFormatted = hijriDateFormatted,
                    )
                }
                PrayerScheduleList(
                    modifier = Modifier
                        .weight(if (widthSizeClass == WindowWidthSizeClass.EXPANDED) 1.2f else 1f)
                        .fillMaxHeight(),
                    displayPrayers = displayPrayers,
                    timeZoneId = location.timeZoneId,
                    currentPrayer = currentPrayer,
                    alarmSettings = alarmSettings,
                    onToggleAlarm = onToggleAlarm,
                )
            }
        }
    }
}

@Composable
private fun PrayerScheduleList(
    modifier: Modifier,
    displayPrayers: List<Pair<Prayer, kotlinx.datetime.Instant>>,
    timeZoneId: String,
    currentPrayer: Prayer?,
    alarmSettings: AllAlarmSettings,
    onToggleAlarm: (Prayer) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.prayer_schedule_today),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        items(displayPrayers, key = { it.first.name }) { (prayer, instant) ->
            PrayerTimeRow(
                prayer = prayer,
                timeInstant = instant,
                timeZoneId = timeZoneId,
                isActivePrayer = currentPrayer == prayer,
                alarmConfig = alarmSettings.getConfigForPrayer(prayer),
                onToggleAlarm = { onToggleAlarm(prayer) },
            )
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

@Composable
private fun UnavailableState(
    modifier: Modifier = Modifier,
    onRetry: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
                Text(
                    text = stringResource(R.string.prayer_data_unavailable),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.prayer_data_unavailable_detail),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onRetry) {
                    Text(text = stringResource(R.string.retry))
                }
            }
        }
    }
}

@Composable
private fun LocationErrorCard(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun rememberMotionAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            AndroidSettings.Global.getFloat(
                context.contentResolver,
                AndroidSettings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) > 0f
        }.getOrDefault(true)
    }
}
