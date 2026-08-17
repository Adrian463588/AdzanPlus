package com.adzannotif.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adzannotif.core.prayer.Prayer
import com.adzannotif.domain.model.AllAlarmSettings
import com.adzannotif.domain.model.LocationInfo
import com.adzannotif.domain.model.PrayerTimeRecord
import com.adzannotif.domain.model.UserSettings
import com.adzannotif.domain.repository.LocationRepository
import com.adzannotif.domain.repository.AstronomyRepository
import com.adzannotif.domain.repository.SettingsRepository
import com.adzannotif.domain.usecase.GetNextPrayerUseCase
import com.adzannotif.domain.usecase.GetTodayPrayerTimesUseCase
import com.adzannotif.domain.usecase.SchedulePrayerAlarmsUseCase
import com.adzannotif.platform.network.NetworkMonitor
import android.content.Context
import com.adzannotif.widget.PrayerTimesWidgetReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject

data class HomeUiState(
    val location: LocationInfo? = null,
    val prayerTimes: PrayerTimeRecord? = null,
    val nextPrayer: Prayer? = null,
    val nextPrayerTarget: Instant? = null,
    val currentPrayer: Prayer? = null,
    val countdownSeconds: Long = 0L,
    val hijriDateFormatted: String? = null,
    val alarmSettings: AllAlarmSettings = AllAlarmSettings(),
    val userSettings: UserSettings = UserSettings(),
    val isOnline: Boolean = false,
    val isRefreshingGps: Boolean = false,
    val locationError: String? = null,
    val isLoading: Boolean = false,
)

sealed interface HomeUiAction {
    data class TogglePrayerAlarm(val prayer: Prayer) : HomeUiAction
    data object RefreshLocation : HomeUiAction
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getTodayPrayerTimesUseCase: GetTodayPrayerTimesUseCase,
    private val getNextPrayerUseCase: GetNextPrayerUseCase,
    private val settingsRepository: SettingsRepository,
    private val astronomyRepository: AstronomyRepository,
    private val locationRepository: LocationRepository,
    private val schedulePrayerAlarmsUseCase: SchedulePrayerAlarmsUseCase,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val _countdownSeconds = MutableStateFlow(0L)
    private val _isRefreshingGps = MutableStateFlow(false)
    private val _nextPrayerTarget = MutableStateFlow<Instant?>(null)
    private val _hijriDateFormatted = MutableStateFlow<String?>(null)
    private val _locationError = MutableStateFlow<String?>(null)

    private val prayerInfoFlow = combine(
        locationRepository.currentOrSelectedLocation,
        getTodayPrayerTimesUseCase(),
        getNextPrayerUseCase(),
    ) { location, todayRecord, nextInfo ->
        val target = nextInfo?.targetTime
        _nextPrayerTarget.value = target
        updateCountdownNow(target)
        Triple(location, todayRecord, nextInfo)
    }

    private val settingsStateFlow = combine(
        settingsRepository.alarmSettings,
        settingsRepository.userSettings,
        _countdownSeconds,
        _hijriDateFormatted,
    ) { alarmSettings, userSettings, countdown, hijriDateFormatted ->
        HomeSettingsSnapshot(alarmSettings, userSettings, countdown, hijriDateFormatted)
    }

    private val connectivityFlow = combine(
        networkMonitor.isOnline,
        _isRefreshingGps,
        _locationError,
    ) { isOnline, isRefreshing, locationError ->
        HomeConnectivitySnapshot(isOnline, isRefreshing, locationError)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        prayerInfoFlow,
        settingsStateFlow,
        connectivityFlow
    ) { (location, todayRecord, nextInfo), settings, connectivity ->
        val current = nextInfo?.currentPrayer ?: todayRecord?.findCurrentPrayer(Clock.System.now())

        HomeUiState(
            location = location,
            prayerTimes = todayRecord,
            nextPrayer = nextInfo?.nextPrayer,
            nextPrayerTarget = nextInfo?.targetTime,
            currentPrayer = current,
            countdownSeconds = settings.countdown,
            hijriDateFormatted = settings.hijriDateFormatted,
            alarmSettings = settings.alarmSettings,
            userSettings = settings.userSettings,
            isOnline = connectivity.isOnline,
            isRefreshingGps = connectivity.isRefreshing,
            locationError = connectivity.locationError,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    init {
        startCountdownTicker()
        refreshHijriDate()
        scheduleInitialAlarms()
    }

    private fun refreshHijriDate() {
        viewModelScope.launch {
            val location = locationRepository.currentOrSelectedLocation.first() ?: run {
                _hijriDateFormatted.value = null
                return@launch
            }
            runCatching {
                astronomyRepository.getHijriDate(
                    gregorianEpochMillis = Clock.System.now().toEpochMilliseconds(),
                    timeZoneId = location.timeZoneId,
                )
            }.onSuccess { hijriDate ->
                _hijriDateFormatted.value =
                    "${hijriDate.day} ${hijriDate.monthName} ${hijriDate.year} H"
            }.onFailure {
                _hijriDateFormatted.value = null
            }
        }
    }

    private fun updateCountdownNow(target: Instant?) {
        if (target != null) {
            val now = Clock.System.now()
            val diff = (target.toEpochMilliseconds() - now.toEpochMilliseconds()) / 1000L
            _countdownSeconds.value = if (diff > 0) diff else 0L
        }
    }

    private fun startCountdownTicker() {
        viewModelScope.launch {
            while (isActive) {
                updateCountdownNow(_nextPrayerTarget.value)
                delay(1000L)
            }
        }
    }

    private fun scheduleInitialAlarms() {
        viewModelScope.launch {
            schedulePrayerAlarmsUseCase()
        }
    }

    fun onAction(action: HomeUiAction) {
        when (action) {
            is HomeUiAction.TogglePrayerAlarm -> {
                viewModelScope.launch {
                    val currentConfig = uiState.value.alarmSettings.getConfigForPrayer(action.prayer)
                    val updated = currentConfig.copy(isEnabled = !currentConfig.isEnabled)
                    settingsRepository.updateAlarmSettings { it.updateConfig(updated) }
                    schedulePrayerAlarmsUseCase()
                    PrayerTimesWidgetReceiver.updateAll(context)
                }
            }
            is HomeUiAction.RefreshLocation -> {
                viewModelScope.launch {
                    _isRefreshingGps.value = true
                    _locationError.value = null
                    try {
                        locationRepository.getDeviceLocation()
                            .onSuccess { loc ->
                                locationRepository.saveLocation(loc)
                                settingsRepository.updateUserSettings {
                                    it.copy(selectedLocation = loc, useAutoLocation = true)
                                }
                                schedulePrayerAlarmsUseCase()
                                PrayerTimesWidgetReceiver.updateAll(context)
                            }
                            .onFailure { error ->
                                _locationError.value = error.message
                                    ?.takeIf(String::isNotBlank)
                            }
                    } finally {
                        _isRefreshingGps.value = false
                    }
                }
            }
        }
    }

    private data class HomeSettingsSnapshot(
        val alarmSettings: AllAlarmSettings,
        val userSettings: UserSettings,
        val countdown: Long,
        val hijriDateFormatted: String?,
    )

    private data class HomeConnectivitySnapshot(
        val isOnline: Boolean,
        val isRefreshing: Boolean,
        val locationError: String?,
    )
}
