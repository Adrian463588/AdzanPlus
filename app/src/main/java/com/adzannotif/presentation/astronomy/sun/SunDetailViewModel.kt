package com.adzannotif.presentation.astronomy.sun

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adzannotif.domain.model.astronomy.SunInfo
import com.adzannotif.domain.repository.LocationRepository
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
class SunDetailViewModel @Inject constructor(
    private val getSunInfoUseCase: GetSunInfoUseCase,
    private val locationRepository: LocationRepository
) : ViewModel() {

    data class UiState(
        val sunInfo: SunInfo? = null,
        val isLoading: Boolean = true,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        startSunUpdates()
    }

    private fun startSunUpdates() {
        viewModelScope.launch {
            while (isActive) {
                try {
                    val loc = locationRepository.currentOrSelectedLocation.firstOrNull()
                    if (loc != null) {
                        val sunInfo = getSunInfoUseCase(loc, System.currentTimeMillis()).firstOrNull()
                        
                        _uiState.value = _uiState.value.copy(
                            sunInfo = sunInfo,
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
