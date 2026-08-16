package com.adzannotif.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adzannotif.core.prayer.CalculationMethod
import com.adzannotif.core.prayer.HighLatitudeRule
import com.adzannotif.core.prayer.Madhab
import com.adzannotif.core.prayer.Prayer
import com.adzannotif.domain.model.AdhanVoice
import com.adzannotif.domain.model.AllAlarmSettings
import com.adzannotif.domain.model.LocationInfo
import com.adzannotif.domain.model.ThemeMode
import com.adzannotif.domain.model.UserSettings
import com.adzannotif.domain.repository.LocationRepository
import com.adzannotif.domain.repository.SettingsRepository
import com.adzannotif.domain.usecase.SchedulePrayerAlarmsUseCase
import com.adzannotif.platform.audio.AdhanAudioPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val userSettings: UserSettings = UserSettings(),
    val alarmSettings: AllAlarmSettings = AllAlarmSettings(),
    val allOfflineCities: List<LocationInfo> = emptyList(),
    val searchQuery: String = "",
    val filteredCities: List<LocationInfo> = emptyList(),
    val isCityPickerVisible: Boolean = false,
    val currentlyPlayingVoice: AdhanVoice? = null,
    val isLoading: Boolean = false,
)

sealed interface SettingsUiAction {
    data class SetCalculationMethod(val method: CalculationMethod) : SettingsUiAction
    data class SetMadhab(val madhab: Madhab) : SettingsUiAction
    data class SetHighLatitudeRule(val rule: HighLatitudeRule) : SettingsUiAction
    data class SetThemeMode(val themeMode: ThemeMode) : SettingsUiAction
    data class SetPrayerAdjustment(val prayer: Prayer, val minutes: Int) : SettingsUiAction
    data class SetAdhanVoice(val prayer: Prayer, val voice: AdhanVoice) : SettingsUiAction
    data class ToggleAdhanPreview(val voice: AdhanVoice) : SettingsUiAction
    data class SetPreReminder(val prayer: Prayer, val minutesBefore: Int) : SettingsUiAction
    data class SetDndSilenceMinutes(val minutes: Int) : SettingsUiAction
    data class SearchCity(val query: String) : SettingsUiAction
    data class SelectCity(val city: LocationInfo) : SettingsUiAction
    data class SetCityPickerVisible(val visible: Boolean) : SettingsUiAction
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val locationRepository: LocationRepository,
    private val schedulePrayerAlarmsUseCase: SchedulePrayerAlarmsUseCase,
    private val audioPlayer: AdhanAudioPlayer,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isCityPickerVisible = MutableStateFlow(false)
    private val _allCities = MutableStateFlow<List<LocationInfo>>(emptyList())
    private val _currentlyPlayingVoice = MutableStateFlow<AdhanVoice?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(settingsRepository.userSettings, settingsRepository.alarmSettings, ::Pair),
        combine(_allCities, _searchQuery, _isCityPickerVisible) { cities, query, isPickerOpen ->
            Triple(cities, query, isPickerOpen)
        },
        _currentlyPlayingVoice
    ) { (userSettings, alarmSettings), (cities, query, isPickerOpen), playingVoice ->
        val filtered = if (query.isEmpty()) {
            cities
        } else {
            cities.filter {
                it.name.contains(query, ignoreCase = true) || it.country.contains(query, ignoreCase = true)
            }
        }
        SettingsUiState(
            userSettings = userSettings,
            alarmSettings = alarmSettings,
            allOfflineCities = cities,
            searchQuery = query,
            filteredCities = filtered,
            isCityPickerVisible = isPickerOpen,
            currentlyPlayingVoice = playingVoice,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(isLoading = true)
    )

    init {
        loadCities()
    }

    private fun loadCities() {
        viewModelScope.launch {
            val list = locationRepository.getAllOfflineCities()
            _allCities.value = list
        }
    }

    fun onAction(action: SettingsUiAction) {
        when (action) {
            is SettingsUiAction.SetCalculationMethod -> {
                viewModelScope.launch {
                    settingsRepository.updateUserSettings { it.copy(calculationMethod = action.method) }
                    schedulePrayerAlarmsUseCase()
                }
            }
            is SettingsUiAction.SetMadhab -> {
                viewModelScope.launch {
                    settingsRepository.updateUserSettings { it.copy(madhab = action.madhab) }
                    schedulePrayerAlarmsUseCase()
                }
            }
            is SettingsUiAction.SetHighLatitudeRule -> {
                viewModelScope.launch {
                    settingsRepository.updateUserSettings { it.copy(highLatitudeRule = action.rule) }
                    schedulePrayerAlarmsUseCase()
                }
            }
            is SettingsUiAction.SetThemeMode -> {
                viewModelScope.launch {
                    settingsRepository.updateUserSettings { it.copy(themeMode = action.themeMode) }
                }
            }
            is SettingsUiAction.SetPrayerAdjustment -> {
                viewModelScope.launch {
                    settingsRepository.updateUserSettings { current ->
                        when (action.prayer) {
                            Prayer.FAJR -> current.copy(fajrAdjustment = action.minutes)
                            Prayer.DHUHR -> current.copy(dhuhrAdjustment = action.minutes)
                            Prayer.ASR -> current.copy(asrAdjustment = action.minutes)
                            Prayer.MAGHRIB -> current.copy(maghribAdjustment = action.minutes)
                            Prayer.ISHA -> current.copy(ishaAdjustment = action.minutes)
                            else -> current
                        }
                    }
                    schedulePrayerAlarmsUseCase()
                }
            }
            is SettingsUiAction.SetAdhanVoice -> {
                viewModelScope.launch {
                    val currentConfig = uiState.value.alarmSettings.getConfigForPrayer(action.prayer)
                    val updated = currentConfig.copy(adhanVoice = action.voice)
                    settingsRepository.updateAlarmSettings { it.updateConfig(updated) }
                }
            }
            is SettingsUiAction.ToggleAdhanPreview -> {
                if (_currentlyPlayingVoice.value == action.voice) {
                    audioPlayer.stop()
                    _currentlyPlayingVoice.value = null
                } else {
                    _currentlyPlayingVoice.value = action.voice
                    audioPlayer.playAdhan(
                        voice = action.voice,
                        durationMinutes = 1,
                        onCompletion = {
                            _currentlyPlayingVoice.value = null
                        }
                    )
                }
            }
            is SettingsUiAction.SetPreReminder -> {
                viewModelScope.launch {
                    val currentConfig = uiState.value.alarmSettings.getConfigForPrayer(action.prayer)
                    val updated = currentConfig.copy(preReminderMinutes = action.minutesBefore)
                    settingsRepository.updateAlarmSettings { it.updateConfig(updated) }
                    schedulePrayerAlarmsUseCase()
                }
            }
            is SettingsUiAction.SetDndSilenceMinutes -> {
                viewModelScope.launch {
                    settingsRepository.updateAlarmSettings { it.copy(dndAutoSilenceMinutes = action.minutes) }
                }
            }
            is SettingsUiAction.SearchCity -> {
                _searchQuery.value = action.query
            }
            is SettingsUiAction.SelectCity -> {
                viewModelScope.launch {
                    locationRepository.saveLocation(action.city)
                    settingsRepository.updateUserSettings {
                        it.copy(selectedLocation = action.city, useAutoLocation = false)
                    }
                    _isCityPickerVisible.value = false
                    schedulePrayerAlarmsUseCase()
                }
            }
            is SettingsUiAction.SetCityPickerVisible -> {
                _isCityPickerVisible.value = action.visible
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
    }
}
