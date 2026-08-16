package com.adzannotif.presentation.astronomy.starmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adzannotif.domain.model.astronomy.StarMapData
import com.adzannotif.domain.repository.LocationRepository
import com.adzannotif.domain.usecase.GetStarMapUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StarMapViewModel @Inject constructor(
    private val getStarMapUseCase: GetStarMapUseCase,
    private val locationRepository: LocationRepository
) : ViewModel() {

    data class UiState(
        val starMapData: StarMapData? = null,
        val observedMillis: Long = System.currentTimeMillis(),
        val isLoading: Boolean = true,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        updateStarMap()
    }

    fun setObservedTime(millis: Long) {
        _uiState.value = _uiState.value.copy(observedMillis = millis)
        updateStarMap()
    }

    private fun updateStarMap() {
        viewModelScope.launch {
            try {
                val loc = locationRepository.currentOrSelectedLocation.firstOrNull()
                if (loc != null) {
                    val data = getStarMapUseCase(loc, _uiState.value.observedMillis).firstOrNull()
                    _uiState.value = _uiState.value.copy(
                        starMapData = data,
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
        }
    }
}
