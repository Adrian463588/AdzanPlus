package com.adzannotif.domain.usecase

import com.adzannotif.domain.model.LocationInfo
import com.adzannotif.domain.model.astronomy.StarMapData
import com.adzannotif.domain.repository.AstronomyRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetStarMapUseCase @Inject constructor(
    private val repository: AstronomyRepository
) {
    operator fun invoke(location: LocationInfo, epochMillis: Long = System.currentTimeMillis()): Flow<StarMapData> {
        return repository.getStarMapData(location, epochMillis)
    }
}
