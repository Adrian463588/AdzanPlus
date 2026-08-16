package com.adzannotif.presentation.qibla

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adzannotif.domain.model.LocationInfo
import com.adzannotif.domain.model.QiblaDirection
import com.adzannotif.domain.repository.LocationRepository
import com.adzannotif.domain.usecase.GetQiblaDirectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QiblaUiState(
    val location: LocationInfo = LocationInfo.JAKARTA,
    val qiblaDirection: QiblaDirection? = null,
    val deviceHeading: Float = 0f,
    val isLoading: Boolean = false,
) {
    val qiblaBearing: Double
        get() = qiblaDirection?.directionDegrees ?: 295.0

    val qiblaOffsetFromDevice: Float
        get() = ((qiblaBearing - deviceHeading + 360) % 360).toFloat()

    val isFacingQibla: Boolean
        get() = qiblaOffsetFromDevice in 357f..360f || qiblaOffsetFromDevice in 0f..3f
}

@HiltViewModel
class QiblaViewModel @Inject constructor(
    private val getQiblaDirectionUseCase: GetQiblaDirectionUseCase,
    private val locationRepository: LocationRepository,
    private val compassSensorManager: CompassSensorManager,
) : ViewModel() {

    private val _deviceHeading = MutableStateFlow(0f)

    val uiState: StateFlow<QiblaUiState> = combine(
        locationRepository.currentOrSelectedLocation,
        getQiblaDirectionUseCase(),
        _deviceHeading,
    ) { location, qibla, heading ->
        QiblaUiState(
            location = location,
            qiblaDirection = qibla,
            deviceHeading = heading,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = QiblaUiState(isLoading = true)
    )

    init {
        observeSensor()
    }

    private fun observeSensor() {
        viewModelScope.launch {
            compassSensorManager.getHeadingFlow().collect { heading ->
                _deviceHeading.value = heading
            }
        }
    }
}
