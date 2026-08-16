package com.adzannotif.domain.usecase

import com.adzannotif.core.prayer.Coordinates
import com.adzannotif.core.prayer.Qibla
import com.adzannotif.domain.model.LocationInfo
import com.adzannotif.domain.model.QiblaDirection
import com.adzannotif.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetQiblaDirectionUseCase @Inject constructor(
    private val locationRepository: LocationRepository,
) {
    operator fun invoke(): Flow<QiblaDirection> {
        return locationRepository.currentOrSelectedLocation.map { location ->
            computeQibla(location)
        }
    }

    fun computeQibla(location: LocationInfo): QiblaDirection {
        val coords = location.toCoordinates()
        val qibla = Qibla.fromCoordinates(coords)
        return QiblaDirection(
            observerCoordinates = coords,
            directionDegrees = qibla.direction,
            distanceKm = qibla.distanceKm,
        )
    }
}
