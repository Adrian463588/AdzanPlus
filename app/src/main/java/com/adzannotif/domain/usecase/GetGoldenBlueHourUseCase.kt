package com.adzannotif.domain.usecase

import com.adzannotif.domain.model.LocationInfo
import com.adzannotif.domain.model.astronomy.SunInfo
import com.adzannotif.domain.repository.AstronomyRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetGoldenBlueHourUseCase @Inject constructor(
    private val repository: AstronomyRepository
) {
    operator fun invoke(location: LocationInfo, epochMillis: Long): Flow<SunInfo> {
        return repository.getSunInfo(location, epochMillis)
    }
}
