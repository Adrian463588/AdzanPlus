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
import kotlin.math.abs
import kotlin.math.roundToInt

data class QiblaUiState(
    val location: LocationInfo = LocationInfo.JAKARTA,
    val qiblaDirection: QiblaDirection? = null,
    val deviceHeading: Float = 0f,
    val accuracy: Int = 3,
    val isSensorAvailable: Boolean = true,
    val isLoading: Boolean = false,
) {
    val qiblaBearing: Double
        get() = qiblaDirection?.directionDegrees ?: 295.0

    val qiblaOffsetFromDevice: Float
        get() = ((qiblaBearing.toFloat() - deviceHeading + 360f) % 360f)

    /**
     * Shortest angle difference between current phone heading and Qibla (-180° to +180°).
     * Negative means need to turn left, positive means need to turn right.
     */
    val shortestOffset: Float
        get() = ((qiblaOffsetFromDevice + 180f) % 360f + 360f) % 360f - 180f

    val isFacingQibla: Boolean
        get() = abs(shortestOffset) <= 2.5f

    val turnInstruction: String
        get() {
            if (!isSensorAvailable) return "Sensor Kompas tidak terdeteksi pada perangkat ini"
            if (isFacingQibla) return "Sempurna! Anda menghadap tepat ke Ka'bah"
            val offsetInt = shortestOffset.roundToInt()
            return if (offsetInt > 0) {
                "Putar $offsetInt° ke Kanan"
            } else {
                "Putar ${abs(offsetInt)}° ke Kiri"
            }
        }
}

@HiltViewModel
class QiblaViewModel @Inject constructor(
    private val getQiblaDirectionUseCase: GetQiblaDirectionUseCase,
    private val locationRepository: LocationRepository,
    private val compassSensorManager: CompassSensorManager,
) : ViewModel() {

    private val _sensorData = MutableStateFlow(CompassSensorData(0f, 3, true))

    val uiState: StateFlow<QiblaUiState> = combine(
        locationRepository.currentOrSelectedLocation,
        getQiblaDirectionUseCase(),
        _sensorData,
    ) { location, qibla, sensor ->
        QiblaUiState(
            location = location,
            qiblaDirection = qibla,
            deviceHeading = sensor.azimuthDegrees,
            accuracy = sensor.accuracy,
            isSensorAvailable = sensor.isSensorAvailable,
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
            compassSensorManager.getHeadingFlow().collect { data ->
                _sensorData.value = data
            }
        }
    }
}
