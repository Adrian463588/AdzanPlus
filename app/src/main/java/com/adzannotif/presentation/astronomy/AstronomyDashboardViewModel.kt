package com.adzannotif.presentation.astronomy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adzannotif.domain.model.astronomy.MoonInfo
import com.adzannotif.domain.model.astronomy.SunInfo
import com.adzannotif.domain.repository.LocationRepository
import com.adzannotif.domain.usecase.GetMoonInfoUseCase
import com.adzannotif.domain.usecase.GetSunInfoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AstronomyDashboardViewModel @Inject constructor(
    private val getSunInfoUseCase: GetSunInfoUseCase,
    private val getMoonInfoUseCase: GetMoonInfoUseCase,
    private val locationRepository: LocationRepository
) : ViewModel() {

    data class UiState(
        val sunInfo: SunInfo? = null,
        val moonInfo: MoonInfo? = null,
        val isLoading: Boolean = true,
        val error: String? = null
    )

    sealed class UiAction {
        data object NavigateToMoonDetail : UiAction()
        data object NavigateToSunDetail : UiAction()
        data object NavigateToStarMap : UiAction()
        data object NavigateToCalendar : UiAction()
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        startAstronomyUpdates()
    }

    private fun startAstronomyUpdates() {
        viewModelScope.launch {
            while (isActive) {
                try {
                    val loc = locationRepository.currentOrSelectedLocation.firstOrNull()
                    if (loc != null) {
                        val sunInfo = getSunInfoUseCase(loc, System.currentTimeMillis()).firstOrNull()
                        val moonInfo = getMoonInfoUseCase(loc, System.currentTimeMillis()).firstOrNull()
                        
                        _uiState.value = _uiState.value.copy(
                            sunInfo = sunInfo,
                            moonInfo = moonInfo,
                            isLoading = false,
                            error = null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Location not available"
                        )
                    }
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
