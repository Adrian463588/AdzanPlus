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
import com.adzannotif.platform.network.NetworkMonitor
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
    val favoriteLocations: List<LocationInfo> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<LocationInfo> = emptyList(),
    val isLocationPickerVisible: Boolean = false,
    val isSearching: Boolean = false,
    val isRefreshingGps: Boolean = false,
    val isOnline: Boolean = true,
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
    data class SearchLocation(val query: String) : SettingsUiAction
    data class SelectLocation(val location: LocationInfo) : SettingsUiAction
    data class DeleteSavedLocation(val locationId: String) : SettingsUiAction
    data class SaveCustomCoordinates(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val elevation: Double = 0.0,
        val timeZoneId: String = "Asia/Jakarta"
    ) : SettingsUiAction
    data object RefreshGpsLocation : SettingsUiAction
    data class SetLocationPickerVisible(val visible: Boolean) : SettingsUiAction
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val locationRepository: LocationRepository,
    private val schedulePrayerAlarmsUseCase: SchedulePrayerAlarmsUseCase,
    private val audioPlayer: AdhanAudioPlayer,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isLocationPickerVisible = MutableStateFlow(false)
    private val _allOfflineCities = MutableStateFlow<List<LocationInfo>>(emptyList())
    private val _searchResults = MutableStateFlow<List<LocationInfo>>(emptyList())
    private val _isSearching = MutableStateFlow(false)
    private val _isRefreshingGps = MutableStateFlow(false)
    private val _currentlyPlayingVoice = MutableStateFlow<AdhanVoice?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(settingsRepository.userSettings, settingsRepository.alarmSettings, ::Pair),
        combine(
            _allOfflineCities,
            locationRepository.favoriteLocations,
            _searchQuery,
            _searchResults,
            _isLocationPickerVisible
        ) { cities, favorites, query, results, isPickerOpen ->
            LocationStateTuple(cities, favorites, query, results, isPickerOpen)
        },
        combine(_isSearching, _isRefreshingGps, networkMonitor.isOnline, _currentlyPlayingVoice) { isSearching, isRefreshing, isOnline, voice ->
            ExtraStateTuple(isSearching, isRefreshing, isOnline, voice)
        }
    ) { (userSettings, alarmSettings), locTuple, extraTuple ->
        SettingsUiState(
            userSettings = userSettings,
            alarmSettings = alarmSettings,
            allOfflineCities = locTuple.cities,
            favoriteLocations = locTuple.favorites,
            searchQuery = locTuple.query,
            searchResults = if (locTuple.query.isEmpty()) locTuple.cities else locTuple.results,
            isLocationPickerVisible = locTuple.isPickerOpen,
            isSearching = extraTuple.isSearching,
            isRefreshingGps = extraTuple.isRefreshing,
            isOnline = extraTuple.isOnline,
            currentlyPlayingVoice = extraTuple.voice,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(isLoading = true)
    )

    init {
        loadOfflineCities()
    }

    private fun loadOfflineCities() {
        viewModelScope.launch {
            val list = locationRepository.getAllOfflineCities()
            _allOfflineCities.value = list
            _searchResults.value = list
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
            is SettingsUiAction.SearchLocation -> {
                _searchQuery.value = action.query
                viewModelScope.launch {
                    _isSearching.value = true
                    try {
                        val results = locationRepository.searchLocations(action.query)
                        _searchResults.value = results
                    } finally {
                        _isSearching.value = false
                    }
                }
            }
            is SettingsUiAction.SelectLocation -> {
                viewModelScope.launch {
                    locationRepository.saveLocation(action.location)
                    settingsRepository.updateUserSettings {
                        it.copy(selectedLocation = action.location, useAutoLocation = action.location.isAutoDetected)
                    }
                    _isLocationPickerVisible.value = false
                    schedulePrayerAlarmsUseCase()
                }
            }
            is SettingsUiAction.DeleteSavedLocation -> {
                viewModelScope.launch {
                    locationRepository.deleteLocation(action.locationId)
                }
            }
            is SettingsUiAction.SaveCustomCoordinates -> {
                viewModelScope.launch {
                    val customLoc = LocationInfo(
                        id = "custom_${action.latitude.hashCode()}_${action.longitude.hashCode()}",
                        name = action.name.ifBlank { "Koordinat Kustom" },
                        country = "Kustom",
                        latitude = action.latitude,
                        longitude = action.longitude,
                        elevation = action.elevation,
                        timeZoneId = action.timeZoneId,
                        isAutoDetected = false
                    )
                    locationRepository.saveLocation(customLoc)
                    settingsRepository.updateUserSettings {
                        it.copy(selectedLocation = customLoc, useAutoLocation = false)
                    }
                    _isLocationPickerVisible.value = false
                    schedulePrayerAlarmsUseCase()
                }
            }
            is SettingsUiAction.RefreshGpsLocation -> {
                viewModelScope.launch {
                    _isRefreshingGps.value = true
                    try {
                        locationRepository.getDeviceLocation().onSuccess { loc ->
                            locationRepository.saveLocation(loc)
                            settingsRepository.updateUserSettings {
                                it.copy(selectedLocation = loc, useAutoLocation = true)
                            }
                            _isLocationPickerVisible.value = false
                            schedulePrayerAlarmsUseCase()
                        }
                    } finally {
                        _isRefreshingGps.value = false
                    }
                }
            }
            is SettingsUiAction.SetLocationPickerVisible -> {
                _isLocationPickerVisible.value = action.visible
                if (action.visible && _searchQuery.value.isNotEmpty()) {
                    _searchQuery.value = ""
                    _searchResults.value = _allOfflineCities.value
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
    }

    private data class LocationStateTuple(
        val cities: List<LocationInfo>,
        val favorites: List<LocationInfo>,
        val query: String,
        val results: List<LocationInfo>,
        val isPickerOpen: Boolean,
    )

    private data class ExtraStateTuple(
        val isSearching: Boolean,
        val isRefreshing: Boolean,
        val isOnline: Boolean,
        val voice: AdhanVoice?,
    )
}
