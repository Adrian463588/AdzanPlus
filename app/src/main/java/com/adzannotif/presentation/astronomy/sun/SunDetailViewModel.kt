package com.adzannotif.presentation.astronomy.sun

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adzannotif.domain.model.LocationInfo
import com.adzannotif.domain.model.astronomy.SunInfo
import com.adzannotif.domain.repository.LocationRepository
import com.adzannotif.domain.usecase.GetSunInfoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
        val location: LocationInfo? = null,
        val scrubbedTimeMillis: Long? = null,
        val isLoading: Boolean = true,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        startSunUpdates()
    }

    fun setScrubbedTime(millis: Long?) {
        _uiState.value = _uiState.value.copy(scrubbedTimeMillis = millis)
    }

    private fun startSunUpdates() {
        viewModelScope.launch {
            locationRepository.currentOrSelectedLocation.collectLatest { loc ->
                _uiState.value = _uiState.value.copy(location = loc, isLoading = true)
                while (isActive) {
                    try {
                        val sunInfo = getSunInfoUseCase(loc, System.currentTimeMillis()).firstOrNull()

                        _uiState.value = _uiState.value.copy(
                            sunInfo = sunInfo,
                            location = loc,
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
