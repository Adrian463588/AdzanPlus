package com.adzannotif.presentation.astronomy.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adzannotif.domain.model.astronomy.CalendarDay
import com.adzannotif.domain.repository.LocationRepository
import com.adzannotif.domain.usecase.GetHijriCalendarUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class HijriCalendarViewModel @Inject constructor(
    private val getHijriCalendarUseCase: GetHijriCalendarUseCase,
    private val locationRepository: LocationRepository
) : ViewModel() {

    data class UiState(
        val days: List<CalendarDay> = emptyList(),
        val year: Int = 0,
        val month: Int = 0, // 0-based
        val selectedDay: CalendarDay? = null,
        val isLoading: Boolean = true,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        val cal = Calendar.getInstance()
        _uiState.value = _uiState.value.copy(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH)
        )
        loadCalendar()
    }

    fun previousMonth() {
        val st = _uiState.value
        var y = st.year
        var m = st.month - 1
        if (m < 0) {
            m = 11
            y--
        }
        _uiState.value = st.copy(year = y, month = m)
        loadCalendar()
    }

    fun nextMonth() {
        val st = _uiState.value
        var y = st.year
        var m = st.month + 1
        if (m > 11) {
            m = 0
            y++
        }
        _uiState.value = st.copy(year = y, month = m)
        loadCalendar()
    }

    fun selectDay(day: CalendarDay) {
        _uiState.value = _uiState.value.copy(selectedDay = day)
    }

    fun dismissDay() {
        _uiState.value = _uiState.value.copy(selectedDay = null)
    }

    private fun loadCalendar() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val loc = locationRepository.currentOrSelectedLocation.firstOrNull()
                if (loc != null) {
                    val days = getHijriCalendarUseCase(
                        location = loc,
                        year = _uiState.value.year,
                        month = _uiState.value.month + 1
                    )
                    _uiState.value = _uiState.value.copy(
                        days = days,
                        isLoading = false,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Location not available")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}
