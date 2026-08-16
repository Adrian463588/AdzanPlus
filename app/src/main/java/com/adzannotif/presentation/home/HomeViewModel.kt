package com.adzannotif.presentation.home

import com.adzannotif.core.prayer.Prayer
import com.adzannotif.domain.model.AllAlarmSettings
import com.adzannotif.domain.model.LocationInfo
import com.adzannotif.domain.model.PrayerTimeRecord
import com.adzannotif.domain.model.UserSettings
import com.adzannotif.domain.repository.LocationRepository
import com.adzannotif.domain.repository.SettingsRepository
import com.adzannotif.domain.usecase.GetNextPrayerUseCase
import com.adzannotif.domain.usecase.GetTodayPrayerTimesUseCase
import com.adzannotif.domain.usecase.SchedulePrayerAlarmsUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject

data class HomeUiState(
    val location: LocationInfo = LocationInfo.JAKARTA,
    val prayerTimes: PrayerTimeRecord? = null,
    val nextPrayer: Prayer = Prayer.FAJR,
    val nextPrayerTarget: Instant? = null,
    val currentPrayer: Prayer? = null,
    val countdownSeconds: Long = 0L,
    val hijriDateFormatted: String = "1 Safar 1448 H",
    val alarmSettings: AllAlarmSettings = AllAlarmSettings(),
    val userSettings: UserSettings = UserSettings(),
    val isLoading: Boolean = false,
)

sealed interface HomeUiAction {
    data class TogglePrayerAlarm(val prayer: Prayer) : HomeUiAction
    data object RefreshLocation : HomeUiAction
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTodayPrayerTimesUseCase: GetTodayPrayerTimesUseCase,
    private val getNextPrayerUseCase: GetNextPrayerUseCase,
    private val settingsRepository: SettingsRepository,
    private val locationRepository: LocationRepository,
    private val schedulePrayerAlarmsUseCase: SchedulePrayerAlarmsUseCase,
) : ViewModel() {

    private val _countdownSeconds = MutableStateFlow(0L)

    private val prayerInfoFlow = combine(
        locationRepository.currentOrSelectedLocation,
        getTodayPrayerTimesUseCase(),
        getNextPrayerUseCase(),
    ) { location, todayRecord, nextInfo ->
        Triple(location, todayRecord, nextInfo)
    }

    private val settingsStateFlow = combine(
        settingsRepository.alarmSettings,
        settingsRepository.userSettings,
        _countdownSeconds,
    ) { alarmSettings, userSettings, countdown ->
        Triple(alarmSettings, userSettings, countdown)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        prayerInfoFlow,
        settingsStateFlow
    ) { (location, todayRecord, nextInfo), (alarmSettings, userSettings, countdown) ->
        val nextPrayer = nextInfo?.nextPrayer ?: Prayer.FAJR
        val nextTarget = nextInfo?.targetTime ?: todayRecord.fajr
        val current = nextInfo?.currentPrayer ?: todayRecord.findCurrentPrayer(Clock.System.now())

        HomeUiState(
            location = location,
            prayerTimes = todayRecord,
            nextPrayer = nextPrayer,
            nextPrayerTarget = nextTarget,
            currentPrayer = current,
            countdownSeconds = countdown,
            hijriDateFormatted = "29 Shafar 1448 H",
            alarmSettings = alarmSettings,
            userSettings = userSettings,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    init {
        startCountdownTicker()
        scheduleInitialAlarms()
    }

    private fun startCountdownTicker() {
        viewModelScope.launch {
            while (isActive) {
                val state = uiState.value
                val target = state.nextPrayerTarget
                if (target != null) {
                    val now = Clock.System.now()
                    val diff = (target.toEpochMilliseconds() - now.toEpochMilliseconds()) / 1000L
                    _countdownSeconds.value = if (diff > 0) diff else 0L
                }
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
                }
            }
            is HomeUiAction.RefreshLocation -> {
                viewModelScope.launch {
                    locationRepository.getDeviceLocation().onSuccess { loc ->
                        locationRepository.saveLocation(loc)
                        settingsRepository.updateUserSettings { it.copy(selectedLocation = loc) }
                        schedulePrayerAlarmsUseCase()
                    }
                }
            }
        }
    }
}
