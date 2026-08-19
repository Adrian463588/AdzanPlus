package com.adzannotif.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adzannotif.R
import com.adzannotif.core.prayer.CalculationMethod
import com.adzannotif.core.prayer.HighLatitudeRule
import com.adzannotif.core.prayer.Madhab
import com.adzannotif.core.prayer.Prayer
import com.adzannotif.domain.model.AdhanSoundType
import com.adzannotif.domain.model.AdhanVoice
import com.adzannotif.domain.model.AllAlarmSettings
import com.adzannotif.domain.model.CelestialAlertType
import com.adzannotif.domain.model.LocationInfo
import com.adzannotif.domain.model.ThemeMode
import com.adzannotif.domain.model.UserSettings
import com.adzannotif.domain.repository.LocationRepository
import com.adzannotif.domain.repository.SettingsRepository
import com.adzannotif.platform.alarm.AlarmScheduler
import com.adzannotif.platform.alarm.CelestialAlarmScheduler
import com.adzannotif.platform.audio.AudioGateway
import com.adzannotif.platform.network.NetworkMonitor
import android.content.Context
import com.adzannotif.widget.AstronomyWidgetUpdater
import com.adzannotif.presentation.widget.PrayerTimesWidgetReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SettingsDataState {
    LOADING,
    READY,
    UNAVAILABLE,
    ERROR,
}

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
    val isOnline: Boolean = false,
    val currentlyPlayingVoice: AdhanVoice? = null,
    val isLoading: Boolean = false,
    val dataState: SettingsDataState = SettingsDataState.LOADING,
    val errorMessage: String? = null,
)

sealed interface SettingsUiAction {
    data class SetCalculationMethod(val method: CalculationMethod) : SettingsUiAction
    data class SetMadhab(val madhab: Madhab) : SettingsUiAction
    data class SetHighLatitudeRule(val rule: HighLatitudeRule) : SettingsUiAction
    data class SetThemeMode(val themeMode: ThemeMode) : SettingsUiAction
    data class SetPrayerAdjustment(val prayer: Prayer, val minutes: Int) : SettingsUiAction
    data class SetPrayerEnabled(val prayer: Prayer, val enabled: Boolean) : SettingsUiAction
    data class SetSoundType(val prayer: Prayer, val soundType: AdhanSoundType) : SettingsUiAction
    data class SetVibrate(val prayer: Prayer, val vibrate: Boolean) : SettingsUiAction
    data class SetAdhanVoice(val prayer: Prayer, val voice: AdhanVoice) : SettingsUiAction
    data class SetCustomSound(val prayer: Prayer, val uriString: String?) : SettingsUiAction
    data class ToggleAdhanPreview(val voice: AdhanVoice) : SettingsUiAction
    data class SetPreReminder(val prayer: Prayer, val minutesBefore: Int) : SettingsUiAction
    data class SetDndSilenceMinutes(val minutes: Int) : SettingsUiAction
    data class SetCelestialAlert(val type: CelestialAlertType, val enabled: Boolean) : SettingsUiAction
    data class SetCelestialAlertOffset(val minutesBefore: Int) : SettingsUiAction
    data class SearchLocation(val query: String) : SettingsUiAction
    data class SelectLocation(val location: LocationInfo) : SettingsUiAction
    data class DeleteSavedLocation(val locationId: String) : SettingsUiAction
    data class SaveCustomCoordinates(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val elevation: Double,
        val timeZoneId: String,
    ) : SettingsUiAction
    data object RefreshGpsLocation : SettingsUiAction
    data class SetLocationPickerVisible(val visible: Boolean) : SettingsUiAction
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val locationRepository: LocationRepository,
    private val alarmScheduler: AlarmScheduler,
    private val celestialAlarmScheduler: CelestialAlarmScheduler,
    private val audioGateway: AudioGateway,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isLocationPickerVisible = MutableStateFlow(false)
    private val _allOfflineCities = MutableStateFlow<List<LocationInfo>>(emptyList())
    private val _searchResults = MutableStateFlow<List<LocationInfo>>(emptyList())
    private val _isSearching = MutableStateFlow(false)
    private val _isRefreshingGps = MutableStateFlow(false)
    private val _currentlyPlayingVoice = MutableStateFlow<AdhanVoice?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

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
        combine(
            combine(_isSearching, _isRefreshingGps, networkMonitor.isOnline, _currentlyPlayingVoice) { isSearching, isRefreshing, isOnline, voice ->
                ExtraStateTuple(isSearching, isRefreshing, isOnline, voice)
            },
            _errorMessage,
        ) { extra, errorMessage -> extra.copy(errorMessage = errorMessage) }
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
            isLoading = false,
            dataState = SettingsDataState.READY,
            errorMessage = extraTuple.errorMessage,
        )
    }.catch { error ->
        emit(
            SettingsUiState(
                isLoading = false,
                dataState = SettingsDataState.ERROR,
                errorMessage = error.message,
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(isLoading = true, dataState = SettingsDataState.LOADING)
    )

    init {
        loadOfflineCities()
    }

    private fun loadOfflineCities() {
        viewModelScope.launch {
            runCatching { locationRepository.getAllOfflineCities() }
                .onSuccess { list ->
                    _allOfflineCities.value = list
                    _searchResults.value = list
                }
                .onFailure { error -> _errorMessage.value = error.message }
        }
    }

    fun onAction(action: SettingsUiAction) {
        when (action) {
            is SettingsUiAction.SetCalculationMethod -> {
                launchSettingsAction {
                    settingsRepository.updateUserSettings { it.copy(calculationMethod = action.method) }
                    refreshAlarms()
                    PrayerTimesWidgetReceiver.updateAll(context)
                    AstronomyWidgetUpdater.updateAll(context)
                }
            }
            is SettingsUiAction.SetMadhab -> {
                launchSettingsAction {
                    settingsRepository.updateUserSettings { it.copy(madhab = action.madhab) }
                    refreshAlarms()
                    PrayerTimesWidgetReceiver.updateAll(context)
                    AstronomyWidgetUpdater.updateAll(context)
                }
            }
            is SettingsUiAction.SetHighLatitudeRule -> {
                launchSettingsAction {
                    settingsRepository.updateUserSettings { it.copy(highLatitudeRule = action.rule) }
                    refreshAlarms()
                    PrayerTimesWidgetReceiver.updateAll(context)
                    AstronomyWidgetUpdater.updateAll(context)
                }
            }
            is SettingsUiAction.SetThemeMode -> {
                launchSettingsAction {
                    settingsRepository.updateUserSettings { it.copy(themeMode = action.themeMode) }
                    PrayerTimesWidgetReceiver.updateAll(context)
                    AstronomyWidgetUpdater.updateAll(context)
                }
            }
            is SettingsUiAction.SetPrayerAdjustment -> {
                launchSettingsAction {
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
                    refreshAlarms()
                    PrayerTimesWidgetReceiver.updateAll(context)
                    AstronomyWidgetUpdater.updateAll(context)
                }
            }
            is SettingsUiAction.SetPrayerEnabled -> {
                launchSettingsAction {
                    val currentConfig = uiState.value.alarmSettings.getConfigForPrayer(action.prayer)
                    settingsRepository.updateAlarmSettings {
                        it.updateConfig(currentConfig.copy(isEnabled = action.enabled))
                    }
                    refreshAlarms()
                    PrayerTimesWidgetReceiver.updateAll(context)
                }
            }
            is SettingsUiAction.SetSoundType -> {
                launchSettingsAction {
                    val currentConfig = uiState.value.alarmSettings.getConfigForPrayer(action.prayer)
                    val updated = currentConfig.copy(soundType = action.soundType)
                    settingsRepository.updateAlarmSettings { it.updateConfig(updated) }
                    refreshAlarms()
                }
            }
            is SettingsUiAction.SetVibrate -> {
                launchSettingsAction {
                    val currentConfig = uiState.value.alarmSettings.getConfigForPrayer(action.prayer)
                    val updated = currentConfig.copy(isVibrate = action.vibrate)
                    settingsRepository.updateAlarmSettings { it.updateConfig(updated) }
                }
            }
            is SettingsUiAction.SetAdhanVoice -> {
                launchSettingsAction {
                    val currentConfig = uiState.value.alarmSettings.getConfigForPrayer(action.prayer)
                    val updated = currentConfig.copy(adhanVoice = action.voice)
                    settingsRepository.updateAlarmSettings { it.updateConfig(updated) }
                }
            }
            is SettingsUiAction.SetCustomSound -> {
                launchSettingsAction {
                    val currentConfig = uiState.value.alarmSettings.getConfigForPrayer(action.prayer)
                    settingsRepository.updateAlarmSettings {
                        it.updateConfig(currentConfig.copy(customSoundUri = action.uriString))
                    }
                }
            }
            is SettingsUiAction.ToggleAdhanPreview -> {
                if (_currentlyPlayingVoice.value == action.voice) {
                    audioGateway.stop()
                    _currentlyPlayingVoice.value = null
                } else {
                    _currentlyPlayingVoice.value = action.voice
                    audioGateway.playAdhan(
                        voice = action.voice,
                        onCompletion = {
                            _currentlyPlayingVoice.value = null
                        }
                    )
                }
            }
            is SettingsUiAction.SetPreReminder -> {
                launchSettingsAction {
                    val currentConfig = uiState.value.alarmSettings.getConfigForPrayer(action.prayer)
                    val updated = currentConfig.copy(preReminderMinutes = action.minutesBefore)
                    settingsRepository.updateAlarmSettings { it.updateConfig(updated) }
                    refreshAlarms()
                    PrayerTimesWidgetReceiver.updateAll(context)
                }
            }
            is SettingsUiAction.SetDndSilenceMinutes -> {
                launchSettingsAction {
                    settingsRepository.updateAlarmSettings { it.copy(dndAutoSilenceMinutes = action.minutes) }
                }
            }
            is SettingsUiAction.SetCelestialAlert -> {
                launchSettingsAction {
                    settingsRepository.updateAlarmSettings {
                        it.copy(celestialAlerts = it.celestialAlerts.withEnabled(action.type, action.enabled))
                    }
                    celestialAlarmScheduler.rescheduleAllAlarms()
                    AstronomyWidgetUpdater.updateAll(context)
                }
            }
            is SettingsUiAction.SetCelestialAlertOffset -> {
                launchSettingsAction {
                    settingsRepository.updateAlarmSettings {
                        it.copy(
                            celestialAlerts = it.celestialAlerts.copy(
                                minutesBefore = action.minutesBefore.coerceIn(0, 60),
                            ),
                        )
                    }
                    celestialAlarmScheduler.rescheduleAllAlarms()
                    AstronomyWidgetUpdater.updateAll(context)
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
                launchSettingsAction {
                    locationRepository.saveLocation(action.location)
                    settingsRepository.updateUserSettings {
                        it.copy(selectedLocation = action.location, useAutoLocation = action.location.isAutoDetected)
                    }
                    _isLocationPickerVisible.value = false
                    refreshAlarms()
                    PrayerTimesWidgetReceiver.updateAll(context)
                    AstronomyWidgetUpdater.updateAll(context)
                }
            }
            is SettingsUiAction.DeleteSavedLocation -> {
                launchSettingsAction {
                    locationRepository.deleteLocation(action.locationId)
                }
            }
            is SettingsUiAction.SaveCustomCoordinates -> {
                launchSettingsAction {
                    require(action.name.isNotBlank()) {
                        context.getString(R.string.settings_location_name_required)
                    }
                    val customLoc = LocationInfo(
                        id = "custom_${action.latitude.hashCode()}_${action.longitude.hashCode()}",
                        name = action.name.trim(),
                        country = context.getString(R.string.settings_custom_country),
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
                    refreshAlarms()
                    PrayerTimesWidgetReceiver.updateAll(context)
                    AstronomyWidgetUpdater.updateAll(context)
                }
            }
            is SettingsUiAction.RefreshGpsLocation -> {
                launchSettingsAction {
                    _isRefreshingGps.value = true
                    try {
                        locationRepository.getDeviceLocation().onSuccess { loc ->
                            locationRepository.saveLocation(loc)
                            settingsRepository.updateUserSettings {
                                it.copy(selectedLocation = loc, useAutoLocation = true)
                            }
                            _isLocationPickerVisible.value = false
                            refreshAlarms()
                            PrayerTimesWidgetReceiver.updateAll(context)
                            AstronomyWidgetUpdater.updateAll(context)
                        }.onFailure { error ->
                            _errorMessage.value = error.message
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
        audioGateway.stop()
    }

    private fun refreshAlarms() {
        alarmScheduler.rescheduleAllAlarms()
        celestialAlarmScheduler.rescheduleAllAlarms()
    }

    private fun launchSettingsAction(block: suspend () -> Unit) = viewModelScope.launch {
        _errorMessage.value = null
        runCatching { block() }
            .onFailure { error -> _errorMessage.value = error.message }
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
        val errorMessage: String? = null,
    )
}
