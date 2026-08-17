package com.adzannotif.domain.usecase

import com.adzannotif.domain.model.LocationInfo
import com.adzannotif.domain.model.astronomy.MoonInfo
import com.adzannotif.domain.repository.AstronomyRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMoonInfoUseCase @Inject constructor(
    private val repository: AstronomyRepository
) {
    operator fun invoke(location: LocationInfo, epochMillis: Long): Flow<MoonInfo> {
        return repository.getMoonInfo(location, epochMillis)
    }
}
