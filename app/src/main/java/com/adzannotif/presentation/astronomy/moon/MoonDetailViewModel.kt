package com.adzannotif.presentation.astronomy.moon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adzannotif.domain.model.LocationInfo
import com.adzannotif.domain.model.astronomy.CalendarDay
import com.adzannotif.domain.model.astronomy.MoonInfo
import com.adzannotif.domain.repository.LocationRepository
import com.adzannotif.domain.usecase.GetHijriCalendarUseCase
import com.adzannotif.domain.usecase.GetMoonInfoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class MoonDetailViewModel @Inject constructor(
    private val getMoonInfoUseCase: GetMoonInfoUseCase,
    private val getHijriCalendarUseCase: GetHijriCalendarUseCase,
    private val locationRepository: LocationRepository
) : ViewModel() {

    data class UiState(
        val moonInfo: MoonInfo? = null,
        val location: LocationInfo? = null,
        val calendarDays: List<CalendarDay> = emptyList(),
        val isLoading: Boolean = true,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        startMoonUpdates()
    }

    private fun startMoonUpdates() {
        viewModelScope.launch {
            locationRepository.currentOrSelectedLocation.collectLatest { loc ->
                _uiState.value = _uiState.value.copy(
                    location = loc,
                    moonInfo = null,
                    calendarDays = emptyList(),
                    isLoading = loc != null,
                    error = null,
                )
                if (loc == null) {
                    return@collectLatest
                }
                while (isActive) {
                    try {
                        val moonInfo = getMoonInfoUseCase(loc, System.currentTimeMillis()).firstOrNull()
                        val cal = Calendar.getInstance(java.util.TimeZone.getTimeZone(loc.timeZoneId))
                        val calendarDays = getHijriCalendarUseCase(
                            loc,
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH) + 1
                        )

                        _uiState.value = _uiState.value.copy(
                            moonInfo = moonInfo,
                            location = loc,
                            calendarDays = calendarDays,
                            isLoading = false,
                            error = null
                        )
                    } catch (e: Exception) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = e.message
                        )
                    }
                    delay(60_000)
                }
            }
        }
    }
}
