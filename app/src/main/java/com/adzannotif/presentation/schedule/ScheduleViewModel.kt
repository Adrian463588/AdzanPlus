package com.adzannotif.presentation.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adzannotif.domain.model.LocationInfo
import com.adzannotif.domain.model.PrayerTimeRecord
import com.adzannotif.domain.repository.LocationRepository
import com.adzannotif.domain.repository.SettingsRepository
import com.adzannotif.domain.usecase.GetMonthlyPrayerTimesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

data class ScheduleUiState(
    val currentMonth: Int = 8,
    val currentYear: Int = 2026,
    val location: LocationInfo = LocationInfo.JAKARTA,
    val monthlyRecords: List<PrayerTimeRecord> = emptyList(),
    val todayDate: LocalDate = LocalDate(2026, 8, 16),
    val isLoading: Boolean = false,
)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val getMonthlyPrayerTimesUseCase: GetMonthlyPrayerTimesUseCase,
    private val locationRepository: LocationRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val nowLocal = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    private val _selectedYear = MutableStateFlow(nowLocal.year)
    private val _selectedMonth = MutableStateFlow(nowLocal.monthNumber)

    val uiState: StateFlow<ScheduleUiState> = combine(
        _selectedYear,
        _selectedMonth,
        locationRepository.currentOrSelectedLocation,
    ) { year, month, location ->
        Triple(year, month, location)
    }.flatMapLatest { (year, month, location) ->
        getMonthlyPrayerTimesUseCase(year, month).combine(settingsRepository.userSettings) { records, _ ->
            ScheduleUiState(
                currentMonth = month,
                currentYear = year,
                location = location,
                monthlyRecords = records,
                todayDate = nowLocal.date,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ScheduleUiState(isLoading = true)
    )

    fun onPreviousMonth() {
        val currentMonth = _selectedMonth.value
        val currentYear = _selectedYear.value
        if (currentMonth == 1) {
            _selectedMonth.value = 12
            _selectedYear.value = currentYear - 1
        } else {
            _selectedMonth.value = currentMonth - 1
        }
    }

    fun onNextMonth() {
        val currentMonth = _selectedMonth.value
        val currentYear = _selectedYear.value
        if (currentMonth == 12) {
            _selectedMonth.value = 1
            _selectedYear.value = currentYear + 1
        } else {
            _selectedMonth.value = currentMonth + 1
        }
    }
}
