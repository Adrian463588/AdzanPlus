package com.adzannotif.presentation.astronomy.starmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adzannotif.domain.model.LocationInfo
import com.adzannotif.domain.model.astronomy.StarMapData
import com.adzannotif.domain.repository.LocationRepository
import com.adzannotif.domain.usecase.GetStarMapUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
        val location: LocationInfo? = null,
        val observedMillis: Long = System.currentTimeMillis(),
        val isLoading: Boolean = true,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        observeLocationAndUpdates()
    }

    fun setObservedTime(millis: Long) {
        _uiState.value = _uiState.value.copy(observedMillis = millis)
        updateStarMap()
    }

    private fun observeLocationAndUpdates() {
        viewModelScope.launch {
            locationRepository.currentOrSelectedLocation.collectLatest { loc ->
                _uiState.value = _uiState.value.copy(location = loc)
                updateStarMap()
            }
        }
    }

    private fun updateStarMap() {
        viewModelScope.launch {
            val loc = _uiState.value.location ?: locationRepository.currentOrSelectedLocation.firstOrNull()
            if (loc != null) {
                try {
                    val data = getStarMapUseCase(loc, _uiState.value.observedMillis).firstOrNull()
                    _uiState.value = _uiState.value.copy(
                        starMapData = data,
                        location = loc,
                        isLoading = false,
                        error = null
                    )
                } catch (_: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Data peta langit belum tersedia. Pastikan katalog bintang aplikasi dapat dibaca."
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Lokasi belum tersedia. Pilih lokasi sebelum membuka peta langit."
                )
            }
        }
    }
}
